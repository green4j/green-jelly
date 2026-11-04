/**
 * MIT License
 *
 * Copyright (c) 2018-2026 Anatoly Gudkov and others.
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

public class Utf8ByteArrayWriter implements ClearableByteArrayBufferingWriter {
    private final Frame frame = new Frame() {
        @Override
        public void setCharAt(final int index, final char c) {
            array[frameStart + index] = (byte) c;
        }

        @Override
        public char charAt(final int index) {
            throw new UnsupportedOperationException();
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

    public Utf8ByteArrayWriter() {
    }

    public Utf8ByteArrayWriter(final int initialSize) {
        this(new byte[initialSize]);
    }

    public Utf8ByteArrayWriter(final byte[] array) {
        this(array, 0);
    }

    public Utf8ByteArrayWriter(final byte[] array, final int start) {
        set(array, start);
    }

    public final void set(final byte[] array) {
        set(array, 0);
    }

    public final void set(final byte[] array, final int start) {
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

        makeSureRoomSize(3);

        int charIndex = start + length;

        if (c < 0x80) {
            length++;
            array[charIndex] = (byte) c;
            return;
        }

        if (c < 0x800) {
            length += 2;
            array[charIndex++] = (byte) (0xc0 | (c >> 6));
            array[charIndex] = (byte) (0x80 | (c & 0x3f));
            return;
        }

        // a single char of a surrogate pair is written as is, 3 bytes;
        // a pair passed as a whole by the append() of a sequence of chars is written as one code point, 4 bytes
        length += 3;
        array[charIndex++] = (byte) (0xe0 | ((c >> 12)));
        array[charIndex++] = (byte) (0x80 | ((c >> 6) & 0x3f));
        array[charIndex] = (byte) (0x80 | (c & 0x3f));
    }

    private void appendSurrogatePair(final char high, final char low) {
        makeSureRoomSize(4);

        final int codePoint = Character.toCodePoint(high, low);
        int charIndex = start + length;
        length += 4;
        array[charIndex++] = (byte) (0xf0 | (codePoint >> 18));
        array[charIndex++] = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
        array[charIndex++] = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
        array[charIndex] = (byte) (0x80 | (codePoint & 0x3f));
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
        final int plain = data instanceof String
                ? appendPlain((String) data, start, len)
                : appendPlain(data, start, len);
        for (int i = plain; i < len; i++) {
            final char c = data.charAt(start + i);
            if (Character.isHighSurrogate(c) && i + 1 < len) {
                final char low = data.charAt(start + i + 1);
                if (Character.isLowSurrogate(low)) {
                    appendSurrogatePair(c, low);
                    i++;
                    continue;
                }
            }
            append(c);
        }
    }

    @Override
    public void append(final char[] data, final int start, final int len) {
        makeSureRoomSize(len);
        final byte[] bytes = array;
        final int to = this.start + length;
        int plain = 0;
        while (plain < len) {
            final char c = data[start + plain];
            if (c >= 0x80) {
                break;
            }
            bytes[to + plain] = (byte) c;
            plain++;
        }
        length += plain;
        for (int i = plain; i < len; i++) {
            final char c = data[start + i];
            if (Character.isHighSurrogate(c) && i + 1 < len) {
                final char low = data[start + i + 1];
                if (Character.isLowSurrogate(low)) {
                    appendSurrogatePair(c, low);
                    i++;
                    continue;
                }
            }
            append(c);
        }
    }

    // A String has a call site of its own: unlike CharSequence.charAt(), which is a real
    // virtual call in a process using several CharSequences, String.charAt() is always inlined.
    private int appendPlain(final String data, final int start, final int len) {
        makeSureRoomSize(len);
        final byte[] bytes = array;
        final int to = this.start + length;
        int plain = 0;
        while (plain < len) {
            final char c = data.charAt(start + plain);
            if (c >= 0x80) {
                break;
            }
            bytes[to + plain] = (byte) c;
            plain++;
        }
        length += plain;
        return plain;
    }

    private int appendPlain(final CharSequence data, final int start, final int len) {
        makeSureRoomSize(len);
        final byte[] bytes = array;
        final int to = this.start + length;
        int plain = 0;
        while (plain < len) {
            final char c = data.charAt(start + plain);
            if (c >= 0x80) {
                break;
            }
            bytes[to + plain] = (byte) c;
            plain++;
        }
        length += plain;
        return plain;
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
        return new String(array, start, length, StandardCharsets.UTF_8);
    }
}
