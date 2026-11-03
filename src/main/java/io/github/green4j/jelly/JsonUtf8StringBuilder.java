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
package io.github.green4j.jelly;

import java.nio.ByteBuffer;

/**
 * The same as {@link JsonStringBuilder}, but for a JSON message encoded in UTF-8,
 * which {@link Utf8JsonParser} parses. The builder decodes the bytes of a string value
 * to chars. A char encoded with several bytes may be cut by the end of a part of the message,
 * so the builder keeps the bytes of the char, which have been read, until the rest of them comes.
 * @see CopyingStringBuilder
 */
public interface JsonUtf8StringBuilder extends CharSequence {
    /**
     * Notifies the builder that a new string value is going to be parsed.
     */
    void start();

    /**
     * Appends bytes to the current string value. The bytes may end with
     * a part of a char, the rest of the char comes with the next call.
     *
     * @param data - bytes of JSON message
     * @param start - index of the first byte
     * @param len - number of bytes to be append
     * @return the number of the bytes appended. Less than {@code len} only if the bytes
     *         are malformed and the builder doesn't replace malformed bytes;
     *         the malformed sequence is detected at the byte with this offset
     */
    int append(byte[] data, int start, int len);

    /**
     * The same as {@link #append(byte[], int, int)}, the bytes are read at the indexes given,
     * neither the position nor the limit of the buffer is used.
     *
     * @param data - bytes of JSON message
     * @param start - index of the first byte
     * @param len - number of bytes to be append
     * @return the number of the bytes appended
     */
    int append(ByteBuffer data, int start, int len);

    /**
     * The same as {@link #append(byte[], int, int)}.
     *
     * @param data - bytes of JSON message
     * @param start - index of the first byte
     * @param len - number of bytes to be append
     * @return the number of the bytes appended
     */
    int append(ByteSequence data, int start, int len);

    /**
     * Notifies the builder that the bytes appended are followed by a quote or an escape.
     *
     * @return false only if the bytes appended end with a part of a char and
     *         the builder doesn't replace malformed bytes
     */
    boolean complete();

    /**
     * Appends '\' char to the current string value.
     */
    void appendEscape();

    void appendEscapedQuotationMark();

    void appendEscapedReverseSolidus();

    void appendEscapedSolidus();

    void appendEscapedBackspace();

    void appendEscapedFormfeed();

    void appendEscapedNewLine();

    void appendEscapedCarriageReturn();

    void appendEscapedHorisontalTab();

    void appendEscapedUnicodeU();

    boolean appendEscapedUnicodeChar1(char c);

    boolean appendEscapedUnicodeChar2(char c);

    boolean appendEscapedUnicodeChar3(char c);

    boolean appendEscapedUnicodeChar4(char c);
}
