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

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpillTest {
    private static final int SMALL = 64;

    @Test
    void aSpillingUtf8WriterWritesTheSameBytesAndNeverGrows() {
        final Random random = new Random(11);
        for (int round = 0; round < 300; round++) {
            final String text = text(random, random.nextInt(300));
            final long number = random.nextLong();
            final ByteArrayOutputStream spilled = new ByteArrayOutputStream();
            final Utf8ByteArrayWriter spilling = spillingUtf8(new byte[SMALL], 0, spilled);
            final Utf8ByteArrayWriter growing = new Utf8ByteArrayWriter(16);
            write(spilling, text, number, 50);
            write(growing, text, number, 50);
            spilled.write(spilling.array(), spilling.start(), spilling.length());

            assertArrayEquals(Arrays.copyOfRange(growing.array(), 0, growing.length()), spilled.toByteArray(),
                    "round " + round);
            assertEquals(SMALL, spilling.array().length, "round " + round);
        }
    }

    @Test
    void aSpillingAsciiWriterWritesTheSameBytesAndNeverGrows() {
        final Random random = new Random(12);
        for (int round = 0; round < 300; round++) {
            final String text = text(random, random.nextInt(300));
            final long number = random.nextLong();
            final ByteArrayOutputStream spilled = new ByteArrayOutputStream();
            final AsciiByteArrayWriter spilling = new AsciiByteArrayWriter(SMALL) {
                @Override
                protected boolean spill(final int roomSize) {
                    spilled.write(array(), start(), length());
                    clear();
                    return true;
                }
            };
            final AsciiByteArrayWriter growing = new AsciiByteArrayWriter(16);
            write(spilling, text, number, 50);
            write(growing, text, number, 50);
            spilled.write(spilling.array(), spilling.start(), spilling.length());

            assertArrayEquals(Arrays.copyOfRange(growing.array(), 0, growing.length()), spilled.toByteArray(),
                    "round " + round);
            assertEquals(SMALL, spilling.array().length, "round " + round);
        }
    }

    @Test
    void aSpillingCharWriterWritesTheSameCharsAndNeverGrows() {
        final Random random = new Random(13);
        for (int round = 0; round < 300; round++) {
            final String text = text(random, random.nextInt(300));
            final long number = random.nextLong();
            final StringBuilder spilled = new StringBuilder();
            final CharArrayWriter spilling = new CharArrayWriter(SMALL) {
                @Override
                protected boolean spill(final int roomSize) {
                    spilled.append(array(), start(), length());
                    clear();
                    return true;
                }
            };
            final CharArrayWriter growing = new CharArrayWriter(16);
            write(spilling, text, number, 50);
            write(growing, text, number, 50);
            spilled.append(spilling.array(), spilling.start(), spilling.length());

            assertEquals(growing.toString(), spilled.toString(), "round " + round);
            assertEquals(SMALL, spilling.array().length, "round " + round);
        }
    }

    @Test
    void aSpillingWriterStartingPastZeroWritesTheSameBytesAndNeverGrows() {
        final byte[] array = new byte[SMALL + 32];
        final ByteArrayOutputStream spilled = new ByteArrayOutputStream();
        final Utf8ByteArrayWriter spilling = spillingUtf8(array, 32, spilled);
        final Utf8ByteArrayWriter growing = new Utf8ByteArrayWriter(16);
        final String text = text(new Random(14), 1000);
        write(spilling, text, 42, 20);
        write(growing, text, 42, 20);
        spilled.write(spilling.array(), spilling.start(), spilling.length());

        assertArrayEquals(Arrays.copyOfRange(growing.array(), 0, growing.length()), spilled.toByteArray());
        assertSame(array, spilling.array());
        assertEquals(32, spilling.start());
    }

    @Test
    void aFrameLargerThanTheArrayGrowsASpillingWriter() {
        final ByteArrayOutputStream spilled = new ByteArrayOutputStream();
        final Utf8ByteArrayWriter spilling = spillingUtf8(new byte[SMALL], 0, spilled);
        spilling.append("abc");
        final BufferingWriter.Frame frame = spilling.append(SMALL * 2);
        for (int i = 0; i < frame.length(); i++) {
            frame.setCharAt(i, (char) ('0' + i % 10));
        }
        spilled.write(spilling.array(), spilling.start(), spilling.length());

        final StringBuilder expected = new StringBuilder("abc");
        for (int i = 0; i < SMALL * 2; i++) {
            expected.append((char) ('0' + i % 10));
        }
        assertEquals(expected.toString(), spilled.toString());
        assertTrue(spilling.array().length >= SMALL * 2);
    }

    @Test
    void aWriterWhichSpillsButDoesNotClearGrows() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(SMALL) {
            @Override
            protected boolean spill(final int roomSize) {
                return true;
            }
        };
        final Utf8ByteArrayWriter growing = new Utf8ByteArrayWriter(16);
        final String text = text(new Random(15), 200);
        write(writer, text, 1, 5);
        write(growing, text, 1, 5);

        assertEquals(growing.toString(), writer.toString());
        assertTrue(writer.array().length > SMALL);
    }

    @Test
    void aWriterWhichDoesNotSpillGrows() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(SMALL);
        write(writer, "abc", 1, 50);
        assertTrue(writer.array().length > SMALL);
    }

    private static Utf8ByteArrayWriter spillingUtf8(final byte[] array, final int start,
                                                    final ByteArrayOutputStream spilled) {
        return new Utf8ByteArrayWriter(array, start) {
            @Override
            protected boolean spill(final int roomSize) {
                spilled.write(array(), start(), length());
                clear();
                return true;
            }
        };
    }

    private static void write(final BufferingWriter writer, final String text, final long number, final int items) {
        final JsonGenerator out = new JsonGenerator(writer, false);
        out.startArray();
        for (int i = 0; i < items; i++) {
            out.startObject();
            out.objectMember("text");
            out.stringValue(text, true);
            out.objectMember("number");
            out.numberValue(number + i);
            out.objectMember("price");
            out.numberValueAsString(number % 100_000, -2);
            out.endObject();
        }
        out.endArray();
        out.eoj();
    }

    // Runs of Latin or Cyrillic letters, escapes, surrogate pairs or spaces, up to twice as long as the array
    private static String text(final Random random, final int length) {
        final StringBuilder text = new StringBuilder();
        while (text.length() < length) {
            final int kind = random.nextInt(5);
            final int run = 1 + random.nextInt(2 * SMALL);
            for (int i = 0; i < run; i++) {
                switch (kind) {
                    case 0:
                        text.append((char) ('a' + random.nextInt(26)));
                        break;
                    case 1:
                        text.append((char) ('а' + random.nextInt(32)));
                        break;
                    case 2:
                        text.append("€\"\\\n");
                        break;
                    case 3:
                        text.appendCodePoint(0x1F600 + random.nextInt(50));
                        break;
                    default:
                        text.append(' ');
                        break;
                }
            }
        }
        return text.toString();
    }
}
