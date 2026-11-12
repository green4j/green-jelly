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

/**
 * How a buffer grows: doubled while it is small, then by half of itself. Doubling a large buffer allocates up to
 * twice the document, and holds the old array and one twice its size at once while it copies; growing by half
 * copies about as much in all, and holds far less. A buffer which needs more than that grows the same way from
 * what it needs, so that the next small write doesn't copy it again. Never past the largest array the JVM allows.
 */
final class Capacity {
    /** Below this a buffer doubles. */
    static final int DOUBLE_UNTIL = 16 * 1024 * 1024;
    /** The largest array some JVMs allow. */
    static final int MAX = Integer.MAX_VALUE - 8;

    private Capacity() {
    }

    /**
     * @param current  the capacity a buffer has
     * @param required the capacity it needs, at least
     * @return the capacity to grow it to
     * @throws OutOfMemoryError if more is required than an array can hold
     */
    static int next(final int current, final long required) {
        if (required > MAX) {
            throw new OutOfMemoryError("Required array size too large: " + required);
        }
        final long grown = grow(current);
        return (int) Math.min(MAX, grown >= required ? grown : grow(required));
    }

    private static long grow(final long capacity) {
        return capacity < DOUBLE_UNTIL ? 2 * capacity : capacity + (capacity >> 1);
    }
}
