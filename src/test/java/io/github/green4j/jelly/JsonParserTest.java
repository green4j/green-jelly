/**
 * MIT License
 * <p>
 * Copyright (c) 2018-2024 Anatoly Gudkov and others.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.green4j.jelly;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonParserTest {

    private static final String[] OVERFLOWN_NUMBERS = new String[] {
            (JsonParser.MAX_MANTISSA_VALUE + 1) + "",
            "9999999999999999999999",
            "9999999999999999999999E10",
            "9999999999999999999999E-10",
            "0." + (JsonParser.MAX_MANTISSA_VALUE + 1),
            "0.9999999999999999999999",
            "9.9999999999999999999999",
            "9.9999999999999999999999E10",
            "9.9999999999999999999999E-10",
            "922337203685477580.5",
            "0.00000000000000000000000123456789012345678901234",
            "-99999999999999999999999.55e-7",
            "+99999999999999999999999E10",
            "-0000009999999999999999999999"
    };

    @Test
    public void parseNumberTest() {
        final String[] numbers = new String[]{
                Long.toString(-JsonParser.MAX_MANTISSA_VALUE),
                Double.toString(Double.MIN_VALUE),
                Float.toString(Float.MIN_VALUE),
                "-1034567770766.0001",
                "-1034567770766",
                "-00234.6783456789",
                "-0023400",
                "-4.000000123e4",
                "-0.123e-15",
                "-0.123e+10",
                "-0.123e-10",
                "-0.005000000000000",
                "0e0",
                "0",
                "-0.0",
                "0.0",
                "0.005000000000000",
                "0.0000000000000001",
                "0.123E-10",
                "+0.123E+10",
                "0.123E-15",
                "4.000000123E+4",
                "14.000000123",
                "+0023400",
                "00234.6783456789",
                "1034567770766",
                "+1034567770766.0001",
                Float.toString(Float.MAX_VALUE),
                Double.toString(Double.MAX_VALUE),
                Long.toString(JsonParser.MAX_MANTISSA_VALUE)
        };

        final MutableJsonNumber result = new MutableJsonNumber();

        for (final String number : numbers) {
            JsonParser.parseNumber(number, result);

            final BigDecimal parsed = BigDecimal.valueOf(result.mantissa(), -result.exp());
            final BigDecimal expected = new BigDecimal(number);
            assertEquals(expected, parsed);
        }
    }

    @Test
    public void parseNumberOverflowTest() {
        final String[] numbers = new String[] {
                JsonParser.MAX_MANTISSA_VALUE + "",
                (JsonParser.MAX_MANTISSA_VALUE + 1) + "",
                "9999999999999999999999",
                "9999999999999999999999E10",
                "9999999999999999999999E-10",
                "0." + JsonParser.MAX_MANTISSA_VALUE,
                "0." + (JsonParser.MAX_MANTISSA_VALUE + 1),
                "0.9999999999999999999999",
                "9.9999999999999999999999",
                "9.9999999999999999999999E10",
                "9.9999999999999999999999E-10"
        };
        final boolean[] overflow = new boolean[] {
                false,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                true
        };

        final long[] mantissas = new long[] {
                9223372036854775799L,
                922337203685477580L,
                999999999999999999L,
                999999999999999999L,
                999999999999999999L,
                9223372036854775799L,
                922337203685477580L,
                999999999999999999L,
                999999999999999999L,
                999999999999999999L,
                999999999999999999L
        };
        final int[] exponents = new int[] {
                0,
                1,
                4,
                14,
                -6,
                -19,
                -18,
                -18,
                -17,
                -7,
                -27
        };

        final MutableJsonNumber number = new MutableJsonNumber();
        for (int i = 0; i < numbers.length; i++) {
            assertEquals(overflow[i], JsonParser.parseNumber(numbers[i], number));
            assertEquals(mantissas[i], number.mantissa());
            assertEquals(exponents[i], number.exp());
        }
    }

    @Test
    public void parseNumberOverflownTextTest() {
        final MutableJsonNumber number = new MutableJsonNumber();
        final StringBuilder overflown = new StringBuilder();

        for (final String overflownNumber : OVERFLOWN_NUMBERS) {
            assertTrue(JsonParser.parseNumber(overflownNumber, number, overflown));

            final BigDecimal expected = new BigDecimal(overflownNumber);
            assertEquals(expected, new BigDecimal(overflown.toString()));
        }
    }

    @Test
    public void parseNumberOverflownTextThreadLocalTest() {
        final MutableJsonNumber number = new MutableJsonNumber();

        for (final String overflownNumber : OVERFLOWN_NUMBERS) {
            assertTrue(JsonParser.parseNumber(overflownNumber, number));

            final BigDecimal expected = new BigDecimal(overflownNumber);
            assertEquals(expected, new BigDecimal(JsonParser.lastOverflownNumber().toString()));
        }
    }

    @Test
    public void parseNumberOverflownTextExactTest() {
        final String[] numbers = new String[] {
                (JsonParser.MAX_MANTISSA_VALUE + 1) + "",
                "9999999999999999999999E10",
                "0." + (JsonParser.MAX_MANTISSA_VALUE + 1),
                "9.9999999999999999999999E-10",
                "922337203685477580.5",
                "-99999999999999999999999.55e-7",
                "+99999999999999999999999E10",
                "-0000009999999999999999999999"
        };
        final String[] texts = new String[] {
                "9223372036854775800",
                "9999999999999999999999e10",
                "0.9223372036854775800",
                "9.9999999999999999999999e-10",
                "922337203685477580.5",
                "-99999999999999999999999.55e-7",
                "99999999999999999999999e10",
                "-9999999999999999999999"
        };

        final MutableJsonNumber number = new MutableJsonNumber();
        final StringBuilder overflown = new StringBuilder();

        for (int i = 0; i < numbers.length; i++) {
            assertTrue(JsonParser.parseNumber(numbers[i], number, overflown));
            assertEquals(texts[i], overflown.toString());
        }
    }

    @Test
    public void parseNumberNotOverflownTextTest() {
        final MutableJsonNumber number = new MutableJsonNumber();
        final StringBuilder overflown = new StringBuilder();

        assertTrue(JsonParser.parseNumber("9999999999999999999999", number, overflown));
        assertEquals("9999999999999999999999", overflown.toString());

        assertFalse(JsonParser.parseNumber("123.45", number, overflown));
        assertEquals("9999999999999999999999", overflown.toString());
    }

    @Test
    public void stringTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser copyingParser = new JsonParser(new CopyingStringBuilder(/*false*/)).setListener(events);

        JsonEvents.StringValue event;

        // empty string
        events.clear();
        copyingParser.parse("\"");
        copyingParser.parse("\"");
        copyingParser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.StringValue.class);
        assertNotNull(event);
        assertEquals("", event.string());
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        // a string
        events.clear();
        copyingParser.parse("\"abcd\"");
        copyingParser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.StringValue.class);
        assertNotNull(event);
        assertEquals("abcd", event.string());
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        // a string by chunks
        events.clear();
        copyingParser.parse("\"ab");
        copyingParser.parse("cd\"");
        copyingParser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.StringValue.class);
        assertNotNull(event);
        assertEquals("abcd", event.string());
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        // one more string by chunks
        events.clear();
        copyingParser.parse("\"a");
        copyingParser.parse("b");
        copyingParser.parse("c");
        copyingParser.parse("d\"");
        copyingParser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.StringValue.class);
        assertNotNull(event);
        assertEquals("abcd", event.string());
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        // one more string by chunks
        events.clear();
        copyingParser.parse("\"");
        copyingParser.parse("");
        copyingParser.parse("abc");
        copyingParser.parse("d");
        copyingParser.parse("\"");
        copyingParser.parse("");
        copyingParser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.StringValue.class);
        assertNotNull(event);
        assertEquals("abcd", event.string());
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        // whitespaces out of the literal
        events.clear();
        copyingParser.parse(" \t\r\n\"te st\"  \t\r\n");
        copyingParser.eoj();
        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.StringValue.class);
        assertNotNull(event);
        assertEquals("te st", event.string());
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        // unescaping test
        final String part1 = "123 \\b\\t\\n\\f\\r\\/\\\\\\\"";
        final String part1Decoded = "123 \b\t\n\f\r/\\\"";

        final String part2 = "45 \\u0004\\u0014\\u0145\\u2300\\u2028\\u2029";
        final String part2Decoded = "45 \u0004\u0014\u0145\u2300\u2028\u2029";

        // unescaping with copying string builder
        events.clear();
        copyingParser.parse("\"" + part1);
        copyingParser.parse(part2 + "\"");
        copyingParser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.StringValue.class);
        assertNotNull(event);
        assertEquals(part1Decoded + part2Decoded, event.string());
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        // raw string (the result is not unescaped) with copying string builder
        final JsonParser copyingRawParser = new JsonParser(new CopyingStringBuilder(true)).setListener(events);
        events.clear();
        copyingRawParser.parse("  \"" + part1);
        copyingRawParser.parse(part2 + "\"  ");
        copyingRawParser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.StringValue.class);
        assertNotNull(event);
        assertEquals(part1 + part2, event.string());
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        // raw string only with flyweight (zero copy) string builder
        final JsonParser flyweightParser = new JsonParser(new FlyweightStringBuilder()).setListener(events);
        events.clear();
        flyweightParser.parse("  \"" + part1 + part2 + "\"  ");
        flyweightParser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.StringValue.class);
        assertNotNull(event);
        assertEquals(part1 + part2, event.string());
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());
    }

    @Test
    public void numberTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        final String[] numbers = new String[]{
                Long.toString(-JsonParser.MAX_MANTISSA_VALUE),
                Double.toString(Double.MIN_VALUE),
                Float.toString(Float.MIN_VALUE),
                "-1034567770766.0001",
                "-1034567770766",
                "-00234.6783456789",
                "-0023400",
                "-4.000000123e4",
                "-0.123e-15",
                "-0.123e+10",
                "-0.123e-10",
                "-0.005000000000000",
                "0e0",
                "0",
                "-0.0",
                "0.0",
                "0.005000000000000",
                "0.0000000000000001",
                "0.123E-10",
                "+0.123E+10",
                "0.123E-15",
                "4.000000123E+4",
                "14.000000123",
                "+0023400",
                "00234.6783456789",
                "1034567770766",
                "+1034567770766.0001",
                Float.toString(Float.MAX_VALUE),
                Double.toString(Double.MAX_VALUE),
                Long.toString(JsonParser.MAX_MANTISSA_VALUE)
        };

        for (final String number : numbers) {
            events.clear();
            parser.parse(number);
            parser.eoj();

            assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
            final JsonEvents.NumberValue event = events.pop().as(JsonEvents.NumberValue.class);
            assertNotNull(event);
            assertFalse(event.overflow());
            final BigDecimal expected = new BigDecimal(number);
            assertEquals(expected, event.number());
            assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
            assertTrue(events.isEmpty());
        }

        final String[][] splitNumbers = new String[][]{
                new String[]{"1", "2"},
                new String[]{"+", "123"},
                new String[]{"-", "123.45"},
                new String[]{"-", "123", ".", "45"},
                new String[]{"+", "1", "2", "3", ".", "4", "5", "e", "+", "2"}
        };

        final StringBuilder fullNumber = new StringBuilder();
        for (final String[] numberPart : splitNumbers) {
            events.clear();
            fullNumber.setLength(0);
            for (final String part : numberPart) {
                parser.parse(part);
                fullNumber.append(part);
            }
            parser.eoj();

            assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
            final JsonEvents.NumberValue event = events.pop().as(JsonEvents.NumberValue.class);
            assertNotNull(event);
            assertFalse(event.overflow());
            final BigDecimal expected = new BigDecimal(fullNumber.toString());
            assertEquals(expected, event.number());
            assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
            assertTrue(events.isEmpty());
        }
    }

    @Test
    public void numberOverflowTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        final String[] numbers = new String[] {
                JsonParser.MAX_MANTISSA_VALUE + "",
                (JsonParser.MAX_MANTISSA_VALUE + 1) + "",
                "9999999999999999999999",
                "9999999999999999999999E10",
                "9999999999999999999999E-10",
                "0." + JsonParser.MAX_MANTISSA_VALUE,
                "0." + (JsonParser.MAX_MANTISSA_VALUE + 1),
                "0.9999999999999999999999",
                "9.9999999999999999999999",
                "9.9999999999999999999999E10",
                "9.9999999999999999999999E-10"
        };
        final boolean[] overflow = new boolean[] {
                false,
                true,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                true
        };

        final long[] mantissas = new long[] {
                9223372036854775799L,
                922337203685477580L,
                999999999999999999L,
                999999999999999999L,
                999999999999999999L,
                9223372036854775799L,
                922337203685477580L,
                999999999999999999L,
                999999999999999999L,
                999999999999999999L,
                999999999999999999L
        };
        final int[] exponents = new int[] {
                0,
                1,
                4,
                14,
                -6,
                -19,
                -18,
                -18,
                -17,
                -7,
                -27
        };

        for (int i = 0; i < numbers.length; i++) {
            events.clear();
            parser.parseAndEoj(numbers[i]);
            assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
            final JsonEvents.NumberValue event = events.pop().as(JsonEvents.NumberValue.class);
            assertNotNull(event);
            assertEquals(overflow[i], event.overflow());
            assertEquals(mantissas[i], event.jsonNumber().mantissa());
            assertEquals(exponents[i], event.jsonNumber().exp());
            assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
            assertTrue(events.isEmpty());
        }
    }

    @Test
    public void numberOverflownTextTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        for (final String overflownNumber : OVERFLOWN_NUMBERS) {
            events.clear();
            parser.parseAndEoj(overflownNumber);

            assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
            final JsonEvents.NumberValue event = events.pop().as(JsonEvents.NumberValue.class);
            assertNotNull(event);
            assertTrue(event.overflow());

            final BigDecimal expected = new BigDecimal(overflownNumber);
            assertEquals(expected, new BigDecimal(parser.overflownNumber().toString()));

            assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
            assertTrue(events.isEmpty());
        }
    }

    @Test
    public void numberOverflownTextSplitTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        for (final String overflownNumber : OVERFLOWN_NUMBERS) {
            events.clear();
            for (int i = 0; i < overflownNumber.length(); i++) {
                parser.parse(overflownNumber.substring(i, i + 1));
            }
            parser.eoj();

            assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
            final JsonEvents.NumberValue event = events.pop().as(JsonEvents.NumberValue.class);
            assertNotNull(event);
            assertTrue(event.overflow());

            final BigDecimal expected = new BigDecimal(overflownNumber);
            assertEquals(expected, new BigDecimal(parser.overflownNumber().toString()));

            assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
            assertTrue(events.isEmpty());
        }
    }

    @Test
    public void numberOverflownTextInCallbackTest() {
        final StringBuilder captured = new StringBuilder();
        final JsonParser parser = new JsonParser();
        parser.setListener(new JsonParserListenerAdapter() {
            @Override
            public boolean onNumberValue(final JsonNumber number, final boolean overflow) {
                captured.setLength(0);
                captured.append(overflow ? parser.overflownNumber() : "");
                return true;
            }
        });

        parser.parseAndEoj("[-0.00000000000000000000000123456789012345678901234e5,123.45]");

        assertEquals("", captured.toString());

        parser.parseAndEoj("9999999999999999999999.123E-7");

        assertEquals("9999999999999999999999.123e-7", captured.toString());
    }

    @Test
    public void trueTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("true");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        JsonEvents.TrueValue event = events.pop().as(JsonEvents.TrueValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("tr");
        parser.parse("ue");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.TrueValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("t");
        parser.parse("r");
        parser.parse("u");
        parser.parse("e");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.TrueValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("truetrue");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        final JsonEvents.Error error = events.pop().as(JsonEvents.Error.class);
        assertNotNull(error);
        event = events.pop().as(JsonEvents.TrueValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(parser.hasError());
        assertEquals(4, parser.getErrorPosition());
    }

    @Test
    public void falseTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("false");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        JsonEvents.FalseValue event = events.pop().as(JsonEvents.FalseValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("fal");
        parser.parse("se");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.FalseValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("f");
        parser.parse("a");
        parser.parse("l");
        parser.parse("s");
        parser.parse("e");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.FalseValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("falsefalse");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        final JsonEvents.Error error = events.pop().as(JsonEvents.Error.class);
        assertNotNull(error);
        event = events.pop().as(JsonEvents.FalseValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(parser.hasError());
        assertEquals(5, parser.getErrorPosition());
    }

    @Test
    public void nullTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("null");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        JsonEvents.NullValue event = events.pop().as(JsonEvents.NullValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("nu");
        parser.parse("ll");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.NullValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("n");
        parser.parse("u");
        parser.parse("l");
        parser.parse("l");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        event = events.pop().as(JsonEvents.NullValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("nullnull");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        final JsonEvents.Error error = events.pop().as(JsonEvents.Error.class);
        assertNotNull(error);
        event = events.pop().as(JsonEvents.NullValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(parser.hasError());
        assertEquals(4, parser.getErrorPosition());
    }

    @Test
    public void arrayTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("[[true, false], [null], [[1, 2], [2], [3], [{},{},{}], [");
        parser.parse("]");
        parser.parse("], [\"a\", \"b\", \"c\"]");
        parser.parse(", [\n]\t]");
        parser.eoj();

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onArrayStarted();
        expectedEvents.onArrayStarted();
        expectedEvents.onTrueValue();
        expectedEvents.onFalseValue();
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayStarted();
        expectedEvents.onNullValue();
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayStarted();
        expectedEvents.onArrayStarted();
        expectedEvents.onNumberValue(1, 0, false);
        expectedEvents.onNumberValue(2, 0, false);
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayStarted();
        expectedEvents.onNumberValue(2, 0, false);
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayStarted();
        expectedEvents.onNumberValue(3, 0, false);
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayStarted();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectEnded();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectEnded();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectEnded();
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayStarted();
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayStarted();
        expectedEvents.onStringValue("a");
        expectedEvents.onStringValue("b");
        expectedEvents.onStringValue("c");
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayStarted();
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayEnded();
        expectedEvents.onJsonEnded();

        assertEquals(expectedEvents, events);

        events.clear();
        parser.parse("[");
        parser.parse("{ \"a_1\": 1, \"a_2\": true},");
        parser.parse("{ \"b_1\": \"b_1 value\", \"b_2\": null},");
        parser.parse("{}");
        parser.parse("]");
        parser.eoj();

        expectedEvents.clear();
        expectedEvents.onJsonStarted();
        expectedEvents.onArrayStarted();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember("a_1");
        expectedEvents.onNumberValue(1, 0, false);
        expectedEvents.onObjectMember("a_2");
        expectedEvents.onTrueValue();
        expectedEvents.onObjectEnded();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember("b_1");
        expectedEvents.onStringValue("b_1 value");
        expectedEvents.onObjectMember("b_2");
        expectedEvents.onNullValue();
        expectedEvents.onObjectEnded();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectEnded();
        expectedEvents.onArrayEnded();
        expectedEvents.onJsonEnded();

        assertEquals(expectedEvents, events);
    }

    @Test
    public void objectTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("{");
        parser.parse("  \n}");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        assertNotNull(events.pop().as(JsonEvents.ObjectEnd.class));
        assertNotNull(events.pop().as(JsonEvents.ObjectStart.class));
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        events.clear();
        parser.parse("{");
        parser.parse("}}"); // an error is here in the position 1 (starting from 0)
        parser.eoj();

        assertTrue(parser.hasError());
        assertEquals(1, parser.getErrorPosition());
        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        assertNotNull(events.pop().as(JsonEvents.Error.class));
        assertNotNull(events.pop().as(JsonEvents.ObjectEnd.class));
        assertNotNull(events.pop().as(JsonEvents.ObjectStart.class));
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());

        events.clear();
        parser.parse("{ \"pr");
        parser.parse("op1\":");
        parser.parse(" -12.");
        parser.parse("350, \n\n\"prop2\": [\"aaa\", \"bbb\" ]");
        parser.parse(", \"prop3\": { \"prop3_1\": [1, 2, 3,\n4], \"prop3_2\": null}}");
        parser.eoj();

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember("prop1");
        expectedEvents.onNumberValue(-12350, -3, false);
        expectedEvents.onObjectMember("prop2");
        expectedEvents.onArrayStarted();
        expectedEvents.onStringValue("aaa");
        expectedEvents.onStringValue("bbb");
        expectedEvents.onArrayEnded();
        expectedEvents.onObjectMember("prop3");
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember("prop3_1");
        expectedEvents.onArrayStarted();
        expectedEvents.onNumberValue(1, 0, false);
        expectedEvents.onNumberValue(2, 0, false);
        expectedEvents.onNumberValue(3, 0, false);
        expectedEvents.onNumberValue(4, 0, false);
        expectedEvents.onArrayEnded();
        expectedEvents.onObjectMember("prop3_2");
        expectedEvents.onNullValue();
        expectedEvents.onObjectEnded();
        expectedEvents.onObjectEnded();
        expectedEvents.onJsonEnded();

        assertEquals(expectedEvents, events);
    }

    @Test
    public void resetTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser().setListener(events);

        parser.parse("fal");
        parser.reset();
        parser.parse("null");
        parser.eoj();

        assertNotNull(events.pop().as(JsonEvents.JsonEnd.class));
        final JsonEvents.NullValue event = events.pop().as(JsonEvents.NullValue.class);
        assertNotNull(event);
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertNotNull(events.pop().as(JsonEvents.JsonStart.class));
        assertTrue(events.isEmpty());
    }

    @Test
    public void nextTest() {
        final MutableLong result = new MutableLong(0);

        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(
                new JsonParserListenerAdapter() {
                    @Override
                    public boolean onArrayEnded() {
                        return false;
                    }

                    @Override
                    public boolean onNumberValue(final JsonNumber number) {
                        result.value = result.value + number.mantissa();
                        return false;
                    }

                    @Override
                    public boolean onArrayStarted() {
                        return false;
                    }
                }
        );

        JsonParser.Next next;
        int nextCount = 0;

        next = parser.parse("["); // if no more data, the next is NULL always
        assertNull(next);

        next = parser.parse("10,20,30,40,5");
        while (next != null) {
            nextCount++;
            next = next.next();
        }
        assertEquals(4, nextCount);

        next = parser.parse("0,60,70,80,90,100]");
        while (next != null) {
            nextCount++;
            next = next.next();
        }
        assertEquals(10, nextCount);

        parser.eoj();

        assertEquals(550, result.value);
    }

    @Test
    public void testOnParsingStringTheGivenBounds() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("123456789{\"test_member\": \"test_value\"}abcdefgh", 9, 29);
        parser.eoj();

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember("test_member");
        expectedEvents.onStringValue("test_value");
        expectedEvents.onObjectEnded();
        expectedEvents.onJsonEnded();

        assertEquals(expectedEvents, events);
    }

    @Test
    public void testOnParsingBooleanInTheGivenBounds() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("123456789{\"test_true_member\": true, \"test_false_member\": false}abcdefgh", 9, 54);
        parser.eoj();

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember("test_true_member");
        expectedEvents.onTrueValue();
        expectedEvents.onObjectMember("test_false_member");
        expectedEvents.onFalseValue();
        expectedEvents.onObjectEnded();
        expectedEvents.onJsonEnded();

        assertEquals(expectedEvents, events);
    }

    @Test
    public void testOnParsingNullInTheGivenBounds() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("123456789{\"test_null_member\": null}abcdefgh", 9, 26);
        parser.eoj();

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember("test_null_member");
        expectedEvents.onNullValue();
        expectedEvents.onObjectEnded();
        expectedEvents.onJsonEnded();

        assertEquals(expectedEvents, events);
    }

    @Test
    public void testEscapedStringInTheGivenBounds() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        final String json = "{\"a\\nb\":\"c\\td\"}";
        parser.parse("123" + json + "xyz", 3, json.length());
        parser.eoj();

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember("a\nb");
        expectedEvents.onStringValue("c\td");
        expectedEvents.onObjectEnded();
        expectedEvents.onJsonEnded();

        assertEquals(expectedEvents, events);
    }

    @Test
    public void testStringCutInTheGivenBounds() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("xx\"abyy", 2, 3);
        parser.parse("xxcd\\", 2, 3);
        parser.parse("xxnef\"yy", 2, 4);
        parser.eoj();

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onStringValue("abcd\nef");
        expectedEvents.onJsonEnded();

        assertEquals(expectedEvents, events);
    }

    @Test
    public void testDataWithoutLexemasBeforeJson() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onArrayStarted();
        expectedEvents.onNumberValue(1, 0, false);
        expectedEvents.onArrayEnded();
        expectedEvents.onJsonEnded();

        for (int i = 0; i < 2; i++) { // the second JSON goes after eoj()
            events.clear();
            parser.parse("");
            parser.parse(" \n");
            parser.parse("xx", 1, 0);
            parser.parse("[1]");
            parser.eoj();
            assertEquals(expectedEvents, events);
        }

        events.clear();
        parser.parse("");
        parser.eoj();

        final JsonEvents expectedEmpty = new JsonEvents();
        expectedEmpty.onJsonStarted();
        expectedEmpty.onJsonEnded();
        assertEquals(expectedEmpty, events);
    }

    @Test
    public void testAnyCutOfDataGivesTheSameEvents() {
        final String json = "{\"name\":\"va\\\"l\\u0041ue\", \"t\":true,\"f\":false,\"n\":null,\n"
                + "\"numbers\":[0,-12,+3.25,4e2,5.5E-3,-0.0,99999999999999999999.5e1],"
                + "\"nested\":[{\"a\":[]},{}],\"s\":\"\"}\t";

        final JsonEvents expectedEvents = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(expectedEvents);
        parser.parse(json);
        parser.eoj();

        final JsonEvents events = new JsonEvents();
        parser.setListener(events);
        for (int cut = 1; cut <= json.length(); cut++) {
            events.clear();
            parser.parse(json, 0, cut);
            parser.parse(json, cut, json.length() - cut);
            parser.eoj();
            assertEquals(expectedEvents, events, "Cut at " + cut);
        }
    }

    @Test
    public void testTrailingCommaAndValueAfterRootAreErrors() {
        final Object[][] cases = {
            {"[1,]", "Unexpected ']'", 3},
            {"{\"a\":1,}", "Unexpected '}'", 7},
            {"{\"a\":[1,]}", "Unexpected ']'", 8},
            {"[[1,],2]", "Unexpected ']'", 4},
            {"1 2", "Unexpected number", 2},
            {"[1] 2", "Unexpected number", 4},
            {"[1] 2 ", "Unexpected number", 4},
            {"{} 1", "Unexpected number", 3},
            {"true 1", "Unexpected number", 5},
            {"null 1", "Unexpected number", 5},
            {"\"s\" 1", "Unexpected number", 4},
            {"[1] -", "Incorrect number", 4},
            {"[1] 1e", "Incorrect number", 4},
            {"[1] tr", "'true' expected", 4},
            {"[1] \"ab", "Incorrect string", 4}
        };

        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser().setListener(events);
        for (final Object[] c : cases) {
            final String json = (String) c[0];
            final int position = (Integer) c[2];

            // an error in the first part can be followed by others in the second one,
            // so the first error notified is checked
            for (int cut = 0; cut < json.length(); cut++) {
                events.clear();
                parser.parse(json, 0, cut);
                parser.parse(json, cut, json.length() - cut);
                parser.eoj();
                final JsonEvents.Error error = events.firstError();
                assertNotNull(error, json + ", cut at " + cut);
                assertEquals(c[1], error.string(), json + ", cut at " + cut);
                // the position is in the part the lexema of the error starts in
                assertEquals(position < cut ? position : position - cut, error.position(),
                        json + ", cut at " + cut);
            }
        }
    }

    @Test
    public void testAnyKindOfCharSequence() {
        final String json = "{\"name\":\"value\\n\",\"number\":-12.5}";

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember("name");
        expectedEvents.onStringValue("value\n");
        expectedEvents.onObjectMember("number");
        expectedEvents.onNumberValue(-125, -1, false);
        expectedEvents.onObjectEnded();
        expectedEvents.onJsonEnded();

        final char[] chars = ("**" + json + "**").toCharArray();

        final CharSequence[] inputs = new CharSequence[] {
                json,
                new StringBuilder(json),
                new StringBuffer(json),
                new CharArrayCharSequence(json.toCharArray(), json.length()),
                new CharArrayWithStart(chars, 2, json.length())
        };

        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);
        for (final CharSequence input : inputs) {
            events.clear();
            parser.parse(input);
            parser.eoj();
            assertEquals(expectedEvents, events, input.getClass().getName());
        }
    }

    @Test
    public void testPartOfCharArray() {
        final char[] chars = "**[1,\"ab\",3]**".toCharArray();
        final CharArrayWithStart input = new CharArrayWithStart(chars, 2, 10);

        final FlyweightStringBuilder flyweight = new FlyweightStringBuilder();
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(flyweight).setListener(events);

        parser.parse(input, 3, 4); // "ab"
        parser.eoj();

        assertEquals(input, flyweight.buffer());
        assertEquals(4, flyweight.start());
        assertEquals("ab", flyweight.toString());

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onStringValue("ab");
        expectedEvents.onJsonEnded();
        assertEquals(expectedEvents, events);
    }

    @Test
    public void testLongString() {
        final StringBuilder json = new StringBuilder("[");
        final StringBuilder value = new StringBuilder();
        for (int i = 0; i < 100_000; i++) {
            value.append((char) ('a' + i % 26));
        }
        json.append('"').append(value).append("\"]");

        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse(json);
        parser.eoj();

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onArrayStarted();
        expectedEvents.onStringValue(value);
        expectedEvents.onArrayEnded();
        expectedEvents.onJsonEnded();
        assertEquals(expectedEvents, events);
    }

    @Test
    public void testCopyingStringBuilderAsCharSequence() {
        final CopyingStringBuilder builder = new CopyingStringBuilder();
        builder.start("", 0);
        builder.append(new StringBuffer("0123456789"), 2, 5);
        builder.appendEscapedNewLine();

        assertEquals(6, builder.length());
        assertEquals('2', builder.charAt(0));
        assertEquals('\n', builder.charAt(5));
        assertEquals("23456\n", builder.toString());
        assertEquals("34", builder.subSequence(1, 3).toString());
        assertThrows(IndexOutOfBoundsException.class, () -> builder.charAt(6));
        assertThrows(IndexOutOfBoundsException.class, () -> builder.subSequence(1, 7));
    }

    private static final class CharArrayWithStart implements CharSequence, CharArray {
        private final char[] chars;
        private final int start;
        private final int length;

        CharArrayWithStart(final char[] chars, final int start, final int length) {
            this.chars = chars;
            this.start = start;
            this.length = length;
        }

        @Override
        public char[] array() {
            return chars;
        }

        @Override
        public int start() {
            return start;
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public char charAt(final int index) {
            return chars[start + index];
        }

        @Override
        public CharSequence subSequence(final int from, final int to) {
            return new String(chars, start + from, to - from);
        }
    }

    class MutableLong {
        long value;

        MutableLong(final long value) {
            this.value = value;
        }
    }
}
