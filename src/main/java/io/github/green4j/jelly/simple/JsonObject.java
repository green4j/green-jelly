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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonObject {
    private static final JsonValue NULL_VALUE = JsonValue.newNull();

    private final List<Member> members = new ArrayList<>();
    private final Map<String, JsonValue> membersByName = new HashMap<>();

    JsonObject() {
    }

    public boolean hasMember(final String name) {
        return membersByName.containsKey(name);
    }

    public JsonObject putObject(final String member) {
        return putMember(member, JsonValue.newObject()).asObject();
    }

    public JsonObject getObject(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ? jsonValue.asObject() : null;
    }

    public JsonObject getObjectRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null
                ? jsonValue.asObjectRequired() :
                NULL_VALUE.asObjectRequired();
    }

    public JsonArray putArray(final String member) {
        return putMember(member, JsonValue.newArray()).asArray();
    }

    public JsonArray getArray(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null ? jsonValue.asArray() : null;
    }

    public JsonArray getArrayRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null
                ? jsonValue.asArrayRequired() :
                NULL_VALUE.asArrayRequired();
    }

    public void putString(final String member, final String value) {
        putMember(member, JsonValue.newString(value));
    }

    public String getString(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return null;
        }
        return jsonValue.asString();
    }

    public String getStringRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null
                ? jsonValue.asStringRequired() :
                NULL_VALUE.asStringRequired();
    }

    public void putInteger(final String member, final int value) {
        putMember(member, JsonValue.newInteger(value));
    }

    public int getInteger(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return 0;
        }
        return jsonValue.asInteger();
    }

    public int getIntegerRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null
                ? jsonValue.asIntegerRequired() :
                NULL_VALUE.asIntegerRequired();
    }

    public void putLong(final String member, final long value) {
        putMember(member, JsonValue.newLong(value));
    }

    public long getLong(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return 0L;
        }
        return jsonValue.asLong();
    }

    public long getLongRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null
                ? jsonValue.asLongRequired() :
                NULL_VALUE.asLongRequired();
    }

    public void putDouble(final String member, final double value) {
        putMember(member, JsonValue.newDouble(value));
    }

    public double getDouble(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return 0d;
        }
        return jsonValue.asDouble();
    }

    public double getDoubleRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null
                ? jsonValue.asDoubleRequired() :
                NULL_VALUE.asDoubleRequired();
    }

    public void putDecimal(final String member, final JsonNumber value) {
        putMember(member, JsonValue.newDecimal(value));
    }

    public void putDecimal(final String member, final BigDecimal value) {
        putMember(member, JsonValue.newDecimal(value));
    }

    public BigDecimal getDecimal(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return JsonValue.ZERO;
        }
        return jsonValue.asDecimal();
    }

    public BigDecimal getDecimalRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null
                ? jsonValue.asDecimalRequired() :
                NULL_VALUE.asDecimalRequired();
    }

    public void putBoolean(final String member, final boolean value) {
        putMember(member, JsonValue.newBoolean(value));
    }

    public boolean getBoolean(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return false;
        }
        return jsonValue.asBoolean();
    }

    public boolean getBooleanRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null
                ? jsonValue.asBooleanRequired() :
                NULL_VALUE.asBooleanRequired();
    }

    public void putNull(final String member) {
        putMember(member, null);
    }

    public boolean isNull(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        if (jsonValue == null) {
            return false;
        }
        return jsonValue.isNull();
    }

    public boolean isNullRequired(final String member) {
        final JsonValue jsonValue = membersByName.get(member);
        return jsonValue != null
                ? jsonValue.asBooleanRequired() :
                NULL_VALUE.isNull();
    }

    JsonValue putObjectValue(final String member) {
        return putMember(member, JsonValue.newObject());
    }

    JsonValue putArrayValue(final String member) {
        return putMember(member, JsonValue.newArray());
    }

    private JsonValue putMember(final String member, final JsonValue value) {
        members.add(new Member(member, value));
        membersByName.put(member, value);
        return value;
    }

    public void toJson(final JsonWriter jsonGenerator) {
        jsonGenerator.startObject();
        for (final Member member : members) {
            jsonGenerator.objectMember(member.name);
            member.value.toJson(jsonGenerator);
        }
        jsonGenerator.endObject();
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        final JsonGenerator generator = new JsonGenerator();
        generator.setOutput(new AppendableWriter<>(result));
        toJson(new JsonWriterGenerator(generator));
        return result.toString();
    }

    private final class Member {
        private final String name;
        private final JsonValue value;

        private Member(final String name, final JsonValue value) {
            this.name = name;
            this.value = value;
        }
    }
}
