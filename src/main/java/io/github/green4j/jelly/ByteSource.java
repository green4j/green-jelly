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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The bytes {@link Utf8JsonParser} is parsing. Exactly one of the three kinds of the storage is set.
 * The class is final and {@link #byteAt(int)} has one call site for each kind of the storage,
 * so the reading is inlined and the JIT doesn't see a call of an interface for an array
 * or a buffer, whatever types of the data a process parses.
 *
 * <p>The longest runs of the bytes (plain chars of strings) are read with {@link #longAt(int)}, eight bytes
 * at once. The bytes of a long are always in the little-endian order, whatever the order of a buffer is,
 * so the byte at the lowest index is the lowest byte of the long.
 */
final class ByteSource {
    static final VarHandle ARRAY_LONGS =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);
    static final VarHandle BUFFER_LONGS =
            MethodHandles.byteBufferViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    static final long ONES = 0x0101010101010101L;
    static final long HIGH_BITS = 0x8080808080808080L;

    /**
     * Returns a long with the high bit set in the lowest byte, which is zero in the given long.
     * The bits of the bytes above it may be set wrongly, so only the lowest one is the answer.
     *
     * @param x eight bytes
     * @return zero if there is no zero byte
     */
    static long zeroBytes(final long x) {
        return (x - ONES) & ~x & HIGH_BITS;
    }

    private byte[] array;
    private ByteBuffer buffer;
    private ByteSequence sequence;

    ByteSource() {
    }

    void set(final byte[] array) {
        this.array = array;
        this.buffer = null;
        this.sequence = null;
    }

    void set(final ByteBuffer buffer) {
        this.array = null;
        this.buffer = buffer;
        this.sequence = null;
    }

    void set(final ByteSequence sequence) {
        this.array = null;
        this.buffer = null;
        this.sequence = sequence;
    }

    /**
     * Returns the byte at the index. Neither the position nor the limit of a buffer is used.
     *
     * @param index the index of the byte in the storage
     * @return the byte
     */
    byte byteAt(final int index) {
        final byte[] a = array;
        if (a != null) {
            return a[index];
        }
        final ByteBuffer b = buffer;
        if (b != null) {
            return b.get(index);
        }
        return sequence.byteAt(index);
    }

    /**
     * Returns eight bytes starting at the index, the byte at the index is the lowest byte of the long.
     *
     * @param index the index of the first byte in the storage
     * @return the bytes
     */
    long longAt(final int index) {
        final byte[] a = array;
        if (a != null) {
            return (long) ARRAY_LONGS.get(a, index);
        }
        final ByteBuffer b = buffer;
        if (b != null) {
            return (long) BUFFER_LONGS.get(b, index);
        }
        final ByteSequence s = sequence;
        long result = 0;
        for (int i = 7; i >= 0; i--) {
            result = (result << 8) | (s.byteAt(index + i) & 0xffL);
        }
        return result;
    }

    int appendTo(final JsonUtf8StringBuilder builder, final int start, final int len) {
        final byte[] a = array;
        if (a != null) {
            return builder.append(a, start, len);
        }
        final ByteBuffer b = buffer;
        if (b != null) {
            return builder.append(b, start, len);
        }
        return builder.append(sequence, start, len);
    }
}
