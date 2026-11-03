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

import io.github.green4j.jelly.JsonParser;
import io.github.green4j.jelly.Utf8JsonParser;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonValueBuilderTest {

    static final String[] DOCUMENTS = {
        "{\"name\":\"va\\\"l\\u0041ue\",\"t\":true,\"f\":false,"
                + "\"numbers\":[0,-12,3.25,4e2,5.5E-3],"
                + "\"nested\":[{\"a\":[[],{}]},{\"b\":{\"c\":[1,[2,[3]]]}}],\"s\":\"\"}",
        "[\"желе\",\"数据\",\"😀 é \\u0436\\ud83d\\ude00\\n\","
                + "{\"ключ\":\"значение\"}]",
        "  [1, 2 , 3 ]  ",
        "\"just a string\"",
        "\"é\"",
        "12.5",
        "true",
        "false",
        "null",
        "[]",
        "{}"
    };

    @Test
    void buildsTheSameValueFromBytesAsFromChars() {
        for (final String document : DOCUMENTS) {
            final String expected = json(fromChars(document));
            final byte[] bytes = document.getBytes(StandardCharsets.UTF_8);

            final JsonValueBuilder builder = new JsonValueBuilder();
            final Utf8JsonParser parser = new Utf8JsonParser().setListener(builder);

            parser.parseAndEoj(bytes);
            assertFalse(parser.hasError(), document);
            assertEquals(expected, json(builder.value()), document);

            parser.parseAndEoj(ByteBuffer.wrap(bytes));
            assertEquals(expected, json(builder.value()), document);

            final ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length);
            direct.put(bytes).flip();
            parser.parseAndEoj(direct);
            assertEquals(expected, json(builder.value()), document);

            // byte by byte: every char encoded with several bytes is cut by the parts
            for (int i = 0; i < bytes.length; i++) {
                parser.parse(bytes, i, 1);
            }
            parser.eoj();
            assertFalse(parser.hasError(), document);
            assertEquals(expected, json(builder.value()), document);
        }
    }

    @Test
    void buildsScalarsAtTheTopLevel() {
        assertTrue(fromBytes("true").asBooleanRequired());
        assertFalse(fromBytes("false").asBooleanRequired());
        assertTrue(fromBytes("null").isNull());
        assertEquals("é", fromBytes("\"é\"").asStringRequired());
        assertEquals(new BigDecimal("12.5"), fromBytes("12.5").asDecimalRequired());
    }

    @Test
    void isReusedForTheNextDocument() {
        final JsonValueBuilder builder = new JsonValueBuilder();
        final Utf8JsonParser parser = new Utf8JsonParser().setListener(builder);

        parser.parseAndEoj(utf8("{\"a\":[1,{\"b\":2}]}"));
        assertEquals(json(fromChars("{\"a\":[1,{\"b\":2}]}")), json(builder.value()));

        parser.parseAndEoj(utf8("[true]"));
        assertEquals(json(fromChars("[true]")), json(builder.value()));

        parser.parseAndEoj(new byte[0]);
        assertNull(builder.value());
    }

    @Test
    void hasNoValueAfterAnErrorAndBuildsTheNextDocument() {
        final JsonValueBuilder builder = new JsonValueBuilder();
        final Utf8JsonParser parser = new Utf8JsonParser().setListener(builder);

        parser.parseAndEoj(utf8("[1]"));
        assertNotNull(builder.value());

        parser.parse(utf8("{\"a\":[1 2]"));
        assertTrue(parser.hasError());
        assertNull(builder.value());

        parser.reset();
        parser.parseAndEoj(utf8("{\"a\":[1]}"));
        assertFalse(parser.hasError());
        assertEquals(json(fromChars("{\"a\":[1]}")), json(builder.value()));

        parser.parseAndEoj(utf8("[1,2"));
        assertTrue(parser.hasError());
        assertNull(builder.value());
    }

    private static JsonValue fromChars(final String document) {
        final JsonValueBuilder builder = new JsonValueBuilder();
        final JsonParser parser = new JsonParser().setListener(builder);
        parser.parseAndEoj(document);
        assertFalse(parser.hasError(), document);
        assertNotNull(builder.value(), document);
        return builder.value();
    }

    private static JsonValue fromBytes(final String document) {
        final JsonValueBuilder builder = new JsonValueBuilder();
        final Utf8JsonParser parser = new Utf8JsonParser().setListener(builder);
        parser.parseAndEoj(utf8(document));
        assertFalse(parser.hasError(), document);
        assertNotNull(builder.value(), document);
        return builder.value();
    }

    static String json(final JsonValue value) {
        final StringWriter writer = new StringWriter();
        value.toJsonAndEoj(writer);
        return writer.toString();
    }

    private static byte[] utf8(final String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
