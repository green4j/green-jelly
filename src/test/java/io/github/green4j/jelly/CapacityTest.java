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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CapacityTest {
    private static final int MIB = 1024 * 1024;

    @Test
    void aSmallBufferDoubles() {
        assertEquals(128 * 1024, Capacity.next(64 * 1024, 64 * 1024 + 1));
        assertEquals(16 * MIB, Capacity.next(8 * MIB, 8 * MIB + 1));
    }

    @Test
    void aLargeBufferGrowsByHalf() {
        assertEquals(24 * MIB, Capacity.next(16 * MIB, 16 * MIB + 1));
        assertEquals(384 * MIB, Capacity.next(256 * MIB, 256 * MIB + 1));
    }

    @Test
    void aBufferWhichNeedsMoreGrowsFromWhatItNeeds() {
        assertEquals(20 * MIB, Capacity.next(1024, 10 * MIB));
        assertEquals(150 * MIB, Capacity.next(16 * MIB, 100 * MIB));
    }

    @Test
    void aBufferNeverPassesTheLargestArray() {
        assertEquals(Capacity.MAX, Capacity.next(1536 * MIB, 1536L * MIB + 1));
        assertEquals(Capacity.MAX, Capacity.next(1024, Capacity.MAX));
        assertThrows(OutOfMemoryError.class, () -> Capacity.next(Capacity.MAX, (long) Capacity.MAX + 1));
    }

    @Test
    void aDocumentOfTwoHundredSixtyMibEndsInABufferOfItsSize() {
        int capacity = 64 * 1024;
        final int document = 273_035_891;
        while (capacity < document) {
            capacity = Capacity.next(capacity, capacity + 1L);
        }
        assertEquals(286_654_464, capacity); // 273.4 MiB
    }
}
