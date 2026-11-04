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
package io.github.green4j.jelly;

public final class JsonGenerator {

    private static final String NL = System.getProperty("line.separator", "\n");
    private static final String[] WSS = new String[]{
            "",
            " ",
            "  ",
            "   ",
            "    ",
            "     ",
            "      ",
            "       ",
            "        ",
            "         ",
            "          "
    };

    private static final char[] DIGIT_TENS = {
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
            '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
            '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
            '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
            '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
            '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
            '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
            '9', '9', '9', '9', '9', '9', '9', '9', '9', '9'};

    private static final char[] DIGIT_ONES = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String NULL = "null";

    private static final String[] UNICODE_ASCII_LOW = new String[0x1F + 1];
    private static final String[] ESCAPES = new String[128];
    // In ECMAScript, \u2028 and \u2029 are line terminators and must be encoded
    private static final String UNICODE_2028 = "\\u2028"; // line separator <LS>
    private static final String UNICODE_2029 = "\\u2029"; // paragraph separator <PS>

    private static final int STATE_OBJECT_STARTED = 1;
    private static final int STATE_OBJECT_MEMBER_NAME = STATE_OBJECT_STARTED + 1;
    private static final int STATE_OBJECT_MEMBER_VALUE = STATE_OBJECT_MEMBER_NAME + 1;

    private static final int STATE_ARRAY_STARTED = STATE_OBJECT_MEMBER_VALUE + 1;
    private static final int STATE_ARRAY_ITEM = STATE_ARRAY_STARTED + 1;

    private static final int MAX_NUMBER_OF_DIGITS = 19;

    // A number is formatted around this index of the number buffer: the digits of the mantissa are written
    // backward from it. What goes before the digits is a sign, '0.' and 5 zeros, what goes after them
    // is up to 18 zeros or an exponent.
    private static final int NUMBER_DIGITS_END = 28;
    private static final int NUMBER_BUFFER_SIZE = NUMBER_DIGITS_END + 18;

    static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    static {
        for (int i = 0; i < 0x10; i++) {
            UNICODE_ASCII_LOW[i] = "\\u000" + HEX_DIGITS[i];
        }
        for (int i = 0x10; i < UNICODE_ASCII_LOW.length; i++) {
            UNICODE_ASCII_LOW[i] = "\\u00" + HEX_DIGITS[i >>> 4 & 0x000f] + HEX_DIGITS[i & 0x000f];
        }
        System.arraycopy(UNICODE_ASCII_LOW, 0, ESCAPES, 0, UNICODE_ASCII_LOW.length);
        ESCAPES[0x08] = "\\b";
        ESCAPES[0x09] = "\\t";
        ESCAPES[0x0A] = "\\n";
        ESCAPES[0x0C] = "\\f";
        ESCAPES[0x0D] = "\\r";
        ESCAPES['/'] = "\\/";
        ESCAPES['\\'] = "\\\\";
        ESCAPES['"'] = "\\\"";
    }

    private final boolean indent;

    private final char[] numberBuffer = new char[NUMBER_BUFFER_SIZE];

    private int[] scopeStack = new int[8];
    private int scopeStackDepth;

    private BufferingWriter output;

    public JsonGenerator() {
        this(true);
    }

    public JsonGenerator(final boolean indent) {
        this.indent = indent;
    }

    public JsonGenerator(final BufferingWriter output) {
        this(output, true);
    }

    public JsonGenerator(final BufferingWriter output, final boolean indent) {
        this(indent);
        this.output = output;
    }

    public JsonGenerator(final AppendableWriter<?> output) {
        this(output, true);
    }

    public JsonGenerator(final AppendableWriter<?> output, final boolean indent) {
        this((BufferingWriter) output, indent);
    }

    public JsonGenerator(final Appendable output) {
        this(output, true);
    }

    public JsonGenerator(final Appendable output, final boolean indent) {
        this(new AppendableWriter<>(output), indent);
    }

