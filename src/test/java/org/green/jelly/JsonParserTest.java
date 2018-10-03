/**
 * MIT License
 *
 * Copyright (c) 2018 Anatoly Gudkov
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
package org.green.jelly;

import java.math.BigDecimal;
import org.green.jelly.JsonEvents.FalseValue;
import org.green.jelly.JsonEvents.JsonEnd;
import org.green.jelly.JsonEvents.JsonStart;
import org.green.jelly.JsonEvents.NullValue;
import org.green.jelly.JsonEvents.NumberValue;
import org.green.jelly.JsonEvents.ObjectEnd;
import org.green.jelly.JsonEvents.ObjectStart;
import org.green.jelly.JsonEvents.StringValue;
import org.green.jelly.JsonEvents.TrueValue;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class JsonParserTest {

    @Test
    public void stringTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser copyingParser = new JsonParser(new CopyingStringBuilder(/*false*/)).setListener(events);

        StringValue event;

        // empty string
        events.clear();
        copyingParser.parse("\"").parse("\"").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        event = events.pop().as(StringValue.class);
        Assert.assertNotNull(event);
        assertEquals("", event.string());
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        // whitespaces out of the literal
        events.clear();
        copyingParser.parse(" \t\r\n\"te st\"  \t\r\n\"").eoj();
        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        event = events.pop().as(StringValue.class);
        Assert.assertNotNull(event);
        assertEquals("te st", event.string());
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        // unescaping test
        final String part1 = "123 \\b\\t\\n\\f\\r\\/\\\\\\\"";
        final String part1Decoded = "123 \b\t\n\f\r/\\\"";

        final String part2 = "45 \\u0004\\u0014\\u0145\\u2300\\u2028\\u2029";
        final String part2Decoded = "45 \u0004\u0014\u0145\u2300\u2028\u2029";

        // unescaping with copying string builder
        events.clear();
        copyingParser.parse("\"" + part1).parse(part2 + "\"").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        event = events.pop().as(StringValue.class);
        Assert.assertNotNull(event);
        assertEquals(part1Decoded + part2Decoded, event.string());
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        // raw string (the result is not unescaped) with copying string builder
        final JsonParser copyingRawParser = new JsonParser(new CopyingStringBuilder(true)).setListener(events);
        events.clear();
        copyingRawParser.parse("  \"" + part1).parse(part2 + "\"  ").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        event = events.pop().as(StringValue.class);
        Assert.assertNotNull(event);
        assertEquals(part1 + part2, event.string());
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        // raw string only with flyweight (zero copy) string builder
        final JsonParser flyweightParser = new JsonParser(new FlyweightStringBuilder()).setListener(events);
        events.clear();
        flyweightParser.parse("  \"" + part1 + part2 + "\"  ").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        event = events.pop().as(StringValue.class);
        Assert.assertNotNull(event);
        assertEquals(part1 + part2, event.string());
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());
    }

    @Test
    public void numberTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        final String[] numbers = new String[]{
            Long.toString(Long.MIN_VALUE),
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
            Long.toString(Long.MAX_VALUE)
        };

        for (final String number : numbers) {
            events.clear();
            parser.parse(number).eoj();

            Assert.assertNotNull(events.pop().as(JsonEnd.class));
            final NumberValue event = events.pop().as(NumberValue.class);
            Assert.assertNotNull(event);
            final BigDecimal expected = new BigDecimal(number);
            assertEquals(expected, event.number());
            Assert.assertNotNull(events.pop().as(JsonStart.class));
            assertTrue(events.isEmpty());
        }

        final String[][] splittedNumbers = new String[][]{
            new String[] {"1", "2"},
            new String[] {"+", "123"},
            new String[] {"-", "123.45"},
            new String[] {"-", "123", ".", "45"},
            new String[] {"+", "1", "2", "3", ".", "4", "5", "e", "+", "2"}
        };

        final StringBuilder fullNumber = new StringBuilder();
        for (final String[] numberPart : splittedNumbers) {
            events.clear();
            fullNumber.setLength(0);
            for (String part : numberPart) {
                parser.parse(part);
                fullNumber.append(part);
            }
            parser.eoj();

            Assert.assertNotNull(events.pop().as(JsonEnd.class));
            final NumberValue event = events.pop().as(NumberValue.class);
            Assert.assertNotNull(event);
            final BigDecimal expected = new BigDecimal(fullNumber.toString());
            assertEquals(expected, event.number());
            Assert.assertNotNull(events.pop().as(JsonStart.class));
            assertTrue(events.isEmpty());
        }
    }

    @Test
    public void trueTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("true").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        TrueValue event = events.pop().as(TrueValue.class);
        Assert.assertNotNull(event);
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("tr").parse("ue").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        event = events.pop().as(TrueValue.class);
        Assert.assertNotNull(event);
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("t").parse("r").parse("u").parse("e").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        event = events.pop().as(TrueValue.class);
        Assert.assertNotNull(event);
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());
    }

    @Test
    public void falseTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("false").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        FalseValue event = events.pop().as(FalseValue.class);
        Assert.assertNotNull(event);
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("fal").parse("se").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        event = events.pop().as(FalseValue.class);
        Assert.assertNotNull(event);
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("f").parse("a").parse("l").parse("s").parse("e").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        event = events.pop().as(FalseValue.class);
        Assert.assertNotNull(event);
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());
    }

    @Test
    public void nullTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("null").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        NullValue event = events.pop().as(NullValue.class);
        Assert.assertNotNull(event);
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("nu").parse("ll").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        event = events.pop().as(NullValue.class);
        Assert.assertNotNull(event);
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        parser.parse("n").parse("u").parse("l").parse("l").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        event = events.pop().as(NullValue.class);
        Assert.assertNotNull(event);
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());
    }

    @Test
    public void arrayTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("[[[1, 2], [2], [3], [], [").parse("]").parse("], [\"a\", \"b\", \"c\"]").parse(", [\n]\t]").eoj();

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onArrayStarted();
        expectedEvents.onArrayStarted();
        expectedEvents.onArrayStarted();
        expectedEvents.onNumberValue(1, 0);
        expectedEvents.onNumberValue(2, 0);
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayStarted();
        expectedEvents.onNumberValue(2, 0);
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayStarted();
        expectedEvents.onNumberValue(3, 0);
        expectedEvents.onArrayEnded();
        expectedEvents.onArrayStarted();
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
    }

    @Test
    public void objectTest() {
        final JsonEvents events = new JsonEvents();
        final JsonParser parser = new JsonParser(new CopyingStringBuilder()).setListener(events);

        parser.parse("{").parse("  \n}").eoj();

        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        Assert.assertNotNull(events.pop().as(ObjectEnd.class));
        Assert.assertNotNull(events.pop().as(ObjectStart.class));
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        events.clear();
        parser.parse("{")
                .parse("}}") // an error is here in the position 1 (starting from 0)
                .eoj();

        assertTrue(parser.hasError());
        assertEquals(1, parser.getErrorPosition());
        Assert.assertNotNull(events.pop().as(JsonEnd.class));
        Assert.assertNotNull(events.pop().as(JsonEvents.Error.class));
        Assert.assertNotNull(events.pop().as(ObjectEnd.class));
        Assert.assertNotNull(events.pop().as(ObjectStart.class));
        Assert.assertNotNull(events.pop().as(JsonStart.class));
        assertTrue(events.isEmpty());

        events.clear();
        parser.parse("{ \"prop1\":").parse(" -12.350, \n\n\"prop2\": [\"aaa\", \"bbb\" ]")
                .parse(", \"prop3\": { \"prop3_1\": [1, 2, 3,\n4], \"prop3_2\": null}}").eoj();

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember("prop1");
        expectedEvents.onNumberValue(-12350, -3);
        expectedEvents.onObjectMember("prop2");
        expectedEvents.onArrayStarted();
        expectedEvents.onStringValue("aaa");
        expectedEvents.onStringValue("bbb");
        expectedEvents.onArrayEnded();
        expectedEvents.onObjectMember("prop3");
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember("prop3_1");
        expectedEvents.onArrayStarted();
        expectedEvents.onNumberValue(1, 0);
        expectedEvents.onNumberValue(2, 0);
        expectedEvents.onNumberValue(3, 0);
        expectedEvents.onNumberValue(4, 0);
        expectedEvents.onArrayEnded();
        expectedEvents.onObjectMember("prop3_2");
        expectedEvents.onNullValue();
        expectedEvents.onObjectEnded();
        expectedEvents.onObjectEnded();
        expectedEvents.onJsonEnded();

        assertEquals(expectedEvents, events);
    }
}
