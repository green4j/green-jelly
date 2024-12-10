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

public interface JsonBufferedWriter {
    /**
     * Frame is a pre-allocated part of a buffer.
     * The Frame provides ability of writing by index within a some limited space.
     * This concept is used, for example, for number formatting, when the formatter
     * starts its writing from low (the right side of a result string) to high
     * (the left side of a result string) decimal positions of a number.
     */
    interface Frame extends CharSequence {
        void setCharAt(int index, char c);
    }

    /**
     * Pre-allocates buffer of required size in the current position of writing.
     * @param size of the buffer to pre-allocate
     * @return a buffer
     */
    Frame append(int size);

    void append(char c);

    void append(CharSequence data);

    void append(CharSequence data, int start, int len);

    void flush();

}
