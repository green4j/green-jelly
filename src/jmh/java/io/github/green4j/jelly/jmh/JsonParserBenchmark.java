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

import io.github.green4j.jelly.CopyingStringBuilder;
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
 * Measures how fast a whole document, given as a {@link String}, is parsed.
 *
 * <p>Run with:
 * <pre>
 *   ./gradlew jmh -PjmhArgs="JsonParserBenchmark"
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class JsonParserBenchmark {

    @Param
    BenchmarkData.Document document;

    @Param({"flyweight", "copying"})
    String stringBuilder;

    private final SumListener listener = new SumListener();

    private JsonParser parser;
    private String json;

    @Setup
    public void setup() {
        json = document.json();
        parser = new JsonParser("flyweight".equals(stringBuilder)
                ? new FlyweightStringBuilder() : new CopyingStringBuilder());
        parser.setListener(listener);
    }

    @Benchmark
    public long parse() {
        parser.parse(json);
        parser.eoj();
        return listener.sum;
    }
}
