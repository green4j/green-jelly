package org.green.jelly;

public class CharArrayWriter implements JsonBufferedWriter {

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
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public int length() {
            return frameSize;
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

    public void set(final char[] array) {
        set(array, 0);
    }

    public void set(final char[] array, final int start) {
        assert array.length > start;

        this.array = array;
        this.start = start;
        this.length = 0;
    }

    public char[] array() {
        return array;
    }

    public int start() {
        return start;
    }

    public int length() {
        return length;
    }

    public void clear() {
        length = 0;
    }

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
        append(data, 0, data.length());
    }

    @Override
    public void append(final CharSequence data, final int start, final int len) {
        assert array != null;

        makeSureRoomSize(len);
        for (int i = start; i < len; i++) {
            array[start + length] = data.charAt(i);
        }
        length += len;
    }

    @Override
    public void flush() {
    }

    private void makeSureRoomSize(final int roomSize) {
        final int delta = array.length - (start + length + roomSize);
        if (delta < 0) {
            final int newSize = Math.max(roomSize, array.length - start) << 1;
            final char[] newArray = new char[newSize];
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
        return new String(array, start, length);
    }
}
