package io.github.green4j.jelly;

import java.nio.charset.StandardCharsets;

public class Utf8ByteArrayWriter implements JsonBufferedWriter {

    private final Frame frame = new Frame() {
        @Override
        public void setCharAt(final int index, final char c) {
            assert c > 0x1f // all chars <= 0x1f are expected to be encoded to \\uXXXX by JsonGenerator already
                    && c < 0x80; // only ASCII supported by the Frame

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

    public void set(final byte[] array) {
        set(array, 0);
    }

    public void set(final byte[] array, final int start) {
        assert array.length > start;

        this.array = array;
        this.start = start;
        this.length = 0;
    }

    public byte[] array() {
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

        assert c > 0x1f; // all chars <= 0x1f are expected to be encoded to \\uXXXX by JsonGenerator already

        int charIndex = start + length;

        makeSureRoomSize(3);

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

        if (c <= 0xffff) {
            length += 3;
            array[charIndex++] = (byte) (0xe0 | ((c >> 12)));
            array[charIndex++] = (byte) (0x80 | ((c >> 6) & 0x3f));
            array[charIndex] = (byte) (0x80 | (c & 0x3f));
        }

        throw new IllegalArgumentException("Char with the code " + c + " exceeds BMP range. Use a surrogate pair");
    }

    @Override
    public void append(final CharSequence data) {
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
