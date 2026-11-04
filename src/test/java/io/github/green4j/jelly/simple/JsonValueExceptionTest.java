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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonValueExceptionTest {

    /**
     * Every required accessor there is, asked for a value of another type, for a member which isn't there and
     * for an item past either end: each one throws an {@link IllegalStateException} which names the member or
     * the item and says what it is.
     */
    @Test
    void everyRequiredAccessorThrowsIt() {
        final JsonObject object = new JsonValueParser()
                .parseAndEoj("{\"s\":\"x\",\"n\":1,\"z\":null}").asObjectRequired();
        final JsonArray array = new JsonValueParser().parseAndEoj("[\"x\",1]").asArrayRequired();
        final JsonValue string = JsonValue.newString("x");
        final JsonValue nothing = JsonValue.newNull();

        // the message expected -> the accessor
        final Map<String, Executable> accessors = new LinkedHashMap<>();
        accessors.put("Required object is a string", string::asObjectRequired);
        accessors.put("Required array is a string", string::asArrayRequired);
        accessors.put("Required string is null", nothing::asStringRequired);
        accessors.put("Required integer is a string", string::asIntegerRequired);
        accessors.put("Required long is a string", string::asLongRequired);
        accessors.put("Required double is a string", string::asDoubleRequired);
        accessors.put("Required decimal is a string", string::asDecimalRequired);
        accessors.put("Required boolean is a string", string::asBooleanRequired);
        for (final String member : new String[] {"s", "absent"}) { // of another type, and missed
            final String what = " member '" + member + "' " + ("s".equals(member) ? "is a string" : "missed");
            accessors.put("Required object" + what, () -> object.getObjectRequired(member));
            accessors.put("Required array" + what, () -> object.getArrayRequired(member));
            accessors.put("Required integer" + what, () -> object.getIntegerRequired(member));
            accessors.put("Required long" + what, () -> object.getLongRequired(member));
            accessors.put("Required double" + what, () -> object.getDoubleRequired(member));
            accessors.put("Required decimal" + what, () -> object.getDecimalRequired(member));
            accessors.put("Required boolean" + what, () -> object.getBooleanRequired(member));
        }
        accessors.put("Required string member 'n' is a number", () -> object.getStringRequired("n"));
        accessors.put("Required string member 'z' is null", () -> object.getStringRequired("z"));
        accessors.put("Required string member 'absent' missed", () -> object.getStringRequired("absent"));
        accessors.put("Required member 'absent' missed", () -> object.isNullRequired("absent"));
        for (final int index : new int[] {0, 2, -1}) { // of another type, and past either end
            final String what = " item " + index + " " + (index == 0 ? "is a string" : "missed");
            accessors.put("Required object" + what, () -> array.getObjectRequired(index));
            accessors.put("Required array" + what, () -> array.getArrayRequired(index));
            accessors.put("Required integer" + what, () -> array.getIntegerRequired(index));
            accessors.put("Required long" + what, () -> array.getLongRequired(index));
            accessors.put("Required double" + what, () -> array.getDoubleRequired(index));
            accessors.put("Required decimal" + what, () -> array.getDecimalRequired(index));
            accessors.put("Required boolean" + what, () -> array.getBooleanRequired(index));
        }
        accessors.put("Required string item 1 is a number", () -> array.getStringRequired(1));
        accessors.put("Required string item 2 missed", () -> array.getStringRequired(2));

        for (final Map.Entry<String, Executable> accessor : accessors.entrySet()) {
            final JsonValueException missed =
                    assertThrows(JsonValueException.class, accessor.getValue(), accessor.getKey());
            assertInstanceOf(IllegalStateException.class, missed);
            assertEquals(accessor.getKey(), missed.getMessage());
        }
    }

    /**
     * A value which is there and of the type asked for is returned as it is, a member or an item.
     */
    @Test
    void aValueOfTheTypeAskedForIsReturned() {
        final JsonObject object = new JsonValueParser()
                .parseAndEoj("{\"o\":{},\"a\":[\"x\",2,true,null],\"n\":1.5,\"z\":null}").asObjectRequired();
        final JsonArray array = object.getArrayRequired("a");

        assertNotNull(object.getObjectRequired("o"));
        assertEquals(1.5d, object.getDoubleRequired("n"));
        assertEquals(new BigDecimal("1.5"), object.getDecimalRequired("n"));
        assertTrue(object.isNullRequired("z"));
        assertEquals("x", array.getStringRequired(0));
        assertEquals(2L, array.getLongRequired(1));
        assertTrue(array.getBooleanRequired(2));
    }

    /**
     * A required integer or long is a whole number within the range of its type, however it's written; a
     * fraction, or a number out of the range, is named as such.
     */
    @Test
    void aRequiredIntegerIsAWholeNumberWithinItsRange() {
        final JsonObject object = new JsonValueParser().parseAndEoj("{"
                + "\"one\":1.0,\"hundred\":1e2,\"min\":-2147483648,\"max\":9223372036854775807,"
                + "\"half\":1.5,\"tiny\":1e-30,\"int+1\":2147483648,\"long+1\":9223372036854775808,\"huge\":1e30"
                + "}").asObjectRequired();
        final JsonArray array = new JsonValueParser().parseAndEoj("[2.50,-1e2]").asArrayRequired();

        assertEquals(1, object.getIntegerRequired("one"));
        assertEquals(100, object.getIntegerRequired("hundred"));
        assertEquals(Integer.MIN_VALUE, object.getIntegerRequired("min"));
        assertEquals(Long.MAX_VALUE, object.getLongRequired("max"));
        assertEquals(2147483648L, object.getLongRequired("int+1"));
        assertEquals(-100L, array.getLongRequired(1));
        assertEquals(3, JsonValue.newDouble(3d).asIntegerRequired());
        assertEquals(7L, JsonValue.newDecimal(new BigDecimal("7.000")).asLongRequired());

        // the message expected -> the accessor
        final Map<String, Executable> accessors = new LinkedHashMap<>();
        accessors.put("Required integer member 'half' is a fraction", () -> object.getIntegerRequired("half"));
        accessors.put("Required long member 'tiny' is a fraction", () -> object.getLongRequired("tiny"));
        accessors.put("Required integer member 'int+1' is out of range", () -> object.getIntegerRequired("int+1"));
        accessors.put("Required integer member 'max' is out of range", () -> object.getIntegerRequired("max"));
        accessors.put("Required long member 'long+1' is out of range", () -> object.getLongRequired("long+1"));
        accessors.put("Required long member 'huge' is out of range", () -> object.getLongRequired("huge"));
        accessors.put("Required integer item 0 is a fraction", () -> array.getIntegerRequired(0));
        accessors.put("Required long is a fraction", JsonValue.newDouble(0.5d)::asLongRequired);
        accessors.put("Required integer is out of range",
                JsonValue.newDouble(Double.POSITIVE_INFINITY)::asIntegerRequired);
        accessors.put("Required long is out of range", JsonValue.newDouble(Double.NaN)::asLongRequired);

        for (final Map.Entry<String, Executable> accessor : accessors.entrySet()) {
            final JsonValueException notWhole =
                    assertThrows(JsonValueException.class, accessor.getValue(), accessor.getKey());
            assertEquals(accessor.getKey(), notWhole.getMessage());
        }
    }

    /**
     * An item past either end is read by a getter which isn't required as null, as a member which isn't there is.
     */
    @Test
    void anItemPastEitherEndIsReadAsNull() {
        final JsonArray array = new JsonValueParser().parseAndEoj("[1]").asArrayRequired();

        for (final int index : new int[] {1, -1}) {
            assertNull(array.getObject(index));
            assertNull(array.getArray(index));
            assertNull(array.getString(index));
            assertEquals(0, array.getInteger(index));
            assertEquals(0L, array.getLong(index));
            assertEquals(0d, array.getDouble(index));
            assertEquals(BigDecimal.ZERO, array.getDecimal(index));
            assertFalse(array.getBoolean(index));
        }
        assertEquals(1, array.getInteger(0));
    }
}
