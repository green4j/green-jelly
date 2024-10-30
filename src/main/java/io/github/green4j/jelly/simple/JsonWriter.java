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

public interface JsonWriter {

    void startObject();

    void objectMember(CharSequence name);

    void objectMember(CharSequence name, int start, int len);

    void endObject();

    void startArray();

    void endArray();

    void stringValue(CharSequence value, boolean escaping);

    void stringValue(CharSequence value);

    void stringValue(CharSequence name, int start, int len, boolean escaping);

    void stringValue(CharSequence name, int start, int len);

    void numberValueAsString(JsonNumber value);

    void numberValueAsString(long value);

    void numberValueAsString(long mantissa, int exp);

    void numberValue(long value);

    void numberValue(JsonNumber value);

    void numberValue(long mantissa, int exp);

    void trueValue();

    void falseValue();

    void nullValue();

    void eoj();
}
