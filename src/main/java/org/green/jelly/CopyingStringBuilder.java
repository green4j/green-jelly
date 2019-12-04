/**
 * MIT License
 *
 * Copyright (c) 2018 Anatoly Gudkov
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
package org.green.jelly;

public final class CopyingStringBuilder implements JsonStringBuilder {

    private final StringBuilder buffer = new StringBuilder();
    private final boolean rawString;

    private int escapedUnicodeChar;

    public CopyingStringBuilder() {
        this(false);
    }

    public CopyingStringBuilder(final boolean rawString) {
        this.rawString = rawString;
    }

    @Override
    public int length() {
        return buffer.length();
    }

    @Override
    public char charAt(final int index) {
        return buffer.charAt(index);
    }

    @Override
    public void start(final CharSequence data, final int position) {
        buffer.setLength(0);
    }

    @Override
    public void append(final CharSequence data, final int start, final int len) {
        buffer.append(data, start, start + len);
    }

    @Override
    public void appendEscape() {
        if (rawString) {
            buffer.append('\\');
        }
    }

    @Override
    public void appendEscapedQuotationMark() {
        buffer.append('"');
    }

    @Override
    public void appendEscapedReverseSolidus() {
        buffer.append('\\');
    }

    @Override
    public void appendEscapedSolidus() {
        buffer.append('/');
    }

    @Override
    public void appendEscapedBackspace() {
        if (rawString) {
            buffer.append('b');
        } else {
            buffer.append((char) 0x08);
        }
    }

    @Override
    public void appendEscapedFormfeed() {
        if (rawString) {
            buffer.append('f');
        } else {
            buffer.append((char) 0x0c);
        }
    }

    @Override
    public void appendEscapedNewLine() {
        if (rawString) {
            buffer.append('n');
        } else {
            buffer.append((char) 0x0a);
        }
    }

    @Override
    public void appendEscapedCarriageReturn() {
        if (rawString) {
            buffer.append('r');
        } else {
            buffer.append((char) 0x0d);
        }
    }

    @Override
    public void appendEscapedHorisontalTab() {
        if (rawString) {
            buffer.append('t');
        } else {
            buffer.append((char) 0x09);
        }
    }

    @Override
    public void appendEscapedUnicodeU() {
        if (rawString) {
            buffer.append('u');
        }
    }

    @Override
    public boolean appendEscapedUnicodeChar1(final char c) {
        if (rawString) {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                    buffer.append(c);
                    break;
                default:
                    return false;
            }
        } else {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    escapedUnicodeChar = 4096 * (c - '0');
                    break;
                case 'A':
                case 'a':
                    escapedUnicodeChar = 4096 * 10;
                    break;
                case 'B':
                case 'b':
                    escapedUnicodeChar = 4096 * 11;
                    break;
                case 'C':
                case 'c':
                    escapedUnicodeChar = 4096 * 12;
                    break;
                case 'D':
                case 'd':
                    escapedUnicodeChar = 4096 * 13;
                    break;
                case 'E':
                case 'e':
                    escapedUnicodeChar = 4096 * 14;
                    break;
                case 'F':
                case 'f':
                    escapedUnicodeChar = 4096 * 15;
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean appendEscapedUnicodeChar2(final char c) {
        if (rawString) {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                    buffer.append(c);
                    break;
                default:
                    return false;
            }
        } else {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    escapedUnicodeChar = escapedUnicodeChar + 256 * (c - '0');
                    break;
                case 'A':
                case 'a':
                    escapedUnicodeChar = escapedUnicodeChar + 256 * 10;
                    break;
                case 'B':
                case 'b':
                    escapedUnicodeChar = escapedUnicodeChar + 256 * 11;
                    break;
                case 'C':
                case 'c':
                    escapedUnicodeChar = escapedUnicodeChar + 256 * 12;
                    break;
                case 'D':
                case 'd':
                    escapedUnicodeChar = escapedUnicodeChar + 256 * 13;
                    break;
                case 'E':
                case 'e':
                    escapedUnicodeChar = escapedUnicodeChar + 256 * 14;
                    break;
                case 'F':
                case 'f':
                    escapedUnicodeChar = escapedUnicodeChar + 256 * 15;
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean appendEscapedUnicodeChar3(final char c) {
        if (rawString) {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                    buffer.append(c);
                    break;
                default:
                    return false;
            }
        } else {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    escapedUnicodeChar = escapedUnicodeChar + 16 * (c - '0');
                    break;
                case 'A':
                case 'a':
                    escapedUnicodeChar = escapedUnicodeChar + 16 * 10;
                    break;
                case 'B':
                case 'b':
                    escapedUnicodeChar = escapedUnicodeChar + 16 * 11;
                    break;
                case 'C':
                case 'c':
                    escapedUnicodeChar = escapedUnicodeChar + 16 * 12;
                    break;
                case 'D':
                case 'd':
                    escapedUnicodeChar = escapedUnicodeChar + 16 * 13;
                    break;
                case 'E':
                case 'e':
                    escapedUnicodeChar = escapedUnicodeChar + 16 * 14;
                    break;
                case 'F':
                case 'f':
                    escapedUnicodeChar = escapedUnicodeChar + 16 * 15;
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean appendEscapedUnicodeChar4(final char c) {
        if (rawString) {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                    buffer.append(c);
                    break;
                default:
                    return false;
            }
        } else {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    escapedUnicodeChar = escapedUnicodeChar + 1 * (c - '0');
                    break;
                case 'A':
                case 'a':
                    escapedUnicodeChar = escapedUnicodeChar + 1 * 10;
                    break;
                case 'B':
                case 'b':
                    escapedUnicodeChar = escapedUnicodeChar + 1 * 11;
                    break;
                case 'C':
                case 'c':
                    escapedUnicodeChar = escapedUnicodeChar + 1 * 12;
                    break;
                case 'D':
                case 'd':
                    escapedUnicodeChar = escapedUnicodeChar + 1 * 13;
                    break;
                case 'E':
                case 'e':
                    escapedUnicodeChar = escapedUnicodeChar + 1 * 14;
                    break;
                case 'F':
                case 'f':
                    escapedUnicodeChar = escapedUnicodeChar + 1 * 15;
                    break;
                default:
                    return false;
            }
            buffer.append((char) escapedUnicodeChar);
        }
        return true;
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return buffer.subSequence(start, end);
    }

    @Override
    public String toString() {
        return buffer.toString();
    }
}
