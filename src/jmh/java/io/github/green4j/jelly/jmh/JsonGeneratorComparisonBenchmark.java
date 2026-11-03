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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.google.gson.stream.JsonWriter;
import io.github.green4j.jelly.JsonGenerator;
import io.github.green4j.jelly.Utf8ByteArrayWriter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Compares the streaming generators on documents of 40 records, the same as {@link GeneratorFixture} generates.
 *
 * <p>green-jelly and Jackson write UTF-8 bytes, Gson can write chars only. All of them escape the string values.
 * A decimal is given to green-jelly as a mantissa and an exponent, while Jackson and Gson get a {@code double}.
 * Jackson needs a new generator for a document and Gson a new writer, both are created in the measured code,
 * since a real application has to do that as well.
 *
 * <p>Run with:
 * <pre>
 *   ./gradlew jmh -PjmhArgs="JsonGeneratorComparisonBenchmark"
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class JsonGeneratorComparisonBenchmark {

    private static final int RECORDS = GeneratorFixture.RECORDS;
    private static final long TIME_BASE = 1_730_000_000_123L;

    @Param({"NUMBERS", "DECIMALS", "STRINGS"})
    GeneratorFixture.Shape shape;

    private final String[] symbols = BenchmarkData.symbols();
    private final String[] names = BenchmarkData.sentences(RECORDS, BenchmarkData.asciiWords(), 30);
    private final String[] descriptions = BenchmarkData.sentences(RECORDS, BenchmarkData.asciiWords(), 100);

    private final long[] bids = new long[RECORDS];
    private final long[] quantities = new long[RECORDS];
    private final double[] bidValues = new double[RECORDS];
    private final double[] askValues = new double[RECORDS];
    private final double[] quantityValues = new double[RECORDS];

    private final JsonFactory jacksonFactory = new JsonFactory();
    private final ByteArrayOutputStream jacksonOutput = new ByteArrayOutputStream(8192);

    private final Utf8ByteArrayWriter jellyOutput = new Utf8ByteArrayWriter(8192);
    private final JsonGenerator jelly = new JsonGenerator(jellyOutput, false);

    @Setup
    public void setup() {
        final Random random = new Random(42);
        for (int i = 0; i < RECORDS; i++) {
            bids[i] = 6_400_000L + random.nextInt(1_000_000);
            quantities[i] = 1000 + random.nextInt(9000);
            bidValues[i] = bids[i] / 100.0;
            askValues[i] = (bids[i] + 25) / 100.0;
            quantityValues[i] = quantities[i] / 10000.0;
        }
        jellyOutput.clear();
    }

    @Benchmark
    public int greenJelly() {
        final JsonGenerator generator = jelly;
        jellyOutput.clear();
        generator.startArray();
        for (int i = 0; i < RECORDS; i++) {
            generator.startObject();
            switch (shape) {
                case NUMBERS:
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
                    generator.stringValue("value", true);
                    break;
                case DECIMALS:
                    generator.objectMember("symbol");
                    generator.stringValue(symbols[i % symbols.length], true);
                    generator.objectMember("bid");
                    generator.numberValue(bids[i], -2);
                    generator.objectMember("ask");
                    generator.numberValue(bids[i] + 25, -2);
                    generator.objectMember("qty");
                    generator.numberValue(quantities[i], -4);
                    generator.objectMember("time");
                    generator.numberValue(TIME_BASE + i * 37L);
                    break;
                default:
                    generator.objectMember("id");
                    generator.stringValue(symbols[i % symbols.length], true);
                    generator.objectMember("name");
                    generator.stringValue(names[i], true);
                    generator.objectMember("description");
                    generator.stringValue(descriptions[i], true);
                    break;
            }
            generator.endObject();
        }
        generator.endArray();
        generator.eoj();
        return jellyOutput.length();
    }

    @Benchmark
    public int jackson() throws IOException {
        jacksonOutput.reset();
        try (com.fasterxml.jackson.core.JsonGenerator generator =
                     jacksonFactory.createGenerator(jacksonOutput, JsonEncoding.UTF8)) {
            generator.writeStartArray();
            for (int i = 0; i < RECORDS; i++) {
                generator.writeStartObject();
                switch (shape) {
                    case NUMBERS:
                        generator.writeFieldName("property1");
                        generator.writeNumber(100);
                        generator.writeFieldName("property2");
                        generator.writeNumber(200);
                        generator.writeFieldName("property3");
                        generator.writeNumber(300);
                        generator.writeFieldName("property4");
                        generator.writeStartArray();
                        generator.writeNumber(400);
                        generator.writeNumber(500);
                        generator.writeNumber(600);
                        generator.writeNumber(700);
                        generator.writeEndArray();
                        generator.writeFieldName("property5");
                        generator.writeString("value");
                        break;
                    case DECIMALS:
                        generator.writeFieldName("symbol");
                        generator.writeString(symbols[i % symbols.length]);
                        generator.writeFieldName("bid");
                        generator.writeNumber(bidValues[i]);
                        generator.writeFieldName("ask");
                        generator.writeNumber(askValues[i]);
                        generator.writeFieldName("qty");
                        generator.writeNumber(quantityValues[i]);
                        generator.writeFieldName("time");
                        generator.writeNumber(TIME_BASE + i * 37L);
                        break;
                    default:
                        generator.writeFieldName("id");
                        generator.writeString(symbols[i % symbols.length]);
                        generator.writeFieldName("name");
                        generator.writeString(names[i]);
                        generator.writeFieldName("description");
                        generator.writeString(descriptions[i]);
                        break;
                }
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }
        return jacksonOutput.size();
    }

    @Benchmark
    public int gson() throws IOException {
        final StringWriter output = new StringWriter(8192);
        try (JsonWriter writer = new JsonWriter(output)) {
            writer.beginArray();
            for (int i = 0; i < RECORDS; i++) {
                writer.beginObject();
                switch (shape) {
                    case NUMBERS:
                        writer.name("property1").value(100);
                        writer.name("property2").value(200);
                        writer.name("property3").value(300);
                        writer.name("property4").beginArray();
                        writer.value(400).value(500).value(600).value(700);
                        writer.endArray();
                        writer.name("property5").value("value");
                        break;
                    case DECIMALS:
                        writer.name("symbol").value(symbols[i % symbols.length]);
                        writer.name("bid").value(bidValues[i]);
                        writer.name("ask").value(askValues[i]);
                        writer.name("qty").value(quantityValues[i]);
                        writer.name("time").value(TIME_BASE + i * 37L);
                        break;
                    default:
                        writer.name("id").value(symbols[i % symbols.length]);
                        writer.name("name").value(names[i]);
                        writer.name("description").value(descriptions[i]);
                        break;
                }
                writer.endObject();
            }
            writer.endArray();
        }
        return output.getBuffer().length();
    }
}
