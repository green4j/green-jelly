/**
 * MIT License
 *
 * Copyright (c) 2018-2026 Anatoly Gudkov and others.
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

import io.github.green4j.jelly.JsonParser;

/**
 * Parses chars of a JSON document into a {@link JsonValue} with {@link JsonParser} and {@link JsonValueBuilder}.
 * An error of the parsing is thrown as {@link IllegalArgumentException}, and the parser is ready
 * for the next document after it. To build a value right from UTF-8 bytes, use {@link JsonValueBuilder}
 * with {@link io.github.green4j.jelly.Utf8JsonParser}.
 */
public class JsonValueParser {
    private final JsonParser parser = new JsonParser();
    private final JsonValueBuilder builder = new JsonValueBuilder(parser);

    public JsonValueParser() {
        parser.setListener(builder);
    }

    public void parse(final CharSequence json) {
        parser.parse(json);
        throwIfError();
    }

    public JsonValue eoj() {
        parser.eoj();
        throwIfError();
        return builder.value();
    }

    public JsonValue parseAndEoj(final CharSequence json) {
        parse(json);
        return eoj();
    }

    /**
     * Drops the document being parsed, so the next {@code parse} starts a new one.
     */
    public void reset() {
        parser.reset();
    }

    private void throwIfError() {
        if (parser.hasError()) {
            final String error = parser.getError();
            final int position = parser.getErrorPosition();
            parser.reset(); // the next document starts from scratch
            throw new IllegalArgumentException("An error '" + error + "' at position: " + position);
        }
    }
}
