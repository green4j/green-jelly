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

import io.github.green4j.jelly.JsonNumber;
import io.github.green4j.jelly.JsonParser;
import io.github.green4j.jelly.JsonParserListener;

import java.util.LinkedList;

public class JsonValueParser {
    private final JsonParser parser = new JsonParser();
    private final LinkedList<JsonValue> stack = new LinkedList<>();

    private JsonValue currentValue;
    private String currentMember;

    private boolean newJson = true;

    public JsonValueParser() {
        parser.setListener(new JsonDeserializer());
    }

    public void parse(final CharSequence json) {
        if (newJson) {
            stack.clear();
            currentValue = null;
            currentMember = null;
            newJson = false;
        }
        parser.parse(json);

        if (parser.hasError()) {
            throw new IllegalArgumentException("An error '" + parser.getError()
                    + "' as position: " + parser.getErrorPosition());
        }
    }

    public JsonValue eoj() {
        newJson = true;

        parser.eoj();
        return currentValue;
    }

    public JsonValue parseAndEoj(final CharSequence json) {
        parse(json);
        return eoj();
    }

    private class JsonDeserializer implements JsonParserListener {
        @Override
        public void onJsonStarted() {
        }

        @Override
        public void onError(final String error, final int position) {
            throw new IllegalArgumentException("An error " + error + " while parsing");
        }

        @Override
        public void onJsonEnded() {
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean onObjectStarted() {
            if (currentValue == null) {
                currentValue = JsonValue.newObject();
                return true;
            }

            stack.push(currentValue);

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                currentValue = ((JsonObject) value).putObjectValue(currentMember);
                return true;
            }
            if (value instanceof JsonArray) {
                currentValue = ((JsonArray) value).addObjectValue();
                return true;
            }

            throw new IllegalStateException("Object isn't allowed here");
        }

        @Override
        public boolean onObjectMember(final CharSequence name) {
            currentMember = name.toString();
            return true;
        }

        @Override
        public boolean onObjectEnded() {
            if (!stack.isEmpty()) {
                currentValue = stack.pop();
            }
            currentMember = null;
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean onArrayStarted() {
            if (currentValue == null) {
                currentValue = JsonValue.newArray();
                return true;
            }

            stack.push(currentValue);

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                currentValue = ((JsonObject) value).putArrayValue(currentMember);
                return true;
            }
            if (value instanceof JsonArray) {
                currentValue = ((JsonArray) value).addArrayValue();
                return true;
            }

            throw new IllegalStateException("Array isn't allowed here");
        }

        @Override
        public boolean onArrayEnded() {
            if (!stack.isEmpty()) {
                currentValue = stack.pop();
            }
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean onStringValue(final CharSequence data) {
            final String string = data.toString();
            if (currentValue == null) {
                currentValue = JsonValue.newString(string);
                return true;
            }

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                ((JsonObject) value).putString(currentMember, string);
                return true;
            }
            if (value instanceof JsonArray) {
                ((JsonArray) value).addString(string);
                return true;
            }

            throw new IllegalStateException("Scalar string " + string + " isn't allowed here");
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean onNumberValue(final JsonNumber number) {
            if (currentValue == null) {
                currentValue = JsonValue.newDecimal(number);
                return true;
            }

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                ((JsonObject) value).putDecimal(currentMember, number);
                return true;
            }
            if (value instanceof JsonArray) {
                ((JsonArray) value).addDecimal(number);
                return true;
            }

            throw new IllegalStateException("Scalar number " + number + " isn't allowed here");
        }

        @Override
        public boolean onTrueValue() {
            onBoolean(true);
            return true;
        }

        @Override
        public boolean onFalseValue() {
            onBoolean(false);
            return true;
        }

        @SuppressWarnings("unchecked")
        private void onBoolean(final boolean v) {
            if (currentValue == null) {
                currentValue = JsonValue.newBoolean(v);
            }

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                ((JsonObject) value).putBoolean(currentMember, v);
                return;
            }
            if (value instanceof JsonArray) {
                ((JsonArray) value).addBoolean(v);
                return;
            }

            throw new IllegalStateException("Scalar boolean " + v + " isn't allowed here");
        }


        @Override
        @SuppressWarnings("unchecked")
        public boolean onNullValue() {
            if (currentValue == null) {
                currentValue = JsonValue.newNull();
                return true;
            }

            final Object value = currentValue.value();

            if (value instanceof JsonObject) {
                ((JsonObject) value).putNull(currentMember);
                return true;
            }
            if (value instanceof JsonArray) {
                ((JsonArray) value).addNull();
                return true;
            }

            throw new IllegalStateException("Scalar null isn't allowed here");
        }
    }
}
