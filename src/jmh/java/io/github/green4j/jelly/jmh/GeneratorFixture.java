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
package io.github.green4j.jelly.jmh;

import io.github.green4j.jelly.AppendableWriter;
import io.github.green4j.jelly.AsciiByteArrayWriter;
import io.github.green4j.jelly.BufferingWriter;
import io.github.green4j.jelly.CharArrayWriter;
import io.github.green4j.jelly.JsonGenerator;
import io.github.green4j.jelly.Utf8ByteArrayWriter;

import java.util.Random;

/**
 * Generates a document of a chosen shape into a chosen writer. Shared by the generator benchmarks.
 */
public final class GeneratorFixture {
    static final int RECORDS = 40;

    public enum Writer {
        ASCII_BYTES, UTF8_BYTES, CHARS, STRING_BUILDER
    }

    public enum Shape {
        /** Small integers, short member names: mostly structure. */
        NUMBERS,
        /** Market-data-like records: decimals, long timestamps, short strings. */
        DECIMALS,
        /** Long plain strings, nothing to escape. */
        STRINGS,
        /** Long strings with the escaping turned on, a few chars really need that. */
        ESCAPED
    }

    private final String[] symbols = BenchmarkData.symbols();
    private final String[] names = BenchmarkData.sentences(RECORDS, BenchmarkData.asciiWords(), 30);
    private final String[] descriptions = BenchmarkData.sentences(RECORDS, BenchmarkData.asciiWords(), 100);
    private final String[] escapedDescriptions = new String[RECORDS];

    private final long[] bids = new long[RECORDS];
    private final long[] quantities = new long[RECORDS];

    private final AsciiByteArrayWriter asciiWriter = new AsciiByteArrayWriter(1024);
    private final Utf8ByteArrayWriter utf8Writer = new Utf8ByteArrayWriter(1024);
    private final CharArrayWriter charWriter = new CharArrayWriter(1024);
    private final StringBuilder stringBuilder = new StringBuilder(1024);
    private final AppendableWriter<StringBuilder> appendableWriter = new AppendableWriter<>(stringBuilder);

    GeneratorFixture() {
        final Random random = new Random(42);
        for (int i = 0; i < RECORDS; i++) {
            bids[i] = 6_400_000L + random.nextInt(1_000_000);
            quantities[i] = 1000 + random.nextInt(9000);
            escapedDescriptions[i] = descriptions[i] + "\n\t\"quoted\" tail";
        }
    }

    BufferingWriter writer(final Writer writer) {
        switch (writer) {
            case ASCII_BYTES:
                return asciiWriter;
            case UTF8_BYTES:
                return utf8Writer;
            case CHARS:
                return charWriter;
            default:
                return appendableWriter;
        }
    }

    void clear() {
        asciiWriter.clear();
        utf8Writer.clear();
        charWriter.clear();
        stringBuilder.setLength(0);
    }

    /**
     * Generates a document.
     *
     * @param generator the generator with an output set
     * @param shape the kind of the document
     */
    void generate(final JsonGenerator generator, final Shape shape) {
        generator.startArray();
        for (int i = 0; i < RECORDS; i++) {
            generator.startObject();
            switch (shape) {
                case NUMBERS:
                    numbersRecord(generator, i);
                    break;
                case DECIMALS:
                    decimalsRecord(generator, i);
                    break;
                case STRINGS:
                    stringsRecord(generator, i, descriptions, false);
                    break;
                default:
                    stringsRecord(generator, i, escapedDescriptions, true);
                    break;
            }
            generator.endObject();
        }
        generator.endArray();
        generator.eoj();
    }

    private static void numbersRecord(final JsonGenerator generator, final int i) {
        generator.objectMember("property1");
        generator.numberValue(100);
        generator.objectMember("property2");
        generator.numberValue(200);
        generator.objectMember("property3");
        generator.numberValue(300);
        generator.objectMember("property4");
        generator.startArray();
        generator.numberValue(400);
        generator.numberValue(500);
        generator.numberValue(600);
        generator.numberValue(700);
        generator.endArray();
        generator.objectMember("property5");
        if (i % 2 == 0) {
            generator.trueValue();
        } else {
            generator.falseValue();
        }
    }

    private void decimalsRecord(final JsonGenerator generator, final int i) {
        generator.objectMember("symbol");
        generator.stringValue(symbols[i % symbols.length]);
        generator.objectMember("bid");
        generator.numberValue(bids[i], -2);
        generator.objectMember("ask");
        generator.numberValue(bids[i] + 25, -2);
        generator.objectMember("qty");
        generator.numberValue(quantities[i], -4);
        generator.objectMember("time");
        generator.numberValue(1_730_000_000_123L + i * 37L);
    }

    private void stringsRecord(
            final JsonGenerator generator,
            final int i,
            final String[] values,
            final boolean escaping) {
        generator.objectMember("id");
        generator.stringValue(symbols[i % symbols.length], escaping);
        generator.objectMember("name");
        generator.stringValue(names[i], escaping);
        generator.objectMember("description");
        generator.stringValue(values[i], escaping);
    }
}
