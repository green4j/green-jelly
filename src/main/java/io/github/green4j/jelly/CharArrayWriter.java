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

public class CharArrayWriter implements
        ClearableBufferingWriter,
        CharArray {
    private final Frame frame = new Frame() {
        @Override
        public void setCharAt(final int index, final char c) {
            array[frameStart + index] = c;
        }

        @Override
        public char charAt(final int index) {
            return array[frameStart + index];
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

    private char[] array;
    private int start;
    private int length;

    private int frameStart;
    private int frameSize;

    public CharArrayWriter() {
    }

    public CharArrayWriter(final int initialSize) {
        this(new char[initialSize]);
    }

    public CharArrayWriter(final char[] array) {
        this(array, 0);
    }

    public CharArrayWriter(final char[] array, final int start) {
        set(array, start);
    }

    public final void set(final char[] array) {
        set(array, 0);
    }

    public final void set(final char[] array, final int start) {
        assert array.length > start;

        this.array = array;
        this.start = start;
        this.length = 0;
    }

    @Override
    public char[] array() {
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

        makeSureRoomSize(1);
        array[start + length] = c;
        length++;
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
        assert array != null;

        if (array.length - (this.start + length) < len) {
            appendInParts(data, start, len);
            return;
        }
        final char[] chars = array;
        final int startIndex = this.start + length;
        if (data instanceof String) {
            ((String) data).getChars(start, start + len, chars, startIndex);
        } else if (data instanceof StringBuilder) {
            ((StringBuilder) data).getChars(start, start + len, chars, startIndex);
        } else {
            for (int i = 0; i < len; i++) {
                chars[startIndex + i] = data.charAt(start + i);
            }
        }
        length += len;
    }

    @Override
    public void append(final char[] data, final int start, final int len) {
        assert array != null;

        if (array.length - (this.start + length) < len) {
            appendInParts(data, start, len);
            return;
        }
        final char[] chars = array;
        final int startIndex = this.start + length;
        for (int i = 0; i < len; i++) { // a number mostly, so too short for System.arraycopy() to pay off
            chars[startIndex + i] = data[start + i];
        }
        length += len;
    }

    // The chars which don't fit the room left are written in parts, each of them fitting the room there is
    // (see makeRoomUpTo()), so that a spilling writer doesn't grow for them.
    private void appendInParts(final CharSequence data, final int start, final int len) {
        int done = 0;
        while (done < len) {
            final int part = makeRoomUpTo(len - done);
            append(data, start + done, part);
            done += part;
        }
    }

    private void appendInParts(final char[] data, final int start, final int len) {
        int done = 0;
        while (done < len) {
            final int part = makeRoomUpTo(len - done);
            append(data, start + done, part);
            done += part;
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

    /**
     * Called when the array has no room for the next chars, before it grows. An override may take what has been
     * written - {@link #array()} from {@link #start()}, {@link #length()} chars - elsewhere and {@link #clear()}
     * the writer, returning true: the writing then goes on in the same array. Strings and numbers are written in
     * parts as long as the room there is, so the array grows only if a frame from {@link #append(int)} needs more
     * than it holds. A frame is filled before anything else is appended, so it is never taken half-written.
     * <p>
     * It is never called while nothing has been written. If it returns true, but there is still no room, e.g.
     * since the writer hasn't been cleared, the array grows. It must not append to this writer.
     *
     * @param roomSize the chars the next write needs; for a string or a number written in parts, all the rest of
     *                 it, which may be more than the array holds
     * @return true if the chars written have been taken and the writer cleared; false to grow the array
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
        final char[] newArray = new char[newSize];
        System.arraycopy(array, start, newArray, 0, length);
        array = newArray;
        start = 0;
    }

    @Override
    public String toString() {
        if (array == null) {
            return "null";
        }
        return new String(array, start, length);
    }
}
