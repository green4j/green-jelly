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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public final class CopyingStringBuilder implements JsonStringBuilder, JsonUtf8StringBuilder {

    private static final int INITIAL_BUFFER_SIZE = 64;

    private static final char REPLACEMENT_CHAR = '\uFFFD';

    private static final int ASCII_LONGS_MIN_LENGTH = 16;

    private char[] buffer = new char[INITIAL_BUFFER_SIZE];
    private int length;

    private final boolean rawString;
    private final boolean strictUtf8;

    private int escapedUnicodeChar;

    // the state of a char, encoded in UTF-8, which hasn't been read completely yet
    private int utf8Pending; // the number of the bytes of the char still expected
    private int utf8CodePoint;
    private int utf8Lower; // the range of the next byte expected
    private int utf8Upper;
    private final ByteSource source = new ByteSource();

    public CopyingStringBuilder() {
        this(false);
    }

    public CopyingStringBuilder(final boolean rawString) {
        this(rawString, false);
    }

    /**
     * Creates the builder.
     *
     * @param rawString if true, the escaped chars are kept as they are
     * @param strictUtf8 if true, malformed UTF-8 bytes are an error of the parsing. Otherwise
     *                   each malformed sequence of bytes is replaced with U+FFFD the same way
     *                   {@code new String(bytes, StandardCharsets.UTF_8)} does
     */
    public CopyingStringBuilder(final boolean rawString, final boolean strictUtf8) {
        this.rawString = rawString;
        this.strictUtf8 = strictUtf8;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(final int index) {
        return buffer[Objects.checkIndex(index, length)];
    }

    @Override
    public void start(final CharSequence data, final int position) {
        length = 0;
    }

    @Override
    public void append(final CharSequence data, final int start, final int len) {
        makeSureRoomSize(len);
        final char[] chars = buffer;
        final int to = length;
        if (data instanceof String) {
            ((String) data).getChars(start, start + len, chars, to);
        } else if (data instanceof StringBuilder) {
            ((StringBuilder) data).getChars(start, start + len, chars, to);
        } else {
            for (int i = 0; i < len; i++) {
                chars[to + i] = data.charAt(start + i);
            }
        }
        length = to + len;
    }

    @Override
    public void start() {
        length = 0;
        utf8Pending = 0;
    }

    // Each kind of the bytes has a loop of its own, so the reading of the bytes is a monomorphic call.
    // The loops read ASCII and the most frequent chars encoded with 2 or 3 bytes, all the rest
    // (4 byte chars, the chars cut by the end of the bytes, malformed bytes) is read by appendChar().
    // appendNonAscii() returns the index next to the last byte, or the index of the byte the malformed
    // sequence is detected at, if the malformed bytes aren't replaced.

    @Override
    public int append(final byte[] data, final int start, final int len) {
        // the first byte may finish a char cut by the previous bytes and give two chars
        makeSureRoomSize(len + 1);
        if (utf8Pending == 0) {
            final char[] chars = buffer;
            final int to = length;
            // a long run is read eight bytes at once; the call is out of the loop for short runs
            int i = len < ASCII_LONGS_MIN_LENGTH ? 0 : appendAsciiLongs(data, start, len, chars, to);
            while (i < len) {
                final byte b = data[start + i];
                if (b < 0) {
                    break;
                }
                chars[to + i] = (char) b;
                i++;
            }
            length = to + i;
            if (i == len) {
                return len;
            }
            return appendNonAscii(data, start + i, start + len) - start;
        }
        return appendNonAscii(data, start, start + len) - start;
    }

    private int appendNonAscii(final byte[] data, final int from, final int end) {
        final char[] chars = buffer;
        int to = length;
        int i = from;
        while (true) {
            if (utf8Pending == 0) {
                while (i < end) {
                    final int b = data[i];
                    if (b >= 0) {
                        chars[to++] = (char) b;
                        i++;
                        continue;
                    }
                    // the most frequent chars encoded with 2 or 3 bytes, if all the bytes are here
                    final int b1 = b & 0xff;
                    if (b1 >= 0xc2 && b1 <= 0xdf) {
                        if (i + 1 < end) {
                            final int b2 = data[i + 1];
                            if ((b2 & 0xc0) == 0x80) {
                                chars[to++] = (char) (((b1 & 0x1f) << 6) | (b2 & 0x3f));
                                i += 2;
                                continue;
                            }
                        }
                    } else if (b1 >= 0xe0 && b1 <= 0xef) {
                        if (i + 2 < end) {
                            final int b2 = data[i + 1] & 0xff;
                            final int b3 = data[i + 2];
                            if (b2 >= (b1 == 0xe0 ? 0xa0 : 0x80) && b2 <= 0xbf && (b3 & 0xc0) == 0x80) {
                                final int c = ((b1 & 0x0f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f);
                                if (c < Character.MIN_SURROGATE || c > Character.MAX_SURROGATE) {
                                    chars[to++] = (char) c;
                                    i += 3;
                                    continue;
                                }
                            }
                        }
                    }
                    break;
                }
                if (i == end) {
                    break;
                }
            }
            length = to;
            source.set(data);
            i = appendChar(i, end);
            if (i < 0) {
                return -i - 1;
            }
            to = length;
            if (i == end) {
                break;
            }
        }
        length = to;
        return end;
    }

    @Override
    public int append(final ByteBuffer data, final int start, final int len) {
        // the first byte may finish a char cut by the previous bytes and give two chars
        makeSureRoomSize(len + 1);
        if (utf8Pending == 0) {
            final char[] chars = buffer;
            final int to = length;
            // a long run is read eight bytes at once; the call is out of the loop for short runs
            int i = len < ASCII_LONGS_MIN_LENGTH ? 0 : appendAsciiLongs(data, start, len, chars, to);
            while (i < len) {
                final byte b = data.get(start + i);
                if (b < 0) {
                    break;
                }
                chars[to + i] = (char) b;
                i++;
            }
            length = to + i;
            if (i == len) {
                return len;
            }
            return appendNonAscii(data, start + i, start + len) - start;
        }
        return appendNonAscii(data, start, start + len) - start;
    }

    private int appendNonAscii(final ByteBuffer data, final int from, final int end) {
        final char[] chars = buffer;
        int to = length;
        int i = from;
        while (true) {
            if (utf8Pending == 0) {
                while (i < end) {
                    final int b = data.get(i);
                    if (b >= 0) {
                        chars[to++] = (char) b;
                        i++;
                        continue;
                    }
                    // the most frequent chars encoded with 2 or 3 bytes, if all the bytes are here
                    final int b1 = b & 0xff;
                    if (b1 >= 0xc2 && b1 <= 0xdf) {
                        if (i + 1 < end) {
                            final int b2 = data.get(i + 1);
                            if ((b2 & 0xc0) == 0x80) {
                                chars[to++] = (char) (((b1 & 0x1f) << 6) | (b2 & 0x3f));
                                i += 2;
                                continue;
                            }
                        }
                    } else if (b1 >= 0xe0 && b1 <= 0xef) {
                        if (i + 2 < end) {
                            final int b2 = data.get(i + 1) & 0xff;
                            final int b3 = data.get(i + 2);
                            if (b2 >= (b1 == 0xe0 ? 0xa0 : 0x80) && b2 <= 0xbf && (b3 & 0xc0) == 0x80) {
                                final int c = ((b1 & 0x0f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f);
                                if (c < Character.MIN_SURROGATE || c > Character.MAX_SURROGATE) {
                                    chars[to++] = (char) c;
                                    i += 3;
                                    continue;
                                }
                            }
                        }
                    }
                    break;
                }
                if (i == end) {
                    break;
                }
            }
            length = to;
            source.set(data);
            i = appendChar(i, end);
            if (i < 0) {
                return -i - 1;
            }
            to = length;
            if (i == end) {
                break;
            }
        }
        length = to;
        return end;
    }

    @Override
    public int append(final ByteSequence data, final int start, final int len) {
        makeSureRoomSize(len + 1);
        final char[] chars = buffer;
        final int end = start + len;
        int to = length;
        int i = start;
        while (true) {
            if (utf8Pending == 0) {
                while (i < end) {
                    final byte b = data.byteAt(i);
                    if (b < 0) {
                        break;
                    }
                    chars[to++] = (char) b;
                    i++;
                }
                if (i == end) {
                    break;
                }
            }
            length = to;
            source.set(data);
            i = appendChar(i, end);
            if (i < 0) {
                return -i - 1 - start;
            }
            to = length;
            if (i == end) {
                break;
            }
        }
        length = to;
        return len;
    }

    // Copies the ASCII bytes eight at once while there are eight bytes and all of them are ASCII.
    // Returns the number of the bytes copied.
    private static int appendAsciiLongs(final byte[] data, final int start, final int len,
                                        final char[] chars, final int to) {
        int i = 0;
        while (len - i >= 8) {
            final long bytes = (long) ByteSource.ARRAY_LONGS.get(data, start + i);
            if ((bytes & ByteSource.HIGH_BITS) != 0) {
                break;
            }
            inflate(bytes, chars, to + i);
            i += 8;
        }
        return i;
    }

    private static int appendAsciiLongs(final ByteBuffer data, final int start, final int len,
                                        final char[] chars, final int to) {
        int i = 0;
        while (len - i >= 8) {
            final long bytes = (long) ByteSource.BUFFER_LONGS.get(data, start + i);
            if ((bytes & ByteSource.HIGH_BITS) != 0) {
                break;
            }
            inflate(bytes, chars, to + i);
            i += 8;
        }
        return i;
    }

    // Writes eight ASCII bytes, the lowest byte first.
    private static void inflate(final long bytes, final char[] chars, final int to) {
        chars[to] = (char) (bytes & 0xff);
        chars[to + 1] = (char) ((bytes >>> 8) & 0xff);
        chars[to + 2] = (char) ((bytes >>> 16) & 0xff);
        chars[to + 3] = (char) ((bytes >>> 24) & 0xff);
        chars[to + 4] = (char) ((bytes >>> 32) & 0xff);
        chars[to + 5] = (char) ((bytes >>> 40) & 0xff);
        chars[to + 6] = (char) ((bytes >>> 48) & 0xff);
        chars[to + 7] = (char) (bytes >>> 56);
    }

    // Reads the bytes of one char, or the bytes up to the end if the char is cut by the end,
    // or one malformed sequence of bytes.
    // Returns the index of the byte next to the last one read, or -(index + 1) of the byte
    // the malformed sequence is detected at, if the malformed bytes aren't replaced.
    private int appendChar(final int from, final int end) {
        final ByteSource data = source;
        final char[] chars = buffer;
        int to = length;
        int pending = utf8Pending;
        int codePoint = utf8CodePoint;
        int lower = utf8Lower;
        int upper = utf8Upper;

        int i = from;
        int result;
        while (true) {
            if (i == end) {
                result = i;
                break;
            }
            final int b = data.byteAt(i) & 0xff;
            if (pending == 0) {
                if (b < 0x80) {
                    chars[to++] = (char) b;
                    result = i + 1;
                    break;
                }
                if (b >= 0xc2 && b <= 0xdf) {
                    pending = 1;
                    codePoint = b & 0x1f;
                    lower = 0x80;
                    upper = 0xbf;
                } else if (b >= 0xe0 && b <= 0xef) {
                    pending = 2;
                    codePoint = b & 0x0f;
                    lower = b == 0xe0 ? 0xa0 : 0x80; // no overlong encoding
                    upper = 0xbf;
                } else if (b >= 0xf0 && b <= 0xf4) {
                    pending = 3;
                    codePoint = b & 0x07;
                    lower = b == 0xf0 ? 0x90 : 0x80; // no overlong encoding
                    upper = b == 0xf4 ? 0x8f : 0xbf; // no code point above U+10FFFF
                } else { // not the first byte of a char
                    if (strictUtf8) {
                        result = -i - 1;
                        break;
                    }
                    chars[to++] = REPLACEMENT_CHAR;
                    result = i + 1;
                    break;
                }
                i++;
                continue;
            }
            if (b < lower || b > upper) {
                // the char is cut, the byte is read once again as the first byte of the next char
                pending = 0;
                if (strictUtf8) {
                    result = -i - 1;
                    break;
                }
                chars[to++] = REPLACEMENT_CHAR;
                result = i;
                break;
            }
            codePoint = (codePoint << 6) | (b & 0x3f);
            lower = 0x80;
            upper = 0xbf;
            if (--pending == 0) {
                if (codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT) {
                    chars[to++] = Character.highSurrogate(codePoint);
                    chars[to++] = Character.lowSurrogate(codePoint);
                } else if (codePoint < Character.MIN_SURROGATE || codePoint > Character.MAX_SURROGATE) {
                    chars[to++] = (char) codePoint;
                } else { // a surrogate encoded on its own isn't a char
                    if (strictUtf8) {
                        result = -i - 1;
                        break;
                    }
                    chars[to++] = REPLACEMENT_CHAR;
                }
                result = i + 1;
                break;
            }
            i++;
        }

        length = to;
        utf8Pending = pending;
        utf8CodePoint = codePoint;
        utf8Lower = lower;
        utf8Upper = upper;
        return result;
    }

    @Override
    public boolean complete() {
        if (utf8Pending == 0) {
            return true;
        }
        utf8Pending = 0;
        if (strictUtf8) {
            return false;
        }
        append(REPLACEMENT_CHAR);
        return true;
    }

    @Override
    public void appendEscape() {
        if (rawString) {
            append('\\');
        }
    }

    @Override
    public void appendEscapedQuotationMark() {
        append('"');
    }

    @Override
    public void appendEscapedReverseSolidus() {
        append('\\');
    }

    @Override
    public void appendEscapedSolidus() {
        append('/');
    }

    @Override
    public void appendEscapedBackspace() {
        if (rawString) {
            append('b');
        } else {
            append((char) 0x08);
        }
    }

    @Override
    public void appendEscapedFormfeed() {
        if (rawString) {
            append('f');
        } else {
            append((char) 0x0c);
        }
    }

    @Override
    public void appendEscapedNewLine() {
        if (rawString) {
            append('n');
        } else {
            append((char) 0x0a);
        }
    }

    @Override
    public void appendEscapedCarriageReturn() {
        if (rawString) {
            append('r');
        } else {
            append((char) 0x0d);
        }
    }

    @Override
    public void appendEscapedHorisontalTab() {
        if (rawString) {
            append('t');
        } else {
            append((char) 0x09);
        }
    }

    @Override
    public void appendEscapedUnicodeU() {
        if (rawString) {
            append('u');
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
                    append(c);
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
                    append(c);
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
                    append(c);
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
                    append(c);
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
            append((char) escapedUnicodeChar);
        }
        return true;
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        Objects.checkFromToIndex(start, end, length);
        return new String(buffer, start, end - start);
    }

    @Override
    public String toString() {
        return new String(buffer, 0, length);
    }

    private void append(final char c) {
        makeSureRoomSize(1);
        buffer[length++] = c;
    }

    private void makeSureRoomSize(final int roomSize) {
        if (buffer.length - length < roomSize) {
            buffer = Arrays.copyOf(buffer, Math.max(length + roomSize, buffer.length << 1));
        }
    }
}
