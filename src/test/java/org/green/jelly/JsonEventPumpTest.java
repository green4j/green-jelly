package org.green.jelly;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class JsonEventPumpTest {

    private static final String testJson = "{" +
            "\"a\": 1," +
            "\"b\": null," +
            "\"c\": [true, false, null, 1, \"string\", " +
            "{ \"cc_1\": [], \"cc_2\": [1, 2, 4], \"cc_3\": \"cc_3 value\"}]" +
            "}";

    @Test
    public void test() {
        final JsonParser parser = new JsonParser(new CopyingStringBuilder(/*false*/));

        final JsonEvents expectedEvents = new JsonEvents();
        parser.setListener(expectedEvents);

        parser.parseAndEoj(testJson);

        final StringBuilder resultJson = new StringBuilder();

        final JsonGenerator generator = new JsonGenerator(new AppendableWriter<>(resultJson));
        final JsonEventPump pump = new JsonEventPump(generator);
        parser.setListener(pump);

        parser.parseAndEoj(testJson);

        final JsonEvents resultEvents = new JsonEvents();
        parser.setListener(resultEvents);

        parser.parseAndEoj(resultJson);

        assertEquals(expectedEvents, resultEvents);
    }
}
