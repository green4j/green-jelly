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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class JsonArray {
    private final List<JsonValue> items = new ArrayList<>();

    JsonArray() {
    }

    public JsonObject addObject() {
        return addObjectValue().asObject();
    }

    public JsonObject getObject(final int index) {
        return items.get(index).asObject();
    }

    public JsonObject getObjectRequired(final int index) {
        return items.get(index).asObjectRequired();
    }

    public JsonArray addArray() {
        return addArrayValue().asArray();
    }

    public JsonArray getArray(final int index) {
        return items.get(index).asArray();
    }

    public JsonArray getArrayRequired(final int index) {
        return items.get(index).asArrayRequired();
    }

    public void addString(final String value) {
        items.add(JsonValue.newString(value));
    }

    public String getString(final int index) {
        return items.get(index).asString();
    }

    public String getStringRequired(final int index) {
        return items.get(index).asStringRequired();
    }

    public void addInteger(final int value) {
        items.add(JsonValue.newInteger(value));
    }

    public int getInteger(final int index) {
        return items.get(index).asInteger();
    }

    public int getIntegerRequired(final int index) {
        return items.get(index).asIntegerRequired();
    }

    public void addLong(final long value) {
        items.add(JsonValue.newLong(value));
    }

    public long getLong(final int index) {
        return items.get(index).asLong();
    }

    public long getLongRequired(final int index) {
        return items.get(index).asLongRequired();
    }

    public void putDouble(final double value) {
        items.add(JsonValue.newDouble(value));
    }

    public double getDouble(final int index) {
        return items.get(index).asDouble();
    }

    public double getDoubleRequired(final int index) {
        return items.get(index).asDoubleRequired();
    }

    public void addDecimal(final JsonNumber value) {
        addDecimal(BigDecimal.valueOf(value.mantissa(), -value.exp()));
    }

    public void addDecimal(final BigDecimal value) {
        items.add(JsonValue.newDecimal(value));
    }

    public BigDecimal getDecimal(final int index) {
        return items.get(index).asDecimal();
    }

    public BigDecimal getDecimalRequired(final int index) {
        return items.get(index).asDecimalRequired();
    }

    public void addBoolean(final boolean value) {
        items.add(JsonValue.newBoolean(value));
    }

    public boolean getBoolean(final int index) {
        return items.get(index).asBoolean();
    }

    public boolean getBooleanRequired(final int index) {
        return items.get(index).asBooleanRequired();
    }

    public List<JsonValue> items() {
        return items;
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }

    public void addNull() {
        items.add(JsonValue.newNull());
    }

    public void toJson(final JsonWriter jsonWriter) {
        jsonWriter.startArray();
        for (final JsonValue item : items) {
            item.toJson(jsonWriter);
        }
        jsonWriter.endArray();
    }

    JsonValue addObjectValue() {
        final JsonValue result = JsonValue.newObject();
        items.add(result);
        return result;
    }

    JsonValue addArrayValue() {
        final JsonValue result = JsonValue.newArray();
        items.add(result);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        final JsonGenerator generator = new JsonGenerator();
        generator.setOutput(new AppendableWriter<>(result));
        toJson(new JsonWriterGenerator(generator));
        return result.toString();
    }
}
