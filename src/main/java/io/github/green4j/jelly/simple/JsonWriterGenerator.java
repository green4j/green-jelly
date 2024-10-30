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

import io.github.green4j.jelly.JsonGenerator;
import io.github.green4j.jelly.JsonNumber;

public class JsonWriterGenerator implements JsonWriter {
    private final JsonGenerator generator;

    public JsonWriterGenerator(final JsonGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void startObject() {
        generator.startObject();
    }

    @Override
    public void objectMember(final CharSequence name) {
        generator.objectMember(name);
    }

    @Override
    public void objectMember(final CharSequence name, final int start, final int len) {
        generator.objectMember(name, start, len);
    }

    @Override
    public void endObject() {
        generator.endObject();
    }

    @Override
    public void startArray() {
        generator.startArray();
    }

    @Override
    public void endArray() {
        generator.endArray();
    }

    @Override
    public void stringValue(final CharSequence value, final boolean escaping) {
        generator.stringValue(value, escaping);
    }

    @Override
    public void stringValue(final CharSequence value) {
        generator.stringValue(value);
    }

    @Override
    public void stringValue(final CharSequence name, final int start, final int len, final boolean escaping) {
        generator.stringValue(name, start, len, escaping);
    }

    @Override
    public void stringValue(final CharSequence name, final int start, final int len) {
        generator.stringValue(name, start, len);
    }

    @Override
    public void numberValueAsString(final JsonNumber value) {
        generator.numberValueAsString(value);
    }

    @Override
    public void numberValueAsString(final long value) {
        generator.numberValueAsString(value);
    }

    @Override
    public void numberValueAsString(final long mantissa, final int exp) {
        generator.numberValueAsString(mantissa, exp);
    }

    @Override
    public void numberValue(final long value) {
        generator.numberValue(value);
    }

    @Override
    public void numberValue(final JsonNumber value) {
        generator.numberValue(value);
    }

    @Override
    public void numberValue(final long mantissa, final int exp) {
        generator.numberValue(mantissa, exp);
    }

    @Override
    public void trueValue() {
        generator.trueValue();
    }

    @Override
    public void falseValue() {
        generator.falseValue();
    }

    @Override
    public void nullValue() {
        generator.nullValue();
    }

    @Override
    public void eoj() {
        generator.eoj();
    }
}
