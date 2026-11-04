/**
 * MIT License
 *
 * Copyright (c) 2018-2024 Anatoly Gudkov and others.
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
package io.github.green4j.jelly.simple;

import io.github.green4j.jelly.AppendableWriter;
import io.github.green4j.jelly.JsonGenerator;
import io.github.green4j.jelly.JsonNumber;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class JsonValue {
    public static JsonValue newObject() {
        return new JsonValue(new JsonObject());
    }

    public static JsonValue newArray() {
        return new JsonValue(new JsonArray());
    }

    public static JsonValue newString(final String value) {
        return new JsonValue(value);
    }

    public static JsonValue newInteger(final int value) {
        return new JsonValue(value);
    }

    public static JsonValue newLong(final long value) {
        return new JsonValue(value);
    }

    public static JsonValue newDouble(final double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("JSON has no number " + value);
        }
        return new JsonValue(value);
    }

    public static JsonValue newDecimal(final BigDecimal value) {
        return new JsonValue(value);
    }

    public static JsonValue newDecimal(final JsonNumber value) {
        return newDecimal(value.mantissa(), value.exp());
    }

    public static JsonValue newDecimal(final long mantissa, final int exp) {
        return exp == 0 ? new JsonValue(mantissa) :
                new JsonValue(BigDecimal.valueOf(mantissa, -exp));
    }

    public static JsonValue newBoolean(final boolean value) {
        return new JsonValue(value);
    }

    public static JsonValue newNull() {
        return new JsonValue(null);
    }

    static final BigDecimal ZERO = new BigDecimal(0);

    // a mantissa of 18 digits always fits into long
    private static final MathContext WRITTEN_MANTISSA = new MathContext(18, RoundingMode.DOWN);

    private final Object value;

    private JsonValue(final Object value) {
        this.value = value;
    }

    Object value() {
        return value;
    }

    public JsonObject asObject() {
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }
        return null;
    }

    public JsonObject asObjectRequired() {
        return required(JsonObject.class, "object").asObject();
    }

    public JsonArray asArray() {
        if (value instanceof JsonArray) {
            return (JsonArray) value;
        }
        return null;
    }

    public JsonArray asArrayRequired() {
        return required(JsonArray.class, "array").asArray();
    }

    public String asString() {
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    public String asStringRequired() {
        return required(String.class, "string").asString();
    }

    public int asInteger() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    public int asIntegerRequired() {
        return (int) requiredWhole("integer", Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public long asLong() {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    public long asLongRequired() {
        return requiredWhole("long", Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public double asDouble() {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0d;
    }

    public double asDoubleRequired() {
        return required(Number.class, "double").asDouble();
    }

    public BigDecimal asDecimal() {
        if (value instanceof Number) {
            final Number number = (Number) value;
            if (number instanceof BigDecimal) {
                return (BigDecimal) number;
            }
            final BigDecimal result;
            if (number instanceof Float
                    || number instanceof Double) {
                result = BigDecimal.valueOf(number.doubleValue());
            } else {
                result = new BigDecimal(number.longValue());
            }
            return result;
        }
        return ZERO;
    }

    public BigDecimal asDecimalRequired() {
        return required(Number.class, "decimal").asDecimal();
    }

    public boolean asBoolean() {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return false;
    }

    public boolean asBooleanRequired() {
        return required(Boolean.class, "boolean").asBoolean();
    }

    public boolean isNull() {
        return value == null;
    }

    boolean is(final Class<?> type) {
        return type.isInstance(value);
    }

    // what the value is, for a message
    String kind() {
        if (value == null) {
            return "null";
        }
        if (value instanceof JsonObject) {
            return "an object";
        }
        if (value instanceof JsonArray) {
            return "an array";
        }
        if (value instanceof String) {
            return "a string";
        }
        if (value instanceof Number) {
            return "a number";
        }
        return "a boolean";
    }

    // what the value is when it isn't a whole number within [min, max], or null when it is
    String notWhole(final long min, final long max) {
        if (!(value instanceof Number)) {
            return kind();
        }
        if (value instanceof Long
                || value instanceof Integer
                || value instanceof Short
                || value instanceof Byte) {
            final long number = ((Number) value).longValue();
            return number >= min && number <= max ? null : "out of range";
        }
        final BigDecimal decimal;
        if (value instanceof BigDecimal) {
            decimal = (BigDecimal) value;
        } else {
            decimal = new BigDecimal(((Number) value).doubleValue());
        }
        if (decimal.scale() > 0 && decimal.stripTrailingZeros().scale() > 0) {
            return "a fraction";
        }
        return decimal.compareTo(BigDecimal.valueOf(min)) >= 0
                && decimal.compareTo(BigDecimal.valueOf(max)) <= 0 ? null : "out of range";
    }

    private long requiredWhole(final String typeName, final long min, final long max) {
        final String notWhole = notWhole(min, max);
        if (notWhole != null) {
            throw new JsonValueException("Required " + typeName + " is " + notWhole);
        }
        return asLong();
    }

    private JsonValue required(final Class<?> type, final String typeName) {
        if (is(type)) {
            return this;
        }
        throw new JsonValueException("Required " + typeName + " is " + kind());
    }

    public void toJsonAndEoj(final JsonWriter jsonWriter) {
        toJson(jsonWriter);
        jsonWriter.eoj();
    }

    public void toJsonAndEoj(final Writer writer) {
        final JsonGenerator jsonGenerator = new JsonGenerator();
        jsonGenerator.setOutput(new AppendableWriter<>(writer));
        toJsonAndEoj(new JsonWriterGenerator(jsonGenerator));
    }

    public void toJson(final JsonWriter jsonWriter) {
        if (isNull()) {
            jsonWriter.nullValue();
            return;
        }
        if (value instanceof JsonObject) {
            ((JsonObject) value).toJson(jsonWriter);
            return;
        }
        if (value instanceof JsonArray) {
            ((JsonArray) value).toJson(jsonWriter);
            return;
        }
        if (value instanceof String) {
            jsonWriter.stringValue((String) value, true);
            return;
        }
        if (value instanceof Number) {
            BigDecimal decimal = asDecimal();
            if (decimal.unscaledValue().bitLength() > 63) { // the mantissa doesn't fit into long, so it's truncated
                decimal = decimal.round(WRITTEN_MANTISSA);
            }
            jsonWriter.numberValue(decimal.unscaledValue().longValue(), -decimal.scale());
            return;
        }
        if (value instanceof Boolean) {
            if (((Boolean) value).booleanValue()) {
                jsonWriter.trueValue();
            } else {
                jsonWriter.falseValue();
            }
            return;
        }
        throw new IllegalStateException("Unsupported type of the value: " + value + ". " + value.getClass());
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
