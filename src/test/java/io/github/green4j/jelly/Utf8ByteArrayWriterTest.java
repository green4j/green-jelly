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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Utf8ByteArrayWriterTest {

    @Test
    void testInitialization() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        assertNotNull(writer.array());
        assertEquals(10, writer.array().length);
        assertEquals(0, writer.length());
    }

    @Test
    void testSetArray() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        final byte[] array = new byte[20];
        writer.set(array, 5);

        assertSame(array, writer.array());
        assertEquals(5, writer.start());
        assertEquals(0, writer.length());
    }

    @Test
    void testAppendAsciiChar() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        writer.append('A'); // append an ASCII character
        assertEquals(1, writer.length());
        assertEquals((byte) 'A', writer.array()[0]);

        writer.append('B'); // append another ASCII character
        assertEquals(2, writer.length());
        assertEquals((byte) 'B', writer.array()[1]);
    }

    @Test
    void testAppendNonAsciiChar() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        writer.append('\u00E9'); // append é (Unicode: U+00E9)
        assertEquals(2, writer.length()); // UTF-8 encoding for é is two bytes
        final byte[] expected = { (byte) 0xC3, (byte) 0xA9 }; // UTF-8 representation of é
        assertArrayEquals(expected, new byte[] { writer.array()[0], writer.array()[1] });
    }

    @Test
    void testAppendSurrogatePairThrowsException() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            writer.append('\uD800'); // high surrogate character (not valid without a pair)
        });
        assertTrue(exception.getMessage().contains("exceeds BMP range"));
    }

    @Test
    void testAppendCharSequence() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        writer.append("Hello");
        assertEquals(5, writer.length());
        assertEquals("Hello", writer.toString());
    }

    @Test
    void testAppendCharSequenceWithNonAscii() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        writer.append("Café"); // append a string with non-ASCII character
        assertEquals(5, writer.length());
        assertEquals("Café", writer.toString());
    }

    @Test
    void testAppendCharSequenceWithRange() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        writer.append("Hello, World!", 7, 5);
        assertEquals(5, writer.length());
        assertEquals("World", writer.toString());
    }

    @Test
    void testClear() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        writer.append("Test");
        assertEquals(4, writer.length());

        writer.clear();
        assertEquals(0, writer.length());
    }

    @Test
    void testFrameAppendAndSetCharAt() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        final Utf8ByteArrayWriter.Frame frame = writer.append(5);
        frame.setCharAt(0, 'H');
        frame.setCharAt(1, 'i');

        assertEquals(5, writer.length());
        assertEquals("Hi", writer.toString().substring(0, 2));
    }

    @Test
    void testFrameToString() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        final Utf8ByteArrayWriter.Frame frame = writer.append(5);
        frame.setCharAt(0, 'T');
        frame.setCharAt(1, 'e');
        frame.setCharAt(2, 's');
        frame.setCharAt(3, 't');
        assertEquals("Test", frame.toString().substring(0, 4));
        assertEquals("Test", writer.toString().substring(0, 4));
    }

    @Test
    void testDynamicResize() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(2); // start with a small buffer
        writer.append("AB");
        assertEquals(2, writer.length());
        assertEquals("AB", writer.toString());

        writer.append("CD");
        assertEquals(4, writer.length());
        assertEquals("ABCD", writer.toString());
    }

    @Test
    void testUnsupportedFrameOperations() {
        final Utf8ByteArrayWriter writer = new Utf8ByteArrayWriter(10);
        final Utf8ByteArrayWriter.Frame frame = writer.append(3);
        assertThrows(UnsupportedOperationException.class, () -> frame.charAt(0));
        assertThrows(UnsupportedOperationException.class, () -> frame.subSequence(0, 2));
    }
}
