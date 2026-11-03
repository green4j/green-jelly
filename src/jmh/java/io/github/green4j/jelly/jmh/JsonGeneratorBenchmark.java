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

import io.github.green4j.jelly.JsonGenerator;
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
 * Measures how fast a document of 40 records is generated with every writer.
 *
 * <p>All the writers are run in the setup, so {@link JsonGenerator} sees several {@code BufferingWriter}
 * implementations, like it does in a process which uses more than one of them. Set {@code mixedWriters} to
 * {@code false} to see the case of a single implementation.
 *
 * <p>Run with:
 * <pre>
 *   ./gradlew jmh -PjmhArgs="JsonGeneratorBenchmark"
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class JsonGeneratorBenchmark {

    private static final int POLLUTION_ROUNDS = 10_000;

    @Param
    GeneratorFixture.Writer writer;

    @Param
    GeneratorFixture.Shape shape;

    @Param({"false", "true"})
    boolean indent;

    @Param({"true", "false"})
    boolean mixedWriters;

    private final GeneratorFixture fixture = new GeneratorFixture();

    private JsonGenerator generator;

    @Setup
    public void setup() {
        generator = new JsonGenerator(indent);
        if (mixedWriters) {
            final GeneratorFixture.Writer[] writers = GeneratorFixture.Writer.values();
            final GeneratorFixture.Shape[] shapes = GeneratorFixture.Shape.values();
            for (int i = 0; i < POLLUTION_ROUNDS; i++) {
                fixture.clear();
                generator.setOutput(fixture.writer(writers[i % writers.length]));
                fixture.generate(generator, shapes[i % shapes.length]);
            }
        }
        generator.setOutput(fixture.writer(writer));
    }

    @Benchmark
    public JsonGenerator generate() {
        fixture.clear();
        fixture.generate(generator, shape);
        return generator;
    }
}
