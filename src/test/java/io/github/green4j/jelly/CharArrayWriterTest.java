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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharArrayWriterTest {

    @Test
    void testInitialization() {
        final CharArrayWriter writer = new CharArrayWriter(10);
        assertNotNull(writer.array());
        assertEquals(10, writer.array().length);
    }

    @Test
    void testSetArray() {
        final char[] initialArray = new char[10];
        final CharArrayWriter writer = new CharArrayWriter(initialArray);
        assertEquals(initialArray, writer.array());
    }

    @Test
    void testAppendNull() {
        final CharArrayWriter writer = new CharArrayWriter(5);
        writer.append(null);
        assertEquals(4, writer.length());
        assertEquals("null", writer.toString());
    }

    @Test
    void testAppendChar() {
        final CharArrayWriter writer = new CharArrayWriter(5);
        writer.append('A');
        assertEquals(1, writer.length());
        assertEquals('A', writer.array()[0]);
    }

    @Test
    void testAppendCharSequence() {
        final CharArrayWriter writer = new CharArrayWriter(5);
        writer.append("Hello");
        assertEquals(5, writer.length());
        assertEquals("Hello", writer.toString());
    }

    @Test
    void testAppendCharSequenceWithRange() {
        final CharArrayWriter writer = new CharArrayWriter(5);
        writer.append("Hello, World!", 7, 5);
        assertEquals(5, writer.length());
        assertEquals("World", writer.toString());
    }

    @Test
    void testFrameAppend() {
        final CharArrayWriter writer = new CharArrayWriter(10);
        final BufferingWriter.Frame frame = writer.append(5);
        frame.setCharAt(0, 'X');
        frame.setCharAt(1, 'Y');
        frame.setCharAt(2, 'Z');
        assertEquals(5, frame.length());
        assertEquals("XYZ", writer.toString().substring(0, 3));
    }

    @Test
    void testResizeArray() {
        final CharArrayWriter writer = new CharArrayWriter(2);
        writer.append("AB");
        writer.append('C'); // this should trigger resizing
        assertTrue(writer.array().length > 2);
        assertEquals("ABC", writer.toString());
    }

    @Test
    void testClear() {
        final CharArrayWriter writer = new CharArrayWriter(10);
        writer.append("Test");
        writer.clear();
        assertEquals(0, writer.length());
        assertEquals("", writer.toString());
    }

    @Test
    void testToString() {
        final CharArrayWriter writer = new CharArrayWriter(10);
        writer.append("Test");
        assertEquals("Test", writer.toString());
    }
}