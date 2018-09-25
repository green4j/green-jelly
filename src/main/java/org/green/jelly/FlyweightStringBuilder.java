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

public final class FlyweightStringBuilder implements JsonStringBuilder {

    private CharSequence buffer;
    private int start;
    private int len;

    public FlyweightStringBuilder() {
    }

    public CharSequence buffer() {
        return buffer;
    }

    public int start() {
        return start;
    }

    @Override
    public int length() {
        return len;
    }

    @Override
    public char charAt(final int index) {
        return buffer.charAt(start + index);
    }

    @Override
    public void start(final CharSequence data, final int position) {
        buffer = data;
        start = position + 1;
        len = 0;
    }

    @Override
    public void append(final CharSequence data, final int position, final char c) {
        appendChar(data);
    }

    @Override
    public void appendEscape(final CharSequence data, final int position) {
        appendChar(data);
    }

    @Override
    public void appendEscapedQuotationMark(final CharSequence data, final int position) {
        appendChar(data);
    }

    @Override
    public void appendEscapedReverseSolidus(final CharSequence data, final int position) {
        appendChar(data);
    }

    @Override
    public void appendEscapedSolidus(final CharSequence data, final int position) {
        appendChar(data);
    }

    @Override
    public void appendEscapedBackspace(final CharSequence data, final int position) {
        appendChar(data);
    }

    @Override
    public void appendEscapedFormfeed(final CharSequence data, final int position) {
        appendChar(data);
    }

    @Override
    public void appendEscapedNewLine(final CharSequence data, final int position) {
        appendChar(data);
    }

    @Override
    public void appendEscapedCarriageReturn(final CharSequence data, final int position) {
        appendChar(data);
    }

    @Override
    public void appendEscapedHorisontalTab(final CharSequence data, final int position) {
        appendChar(data);
    }

    @Override
    public void appendEscapedUnicodeU(final CharSequence data, final int position) {
        appendChar(data);
    }

    @Override
    public boolean appendEscapedUnicodeChar1(final CharSequence data, final int position, final char c) {
        return appendUnicodeChar(data, c);
    }

    @Override
    public boolean appendEscapedUnicodeChar2(final CharSequence data, final int position, final char c) {
        return appendUnicodeChar(data, c);
    }

    @Override
    public boolean appendEscapedUnicodeChar3(final CharSequence data, final int position, final char c) {
        return appendUnicodeChar(data, c);
    }

    @Override
    public boolean appendEscapedUnicodeChar4(final CharSequence data, final int position, final char c) {
        return appendUnicodeChar(data, c);
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        if (buffer == null) {
            return "null";
        }
        if (len == 0) {
            return "";
        }
        return new StringBuilder().append(buffer, start, start + len).toString();
    }

    private void appendChar(final CharSequence data) {
        if (buffer != data) {
            throw new IllegalArgumentException("Buffer cannot be changed after start of a string");
        }
        len++;
    }

    private boolean appendUnicodeChar(final CharSequence data, final char c) {
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
                appendChar(data);
                return true;
            default:
                break;
        }
        return false;
    }
}
