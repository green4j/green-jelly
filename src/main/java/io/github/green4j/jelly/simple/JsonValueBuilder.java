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
package io.github.green4j.jelly.simple;

import io.github.green4j.jelly.JsonNumber;
import io.github.green4j.jelly.JsonParser;
import io.github.green4j.jelly.JsonParserListener;
import io.github.green4j.jelly.Utf8JsonParser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * Builds a {@link JsonValue} of a JSON document from the events of any parser, so a tree can be built
 * from chars with {@link io.github.green4j.jelly.JsonParser} as well as right from UTF-8 bytes with
 * {@link io.github.green4j.jelly.Utf8JsonParser}, in one pass and with no decoding of the bytes beforehand:
 * <pre>{@code
 * final Utf8JsonParser parser = new Utf8JsonParser();
 * final JsonValueBuilder builder = new JsonValueBuilder(parser);
 * parser.setListener(builder);
 * parser.parseAndEoj(bytes);
 * if (parser.hasError()) {
 *     ...
 * }
 * final JsonValue json = builder.value();
 * }</pre>
 * The builder can be reused for the next document: everything it collects is dropped when a document starts.
 * The builder doesn't throw: an error is reported by the parser, with {@code hasError()}, and the value
 * of such a document is {@code null}. If {@code eoj()} isn't called after an error, the parser has to be
 * reset before the next document.
 * <p>
 * A number which overflows the mantissa is kept exact, as {@link BigDecimal}, if the builder is given the parser
 * it listens to. A builder created with no parser keeps such a number truncated, as the parser gives it.
 */
public final class JsonValueBuilder implements JsonParserListener {
    private final ArrayList<JsonValue> stack = new ArrayList<>();
    private final Supplier<CharSequence> overflownNumber;

    private JsonValue currentValue;
    private String currentMember;
    private boolean failed;

    private JsonValue value;

    public JsonValueBuilder() {
        overflownNumber = null;
    }

    public JsonValueBuilder(final JsonParser parser) {
        overflownNumber = parser::overflownNumber;
    }

    public JsonValueBuilder(final Utf8JsonParser parser) {
        overflownNumber = parser::overflownNumber;
    }

    /**
     * Returns the value of the last document ended.
     *
     * @return the value, or {@code null} if the document was empty or had an error
     */
    public JsonValue value() {
        return value;
    }

    @Override
    public void onJsonStarted() {
        clear();
        value = null;
    }

    @Override
    public void onError(final String error, final int position) {
        failed = true;
        value = null;
    }

    @Override
    public void onJsonEnded() {
        value = failed ? null : currentValue;
        clear();
    }

    @Override
    public boolean onObjectStarted() {
        if (currentValue == null) {
            currentValue = JsonValue.newObject();
            return true;
        }

        stack.add(currentValue);

        final Object container = currentValue.value();

        if (container instanceof JsonObject) {
            currentValue = ((JsonObject) container).putObjectValue(currentMember);
            return true;
        }
        if (container instanceof JsonArray) {
            currentValue = ((JsonArray) container).addObjectValue();
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
        pop();
        currentMember = null;
        return true;
    }

    @Override
    public boolean onArrayStarted() {
        if (currentValue == null) {
            currentValue = JsonValue.newArray();
            return true;
        }

        stack.add(currentValue);

        final Object container = currentValue.value();

        if (container instanceof JsonObject) {
            currentValue = ((JsonObject) container).putArrayValue(currentMember);
            return true;
        }
        if (container instanceof JsonArray) {
            currentValue = ((JsonArray) container).addArrayValue();
            return true;
        }

        throw new IllegalStateException("Array isn't allowed here");
    }

    @Override
    public boolean onArrayEnded() {
        pop();
        return true;
    }

    @Override
    public boolean onStringValue(final CharSequence data) {
        final String string = data.toString();
        if (currentValue == null) {
            currentValue = JsonValue.newString(string);
            return true;
        }

        final Object container = currentValue.value();

        if (container instanceof JsonObject) {
            ((JsonObject) container).putString(currentMember, string);
            return true;
        }
        if (container instanceof JsonArray) {
            ((JsonArray) container).addString(string);
            return true;
        }

        throw new IllegalStateException("Scalar string " + string + " isn't allowed here");
    }

    @Override
    public boolean onNumberValue(final JsonNumber number) {
        if (currentValue == null) {
            currentValue = JsonValue.newDecimal(number);
            return true;
        }

        final Object container = currentValue.value();

        if (container instanceof JsonObject) {
            ((JsonObject) container).putDecimal(currentMember, number);
            return true;
        }
        if (container instanceof JsonArray) {
            ((JsonArray) container).addDecimal(number);
            return true;
        }

        throw new IllegalStateException("Scalar number " + number + " isn't allowed here");
    }

    @Override
    public boolean onNumberValue(final JsonNumber number, final boolean overflow) {
        if (overflow && overflownNumber != null) {
            onDecimal(new BigDecimal(overflownNumber.get().toString()));
            return true;
        }
        return onNumberValue(number);
    }

    private void onDecimal(final BigDecimal decimal) {
        if (currentValue == null) {
            currentValue = JsonValue.newDecimal(decimal);
            return;
        }

        final Object container = currentValue.value();

        if (container instanceof JsonObject) {
            ((JsonObject) container).putDecimal(currentMember, decimal);
            return;
        }
        if (container instanceof JsonArray) {
            ((JsonArray) container).addDecimal(decimal);
            return;
        }

        throw new IllegalStateException("Scalar number " + decimal + " isn't allowed here");
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

    private void onBoolean(final boolean v) {
        if (currentValue == null) {
            currentValue = JsonValue.newBoolean(v);
            return;
        }

        final Object container = currentValue.value();

        if (container instanceof JsonObject) {
            ((JsonObject) container).putBoolean(currentMember, v);
            return;
        }
        if (container instanceof JsonArray) {
            ((JsonArray) container).addBoolean(v);
            return;
        }

        throw new IllegalStateException("Scalar boolean " + v + " isn't allowed here");
    }

    @Override
    public boolean onNullValue() {
        if (currentValue == null) {
            currentValue = JsonValue.newNull();
            return true;
        }

        final Object container = currentValue.value();

        if (container instanceof JsonObject) {
            ((JsonObject) container).putNull(currentMember);
            return true;
        }
        if (container instanceof JsonArray) {
            ((JsonArray) container).addNull();
            return true;
        }

        throw new IllegalStateException("Scalar null isn't allowed here");
    }

    private void pop() {
        final int size = stack.size();
        if (size > 0) {
            currentValue = stack.remove(size - 1);
        }
    }

    private void clear() {
        stack.clear();
        currentValue = null;
        currentMember = null;
        failed = false;
    }
}
