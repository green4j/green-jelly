/**
 * MIT License
 *
 * Copyright (c) 2018-2026 Anatoly Gudkov and others.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.green4j.jelly;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Utf8JsonParserTest {

    private static final String[] VALID = {
        "{\"name\":\"va\\\"l\\u0041ue\", \"t\":true,\"f\":false,\"n\":null,\n"
                + "\"numbers\":[0,-12,+3.25,4e2,5.5E-3,-0.0,99999999999999999999.5e1],"
                + "\"nested\":[{\"a\":[]},{}],\"s\":\"\"}\t",
        "[\"желе\",\"数据\",\"\ud83d\ude00 é \\u0436\\ud83d\\ude00\\n\",{\"ключ\":\"значение\"},\"ü\"]",
        "\"just a string\"",
        "\"é\"",
        "12.5",
        "-0",
        "true",
        "false",
        "null",
        "[]",
        "{}",
        "  [1, 2 , 3 ]  ",
        "[922337203685477580.5,0.00000000000000000000000123456789012345678901234,-1e-7]"
    };

    private static final String[] INVALID = {
        "[1,]",
        "{\"a\" 1}",
        "[tru]",
        "[nul",
        "[fals]",
        "[\"\\x\"]",
        "[\"\\u12G4\"]",
        "[-]",
        "[1e]",
        "{]",
        "[1 2]",
        "x",
        "[\"a\":1]",
        "{,}",
        "[\"abc",
        "[\"ж\" x]",
        "{\"ключ\":}",
        "[1]]",
        "{\"a\":1,}",
        "{\"ключ\":[1,]}",
        "1 2",
        "[1] 2",
        "{} 1",
        "\"ж\" 1",
        "[1] -",
        "[1] tr",
        "[1] \"ab"
    };

    private static final byte[][] VALID_CHARS = {
        utf8("é"), utf8("ж"), utf8("\u07ff"), utf8("\u0800"), utf8("数"), utf8("\ud7ff"), utf8("\ue000"),
        utf8("\uffff"), utf8("\ud83d\ude00"), utf8("\ud800\udc00"), utf8("\udbff\udfff")
    };

    private enum Input {
        ARRAY, HEAP_BUFFER, DIRECT_BUFFER, READ_ONLY_BUFFER, SEQUENCE
    }

    // the bytes are put into the storage after a prefix, so the parsing at an index other than 0 is checked too
    private static final int PREFIX = 2;

    private static final class Storage {
        private final Input input;
        private final Object data;

        Storage(final Input input, final byte[] bytes) {
            this.input = input;
            final byte[] padded = new byte[PREFIX + bytes.length + 1];
            Arrays.fill(padded, (byte) '*');
            System.arraycopy(bytes, 0, padded, PREFIX, bytes.length);
            switch (input) {
                case ARRAY:
                    data = padded;
                    break;
                case HEAP_BUFFER: { // with an offset of the array
                    final byte[] array = new byte[padded.length + 1];
                    System.arraycopy(padded, 0, array, 1, padded.length);
                    data = ByteBuffer.wrap(array, 1, padded.length).slice();
                    break;
                }
                case DIRECT_BUFFER: {
                    final ByteBuffer buffer = ByteBuffer.allocateDirect(padded.length);
                    buffer.put(padded).flip();
                    data = buffer;
                    break;
                }
                case READ_ONLY_BUFFER:
                    data = ByteBuffer.wrap(padded).asReadOnlyBuffer();
                    break;
                default:
                    data = new ByteSequence() {
                        @Override
                        public int length() {
                            return padded.length;
                        }

                        @Override
                        public byte byteAt(final int index) {
                            return padded[index];
                        }
                    };
                    break;
            }
        }

        void parse(final Utf8JsonParser parser, final int start, final int len) {
            switch (input) {
                case ARRAY:
                    parser.parse((byte[]) data, PREFIX + start, len);
                    break;
                case HEAP_BUFFER:
                case DIRECT_BUFFER:
                case READ_ONLY_BUFFER:
                    parser.parse((ByteBuffer) data, PREFIX + start, len);
                    break;
                default:
                    parser.parse((ByteSequence) data, PREFIX + start, len);
                    break;
            }
        }
    }

    private static JsonEvents expectedEvents(final String json) {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);
        parser.parse(json);
        parser.eoj();
        return events;
    }

    private static byte[] utf8(final String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    public void testSameEventsAsJsonParser() {
        final JsonEvents events = new JsonEvents();
        for (final boolean strict : new boolean[] {false, true}) {
            final Utf8JsonParser parser = new Utf8JsonParser(new CopyingStringBuilder(false, strict))
                    .setListener(events);
            for (final String[] jsons : new String[][] {VALID, INVALID}) {
                for (final String json : jsons) {
                    final JsonEvents expected = expectedEvents(json);
                    final byte[] bytes = utf8(json);
                    for (final Input input : Input.values()) {
                        final Storage storage = new Storage(input, bytes);
                        events.clear();
                        storage.parse(parser, 0, bytes.length);
                        parser.eoj();
                        assertEquals(expected, events, input + ": " + json);
                    }
                }
            }
        }
    }

    @Test
    public void testSameErrorPositionsAsJsonParser() {
        final Utf8JsonParser parser = new Utf8JsonParser().setListener(new JsonEvents());
        final JsonParser expectedParser = new JsonParser().setListener(new JsonEvents());
        for (final String json : INVALID) {
            expectedParser.parse(json);
            expectedParser.eoj();
            assertTrue(expectedParser.hasError(), json);
            // the positions are of bytes, so only for the chars encoded with one byte they are the same
            final int asciiPrefix = asciiPrefix(json);
            if (expectedParser.getErrorPosition() > asciiPrefix) {
                continue;
            }
            for (final Input input : Input.values()) {
                final byte[] bytes = utf8(json);
                new Storage(input, bytes).parse(parser, 0, bytes.length);
                parser.eoj();
                assertTrue(parser.hasError(), json);
                assertEquals(expectedParser.getErrorPosition(), parser.getErrorPosition(), input + ": " + json);
            }
        }
    }

    private static int asciiPrefix(final String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) < 0x80) {
            i++;
        }
        return i;
    }

    @Test
    public void testAnyCutOfDataGivesTheSameEvents() {
        final JsonEvents events = new JsonEvents();
        for (final boolean strict : new boolean[] {false, true}) {
            final Utf8JsonParser parser = new Utf8JsonParser(new CopyingStringBuilder(false, strict))
                    .setListener(events);
            for (final String json : VALID) {
                final JsonEvents expected = expectedEvents(json);
                final byte[] bytes = utf8(json);
                for (final Input input : Input.values()) {
                    final Storage storage = new Storage(input, bytes);
                    for (int cut = 0; cut <= bytes.length; cut++) {
                        events.clear();
                        storage.parse(parser, 0, cut);
                        storage.parse(parser, cut, bytes.length - cut);
                        parser.eoj();
                        assertEquals(expected, events, input + ", cut at " + cut + ": " + json);
                    }
                }
            }
        }
    }

    @Test
    public void testEscapesAndCharsAtAnyPositionOfLongPlainRuns() {
        // the plain bytes are read eight at once, so a quote, an escape or a char, which isn't ASCII,
        // is put at any position of the eight bytes with any number of the words before it
        final String[] inserts = {"", "\\n", "\\\"", "\\u0041", "é", "ж", "数", "\ud83d\ude00"};
        final JsonEvents events = new JsonEvents();
        final Utf8JsonParser parser = new Utf8JsonParser().setListener(events);
        for (int length = 0; length <= 24; length++) {
            final StringBuilder plain = new StringBuilder();
            for (int i = 0; i < length; i++) {
                plain.append((char) ('a' + i % 26));
            }
            for (int at = 0; at <= length; at++) {
                for (final String insert : inserts) {
                    final String value = plain.substring(0, at) + insert + plain.substring(at);
                    final String json = "[\"" + value + "\",       \"" + value + "\"]";
                    final JsonEvents expected = expectedEvents(json);
                    final byte[] bytes = utf8(json);
                    for (final Input input : Input.values()) {
                        final Storage storage = new Storage(input, bytes);
                        for (int cut = 0; cut <= bytes.length; cut += 3) {
                            events.clear();
                            storage.parse(parser, 0, cut);
                            storage.parse(parser, cut, bytes.length - cut);
                            parser.eoj();
                            assertEquals(expected, events, input + ", cut at " + cut + ": " + json);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testAnyTwoCutsOfDataGiveTheSameEvents() {
        final String json = VALID[1];
        final JsonEvents expected = expectedEvents(json);
        final byte[] bytes = utf8(json);

        final JsonEvents events = new JsonEvents();
        final Utf8JsonParser parser = new Utf8JsonParser().setListener(events);
        final Storage storage = new Storage(Input.ARRAY, bytes);
        for (int cut1 = 0; cut1 <= bytes.length; cut1++) {
            for (int cut2 = cut1; cut2 <= bytes.length; cut2++) {
                events.clear();
                storage.parse(parser, 0, cut1);
                storage.parse(parser, cut1, cut2 - cut1);
                storage.parse(parser, cut2, bytes.length - cut2);
                parser.eoj();
                assertEquals(expected, events, "Cuts at " + cut1 + ", " + cut2);
            }
        }
    }

    @Test
    public void testMalformedBytesAreReplacedTheSameWayAsByString() {
        final int[] specialBytes = {
            0x80, 0x8f, 0x90, 0x9f, 0xa0, 0xbf, 0xc0, 0xc1, 0xc2, 0xdf,
            0xe0, 0xe1, 0xed, 0xee, 0xef, 0xf0, 0xf1, 0xf4, 0xf5, 0xff, 'a', ' '
        };
        final Random random = new Random(42);
        final JsonEvents events = new JsonEvents();
        final Utf8JsonParser parser = new Utf8JsonParser().setListener(events);

        for (int i = 0; i < 5_000; i++) {
            final byte[] value = new byte[random.nextInt(13)];
            for (int j = 0; j < value.length; j++) {
                switch (random.nextInt(3)) {
                    case 0:
                        value[j] = (byte) specialBytes[random.nextInt(specialBytes.length)];
                        break;
                    case 1:
                        value[j] = (byte) (0x80 + random.nextInt(0x80));
                        break;
                    default: { // a valid char, which may be cut by the end of the value
                        final byte[] c = VALID_CHARS[random.nextInt(VALID_CHARS.length)];
                        for (int k = 0; k < c.length && j < value.length; k++) {
                            value[j++] = c[k];
                        }
                        j--;
                        break;
                    }
                }
            }
            final byte[] json = new byte[value.length + 4];
            json[0] = '[';
            json[1] = '"';
            System.arraycopy(value, 0, json, 2, value.length);
            json[json.length - 2] = '"';
            json[json.length - 1] = ']';

            final JsonEvents expected = new JsonEvents();
            expected.onJsonStarted();
            expected.onArrayStarted();
            expected.onStringValue(new String(value, StandardCharsets.UTF_8));
            expected.onArrayEnded();
            expected.onJsonEnded();

            for (final Input input : Input.values()) {
                final Storage storage = new Storage(input, json);
                for (int cut = 0; cut <= json.length; cut++) {
                    events.clear();
                    storage.parse(parser, 0, cut);
                    storage.parse(parser, cut, json.length - cut);
                    parser.eoj();
                    assertEquals(expected, events, input + ", cut at " + cut + ": " + Arrays.toString(value));
                }
            }
        }
    }

    @Test
    public void testMalformedBytesBeforeEscape() {
        // a char cut by an escape
        final byte[] json = {'[', '"', (byte) 0xd0, '\\', 'n', (byte) 0xd0, (byte) 0xb6, '"', ']'};

        final JsonEvents events = new JsonEvents();
        new Utf8JsonParser().setListener(events).parseAndEoj(json);

        final JsonEvents expected = new JsonEvents();
        expected.onJsonStarted();
        expected.onArrayStarted();
        expected.onStringValue("\ufffd\nж");
        expected.onArrayEnded();
        expected.onJsonEnded();
        assertEquals(expected, events);
    }

    @Test
    public void testStrictUtf8() {
        assertStrictError(new byte[] {'[', '"', 'a', (byte) 0xff, 'b', '"', ']'}, 3);
        // a cut char followed by a quote, an escape or another char
        assertStrictError(new byte[] {'[', '"', (byte) 0xc3, '"', ']'}, 3);
        assertStrictError(new byte[] {'[', '"', 'a', (byte) 0xc3, '\\', 'n', '"', ']'}, 4);
        assertStrictError(new byte[] {'[', '"', (byte) 0xe2, (byte) 0x82, 'a', '"', ']'}, 4);
        // overlong encoding
        assertStrictError(new byte[] {'[', '"', (byte) 0xe0, (byte) 0x80, (byte) 0x80, '"', ']'}, 3);
        assertStrictError(new byte[] {'[', '"', (byte) 0xc0, (byte) 0x80, '"', ']'}, 2);
        // a surrogate encoded on its own
        assertStrictError(new byte[] {'[', '"', (byte) 0xed, (byte) 0xa0, (byte) 0x80, '"', ']'}, 4);
        // above U+10FFFF
        assertStrictError(new byte[] {'[', '"', (byte) 0xf4, (byte) 0x90, (byte) 0x80, (byte) 0x80, '"', ']'}, 3);
        // the error is in the second part of the data
        assertStrictError(new byte[] {'[', '"', 'a', 'b', 'c', (byte) 0xc3, 'd', '"', ']'}, 6);
    }

    private static void assertStrictError(final byte[] json, final int position) {
        final Utf8JsonParser parser = new Utf8JsonParser(new CopyingStringBuilder(false, true))
                .setListener(new JsonEvents());
        for (int cut = 0; cut <= json.length; cut++) {
            parser.parse(json, 0, cut);
            if (!parser.hasError()) {
                parser.parse(json, cut, json.length - cut);
            }
            parser.eoj();
            assertTrue(parser.hasError(), "Cut at " + cut + ": " + Arrays.toString(json));
            assertEquals("Incorrect UTF-8 encoding", parser.getError());
            // the error position is relative to the start of the data given
            final int expectedPosition = position < cut ? position : position - cut;
            assertEquals(expectedPosition, parser.getErrorPosition(), "Cut at " + cut + ": " + Arrays.toString(json));
        }

        // the lenient builder gives the same as String
        final JsonEvents events = new JsonEvents();
        new Utf8JsonParser().setListener(events).parseAndEoj(json);
        final JsonEvents expected = new JsonEvents();
        expected.onJsonStarted();
        expected.onArrayStarted();
        expected.onStringValue(new String(json, 2, json.length - 4, StandardCharsets.UTF_8).replace("\\n", "\n"));
        expected.onArrayEnded();
        expected.onJsonEnded();
        assertEquals(expected, events);
    }

    @Test
    public void testByteBufferPositionAndLimit() {
        final byte[] bytes = utf8("**[\"ж\",1]**");
        final ByteBuffer[] buffers = {
            ByteBuffer.wrap(bytes),
            ByteBuffer.allocateDirect(bytes.length).put(bytes).clear()
        };
        for (final ByteBuffer buffer : buffers) {
            buffer.position(2).limit(bytes.length - 2);

            final JsonEvents events = new JsonEvents();
            new Utf8JsonParser().setListener(events).parseAndEoj(buffer);

            assertEquals(expectedEvents("[\"ж\",1]"), events, buffer.toString());
            assertEquals(2, buffer.position());
            assertEquals(bytes.length - 2, buffer.limit());
        }
    }

    @Test
    public void testStoppedByListener() {
        final byte[] json = utf8("[\"ж\",1,\"数据\"]");
        final JsonEvents expected = expectedEvents("[\"ж\",1,\"数据\"]");

        final JsonEvents events = new JsonEvents() {
            @Override
            public boolean onStringValue(final CharSequence data) {
                super.onStringValue(data);
                return false;
            }
        };
        final Utf8JsonParser parser = new Utf8JsonParser().setListener(events);
        JsonParser.Next next = parser.parse(json);
        int stops = 0;
        while (next != null) {
            stops++;
            next = next.next();
        }
        parser.eoj();
        assertEquals(2, stops);
        assertEquals(expected.toString(), events.toString());
        assertFalse(parser.hasError());
    }
}
