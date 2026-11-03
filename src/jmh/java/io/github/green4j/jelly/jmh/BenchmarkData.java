/**
 * MIT License
 * <p>
 * Copyright (c) 2018-2026 Anatoly Gudkov and others.
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
package io.github.green4j.jelly.jmh;

import java.util.Random;

/**
 * The documents the benchmarks work on. The text is built here by hand, not with the code under
 * test, and from a fixed seed, so a run of any version of the library sees exactly the same input.
 */
public final class BenchmarkData {

    private static final int RECORDS = 40;

    private static final String[] SYMBOLS = {
        "BTC-USD", "ETH-USD", "EURUSD", "GBPUSD", "AAPL", "MSFT", "VOD.L", "SAP.DE"
    };

    private static final String[] WORDS = {
        "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit", "sed", "do", "eiusmod",
        "tempor", "incididunt", "ut", "labore", "et", "dolore", "magna", "aliqua"
    };

    private static final String[] UNICODE_WORDS = {
        "желе", "парсер", "数据", "данные",
        "été", "流", "grün"
    };

    public enum Document {
        /** Small integers and booleans, short member names: mostly structure. The document of the README. */
        NUMBERS {
            @Override
            String build() {
                final StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < RECORDS; i++) {
                    json.append(i == 0 ? "\n" : ",\n");
                    json.append("  {\"property1\":100,\"property2\":200,\"property3\":300,"
                            + "\"property4\":[400,500,600,700],\"property5\":").append(i % 2 == 0).append("}");
                }
                return json.append("\n]").toString();
            }
        },
        /** Market-data-like records: decimals, long timestamps, short strings. */
        DECIMALS {
            @Override
            String build() {
                final Random random = new Random(42);
                final StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < RECORDS; i++) {
                    json.append(i == 0 ? "" : ",");
                    json.append("{\"symbol\":\"").append(SYMBOLS[i % SYMBOLS.length]).append('"');
                    final long bid = PRICE_BASE + random.nextInt(1_000_000);
                    json.append(",\"bid\":").append(bid / 100).append('.').append(twoDigits(bid % 100));
                    json.append(",\"ask\":").append((bid + 25) / 100).append('.').append(twoDigits((bid + 25) % 100));
                    json.append(",\"qty\":0.").append(1000 + random.nextInt(9000));
                    json.append(",\"time\":").append(TIME_BASE + i * 37L);
                    json.append('}');
                }
                return json.append(']').toString();
            }
        },
        /** Records with string values of about 100 ASCII chars and no escaping. */
        STRINGS {
            @Override
            String build() {
                return stringRecords(WORDS, "");
            }
        },
        /** The same as STRINGS, but each value has escaped chars in it. */
        ESCAPED {
            @Override
            String build() {
                return stringRecords(WORDS, "line\\n\\t\\\"quoted\\\" \\\\ tail");
            }
        },
        /** The same as STRINGS, but the values are out of Latin-1, so the source is a UTF-16 string. */
        UNICODE {
            @Override
            String build() {
                return stringRecords(UNICODE_WORDS, "");
            }
        },
        /** A mix of everything above, pretty printed: a lot of whitespace. */
        PRETTY {
            @Override
            String build() {
                final Random random = new Random(42);
                final StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < RECORDS; i++) {
                    json.append(i == 0 ? "\n" : ",\n");
                    json.append("    {\n");
                    json.append("        \"id\": ").append(i).append(",\n");
                    json.append("        \"name\": \"").append(sentence(random, WORDS, 20)).append("\",\n");
                    json.append("        \"price\": ").append(random.nextInt(100_000)).append(".5,\n");
                    json.append("        \"active\": ").append(i % 3 == 0).append(",\n");
                    json.append("        \"parent\": null,\n");
                    json.append("        \"tags\": [\n");
                    json.append("            \"").append(WORDS[i % WORDS.length]).append("\",\n");
                    json.append("            \"").append(WORDS[(i + 1) % WORDS.length]).append("\"\n");
                    json.append("        ]\n");
                    json.append("    }");
                }
                return json.append("\n]").toString();
            }
        };

        abstract String build();

        public String json() {
            return build();
        }
    }

    private static final long PRICE_BASE = 6_400_000L;
    private static final long TIME_BASE = 1_730_000_000_123L;

    private BenchmarkData() {
    }

    public static String[] symbols() {
        return SYMBOLS.clone();
    }

    /**
     * Values for the generator benchmarks.
     *
     * @param count number of the values
     * @param words the words to build the values from
     * @param approximateLength the approximate length of a value
     * @return the values
     */
    public static String[] sentences(final int count, final String[] words, final int approximateLength) {
        final Random random = new Random(42);
        final String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = sentence(random, words, approximateLength);
        }
        return result;
    }

    public static String[] asciiWords() {
        return WORDS.clone();
    }

    private static String stringRecords(final String[] words, final String suffix) {
        final Random random = new Random(42);
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < RECORDS; i++) {
            json.append(i == 0 ? "" : ",");
            json.append("{\"id\":\"").append(sentence(random, words, 12)).append(suffix).append('"');
            json.append(",\"name\":\"").append(sentence(random, words, 30)).append(suffix).append('"');
            json.append(",\"description\":\"").append(sentence(random, words, 100)).append(suffix).append('"');
            json.append('}');
        }
        return json.append(']').toString();
    }

    private static String sentence(final Random random, final String[] words, final int approximateLength) {
        final StringBuilder result = new StringBuilder();
        while (result.length() < approximateLength) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(words[random.nextInt(words.length)]);
        }
        return result.toString();
    }

    private static String twoDigits(final long value) {
        return value < 10 ? "0" + value : Long.toString(value);
    }
}
