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
package io.github.green4j.jelly.simple;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static io.github.green4j.jelly.simple.JsonValueBuilderTest.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonValueTest {

    @Test
    void writesMantissaWiderThanLongTruncatedTo18Digits() {
        final JsonValue json = JsonValue.newArray();
        final JsonArray array = json.asArrayRequired();
        array.addDecimal(new BigDecimal("12345678901234567890123"));
        array.addDecimal(new BigDecimal("-99999999999999999999.5"));
        array.addDecimal(new BigDecimal("1.00000000000000000001e-5"));
        array.addLong(Long.MAX_VALUE);
        array.addLong(Long.MIN_VALUE);
        array.addDecimal(new BigDecimal("922337203685477580.7"));

        final JsonArray written = new JsonValueParser().parseAndEoj(json(json)).asArrayRequired();
        assertDecimal("12345678901234567800000", written.getDecimal(0));
        assertDecimal("-99999999999999999900", written.getDecimal(1));
        assertDecimal("0.0000100000000000000000", written.getDecimal(2));
        assertEquals(Long.MAX_VALUE, written.getLong(3));
        assertEquals(Long.MIN_VALUE, written.getLong(4));
        assertDecimal("922337203685477580.7", written.getDecimal(5));
    }

    @Test
    void writesDoublesAsTheirShortestDecimal() {
        final JsonValue json = JsonValue.newArray();
        final JsonArray array = json.asArrayRequired();
        array.putDouble(0.1);
        array.putDouble(-1234.5678);
        array.putDouble(1e-10);
        array.putDouble(3);

        assertEquals("[0.1,-1234.5678,1.0e-10,3.0]", json(json).replaceAll("\\s", ""));
        assertEquals(new BigDecimal("0.1"), array.getDecimal(0));
    }

    @Test
    void doesNotTakeADoubleWhichIsNotAJsonNumber() {
        final JsonArray array = JsonValue.newArray().asArrayRequired();
        final JsonObject object = JsonValue.newObject().asObjectRequired();

        for (final double notJson : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            final IllegalArgumentException notTaken =
                    assertThrows(IllegalArgumentException.class, () -> JsonValue.newDouble(notJson));
            assertEquals("JSON has no number " + notJson, notTaken.getMessage());
            assertThrows(IllegalArgumentException.class, () -> array.putDouble(notJson));
            assertThrows(IllegalArgumentException.class, () -> object.putDouble("d", notJson));
        }
        assertEquals(0, array.size());
        assertFalse(object.hasMember("d"));
    }

    @Test
    void keepsTheLastValueOfAMemberPutAgainAtItsFirstPosition() {
        final JsonValue json = JsonValue.newObject();
        final JsonObject object = json.asObjectRequired();
        object.putInteger("a", 1);
        object.putInteger("b", 2);
        object.putString("a", "x");
        assertEquals("x", object.getString("a"));
        assertEquals("{\"a\":\"x\",\"b\":2}", json(json).replaceAll("\\s", ""));

        final JsonValue parsed = new JsonValueParser().parseAndEoj("{\"a\":1,\"b\":2,\"a\":[3]}");
        assertEquals(3, parsed.asObjectRequired().getArrayRequired("a").getInteger(0));
        assertEquals("{\"a\":[3],\"b\":2}", json(parsed).replaceAll("\\s", ""));
    }

    @Test
    void writesNullAsNullWithToString() {
        assertEquals("null", JsonValue.newNull().toString());
        assertEquals("null", new JsonValueParser().parseAndEoj("null").toString());
    }

    private static void assertDecimal(final String expected, final BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), expected + " != " + actual);
    }
}
