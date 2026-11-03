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
import io.github.green4j.jelly.CopyingStringBuilder;
import io.github.green4j.jelly.JsonParser;
import io.github.green4j.jelly.Utf8JsonParser;
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Measures how fast a whole document, received as UTF-8 bytes, is parsed:
 * <ul>
 * <li>{@code STRING} - the bytes are decoded to a new {@link String}, which {@link JsonParser} parses;</li>
 * <li>{@code DECODER} - the bytes are decoded with a {@link CharsetDecoder} to a reused array of chars,
 * which {@link JsonParser} parses: the way to get chars from bytes with no garbage;</li>
 * <li>{@code UTF8_ARRAY}, {@code UTF8_DIRECT} - {@link Utf8JsonParser} parses the bytes of an array
 * or of a direct buffer as they are.</li>
 * </ul>
 * The string values are copied and unescaped in all the cases.
 *
 * <p>Run with:
 * <pre>
 *   ./gradlew jmh -PjmhArgs="Utf8JsonParserBenchmark"
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class Utf8JsonParserBenchmark {

    public enum Input {
        STRING, DECODER, UTF8_ARRAY, UTF8_DIRECT
    }

    @Param
    BenchmarkData.Document document;

    @Param
    Input input;

    private final SumListener listener = new SumListener();

    private JsonParser parser;
    private Utf8JsonParser utf8Parser;

    private byte[] bytes;
    private ByteBuffer directBytes;

    private ByteBuffer decoderInput;
    private CharBuffer decoderOutput;
    private CharArrayCharSequence decoded;
    private CharsetDecoder decoder;

    @Setup
    public void setup() {
        bytes = document.json().getBytes(StandardCharsets.UTF_8);
        directBytes = ByteBuffer.allocateDirect(bytes.length);
        directBytes.put(bytes).flip();

        parser = new JsonParser(new CopyingStringBuilder()).setListener(listener);
        utf8Parser = new Utf8JsonParser(new CopyingStringBuilder()).setListener(listener);

        decoderInput = ByteBuffer.wrap(bytes);
        decoded = new CharArrayCharSequence(bytes.length);
        decoderOutput = CharBuffer.wrap(decoded.array());
        decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    @Benchmark
    public long parse() {
        switch (input) {
            case STRING:
                parser.parse(new String(bytes, StandardCharsets.UTF_8));
                parser.eoj();
                break;
            case DECODER:
                decoderInput.clear();
                decoderOutput.clear();
                decoder.reset();
                decoder.decode(decoderInput, decoderOutput, true);
                decoder.flush(decoderOutput);
                decoded.setLength(decoderOutput.position());
                parser.parse(decoded);
                parser.eoj();
                break;
            case UTF8_ARRAY:
                utf8Parser.parse(bytes);
                utf8Parser.eoj();
                break;
            default:
                utf8Parser.parse(directBytes);
                utf8Parser.eoj();
                break;
        }
        return listener.sum;
    }
}