    public boolean isIndent() {
        return indent;
    }

    public JsonGenerator setOutput(final BufferingWriter output) {
        this.output = output;
        return this;
    }

    public void startObject() {
        writeStructureStarted('{', STATE_OBJECT_STARTED);
    }

    public void objectMember(final CharSequence name, final boolean escaping) {
        objectMember(name, 0, name.length(), escaping);
    }

    public void objectMember(final CharSequence name) {
        objectMember(name, 0, name.length(), false);
    }

    public void objectMember(final CharSequence name, final int start, final int len) {
        objectMember(name, start, len, false);
    }

    public void objectMember(final CharSequence name, final int start, final int len, final boolean escaping) {
        final BufferingWriter out = output;

        assert out != null;

        final int scope = peekScope();

        switch (scope) {
            case STATE_OBJECT_MEMBER_NAME:
            case STATE_OBJECT_MEMBER_VALUE:
                out.append(',');
                break;
        }

        if (indent) {
            indent(out, false);
        }

        out.append('\"');
        if (escaping) {
            writeEscaped(out, name, start, len);
        } else {
            out.append(name, start, len);
        }
        out.append('\"');
        out.append(':');

        replaceScope(STATE_OBJECT_MEMBER_NAME);
    }

    public void endObject() {
        writeStructureEnded('}');
    }

    public void startArray() {
        writeStructureStarted('[', STATE_ARRAY_STARTED);
    }

    public void endArray() {
        writeStructureEnded(']');
    }

    public void stringValue(final CharSequence value, final boolean escaping) {
        writeStringQuoted(value, 0, value.length(), escaping);
    }

    public void stringValue(final CharSequence value) {
        writeStringQuoted(value, 0, value.length(), false);
    }

    public void stringValue(final CharSequence name, final int start, final int len, final boolean escaping) {
        writeStringQuoted(name, start, len, escaping);
    }

    public void stringValue(final CharSequence name, final int start, final int len) {
        writeStringQuoted(name, start, len, false);
    }

    public void numberValueAsString(final JsonNumber value) {
        numberValueAsString(value.mantissa(), value.exp());
    }

    public void numberValueAsString(final long value) {
        numberValueAsString(value, 0);
    }

    public void numberValueAsString(final long mantissa, final int exp) {
        writeNumberQuoted(mantissa, exp);
    }

    public void numberValue(final long value) {
        numberValue(value, 0);
    }

    public void numberValue(final JsonNumber value) {
        numberValue(value.mantissa(), value.exp());
    }

    public void numberValue(final long mantissa, final int exp) {
        final BufferingWriter out = output;

        assert out != null;

        final int scope = peekScope();

        beforeLiteralAdded(scope, out);

        if (exp == 0) {
            writeLongNumber(out, mantissa);
        } else {
            writeDecimalNumber(out, mantissa, exp);
        }

        afterValueAdded(scope);
    }

    public void trueValue() {
        writeStringDirect(TRUE);
    }

    public void falseValue() {
        writeStringDirect(FALSE);
    }

    public void nullValue() {
        writeStringDirect(NULL);
    }

    public void eoj() {
        while (scopeStackDepth > 0) {
            final int scope = peekScope();
            switch (scope) {
                case STATE_ARRAY_STARTED:
                case STATE_ARRAY_ITEM:
                    endArray();
                    break;
                case STATE_OBJECT_STARTED:
                case STATE_OBJECT_MEMBER_NAME:
                case STATE_OBJECT_MEMBER_VALUE:
                    endObject();
                    break;
                default:
                    throw new IllegalStateException("Internal error. Unexpected state: " + scope);
            }
        }

        output.flush();

        reset();
    }

    public void reset() {
        clearScope();
    }

