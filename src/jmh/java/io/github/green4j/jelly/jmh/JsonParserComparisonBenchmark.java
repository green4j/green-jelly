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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.google.gson.stream.JsonReader;
import io.github.green4j.jelly.CopyingStringBuilder;
import io.github.green4j.jelly.JsonParser;
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

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;

/**
 * Compares the streaming parsers. Every parser gets every value of the document: the length of each member name,
 * the unescaped chars of each string, the value of each number and boolean, and all of them are summed up.
 *
 * <p>Each library is used the cheapest way its API allows: green-jelly unescapes the strings to a reused buffer
 * with {@link CopyingStringBuilder}, Jackson gives the chars of a string from its buffer, while Gson can give
 * a new {@code String} only. A number is given by green-jelly as a mantissa and an exponent, while Jackson
 * and Gson parse a decimal to a {@code double}, so they do more work on the DECIMALS document.
 * Jackson needs a new parser for a document and Gson a new reader, both are created in the measured code,
 * since a real application has to do that as well.
 *
 * <p>Run with:
 * <pre>
 *   ./gradlew jmh -PjmhArgs="JsonParserComparisonBenchmark"
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class JsonParserComparisonBenchmark {

    @Param({"NUMBERS", "DECIMALS", "STRINGS"})
    BenchmarkData.Document document;

    private final JsonFactory jacksonFactory = new JsonFactory();

    private final SumListener listener = new SumListener();

    private JsonParser parser;
    private String json;

    @Setup
    public void setup() {
        json = document.json();
        parser = new JsonParser(new CopyingStringBuilder());
        parser.setListener(listener);
    }

    @Benchmark
    public long greenJelly() {
        parser.parse(json);
        parser.eoj();
        return listener.sum;
    }

    @Benchmark
    public long jackson() throws IOException {
        long sum = 0;
        try (com.fasterxml.jackson.core.JsonParser jackson = jacksonFactory.createParser(json)) {
            JsonToken token;
            while ((token = jackson.nextToken()) != null) {
                switch (token) {
                    case FIELD_NAME:
                        sum += jackson.getTextLength();
                        break;
                    case VALUE_STRING: {
                        final char[] chars = jackson.getTextCharacters();
                        final int length = jackson.getTextLength();
                        sum += length;
                        if (length > 0) {
                            sum += chars[jackson.getTextOffset() + length - 1];
                        }
                        break;
                    }
                    case VALUE_NUMBER_INT:
                        sum += jackson.getLongValue();
                        break;
                    case VALUE_NUMBER_FLOAT:
                        sum += (long) jackson.getDoubleValue();
                        break;
                    case VALUE_TRUE:
                        sum++;
                        break;
                    default:
                        break;
                }
            }
        }
        return sum;
    }

    @Benchmark
    public long gson() throws IOException {
        long sum = 0;
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            while (true) {
                switch (reader.peek()) {
                    case BEGIN_ARRAY:
                        reader.beginArray();
                        break;
                    case END_ARRAY:
                        reader.endArray();
                        break;
                    case BEGIN_OBJECT:
                        reader.beginObject();
                        break;
                    case END_OBJECT:
                        reader.endObject();
                        break;
                    case NAME:
                        sum += reader.nextName().length();
                        break;
                    case STRING: {
                        final String value = reader.nextString();
                        final int length = value.length();
                        sum += length;
                        if (length > 0) {
                            sum += value.charAt(length - 1);
                        }
                        break;
                    }
                    case NUMBER:
                        sum += (long) reader.nextDouble(); // nextLong() fails on a decimal
                        break;
                    case BOOLEAN:
                        if (reader.nextBoolean()) {
                            sum++;
                        }
                        break;
                    case NULL:
                        reader.nextNull();
                        break;
                    default: // END_DOCUMENT
                        return sum;
                }
            }
        }
    }
}
