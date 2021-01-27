/**
 * MIT License
 * <p>
 * Copyright (c) 2018 Anatoly Gudkov
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
package org.green.jelly;

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
    // In ECMAScript, \u2028 and \u2029 are line terminators and must be encoded
    private static final String UNICODE_2028 = "\\u2028"; // line separator <LS>
    private static final String UNICODE_2029 = "\\u2029"; // paragraph separator <PS>

    private static final int STATE_OBJECT_STARTED = 1;
    private static final int STATE_OBJECT_MEMBER_NAME = STATE_OBJECT_STARTED + 1;
    private static final int STATE_OBJECT_MEMBER_VALUE = STATE_OBJECT_MEMBER_NAME + 1;

    private static final int STATE_ARRAY_STARTED = STATE_OBJECT_MEMBER_VALUE + 1;
    private static final int STATE_ARRAY_ITEM = STATE_ARRAY_STARTED + 1;

    private static final int MAX_NUMBER_OF_DIGITS = 19;

    private static final int DECIMAL_FORMAT_NO_POINT = 0; // no decimal point
    private static final int DECIMAL_FORMAT_INSIDE_MANTISSA = 1; // decimal point inside the mantissa
    private static final int DECIMAL_FORMAT_BEFORE_MANTISSA = 2; // decimal point comes before the mantissa
    private static final int DECIMAL_FORMAT_EXP = 3; // exponential notation

    static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    static {
        for (int i = 0; i < 0x10; i++) {
            UNICODE_ASCII_LOW[i] = "\\u000" + HEX_DIGITS[i];
        }
        for (int i = 0x10; i < UNICODE_ASCII_LOW.length; i++) {
            UNICODE_ASCII_LOW[i] = "\\u00" + HEX_DIGITS[i >>> 4 & 0x000f] + HEX_DIGITS[i & 0x000f];
        }
    }

    private final boolean indent;

    private int[] scopeStack = new int[8];
    private int scopeStackDepth;

    private JsonBufferedWriter output;

    public JsonGenerator() {
        this(true);
    }

    public JsonGenerator(final boolean indent) {
        this.indent = indent;
    }

    public JsonGenerator(final JsonBufferedWriter output) {
        this(output, true);
    }

    public JsonGenerator(final JsonBufferedWriter output, final boolean indent) {
        this(indent);
        this.output = output;
    }

    public boolean isIndent() {
        return indent;
    }

    public JsonGenerator setOutput(final JsonBufferedWriter output) {
        this.output = output;
        return this;
    }

    public void startObject() {
        writeStructureStarted('{', STATE_OBJECT_STARTED);
    }

    public void objectMember(final CharSequence name) {
        objectMember(name, 0, name.length());
    }

    public void objectMember(final CharSequence name, final int start, final int len) {
        final JsonBufferedWriter out = output;

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
        out.append(name, start, start + len);
        out.append("\":");

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
        final JsonBufferedWriter out = output;

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
        final JsonBufferedWriter out = output;

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
        final JsonBufferedWriter out = output;

        assert out != null;

        popScope();

        if (indent) {
            indent(out, true);
        }

        out.append(rightBracket);

        afterValueAdded(peekScope());
    }

    private void writeStringQuoted(final CharSequence value, final int start, final int len, final boolean escaping) {
        final JsonBufferedWriter out = output;

        assert out != null;

        final int scope = peekScope();

        beforeLiteralAdded(scope, out);

        out.append('\"');

        if (escaping) {
            for (int i = start; i < start + len; i++) {
                final char c = value.charAt(i);
                switch (c) {
                    case 0x08:
                        out.append("\\b");
                        break;
                    case 0x09:
                        out.append("\\t");
                        break;
                    case 0x0A:
                        out.append("\\n");
                        break;
                    case 0x0C:
                        out.append("\\f");
                        break;
                    case 0x0D:
                        out.append("\\r");
                        break;
                    case '/':
                        out.append("\\/");
                        break;
                    case '\\':
                        out.append("\\\\");
                        break;
                    case '"':
                        out.append("\\\"");
                        break;
                    case 0x2028:
                        out.append(UNICODE_2028);
                        break;
                    case 0x2029:
                        out.append(UNICODE_2029);
                        break;
                    default:
                        if (c < UNICODE_ASCII_LOW.length) {
                            out.append(UNICODE_ASCII_LOW[c]);
                            break;
                        }
                        out.append(c);
                        break;
                }
            }
        } else {
            out.append(value, start, start + len);
        }

        out.append("\"");

        afterValueAdded(scope);
    }

    private void writeStringDirect(final String value) {
        final JsonBufferedWriter out = output;

        assert out != null;

        final int scope = peekScope();

        beforeLiteralAdded(scope, out);

        out.append(value);

        afterValueAdded(scope);
    }

    private void writeNumberQuoted(final long mantissa, final int exp) {
        final JsonBufferedWriter out = output;

        assert out != null;

        final int scope = peekScope();

        beforeLiteralAdded(scope, out);

        out.append('\"');

        if (exp == 0) {
            writeLongNumber(out, mantissa);
        } else {
            writeDecimalNumber(out, mantissa, exp);
        }

        out.append("\"");

        afterValueAdded(scope);
    }

    private void beforeLiteralAdded(final int scope, final JsonBufferedWriter out) {
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

    private void indent(final JsonBufferedWriter out, final boolean ending) {
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

    private static void writeLongNumber(final JsonBufferedWriter out, final long mantissa) {
        long mts = mantissa;
        if (mts == Long.MIN_VALUE) {
            out.append("-9223372036854775808");
            return;
        }
        if (mts == 0) {
            out.append("0");
            return;
        }

        char sgn = 0;
        final int size;
        if (mts < 0) {
            sgn = '-';
            mts = -mts;
            size = numberOfDigits(mts) + 1;
        } else {
            size = numberOfDigits(mts);
        }

        writeLongValueToEndOfTheFrame(out.append(size), mts, sgn);
    }

    private static void writeLongValueToEndOfTheFrame(final JsonBufferedWriter.Frame buf,
                                                      final long mantissa, final char sign) {

        long mts = mantissa;
        int currentCharIndex = buf.length();

        long x;
        int y;
        while (mts > Integer.MAX_VALUE) {
            x = mts / 100;
            y = (int) (mts - ((x << 6) + (x << 5) + (x << 2)));
            mts = x;
            buf.setCharAt(--currentCharIndex, DIGIT_ONES[y]);
            buf.setCharAt(--currentCharIndex, DIGIT_TENS[y]);
        }
        int x2;
        int mts2 = (int) mts;
        while (mts2 >= 65536) {
            x2 = mts2 / 100;
            y = mts2 - ((x2 << 6) + (x2 << 5) + (x2 << 2));
            mts2 = x2;
            buf.setCharAt(--currentCharIndex, DIGIT_ONES[y]);
            buf.setCharAt(--currentCharIndex, DIGIT_TENS[y]);
        }
        while (true) {
            x2 = (mts2 * 52429) >>> (19);
            y = mts2 - ((x2 << 3) + (x2 << 1));
            buf.setCharAt(--currentCharIndex, HEX_DIGITS[y]);
            mts2 = x2;
            if (mts2 == 0) {
                break;
            }
        }
        if (sign != 0) {
            buf.setCharAt(--currentCharIndex, sign);
        }
    }

    private static void writeDecimalNumber(final JsonBufferedWriter out,
                                           final long mantissa, final int exp) {

        long mts = mantissa;
        int e = exp;
        if (mts == 0) {
            out.append("0");
            return;
        }

        char mantissaSign = 0;
        int size;
        final int numOfDigits;
        if (mts < 0) {
            mantissaSign = '-';
            mts = -mts;
            numOfDigits = numberOfDigits(mts);
            size = numOfDigits + 1;
        } else {
            numOfDigits = numberOfDigits(mts);
            size = numOfDigits;
        }

        final int pointPos = e + numOfDigits;

        char expSign = '+';
        int additionalNumbers = 0;
        int expNumOfDigits = 0;

        int mode = DECIMAL_FORMAT_NO_POINT;
        if (numOfDigits <= pointPos && pointPos <= MAX_NUMBER_OF_DIGITS) { // mode = DECIMAL_FORMAT_NO_POINT

            additionalNumbers = pointPos - numOfDigits; // leading zeros
            size += additionalNumbers;

        } else if (0 < pointPos
                && pointPos <= MAX_NUMBER_OF_DIGITS
                && pointPos < numOfDigits) {

            mode = DECIMAL_FORMAT_INSIDE_MANTISSA;
            size++; // for the point

        } else if (-6 < pointPos && pointPos <= 0) {

            mode = DECIMAL_FORMAT_BEFORE_MANTISSA;
            size = size + 2 + (-pointPos); // for 0 and the point and -pointPos zeros before the mantissa

        } else if (pointPos <= -6 || pointPos > MAX_NUMBER_OF_DIGITS) {
            mode = DECIMAL_FORMAT_EXP;

            e = pointPos - 1;
            if (e < 0) {
                expSign = '-';
                e = -e;
            }

            expNumOfDigits = numberOfDigits(e);
            size = size + (numOfDigits > 1 ? 1 : 0) // for the point if more than 1 digit
                    + 2 + expNumOfDigits; // and 'e' and the sign and exponent's numbers
        }

        final JsonBufferedWriter.Frame buf = out.append(size);

        int currentCharIndex = size;

        switch (mode) {
            case DECIMAL_FORMAT_NO_POINT:
                for (int i = 0; i < additionalNumbers; i++) {
                    buf.setCharAt(--currentCharIndex, '0');
                }
                break;
            case DECIMAL_FORMAT_EXP:
                writeLongValueToEndOfTheFrame(buf, e, expSign);
                currentCharIndex = currentCharIndex - expNumOfDigits - 1;
                buf.setCharAt(--currentCharIndex, 'e');
                break;
        }

        int remainingNumCount = numOfDigits;

        long x;
        int y;
        while (mts > Integer.MAX_VALUE) {
            x = mts / 100;
            y = (int) (mts - ((x << 6) + (x << 5) + (x << 2)));
            mts = x;

            buf.setCharAt(--currentCharIndex, DIGIT_ONES[y]);
            remainingNumCount--;
            switch (mode) {
                case DECIMAL_FORMAT_INSIDE_MANTISSA:
                    if (remainingNumCount == pointPos) {
                        buf.setCharAt(--currentCharIndex, '.');
                    }
                    break;
                case DECIMAL_FORMAT_EXP:
                    if (remainingNumCount == 1 && numOfDigits > 1) {
                        buf.setCharAt(--currentCharIndex, '.');
                    }
                    break;
            }

            buf.setCharAt(--currentCharIndex, DIGIT_TENS[y]);
            remainingNumCount--;
            switch (mode) {
                case DECIMAL_FORMAT_INSIDE_MANTISSA:
                    if (remainingNumCount == pointPos) {
                        buf.setCharAt(--currentCharIndex, '.');
                    }
                    break;
                case DECIMAL_FORMAT_EXP:
                    if (remainingNumCount == 1 && numOfDigits > 1) {
                        buf.setCharAt(--currentCharIndex, '.');
                    }
                    break;
            }
        }

        int x2;
        int mantissa2 = (int) mts;
        while (mantissa2 >= 65536) {
            x2 = mantissa2 / 100;
            y = mantissa2 - ((x2 << 6) + (x2 << 5) + (x2 << 2));
            mantissa2 = x2;

            buf.setCharAt(--currentCharIndex, DIGIT_ONES[y]);
            remainingNumCount--;
            switch (mode) {
                case DECIMAL_FORMAT_INSIDE_MANTISSA:
                    if (remainingNumCount == pointPos) {
                        buf.setCharAt(--currentCharIndex, '.');
                    }
                    break;
                case DECIMAL_FORMAT_EXP:
                    if (remainingNumCount == 1 && numOfDigits > 1) {
                        buf.setCharAt(--currentCharIndex, '.');
                    }
                    break;
            }

            buf.setCharAt(--currentCharIndex, DIGIT_TENS[y]);
            remainingNumCount--;
            switch (mode) {
                case DECIMAL_FORMAT_INSIDE_MANTISSA:
                    if (remainingNumCount == pointPos) {
                        buf.setCharAt(--currentCharIndex, '.');
                    }
                    break;
                case DECIMAL_FORMAT_EXP:
                    if (remainingNumCount == 1 && numOfDigits > 1) {
                        buf.setCharAt(--currentCharIndex, '.');
                    }
                    break;
            }
        }

        while (true) {
            x2 = (mantissa2 * 52429) >>> (19);
            y = mantissa2 - ((x2 << 3) + (x2 << 1));

            buf.setCharAt(--currentCharIndex, HEX_DIGITS[y]);
            remainingNumCount--;
            switch (mode) {
                case DECIMAL_FORMAT_INSIDE_MANTISSA:
                    if (remainingNumCount == pointPos) {
                        buf.setCharAt(--currentCharIndex, '.');
                    }
                    break;
                case DECIMAL_FORMAT_EXP:
                    if (remainingNumCount == 1 && numOfDigits > 1) {
                        buf.setCharAt(--currentCharIndex, '.');
                    }
                    break;
            }

            mantissa2 = x2;
            if (mantissa2 == 0) {
                break;
            }
        }

        if (mode == DECIMAL_FORMAT_BEFORE_MANTISSA) {
            for (int i = 0; i < -pointPos; i++) {
                buf.setCharAt(--currentCharIndex, '0');
            }
            buf.setCharAt(--currentCharIndex, '.');
            buf.setCharAt(--currentCharIndex, '0');
        }

        if (mantissaSign != 0) {
            buf.setCharAt(--currentCharIndex, mantissaSign);
        }
    }

    private static int numberOfDigits(final long x) {
        assert x >= 0;

        if (x < 1_000_000L) {
            if (x < 1_000L) {
                if (x < 100L) {
                    if (x < 10L) {
                        return 1;
                    }
                    return 2;
                }
                return 3;
            }
            if (x < 10_000) {
                return 4;
            }
            if (x < 100_000) {
                return 5;
            }
            return 6;
        }
        if (x < 1_000_000_000L) {
            if (x < 100_000_000L) {
                if (x < 10_000_000L) {
                    return 7;
                }
                return 8;
            }
            return 9;
        }
        if (x < 100_000_000_000_000L) {
            if (x < 1_000_000_000_000L) {
                if (x < 10_000_000_000L) {
                    return 10;
                }
                if (x < 100_000_000_000L) {
                    return 11;
                }
                return 12;
            }
            if (x < 10_000_000_000_000L) {
                return 13;
            }
            return 14;
        }
        if (x < 100_000_000_000_000_000L) {
            if (x < 1_000_000_000_000_000L) {
                return 15;
            }
            if (x < 10_000_000_000_000_000L) {
                return 16;
            }
            return 17;
        }
        if (x < 1_000_000_000_000_000_000L) {
            return 18;
        }
        return 19;
    }
}
