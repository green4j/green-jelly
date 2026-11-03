/**
 * MIT License
 * <p>
 * Copyright (c) 2018-2024 Anatoly Gudkov and others.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.green4j.jelly.simple;

import org.junit.jupiter.api.Test;

import static io.github.green4j.jelly.simple.JsonValueBuilderTest.DOCUMENTS;
import static io.github.green4j.jelly.simple.JsonValueBuilderTest.json;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonValueParserTest {

    @Test
    void parsesWholeAndChunkedDocuments() {
        final JsonValueParser parser = new JsonValueParser();
        for (final String document : DOCUMENTS) {
            final String expected = json(parser.parseAndEoj(document));

            for (int i = 0; i < document.length(); i++) {
                parser.parse(document.subSequence(i, i + 1));
            }
            assertEquals(expected, json(parser.eoj()), document);
        }
    }

    @Test
    void parsesScalarsAtTheTopLevel() {
        final JsonValueParser parser = new JsonValueParser();
        assertTrue(parser.parseAndEoj("true").asBooleanRequired());
        assertTrue(parser.parseAndEoj("null").isNull());
        assertEquals("s", parser.parseAndEoj("\"s\"").asStringRequired());
    }

    @Test
    void throwsAnErrorAndParsesTheNextDocument() {
        final JsonValueParser parser = new JsonValueParser();

        final IllegalArgumentException invalid =
                assertThrows(IllegalArgumentException.class, () -> parser.parseAndEoj("{\"a\":[1 2]}"));
        assertTrue(invalid.getMessage().contains("at position: 8"), invalid.getMessage());
        assertEquals("{\"a\":[1]}", parser.parseAndEoj("{\"a\":[1]}").toString().replaceAll("\\s", ""));

        parser.parse("[1,");
        assertThrows(IllegalArgumentException.class, parser::eoj); // the document isn't finished
        assertEquals("[2]", parser.parseAndEoj("[2]").toString().replaceAll("\\s", ""));

        assertNull(parser.parseAndEoj(""));
    }

    @Test
    void dropsTheDocumentBeingParsedOnReset() {
        final JsonValueParser parser = new JsonValueParser();

        parser.parse("{\"a\":[1,");
        parser.reset();
        assertEquals("[2]", parser.parseAndEoj("[2]").toString().replaceAll("\\s", ""));

        parser.parse("[1,");
        assertThrows(IllegalArgumentException.class, () -> parser.parseAndEoj("[2]")); // the same document goes on
    }
}
