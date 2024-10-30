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
    private final Object value;

    private JsonValue(final Object value) {
        this.value = value;
    }

    Object value() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public JsonObject asObject() {
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public JsonObject asObjectRequired() {
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        }
        throw new IllegalStateException("Required object missed");
    }

    @SuppressWarnings("unchecked")
    public JsonArray asArray() {
        if (value instanceof JsonArray) {
            return (JsonArray) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public JsonArray asArrayRequired() {
        if (value instanceof JsonArray) {
            return (JsonArray) value;
        }
        throw new IllegalStateException("Required array missed");
    }

    @SuppressWarnings("unchecked")
    public String asString() {
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public String asStringRequired() {
        if (value instanceof String) {
            return (String) value;
        }
        throw new IllegalStateException("Required string missed");
    }

    @SuppressWarnings("unchecked")
    public int asInteger() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public int asIntegerRequired() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new IllegalStateException("Required integer missed");
    }

    @SuppressWarnings("unchecked")
    public long asLong() {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    @SuppressWarnings("unchecked")
    public long asLongRequired() {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new IllegalStateException("Required long missed");
    }

    @SuppressWarnings("unchecked")
    public double asDouble() {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0d;
    }

    @SuppressWarnings("unchecked")
    public double asDoubleRequired() {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new IllegalStateException("Required double missed");
    }

    @SuppressWarnings("unchecked")
    public BigDecimal asDecimal() {
        if (value instanceof Number) {
            final Number number = (Number) value;
            if (number instanceof BigDecimal) {
                return (BigDecimal) number;
            }
            final BigDecimal result;
            if (number instanceof Float
                    || number instanceof Double) {
                result = new BigDecimal(number.doubleValue());
            } else {
                result = new BigDecimal(number.longValue());
            }
            return result;
        }
        return ZERO;
    }

    public BigDecimal asDecimalRequired() {
        final BigDecimal result = asDecimal();
        if (result == ZERO) {
            throw new IllegalStateException("Required decimal missed");
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public boolean asBoolean() {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public boolean asBooleanRequired() {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        throw new IllegalStateException("Required boolean missed");
    }

    public boolean isNull() {
        return value == null;
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

    @SuppressWarnings("unchecked")
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
            final BigDecimal decimal = asDecimal();
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
        return value != null ? value.toString() : "";
    }
}