    private void writeStructureStarted(final char leftBracket, final int newState) {
        final BufferingWriter out = output;

        assert out != null;

        final int scope = peekScope();

        switch (scope) {
            case STATE_OBJECT_MEMBER_VALUE:
            case STATE_ARRAY_ITEM:
                out.append(',');
                break;
        }

        if (indent) {
            indent(out, false);
        }

        out.append(leftBracket);

        pushScope(newState);
    }

    private void writeStructureEnded(final char rightBracket) {
        final BufferingWriter out = output;

        assert out != null;

        popScope();

        if (indent) {
            indent(out, true);
        }

        out.append(rightBracket);

        afterValueAdded(peekScope());
    }

    private void writeStringQuoted(final CharSequence value, final int start, final int len, final boolean escaping) {
        final BufferingWriter out = output;

        assert out != null;

        final int scope = peekScope();

        beforeLiteralAdded(scope, out);

        out.append('\"');

        if (escaping) {
            writeEscaped(out, value, start, len);
        } else {
            out.append(value, start, len);
        }

        out.append('\"');

        afterValueAdded(scope);
    }

    private static void writeEscaped(
            final BufferingWriter out,
            final CharSequence value,
            final int start,
            final int len) {
        final int end = start + len;
        int plainStart = start;
        for (int i = start; i < end; i++) {
            final char c = value.charAt(i);
            final String escape;
            if (c < ESCAPES.length) {
                escape = ESCAPES[c];
            } else if (c == 0x2028) {
                escape = UNICODE_2028;
            } else if (c == 0x2029) {
                escape = UNICODE_2029;
            } else {
                escape = null;
            }
            if (escape != null) {
                out.append(value, plainStart, i - plainStart);
                out.append(escape);
                plainStart = i + 1;
            }
        }
        out.append(value, plainStart, end - plainStart);
    }

    private void writeStringDirect(final String value) {
        final BufferingWriter out = output;

        assert out != null;

        final int scope = peekScope();

        beforeLiteralAdded(scope, out);

        out.append(value);

        afterValueAdded(scope);
    }

    private void writeNumberQuoted(final long mantissa, final int exp) {
        final BufferingWriter out = output;

        assert out != null;

        final int scope = peekScope();

        beforeLiteralAdded(scope, out);

        out.append('\"');

        if (exp == 0) {
            writeLongNumber(out, mantissa);
        } else {
            writeDecimalNumber(out, mantissa, exp);
        }

        out.append('\"');

        afterValueAdded(scope);
    }

    private void beforeLiteralAdded(final int scope, final BufferingWriter out) {
        switch (scope) {
            case STATE_ARRAY_ITEM:
                out.append(',');
                break;
        }

        if (indent) {
            switch (scope) {
                case STATE_ARRAY_STARTED:
                case STATE_ARRAY_ITEM:
                    indent(out, false);
                    break;
                case STATE_OBJECT_MEMBER_NAME:
                    out.append(' ');
                    break;
            }
        }
    }

    private void afterValueAdded(final int scope) {
        switch (scope) {
            case STATE_OBJECT_MEMBER_NAME:
                replaceScope(STATE_OBJECT_MEMBER_VALUE);
                break;
            case STATE_ARRAY_STARTED:
                replaceScope(STATE_ARRAY_ITEM);
                break;
        }
    }

    private void indent(final BufferingWriter out, final boolean ending) {
        final int depth = scopeStackDepth;
        if (depth == 0) {
            if (ending) {
                out.append(NL);
            }
            return;
        }
        out.append(NL);
        final int delta = depth - WSS.length;
        if (delta > -1) {
            out.append(WSS[WSS.length - 1]);
            for (int i = 0; i < delta + 1; i++) {
                out.append(WSS[1]);
            }
        } else {
            out.append(WSS[depth]);
        }
    }

    private void clearScope() {
        scopeStackDepth = 0;
    }

    private void pushScope(final int scope) {
        if (scopeStackDepth == scopeStack.length) {
            final int[] newScopeStack = new int[scopeStack.length << 1];
            System.arraycopy(scopeStack, 0, newScopeStack, 0, scopeStack.length);
            scopeStack = newScopeStack;
        }
        scopeStack[scopeStackDepth++] = scope;
    }

