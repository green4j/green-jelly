/**
 * MIT License
 * <p>
 * Copyright (c) 2018-2026 Anatoly Gudkov and others.
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

import java.io.StringWriter;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonGeneratorTest {

    private static final String[] ESCAPING = new String[128];

    static {
        for (int i = 0; i <= 0x1f; i++) {
            ESCAPING[i] = String.format("\\u%04x", (int) i);
        }
        ESCAPING['"'] = "\\\"";
        ESCAPING['/'] = "\\/";
        ESCAPING['\\'] = "\\\\";
        ESCAPING['\t'] = "\\t";
        ESCAPING['\b'] = "\\b";
        ESCAPING['\n'] = "\\n";
        ESCAPING['\r'] = "\\r";
        ESCAPING['\f'] = "\\f";
    }

    private static String escape(final String value) {
        final StringBuilder result = new StringBuilder();
        final int length = value.length();
        int lastIndex = 0;
        for (int i = 0; i < length; i++) {
            final char c = value.charAt(i);
            final String escaped;
            switch (c) {
                case '\u2028':
                    escaped = "\\u2028";
                    break;
                case '\u2029':
                    escaped = "\\u2029";
                    break;
                default:
                    if (c < ESCAPING.length) {
                        escaped = ESCAPING[c];
                        if (escaped != null) {
                            break;
                        }
                    }
                    continue;
            }
            if (lastIndex < i) {
                result.append(value, lastIndex, i);
            }
            result.append(escaped);
            lastIndex = i + 1;
        }
        if (lastIndex < length) {
            result.append(value, lastIndex, length);
        }
        return result.toString();
    }

    @Test
    public void stringTest() {
        final StringBuilder text = new StringBuilder();
        final AppendableWriter<StringBuilder> writer = new AppendableWriter<>(text);
        final JsonGenerator generator = new JsonGenerator();
        generator.setOutput(writer);

        // simple strings
        final String[] strings = new String[]{
                "",
                "a",
                "12",
                "test",
                "a b c",
                "test  test  test  test  test",
                "test\\/\\\\'\"\b\t\n\f\r\u0004\u0014\u0145\u2300\u2028\u2029test"
        };
        // unescaping
        for (final String string : strings) {
            text.setLength(0);
            generator.stringValue(string);
            generator.eoj();
            assertEquals("\"" + string + "\"", text.toString());
        }
        // escaping
        for (final String string : strings) {
            text.setLength(0);
            generator.stringValue(string, true);
            generator.eoj();
            assertEquals("\"" + escape(string) + "\"", text.toString());
        }
        // sub-string (head)
        text.setLength(0);
        generator.stringValue("foobar", 0, 3, false);
        generator.eoj();
        assertEquals("\"foo\"", text.toString());
        // sub-string (tail)
        text.setLength(0);
        generator.stringValue("foobar", 3, 3, false);
        generator.eoj();
        assertEquals("\"bar\"", text.toString());
        // sub-string (middle)
        text.setLength(0);
        generator.stringValue("foobar", 1, 2, false);
        generator.eoj();
        assertEquals("\"oo\"", text.toString());
        // sub-string (escaping)
        text.setLength(0);
        generator.stringValue("foo\tbar", 2, 3, true);
        generator.eoj();
        assertEquals("\"" + escape("o\tb") + "\"", text.toString());
    }

    @Test
    public void objectMemberTest() {
        final StringBuilder text = new StringBuilder();
        final AppendableWriter<StringBuilder> writer = new AppendableWriter<>(text);
        final JsonGenerator generator = new JsonGenerator(false);
        generator.setOutput(writer);

        final String name = "a\"b\\c\n\u0001\u2028";

        generator.startObject();
        generator.objectMember(name, true);
        generator.numberValue(1);
        generator.objectMember("x" + name + "x", 1, name.length(), true);
        generator.numberValue(2);
        generator.objectMember("plain");
        generator.numberValue(3);
        generator.objectMember("xplainx", 1, 5);
        generator.numberValue(4);
        generator.endObject();
        generator.eoj();

        final String escaped = "\"" + escape(name) + "\"";
        assertEquals("{" + escaped + ":1," + escaped + ":2,\"plain\":3,\"plain\":4}", text.toString());

        final JsonEvents events = new JsonEvents();
        new JsonParser(new CopyingStringBuilder()).setListener(events).parseAndEoj(text);

        final JsonEvents expectedEvents = new JsonEvents();
        expectedEvents.onJsonStarted();
        expectedEvents.onObjectStarted();
        expectedEvents.onObjectMember(name);
        expectedEvents.onNumberValue(1, 0, false);
        expectedEvents.onObjectMember(name);
        expectedEvents.onNumberValue(2, 0, false);
        expectedEvents.onObjectMember("plain");
        expectedEvents.onNumberValue(3, 0, false);
        expectedEvents.onObjectMember("plain");
        expectedEvents.onNumberValue(4, 0, false);
        expectedEvents.onObjectEnded();
        expectedEvents.onJsonEnded();
        assertEquals(expectedEvents, events);
    }

    @Test
    public void numberTest() {
        final StringBuilder text = new StringBuilder();
        final AppendableWriter<StringBuilder> writer = new AppendableWriter<>(text);
        final JsonGenerator generator = new JsonGenerator();
        generator.setOutput(writer);

        final BigDecimal[] numbers = new BigDecimal[]{
                new BigDecimal(Long.MIN_VALUE),
                new BigDecimal(Double.toString(Double.MIN_VALUE)),
                new BigDecimal(Float.toString(Float.MIN_VALUE)),
                new BigDecimal("-1034567770766.0001"),
                new BigDecimal("-1034567770766"),
                new BigDecimal("-00234.6783456789"),
                new BigDecimal("-0023400"),
                new BigDecimal("-50.123E-15"),
                new BigDecimal("-4.000000123e4"),
                new BigDecimal("-0.123e-15"),
                new BigDecimal("-0.123e+10"),
                new BigDecimal("-0.123e-10"),
                new BigDecimal("-0.005000000000000"),
                new BigDecimal("0e0"),
                new BigDecimal("0"),
                new BigDecimal("-0.0"),
                new BigDecimal("0.0"),
                new BigDecimal("0.005000000000000"),
                new BigDecimal("0.0000000000000001"),
                new BigDecimal("0.123E-10"),
                new BigDecimal("+0.123E+10"),
                new BigDecimal("0.123E-15"),
                new BigDecimal("4.000000123E+4"),
                new BigDecimal("50.123E-15"),
                new BigDecimal("14.000000123"),
                new BigDecimal("+0023400"),
                new BigDecimal("00234.6783456789"),
                new BigDecimal("1034567770766"),
                new BigDecimal("+1034567770766.0001"),
                new BigDecimal(Float.toString(Float.MAX_VALUE)),
                new BigDecimal(Double.toString(Double.MAX_VALUE)),
                new BigDecimal(Long.MAX_VALUE)
        };

        for (final BigDecimal number : numbers) {
            text.setLength(0);
            generator.numberValue(number.unscaledValue().longValue(), -number.scale());
            generator.eoj();
            assertTrue(number.compareTo(new BigDecimal(text.toString())) == 0);
        }
    }

    @Test
    public void numberAsStringTest() {
        final StringBuilder text = new StringBuilder();
        final AppendableWriter<StringBuilder> writer = new AppendableWriter<>(text);
        final JsonGenerator generator = new JsonGenerator();
        generator.setOutput(writer);

        final BigDecimal[] numbers = new BigDecimal[]{
                new BigDecimal(Long.MIN_VALUE),
                new BigDecimal(Double.toString(Double.MIN_VALUE)),
                new BigDecimal(Float.toString(Float.MIN_VALUE)),
                new BigDecimal("-1034567770766.0001"),
                new BigDecimal("-1034567770766"),
                new BigDecimal("-00234.6783456789"),
                new BigDecimal("-0023400"),
                new BigDecimal("-50.123E-15"),
                new BigDecimal("-4.000000123e4"),
                new BigDecimal("-0.123e-15"),
                new BigDecimal("-0.123e+10"),
                new BigDecimal("-0.123e-10"),
                new BigDecimal("-0.005000000000000"),
                new BigDecimal("0e0"),
                new BigDecimal("0"),
                new BigDecimal("-0.0"),
                new BigDecimal("0.0"),
                new BigDecimal("0.005000000000000"),
                new BigDecimal("0.0000000000000001"),
                new BigDecimal("0.123E-10"),
                new BigDecimal("+0.123E+10"),
                new BigDecimal("0.123E-15"),
                new BigDecimal("4.000000123E+4"),
                new BigDecimal("50.123E-15"),
                new BigDecimal("14.000000123"),
                new BigDecimal("+0023400"),
                new BigDecimal("00234.6783456789"),
                new BigDecimal("1034567770766"),
                new BigDecimal("+1034567770766.0001"),
                new BigDecimal(Float.toString(Float.MAX_VALUE)),
                new BigDecimal(Double.toString(Double.MAX_VALUE)),
                new BigDecimal(Long.MAX_VALUE)
        };

        for (final BigDecimal number : numbers) {
            text.setLength(0);
            generator.numberValueAsString(number.unscaledValue().longValue(), -number.scale());
            generator.eoj();
            final String numberString = text.toString();
            assertTrue(numberString.startsWith("\""));
            assertTrue(numberString.endsWith("\""));
            assertTrue(number.compareTo(
                    new BigDecimal(numberString.substring(1, numberString.length() - 1))) == 0);
        }
    }

    @Test
    public void numberOfMinimalMantissaTest() {
        final String[][] expected = new String[][] {
                {"0", "-9223372036854775808"},
                {"-2", "-92233720368547758.08"},
                {"-19", "-0.9223372036854775808"},
                {"-22", "-0.0009223372036854775808"},
                {"-25", "-9.223372036854775808e-7"},
                {"5", "-9.223372036854775808e+23"}
        };
        for (final String[] e : expected) {
            final StringBuilder out = new StringBuilder();
            final JsonGenerator generator = new JsonGenerator(out, false);
            generator.numberValue(Long.MIN_VALUE, Integer.parseInt(e[0]));
            generator.eoj();
            assertEquals(e[1], out.toString());
        }
    }

    @Test
    public void anyKindOfOutputTest() {
        final String expected = "[-12,1.5,\"a\\nb\",true]";

        final StringBuffer buffer = new StringBuffer();
        generateAnyKindOfOutputDocument(new JsonGenerator(buffer, false));
        assertEquals(expected, buffer.toString());

        final StringWriter writer = new StringWriter();
        generateAnyKindOfOutputDocument(new JsonGenerator(writer, false));
        assertEquals(expected, writer.toString());

        final StringBuilder builder = new StringBuilder();
        final AppendableWriter<StringBuilder> charByCharWriter = new AppendableWriter<>(builder);
        generateAnyKindOfOutputDocument(new JsonGenerator(new BufferingWriter() { // the default bulk appending
            @Override
            public Frame append(final int size) {
                return charByCharWriter.append(size);
            }

            @Override
            public void append(final char c) {
                charByCharWriter.append(c);
            }

            @Override
            public void append(final CharSequence data) {
                charByCharWriter.append(data);
            }

            @Override
            public void append(final CharSequence data, final int start, final int len) {
                charByCharWriter.append(data, start, len);
            }

            @Override
            public void flush() {
                charByCharWriter.flush();
            }
        }, false));
        assertEquals(expected, builder.toString());
    }

    private static void generateAnyKindOfOutputDocument(final JsonGenerator generator) {
        generator.startArray();
        generator.numberValue(-12);
        generator.numberValue(15, -1);
        generator.stringValue("a\nb", true);
        generator.trueValue();
        generator.endArray();
        generator.eoj();
    }

    @Test
    public void trueTest() {
        final AppendableWriter<StringBuilder> writer = new AppendableWriter<>(new StringBuilder());
        final JsonGenerator generator = new JsonGenerator();
        generator.setOutput(writer);

        generator.trueValue();
        generator.eoj();

        assertEquals("true", writer.output().toString());
    }

    @Test
    public void falseTest() {
        final AppendableWriter<StringBuilder> writer = new AppendableWriter<>(new StringBuilder());
        final JsonGenerator generator = new JsonGenerator();
        generator.setOutput(writer);

        generator.falseValue();
        generator.eoj();

        assertEquals("false", writer.output().toString());
    }

    @Test
    public void nullTest() {
        final AppendableWriter<StringBuilder> writer = new AppendableWriter<>(new StringBuilder());
        final JsonGenerator generator = new JsonGenerator();
        generator.setOutput(writer);

        generator.nullValue();
        generator.eoj();

        assertEquals("null", writer.output().toString());
    }

    @Test
    public void arrayTest() {
        final AppendableWriter<StringBuilder> writer = new AppendableWriter<>(new StringBuilder());
        final JsonGenerator generator = new JsonGenerator(false);
        generator.setOutput(writer);

        // empty array
        generator.startArray();
        generator.endArray();
        generator.eoj();
        assertEquals("[]", writer.output().toString());

        // simple array
        writer.output().setLength(0);
        generator.startArray();
        generator.numberValue(100);
        generator.numberValue(200);
        generator.trueValue();
        generator.falseValue();
        generator.nullValue();
        generator.stringValue("\test", true);
        generator.endArray();
        generator.eoj();
        assertEquals("[100,200,true,false,null,\"\\test\"]",
                writer.output().toString());

        // inner arrays
        writer.output().setLength(0);
        generator.startArray();
        generator.startArray();
        generator.numberValue(100);
        generator.numberValue(200);
        generator.endArray();
        generator.startArray();
        generator.startArray();
        generator.startObject();
        generator.endObject();
        generator.startObject();
        generator.endObject();
        generator.startObject();
        generator.endObject();
        generator.endArray();
        generator.startArray();
        generator.endArray();
        generator.startArray();
        generator.endArray();
        generator.endArray();
        generator.startArray();
        generator.endArray();
        generator.startArray();
        generator.trueValue();
        generator.falseValue();
        generator.endArray();
        generator.nullValue();
        generator.stringValue("\test", true);
        generator.endArray();
        generator.eoj();
        assertEquals("[[100,200],[[{},{},{}],[],[]],[],[true,false],null,\"\\test\"]",
                writer.output().toString());
    }


    @Test
    public void objectTest() {
        final AppendableWriter<StringBuilder> writer = new AppendableWriter<>(new StringBuilder());
        final JsonGenerator generator = new JsonGenerator(false);
        generator.setOutput(writer);

        // empty object
        generator.startObject();
        generator.endObject();
        generator.eoj();
        assertEquals("{}", writer.output().toString());

        // simple object
        writer.output().setLength(0);
        generator.startObject();
        generator.objectMember("prop1");
        generator.numberValue(100);
        generator.objectMember("prop2");
        generator.numberValue(200);
        generator.objectMember("prop3");
        generator.trueValue();
        generator.objectMember("prop4");
        generator.falseValue();
        generator.objectMember("prop5");
        generator.nullValue();
        generator.objectMember("prop6");
        generator.startArray();
        generator.stringValue("\test1", true);
        generator.stringValue("\test2", true);
        generator.stringValue("\test3", true);
        generator.endArray();
        generator.endObject();
        generator.eoj();
        assertEquals(
                "{\"prop1\":100,\"prop2\":200,\"prop3\":true,\"prop4\":false,"
                        + "\"prop5\":null,\"prop6\":[\"\\test1\",\"\\test2\",\"\\test3\"]}",
                writer.output().toString());

        // inner objects
        writer.output().setLength(0);
        generator.startObject();
        generator.objectMember("prop1");
        generator.startObject();
        generator.endObject();
        generator.objectMember("prop2");
        generator.numberValue(200);
        generator.objectMember("prop3");
        generator.startObject();
        generator.objectMember("prop3_1");
        generator.trueValue();
        generator.objectMember("prop3_2");
        generator.falseValue();
        generator.endObject();
        generator.objectMember("prop4");
        generator.falseValue();
        generator.objectMember("prop5");
        generator.nullValue();
        generator.objectMember("prop6");
        generator.startObject();
        generator.objectMember("prop6_1");
        generator.startObject();
        generator.objectMember("prop6_1_1");
        generator.startArray();
        generator.stringValue("\test1", true);
        generator.stringValue("\test2", true);
        generator.stringValue("\test3", true);
        generator.endArray();
        generator.endObject();
        generator.endObject();
        generator.endObject();
        generator.eoj();
        assertEquals("{\"prop1\":{},\"prop2\":200,\"prop3\":{\"prop3_1\":true,\"prop3_2\":false},"
                        + "\"prop4\":false,\"prop5\":null,\"prop6\":{\"prop6_1\":{\"prop6_1_1\":"
                        + "[\"\\test1\",\"\\test2\",\"\\test3\"]}}}",
                writer.output().toString());

        // sub-string member names
        writer.output().setLength(0);
        generator.startObject();
        generator.objectMember("foobar", 0, 3);
        generator.numberValue(100);
        generator.objectMember("foobar", 3, 3);
        generator.numberValue(200);
        generator.objectMember("foobar", 1, 2);
        generator.stringValue("foobar", 1, 4);
        generator.endObject();
        generator.eoj();
        assertEquals("{\"foo\":100,\"bar\":200,\"oo\":\"ooba\"}", writer.output().toString());
    }

    @Test
    public void subStringTest() {
        final CharArrayWriter writer = new CharArrayWriter(64);
        final JsonGenerator generator = new JsonGenerator(false);
        generator.setOutput(writer);

        // unescaping
        generator.startObject();
        generator.objectMember("foobar", 0, 3);
        generator.stringValue("foobar", 3, 3);
        generator.endObject();
        generator.eoj();
        assertEquals("{\"foo\":\"bar\"}", writer.toString());

        // escaping
        writer.clear();
        generator.startObject();
        generator.objectMember("foobar", 1, 2);
        generator.stringValue("foo\tbar", 2, 3, true);
        generator.endObject();
        generator.eoj();
        assertEquals("{\"oo\":\"" + escape("o\tb") + "\"}", writer.toString());
    }
}
