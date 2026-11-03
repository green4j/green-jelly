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

import io.github.green4j.jelly.CharArrayCharSequence;
import io.github.green4j.jelly.FlyweightStringBuilder;
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

import java.util.concurrent.TimeUnit;

/**
 * Measures the parsing when an application feeds the parser with different {@link CharSequence}
 * implementations. {@link JsonParserBenchmark} gives the parser only strings, so the JIT sees a single
 * receiver type; a process which mixes the types, as this one deliberately does in the setup, pays for
 * real virtual calls in everything that goes through the interface.
 *
 * <p>Run with:
 * <pre>
 *   ./gradlew jmh -PjmhArgs="JsonParserInputBenchmark"
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class JsonParserInputBenchmark {

    private static final int POLLUTION_ROUNDS = 20_000;

    public enum Input {
        STRING, STRING_BUILDER, CHAR_ARRAY
    }

    @Param({"DECIMALS", "STRINGS"})
    BenchmarkData.Document document;

    @Param
    Input input;

    private final SumListener listener = new SumListener();

    private JsonParser parser;
    private CharSequence json;

    @Setup
    public void setup() {
        parser = new JsonParser(new FlyweightStringBuilder());
        parser.setListener(listener);

        final CharSequence[] inputs = new CharSequence[Input.values().length];
        for (final Input i : Input.values()) {
            inputs[i.ordinal()] = create(i, document.json());
        }
        for (int i = 0; i < POLLUTION_ROUNDS; i++) {
            parser.parse(inputs[i % inputs.length]);
            parser.eoj();
        }
        json = inputs[input.ordinal()];
    }

    @Benchmark
    public long parse() {
        parser.parse(json);
        parser.eoj();
        return listener.sum;
    }

    private static CharSequence create(final Input input, final String json) {
        switch (input) {
            case STRING:
                return json;
            case STRING_BUILDER:
                return new StringBuilder(json);
            default:
                return new CharArrayCharSequence(json.toCharArray(), json.length());
        }
    }
}
