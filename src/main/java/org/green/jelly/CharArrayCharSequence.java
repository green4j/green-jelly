package org.green.jelly;

public class CharArrayCharSequence implements CharSequence {
    private final char[] chars;
    private int length;

    public CharArrayCharSequence(final int size) {
        this.chars = new char[size];
    }

    public CharArrayCharSequence(final char[] chars, final int length) {
        this.chars = chars;
        this.length = length;
    }

    public char[] getChars() {
        return chars;
    }

    public void setLength(final int length) {
        this.length = length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(final int index) {
        if (index >= length) {
            throw new ArrayIndexOutOfBoundsException(index + " with length: " +length);
        }
        return chars[index];
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
