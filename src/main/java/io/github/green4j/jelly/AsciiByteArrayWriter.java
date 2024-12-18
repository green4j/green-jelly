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

import java.nio.charset.StandardCharsets;

public final class AsciiByteArrayWriter implements ClearableByteArrayBufferingWriter {
    private final Frame frame = new Frame() {
        @Override
        public void setCharAt(final int index, final char c) {
            array[frameStart + index] = (byte) c;
        }

        @Override
        public char charAt(final int index) {
            return (char) array[frameStart + index];
        }

        @Override
        public CharSequence subSequence(final int start, final int end) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int length() {
            return frameSize;
        }

        @Override
        public String toString() {
            return new String(array, frameStart, frameSize);
        }
    };

    private byte[] array;
    private int start;
    private int length;

    private int frameStart;
    private int frameSize;

    public AsciiByteArrayWriter() {
    }

    public AsciiByteArrayWriter(final int initialSize) {
        this(new byte[initialSize]);
    }

    public AsciiByteArrayWriter(final byte[] array) {
        this(array, 0);
    }

    public AsciiByteArrayWriter(final byte[] array, final int start) {
        set(array, start);
    }

    public void set(final byte[] array) {
        set(array, 0);
    }

    public void set(final byte[] array, final int start) {
        assert array.length > start;

        this.array = array;
        this.start = start;
        this.length = 0;
    }

    @Override
    public byte[] array() {
        return array;
    }

    @Override
    public int start() {
        return start;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public void clear() {
        length = 0;
    }

    /**
     * Returns a Frame of required size starting from the current position.
     * The Frame supports only single byte-length characters.
     * @param size of the buffer to pre-allocate
     * @return a Frame
     */
    @Override
    public Frame append(final int size) {
        assert size > 0;

        makeSureRoomSize(size);
        frameStart = start + length;
        frameSize = size;
        length += size;
        return frame;
    }

    @Override
    public void append(final char c) {
        assert array != null;

        int charIndex = start + length;

        makeSureRoomSize(6);

        if (c < 0x80) {
            length++;
            array[charIndex] = (byte) c;
            return;
        }

        length += 6;
        array[charIndex++] = (byte) '\\';
        array[charIndex++] = (byte) 'u';

        if (c < 0x800) { // 2 bytes
            array[charIndex++] = (byte) '0';
            array[charIndex++] = (byte) '0';
            array[charIndex++] = (byte) JsonGenerator.HEX_DIGITS[c >>> 4 & 0x000f];
            array[charIndex] = (byte) JsonGenerator.HEX_DIGITS[c & 0x000f];
            return;
        }
        if (c < 0x8000) { // 3 bytes
            array[charIndex++] = (byte) '0';
            array[charIndex++] = (byte) JsonGenerator.HEX_DIGITS[c >>> 8 & 0x000f];
            array[charIndex++] = (byte) JsonGenerator.HEX_DIGITS[c >>> 4 & 0x000f];
            array[charIndex] = (byte) JsonGenerator.HEX_DIGITS[c & 0x000f];
            return;
        }
        // 4 bytes
        array[charIndex++] = (byte) JsonGenerator.HEX_DIGITS[c >>> 12 & 0x000f];
        array[charIndex++] = (byte) JsonGenerator.HEX_DIGITS[c >>> 8 & 0x000f];
        array[charIndex++] = (byte) JsonGenerator.HEX_DIGITS[c >>> 4 & 0x000f];
        array[charIndex] = (byte) JsonGenerator.HEX_DIGITS[c & 0x000f];
    }

    @Override
    public void append(final CharSequence data) {
        if (data == null) {
            appendNull();
            return;
        }
        append(data, 0, data.length());
    }

    @Override
    public void append(final CharSequence data, final int start, final int len) {
        for (int i = 0; i < len; i++) {
            append(data.charAt(start + i));
        }
    }

    @Override
    public void flush() {
    }

    private void appendNull() {
        makeSureRoomSize(4);
        int charIndex = start + length;
        array[charIndex++] = 'n';
        array[charIndex++] = 'u';
        array[charIndex++] = 'l';
        array[charIndex] = 'l';
        length += 4;
    }

    private void makeSureRoomSize(final int roomSize) {
        final int delta = array.length - (start + length + roomSize);
        if (delta < 0) {
            final int newSize = Math.max(roomSize, array.length - start) << 1;
            final byte[] newArray = new byte[newSize];
            System.arraycopy(array, start, newArray, 0, length);
            array = newArray;
            start = 0;
        }
    }

    @Override
    public String toString() {
        if (array == null) {
            return "null";
        }
        return new String(array, start, length, StandardCharsets.US_ASCII);
    }
}
