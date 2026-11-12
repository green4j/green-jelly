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

public class AsciiByteArrayWriter implements ClearableByteArrayBufferingWriter {
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

        makeSureRoomSize(6);

        int charIndex = start + length;

        if (c < 0x80) {
            length++;
            array[charIndex] = (byte) c;
            return;
        }

        length += 6;
        array[charIndex++] = (byte) '\\';
        array[charIndex++] = (byte) 'u';
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
        final int plain = data instanceof String
                ? appendPlain((String) data, start, len)
                : appendPlain(data, start, len);
        for (int i = plain; i < len; i++) {
            append(data.charAt(start + i));
        }
    }

    @Override
    public void append(final char[] data, final int start, final int len) {
        final int plain = appendPlain(data, start, len);
        for (int i = plain; i < len; i++) {
            append(data[start + i]);
        }
    }

    // A String has a call site of its own: unlike CharSequence.charAt(), which is a real
    // virtual call in a process using several CharSequences, String.charAt() is always inlined.
    private int appendPlain(final String data, final int start, final int len) {
        if (array.length - (this.start + length) < len) {
            return appendPlainInParts(data, start, len);
        }
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
        if (array.length - (this.start + length) < len) {
            return appendPlainInParts(data, start, len);
        }
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

    private int appendPlain(final char[] data, final int start, final int len) {
        if (array.length - (this.start + length) < len) {
            return appendPlainInParts(data, start, len);
        }
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
        return plain;
    }

    // The chars which don't fit the room left are written in parts, each of them fitting the room there is
    // (see makeRoomUpTo()), so that a spilling writer doesn't grow for them.
    private int appendPlainInParts(final CharSequence data, final int start, final int len) {
        int plain = 0;
        while (plain < len) {
            final int part = makeRoomUpTo(len - plain);
            final int written = data instanceof String
                    ? appendPlain((String) data, start + plain, part)
                    : appendPlain(data, start + plain, part);
            plain += written;
            if (written < part) {
                break;
            }
        }
        return plain;
    }

    private int appendPlainInParts(final char[] data, final int start, final int len) {
        int plain = 0;
        while (plain < len) {
            final int part = makeRoomUpTo(len - plain);
            final int written = appendPlain(data, start + plain, part);
            plain += written;
            if (written < part) {
                break;
            }
        }
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

    /**
     * Called when the array has no room for the next bytes, before it grows. An override may take what has been
     * written - {@link #array()} from {@link #start()}, {@link #length()} bytes - elsewhere and {@link #clear()}
     * the writer, returning true: the writing then goes on in the same array. Strings and numbers are written in
     * parts as long as the room there is, so the array grows only if a single char or a frame from
     * {@link #append(int)} needs more than it holds. A frame is filled before anything else is appended, so it is
     * never taken half-written.
     * <p>
     * It is never called while nothing has been written. If it returns true, but there is still no room, e.g.
     * since the writer hasn't been cleared, the array grows. It must not append to this writer.
     *
     * @param roomSize the bytes the next write needs; for a string or a number written in parts, all the rest of
     *                 it, which may be more than the array holds
     * @return true if the bytes written have been taken and the writer cleared; false to grow the array
     */
    protected boolean spill(final int roomSize) {
        return false;
    }

    private void makeSureRoomSize(final int roomSize) {
        if (array.length - (start + length + roomSize) >= 0
                || (length > 0 && spill(roomSize) && array.length - (start + length + roomSize) >= 0)) {
            return;
        }
        grow(roomSize);
    }

    // The room for the next part of a string or a number: what is left in the array, or in it after a spill,
    // which is at least one and at most wanted; or wanted, once the array has grown.
    private int makeRoomUpTo(final int wanted) {
        int room = array.length - (start + length);
        if (room <= 0 && length > 0 && spill(wanted)) {
            room = array.length - (start + length);
        }
        if (room > 0) {
            return Math.min(room, wanted);
        }
        grow(wanted);
        return wanted;
    }

    private void grow(final int roomSize) {
        final int newSize = Capacity.next(array.length - start, (long) length + roomSize);
        final byte[] newArray = new byte[newSize];
        System.arraycopy(array, start, newArray, 0, length);
        array = newArray;
        start = 0;
    }

    @Override
    public String toString() {
        if (array == null) {
            return "null";
        }
        return new String(array, start, length, StandardCharsets.US_ASCII);
    }
}
