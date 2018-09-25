package org.green.jelly;

import java.io.IOException;

public final class AppendableWriter<T extends Appendable> implements JsonBufferedWriter {

    private final Frame frame = new Frame() {
        @Override
        public void setCharAt(final int index, final char c) {
            frameArray[index] = c;
        }

        @Override
        public char charAt(final int index) {
            return frameArray[index];
        }

        @Override
        public CharSequence subSequence(final int start, final int end) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public int length() {
            return Math.abs(frameSize);
        }
    };

    private char[] frameArray = new char[32];
    private int frameSize;

    private T output;

    public AppendableWriter() {
    }

    public AppendableWriter(final T output) {
        set(output);
    }

    public void set(final T output) {
        this.output = output;
    }

    public T output() {
        return output;
    }

    @Override
    public Frame append(final int size) {
        assert size > 0;

        try {
            tryPushFrame();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        makeSureFrameSize(size);
        frameSize = -size;
        return frame;
    }

    @Override
    public void append(final char c) {
        assert output != null;

        try {
            tryPushFrame();
            output.append(c);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void append(final CharSequence data) {
        assert output != null;

        try {
            tryPushFrame();
            output.append(data);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void append(final CharSequence data, final int start, final int len) {
        assert output != null;

        try {
            tryPushFrame();
            output.append(data, start, start + len);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void flush() {
        if (output == null) {
            return;
        }
        try {
            tryPushFrame();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void tryPushFrame() throws IOException {
        if (frameSize < 0) {
            output.append(frame);
            frameSize = -frameSize;
        }
    }

    private void makeSureFrameSize(final int roomSize) {
        final int delta = frameArray.length - roomSize;
        if (delta < 0) {
            final int newSize = Math.max(roomSize, frameArray.length) << 1;
            final char[] newArray = new char[newSize];
            frameArray = newArray;
        }
    }

    @Override
    public String toString() {
        if (output == null) {
            return "null";
        }
        return output.toString();
    }
}