    private int popScope() {
        assert scopeStackDepth > 0;

        return scopeStack[--scopeStackDepth];
    }

    private int peekScope() {
        if (scopeStackDepth < 1) {
            return -1;
        }
        return scopeStack[scopeStackDepth - 1];
    }

    private void replaceScope(final int scope) {
        assert scopeStackDepth > 0;

        scopeStack[scopeStackDepth - 1] = scope;
    }

    private void writeLongNumber(final BufferingWriter out, final long mantissa) {
        final char[] buf = numberBuffer;
        int first = writeDigits(buf, NUMBER_DIGITS_END, mantissa);
        if (mantissa < 0) {
            buf[--first] = '-';
        }
        out.append(buf, first, NUMBER_DIGITS_END - first);
    }

    private void writeDecimalNumber(final BufferingWriter out, final long mantissa, final int exp) {
        if (mantissa == 0) {
            out.append('0');
            return;
        }

        final char[] buf = numberBuffer;

        int first = NUMBER_DIGITS_END;
        int last = NUMBER_DIGITS_END;

        if (exp < 0) {
            // The fractional digits go first, so a decimal point inside the mantissa,
            // which is the most frequent case, lands in its place with no digits to shift
            long v = mantissa < 0 ? mantissa : -mantissa;
            for (int i = exp; i < 0 && v != 0; i++) {
                final long q = v / 10;
                buf[--first] = (char) ('0' + (int) (q * 10 - v));
                v = q;
            }
            if (v != 0) {
                buf[--first] = '.';
                first = writeDigits(buf, first, v);
                if (mantissa < 0) {
                    buf[--first] = '-';
                }
                out.append(buf, first, last - first);
                return;
            }
        } else {
            first = writeDigits(buf, first, mantissa);
        }

        final int numOfDigits = last - first;
        final long pointPos = (long) exp + numOfDigits;

        if (numOfDigits <= pointPos && pointPos <= MAX_NUMBER_OF_DIGITS) { // no decimal point
            for (int i = numOfDigits; i < pointPos; i++) {
                buf[last++] = '0';
            }
        } else if (-6 < pointPos && pointPos <= 0) { // the decimal point comes before the mantissa
            for (long i = pointPos; i < 0; i++) {
                buf[--first] = '0';
            }
            buf[--first] = '.';
            buf[--first] = '0';
        } else { // the exponential notation
            if (numOfDigits > 1) {
                buf[first - 1] = buf[first];
                buf[first] = '.';
                first--;
            }
            final long e = pointPos - 1;
            buf[last++] = 'e';
            buf[last++] = e < 0 ? '-' : '+';
            last += numberOfDigits(e);
            writeDigits(buf, last, e);
        }

        if (mantissa < 0) {
            buf[--first] = '-';
        }
        out.append(buf, first, last - first);
    }

    /**
     * Writes the digits of the absolute value of a number.
     *
     * @param buf the buffer to write to
     * @param end the index next to the last digit, the digits are written backward from it
     * @param value the number
     * @return the index of the first digit
     */
    private static int writeDigits(final char[] buf, final int end, final long value) {
        long v = value < 0 ? value : -value; // negative, since Long.MIN_VALUE has no positive counterpart
        int p = end;
        while (v <= -100) {
            final long q = v / 100;
            final int r = (int) (q * 100 - v);
            v = q;
            buf[--p] = DIGIT_ONES[r];
            buf[--p] = DIGIT_TENS[r];
        }
        final int r = (int) -v;
        buf[--p] = DIGIT_ONES[r];
        if (r > 9) {
            buf[--p] = DIGIT_TENS[r];
        }
        return p;
    }

    private static int numberOfDigits(final long value) {
        int result = 1;
        for (long v = value; v <= -10 || v >= 10; v /= 10) {
            result++;
        }
        return result;
    }
}
