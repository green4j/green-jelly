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
package io.github.green4j.jelly;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Parses JSON encoded in UTF-8 the same way {@link JsonParser} parses a character sequence,
 * but reads the bytes directly, with no decoding of the whole data to chars and no copying:
 * a {@code byte[]}, a {@link ByteBuffer}, either heap or direct one, or any {@link ByteSequence}.
 * All the chars of the syntax of JSON are ASCII and a byte of a char encoded with several bytes
 * is never ASCII, so only the bytes of string values are decoded, by the {@link JsonUtf8StringBuilder}.
 * A char cut by the end of a part of the data is decoded when the rest of it comes with the next part.
 */
public final class Utf8JsonParser extends AbstractJsonParser {

    private static final String ERROR_INCORRECT_UTF8_MESSAGE = "Incorrect UTF-8 encoding";

    private final JsonParser.Next next = () -> Utf8JsonParser.this.next();

    private final JsonUtf8StringBuilder string;

    private final ByteSource currentData = new ByteSource();

    public Utf8JsonParser() {
        this(new CopyingStringBuilder());
    }

    public Utf8JsonParser(final JsonUtf8StringBuilder stringBuilder) {
        super(stringBuilder);
        string = stringBuilder;
    }

    public Utf8JsonParser setNotifyObjectMemberNameString(final boolean notifyObjectMemberNameString) {
        this.notifyObjectMemberNameString = notifyObjectMemberNameString;
        return this;
    }

    public Utf8JsonParser setListener(final JsonParserListener listener) {
        this.listener = listener;
        return this;
    }

    public void parseAndEoj(final byte[] data) {
        parse(data);
        eoj();
    }

    public void parseAndEoj(final ByteBuffer data) {
        parse(data);
        eoj();
    }

    public void parseAndEoj(final ByteSequence data) {
        parse(data);
        eoj();
    }

    public JsonParser.Next parse(final byte[] data) {
        return parse(data, 0, data.length);
    }

    public JsonParser.Next parse(final byte[] data, final int start, final int len) {
        Objects.checkFromIndexSize(start, len, data.length);
        currentData.set(data);
        return start(start, len);
    }

    /**
     * Parses the bytes between the position and the limit of the buffer.
     * Neither the position nor the limit is changed.
     *
     * @param data the buffer
     * @return not null if the parsing has been stopped by the listener and there are more bytes to parse
     */
    public JsonParser.Next parse(final ByteBuffer data) {
        return parse(data, data.position(), data.remaining());
    }

    /**
     * Parses the bytes of the buffer at the indexes given.
     * Neither the position nor the limit is used or changed.
     *
     * @param data the buffer
     * @param start the index of the first byte
     * @param len the number of the bytes
     * @return not null if the parsing has been stopped by the listener and there are more bytes to parse
     */
    public JsonParser.Next parse(final ByteBuffer data, final int start, final int len) {
        Objects.checkFromIndexSize(start, len, data.limit());
        if (data.hasArray()) {
            // a heap buffer is read as an array, so only direct buffers are read as buffers
            currentData.set(data.array());
            return start(data.arrayOffset() + start, len);
        }
        currentData.set(data);
        return start(start, len);
    }

    public JsonParser.Next parse(final ByteSequence data) {
        return parse(data, 0, data.length());
    }

    public JsonParser.Next parse(final ByteSequence data, final int start, final int len) {
        Objects.checkFromIndexSize(start, len, data.length());
        currentData.set(data);
        return start(start, len);
    }

    private JsonParser.Next start(final int start, final int len) {
        this.currentStart = start;
        this.currentLen = len;
        this.currentPos = 0;
        return next();
    }

    private JsonParser.Next next() {
        final JsonParserListener lnr = listener;

        assert lnr != null;

        onJsonStartedIfRequired(lnr);

        int r = 0;
        if (currentLexemaState < LEXEMA_READY) {
            r = resumeLexema(lnr);
        }
        if (r == 0) {
            r = parseLexemas(lnr);
        }
        return r > 0 && currentPos < currentLen ? next : null;
    }

    // The hot path: reads complete lexemas in a loop. A lexema cut by the end of the data
    // is finished by resumeLexema(), when the rest of it comes.
    // Returns the same as resumeLexema().
    private int parseLexemas(final JsonParserListener lnr) {
        final ByteSource data = currentData;
        final int base = currentStart;
        final int end = base + currentLen;

        int p = base + currentPos;
        int lexPos = currentLexemaPosition;
        int lexState = currentLexemaState;
        boolean failed = false;

        try {
            _end:
            while (p < end) {
                final byte c = data.byteAt(p);
                int r;

                switch (c) {
                    case 0x09:
                    case 0x0a:
                    case 0x0d:
                    case 0x20:
                        p = skipWhitespaces(data, p + 1, end);
                        continue;
                    case '{':
                        lexPos = p++ - base;
                        lexState = LEXEMA_CURLY_BRACKET_LEFT_READY;
                        r = onCurlyBracketLeft(lnr, lexPos);
                        break;
                    case '}':
                        lexPos = p++ - base;
                        lexState = LEXEMA_CURLY_BRACKET_RIGHT_READY;
                        r = onCurlyBracketRight(lnr, lexPos);
                        break;
                    case '[':
                        lexPos = p++ - base;
                        lexState = LEXEMA_BOX_BRACKET_LEFT_READY;
                        r = onBoxBracketLeft(lnr, lexPos);
                        break;
                    case ']':
                        lexPos = p++ - base;
                        lexState = LEXEMA_BOX_BRACKET_RIGHT_READY;
                        r = onBoxBracketRight(lnr, lexPos);
                        break;
                    case ',':
                        lexPos = p++ - base;
                        lexState = LEXEMA_COMMA_READY;
                        r = onComma(lexPos);
                        break;
                    case ':':
                        lexPos = p++ - base;
                        lexState = LEXEMA_COLON_READY;
                        r = onColon(lexPos);
                        break;
                    case 't':
                        if (lexState > LEXEMA_NUMBER_READY) {
                            error(ERROR_TRUE_EXPECTED_MESSAGE, p - base);
                            failed = true;
                            break _end;
                        }
                        lexPos = p - base;
                        if (end - p > 3) { // try to read the whole 'true' value
                            if (data.byteAt(p + 1) == 'r' && data.byteAt(p + 2) == 'u' && data.byteAt(p + 3) == 'e') {
                                p += 4;
                                lexState = LEXEMA_TRUE_READY;
                                r = onTrue(lnr, lexPos);
                                break;
                            }
                            error(ERROR_TRUE_EXPECTED_MESSAGE, lexPos);
                            failed = true;
                            break _end;
                        }
                        p++;
                        lexState = LEXEMA_TRUE_STARTED_T;
                        r = RESUME_LEXEMA_REQUIRED;
                        break;
                    case 'f':
                        if (lexState > LEXEMA_NUMBER_READY) {
                            error(ERROR_FALSE_EXPECTED_MESSAGE, p - base);
                            failed = true;
                            break _end;
                        }
                        lexPos = p - base;
                        if (end - p > 4) { // try to read the whole 'false' value
                            if (data.byteAt(p + 1) == 'a' && data.byteAt(p + 2) == 'l'
                                    && data.byteAt(p + 3) == 's' && data.byteAt(p + 4) == 'e') {
                                p += 5;
                                lexState = LEXEMA_FALSE_READY;
                                r = onFalse(lnr, lexPos);
                                break;
                            }
                            error(ERROR_FALSE_EXPECTED_MESSAGE, lexPos);
                            failed = true;
                            break _end;
                        }
                        p++;
                        lexState = LEXEMA_FALSE_STARTED_F;
                        r = RESUME_LEXEMA_REQUIRED;
                        break;
                    case 'n':
                        if (lexState > LEXEMA_NUMBER_READY) {
                            error(ERROR_NULL_EXPECTED_MESSAGE, p - base);
                            failed = true;
                            break _end;
                        }
                        lexPos = p - base;
                        if (end - p > 3) { // try to read the whole 'null' value
                            if (data.byteAt(p + 1) == 'u' && data.byteAt(p + 2) == 'l' && data.byteAt(p + 3) == 'l') {
                                p += 4;
                                lexState = LEXEMA_NULL_READY;
                                r = onNull(lnr, lexPos);
                                break;
                            }
                            error(ERROR_NULL_EXPECTED_MESSAGE, lexPos);
                            failed = true;
                            break _end;
                        }
                        p++;
                        lexState = LEXEMA_NULL_STARTED_N;
                        r = RESUME_LEXEMA_REQUIRED;
                        break;
                    case '"': {
                        lexPos = p - base;
                        final JsonUtf8StringBuilder stringBuilder = string;
                        stringBuilder.start();

                        final int plainStart = p + 1;
                        final int plainEnd = skipPlainChars(data, plainStart, end);
                        final int plainLen = plainEnd - plainStart;
                        final int appended = data.appendTo(stringBuilder, plainStart, plainLen);
                        if (appended != plainLen) {
                            p = plainStart + appended;
                            lexState = LEXEMA_STRING_STARTED;
                            error(ERROR_INCORRECT_UTF8_MESSAGE, p - base);
                            failed = true;
                            break _end;
                        }
                        if (plainEnd == end) { // no more data
                            p = end;
                            lexState = LEXEMA_STRING_STARTED;
                            break _end;
                        }
                        if (!stringBuilder.complete()) {
                            p = plainEnd;
                            lexState = LEXEMA_STRING_STARTED;
                            error(ERROR_INCORRECT_UTF8_MESSAGE, p - base);
                            failed = true;
                            break _end;
                        }
                        if (data.byteAt(plainEnd) == '"') { // the most frequent case
                            p = plainEnd + 1;
                            lexState = LEXEMA_STRING_READY;
                            r = onStringReady(lnr, lexPos);
                            break;
                        }

                        stringBuilder.appendEscape();
                        final int stringEnd = scanString(LEXEMA_STRING_STARTED_ESCAPE, data, plainEnd + 1, end);
                        lexState = currentLexemaState;
                        if (stringEnd < 0) {
                            failed = true;
                            break _end;
                        }
                        p = stringEnd;
                        if (lexState != LEXEMA_STRING_READY) { // no more data
                            break _end;
                        }
                        r = onStringReady(lnr, lexPos);
                        break;
                    }
                    case '-':
                    case '+':
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9': {
                        lexPos = p - base;
                        number.setMantissa(0);
                        number.setExp(0);
                        numberMantissaExp = 0;
                        numberMinuses = c == '-' ? 2 : 0;
                        numberOverflow = false;

                        final int numberEnd;
                        if (c < '0') {
                            numberEnd = scanNumber(LEXEMA_NUMBER_STARTED_MANTISSA_SIGN, data, p + 1, end);
                        } else {
                            final int digitsEnd = scanDigits(data, p, end);
                            // the most frequent case
                            if (digitsEnd < end && isNumberDelimiter(data.byteAt(digitsEnd))) {
                                p = digitsEnd;
                                lexState = LEXEMA_NUMBER_READY;
                                r = onNumber(lnr, lexPos);
                                break;
                            }
                            numberEnd = scanNumber(LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART, data, digitsEnd, end);
                        }
                        lexState = currentLexemaState;
                        if (numberEnd < 0) {
                            failed = true;
                            break _end;
                        }
                        p = numberEnd;
                        if (lexState != LEXEMA_NUMBER_READY) { // no more data
                            break _end;
                        }
                        r = onNumber(lnr, lexPos);
                        break;
                    }
                    default:
                        error(ERROR_UNEXPECTED_CHAR_MESSAGE, p - base);
                        failed = true;
                        break _end;
                }

                if (r == RESUME_LEXEMA_REQUIRED) {
                    currentPos = p - base;
                    currentLexemaState = lexState;
                    currentLexemaPosition = lexPos;
                    r = resumeLexema(lnr);
                    p = base + currentPos;
                    lexState = currentLexemaState;
                }
                if (r != 0) {
                    return r;
                }
            }
            return failed ? -1 : 0;
        } finally {
            currentPos = p - base;
            currentLexemaState = lexState;
            currentLexemaPosition = lexPos;
        }
    }

    // Continues the lexema which has been cut by the end of the previous part of the data.
    // Returns the same as the on...() methods: 0 to go on (the lexema is either finished or the data
    // is over again), a positive value if the listener has stopped the parsing, a negative one
    // in the case of an error.
    private int resumeLexema(final JsonParserListener lnr) {
        final ByteSource data = currentData;
        final int base = currentStart;
        final int end = base + currentLen;
        final int state = currentLexemaState;

        if (state >= LEXEMA_STRING_STARTED && state <= LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3) {
            final int stringEnd = scanString(state, data, base + currentPos, end);
            if (stringEnd < 0) {
                return -1;
            }
            currentPos = stringEnd - base;
            return currentLexemaState == LEXEMA_STRING_READY ? onStringReady(lnr, currentLexemaPosition) : 0;
        }
        if (state >= LEXEMA_NUMBER_STARTED_MANTISSA_SIGN && state <= LEXEMA_NUMBER_STARTED_E_VALUE) {
            final int numberEnd = scanNumber(state, data, base + currentPos, end);
            if (numberEnd < 0) {
                return -1;
            }
            currentPos = numberEnd - base;
            return currentLexemaState == LEXEMA_NUMBER_READY ? onNumber(lnr, currentLexemaPosition) : 0;
        }
        return resumeLiteral(lnr, data, base + currentPos, end);
    }

    private int resumeLiteral(final JsonParserListener lnr, final ByteSource data, final int from, final int end) {
        int state = currentLexemaState;

        for (int p = from; p < end; p++) {
            if (data.byteAt(p) != LITERAL_EXPECTED_CHARS[state]) {
                currentPos = p - currentStart;
                currentLexemaState = state;
                error(state < LEXEMA_FALSE_STARTED_F ? ERROR_TRUE_EXPECTED_MESSAGE
                        : state < LEXEMA_NULL_STARTED_N ? ERROR_FALSE_EXPECTED_MESSAGE
                        : ERROR_NULL_EXPECTED_MESSAGE, p - currentStart);
                return -1;
            }
            switch (state) {
                case LEXEMA_TRUE_STARTED_TRU:
                    currentPos = p + 1 - currentStart;
                    currentLexemaState = LEXEMA_TRUE_READY;
                    return onTrue(lnr, currentLexemaPosition);
                case LEXEMA_FALSE_STARTED_FALS:
                    currentPos = p + 1 - currentStart;
                    currentLexemaState = LEXEMA_FALSE_READY;
                    return onFalse(lnr, currentLexemaPosition);
                case LEXEMA_NULL_STARTED_NUL:
                    currentPos = p + 1 - currentStart;
                    currentLexemaState = LEXEMA_NULL_READY;
                    return onNull(lnr, currentLexemaPosition);
                default:
                    state++;
                    break;
            }
        }
        currentPos = end - currentStart;
        currentLexemaState = state;
        return 0;
    }

    // Reads a string value from the given state up to the closing quote or the end of the data.
    // The state the reading has finished with is stored to currentLexemaState.
    // Returns the index of the byte next to the last one read, or -1 in the case of an error.
    private int scanString(final int fromState, final ByteSource data, final int from, final int end) {
        final JsonUtf8StringBuilder stringBuilder = string;

        int state = fromState;
        int p = from;

        while (p < end) {
            if (state == LEXEMA_STRING_STARTED) {
                final int plainStart = p;
                p = skipPlainChars(data, p, end);
                final int appended = data.appendTo(stringBuilder, plainStart, p - plainStart);
                if (appended != p - plainStart) {
                    return onIncorrectUtf8(plainStart + appended);
                }
                if (p == end) {
                    break;
                }
                if (!stringBuilder.complete()) {
                    return onIncorrectUtf8(p);
                }
                if (data.byteAt(p++) == '"') {
                    state = LEXEMA_STRING_READY;
                    break;
                }
                stringBuilder.appendEscape();
                state = LEXEMA_STRING_STARTED_ESCAPE;
                continue;
            }

            final byte c = data.byteAt(p);

            switch (state) {
                case LEXEMA_STRING_STARTED_ESCAPE: {
                    state = LEXEMA_STRING_STARTED;
                    switch (c) {
                        case '"':
                            stringBuilder.appendEscapedQuotationMark();
                            break;
                        case '\\':
                            stringBuilder.appendEscapedReverseSolidus();
                            break;
                        case '/':
                            stringBuilder.appendEscapedSolidus();
                            break;
                        case 'b':
                            stringBuilder.appendEscapedBackspace();
                            break;
                        case 'f':
                            stringBuilder.appendEscapedFormfeed();
                            break;
                        case 'n':
                            stringBuilder.appendEscapedNewLine();
                            break;
                        case 'r':
                            stringBuilder.appendEscapedCarriageReturn();
                            break;
                        case 't':
                            stringBuilder.appendEscapedHorisontalTab();
                            break;
                        case 'u':
                            stringBuilder.appendEscapedUnicodeU();
                            state = LEXEMA_STRING_STARTED_ESCAPE_UNICODE;
                            break;
                        default:
                            currentLexemaState = LEXEMA_STRING_STARTED_ESCAPE;
                            error(ERROR_INCORRECT_ESCAPING_MESSAGE, p - currentStart);
                            return -1;
                    }
                    break;
                }
                case LEXEMA_STRING_STARTED_ESCAPE_UNICODE: {
                    if (stringBuilder.appendEscapedUnicodeChar1((char) c)) {
                        state = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1;
                        break;
                    }
                    return onIncorrectUnicodeEscaping(state, p);
                }
                case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1: {
                    if (stringBuilder.appendEscapedUnicodeChar2((char) c)) {
                        state = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2;
                        break;
                    }
                    return onIncorrectUnicodeEscaping(state, p);
                }
                case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2: {
                    if (stringBuilder.appendEscapedUnicodeChar3((char) c)) {
                        state = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3;
                        break;
                    }
                    return onIncorrectUnicodeEscaping(state, p);
                }
                default: { // LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3
                    if (stringBuilder.appendEscapedUnicodeChar4((char) c)) {
                        state = LEXEMA_STRING_STARTED;
                        break;
                    }
                    return onIncorrectUnicodeEscaping(state, p);
                }
            }
            p++;
        }

        currentLexemaState = state;
        return p;
    }

    private int onIncorrectUtf8(final int p) {
        currentLexemaState = LEXEMA_STRING_STARTED;
        error(ERROR_INCORRECT_UTF8_MESSAGE, p - currentStart);
        return -1;
    }

    private static final long QUOTES = '"' * ByteSource.ONES;
    private static final long REVERSE_SOLIDUSES = '\\' * ByteSource.ONES;

    private static int skipWhitespaces(final ByteSource data, final int from, final int end) {
        int p = from;
        while (p < end) {
            final byte c = data.byteAt(p);
            if (c != 0x20 && c != 0x0a && c != 0x0d && c != 0x09) {
                break;
            }
            p++;
        }
        return p;
    }

    private static int skipPlainChars(final ByteSource data, final int from, final int end) {
        int p = from;
        while (end - p >= 8) {
            final long bytes = data.longAt(p);
            final long found = ByteSource.zeroBytes(bytes ^ QUOTES) | ByteSource.zeroBytes(bytes ^ REVERSE_SOLIDUSES);
            if (found != 0) {
                return p + (Long.numberOfTrailingZeros(found) >>> 3);
            }
            p += 8;
        }
        while (p < end) {
            final byte c = data.byteAt(p);
            if (c == '"' || c == '\\') {
                break;
            }
            p++;
        }
        return p;
    }

    // Reads as many digits of a number, which can't overflow the mantissa, as there are in a row.
    // The result is left in the number, so scanNumber() can continue from the index returned.
    private int scanDigits(final ByteSource data, final int from, final int end) {
        final int limit = Math.min(end, from + MAX_SAFE_DIGITS);
        long mantissa = 0;
        int p = from;
        while (p < limit) {
            final int digit = data.byteAt(p) - '0';
            if (digit < 0 || digit > 9) {
                break;
            }
            mantissa = mantissa * 10 + digit;
            p++;
        }
        number.setMantissa(mantissa);
        return p;
    }

    private static boolean isNumberDelimiter(final byte c) {
        return c >= 0 && NUMBER_DELIMITERS[c];
    }

    // Reads a number value from the given state up to the delimiter next to the number or the end
    // of the data. The state the reading has finished with is stored to currentLexemaState.
    // Returns the index of the first byte which hasn't been read, or -1 in the case of an error.
    private int scanNumber(final int fromState, final ByteSource data, final int from, final int end) {
        long mantissa = number.mantissa();
        int mantissaExp = numberMantissaExp;
        int exp = number.exp();

        int state = fromState;
        int p = from;
        boolean failed = false;

        _end:
        while (p < end) {
            byte c = data.byteAt(p);

            switch (state) {
                case LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART: {
                    int digit = c - '0';
                    while (digit >= 0 && digit <= 9) {
                        if (mantissa <= MAX_MANTISSA_BASE) {
                            mantissa = mantissa * 10 + digit;
                        } else {
                            onMantissaOverflown(mantissa, false, mantissaExp, (char) c);
                            mantissaExp++;
                        }
                        if (++p == end) {
                            break _end;
                        }
                        c = data.byteAt(p);
                        digit = c - '0';
                    }
                    if (c == '.') {
                        if (numberOverflow) {
                            overflownNumber.append('.');
                        }
                        state = LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART;
                        p++;
                        continue;
                    }
                    if (c == 'e' || c == 'E') {
                        state = LEXEMA_NUMBER_STARTED_E;
                        p++;
                        continue;
                    }
                    break;
                }
                case LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART: {
                    int digit = c - '0';
                    while (digit >= 0 && digit <= 9) {
                        if (mantissa <= MAX_MANTISSA_BASE) {
                            mantissa = mantissa * 10 + digit;
                            mantissaExp--;
                        } else {
                            onMantissaOverflown(mantissa, true, mantissaExp, (char) c);
                        }
                        if (++p == end) {
                            break _end;
                        }
                        c = data.byteAt(p);
                        digit = c - '0';
                    }
                    if (c == 'e' || c == 'E') {
                        state = LEXEMA_NUMBER_STARTED_E;
                        p++;
                        continue;
                    }
                    break;
                }
                case LEXEMA_NUMBER_STARTED_MANTISSA_SIGN: {
                    final int digit = c - '0';
                    if (digit >= 0 && digit <= 9) {
                        mantissa = digit;
                        state = LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART;
                        p++;
                        continue;
                    }
                    failed = true;
                    break _end;
                }
                case LEXEMA_NUMBER_STARTED_E:
                case LEXEMA_NUMBER_STARTED_E_SIGN: {
                    if (state == LEXEMA_NUMBER_STARTED_E && (c == '+' || c == '-')) {
                        if (c == '-') {
                            numberMinuses = numberMinuses | 1;
                        }
                        state = LEXEMA_NUMBER_STARTED_E_SIGN;
                        p++;
                        continue;
                    }
                    final int digit = c - '0';
                    if (digit >= 0 && digit <= 9) {
                        exp = exp * 10 + digit;
                        state = LEXEMA_NUMBER_STARTED_E_VALUE;
                        p++;
                        continue;
                    }
                    failed = true;
                    break _end;
                }
                default: { // LEXEMA_NUMBER_STARTED_E_VALUE
                    final int digit = c - '0';
                    if (digit >= 0 && digit <= 9) {
                        exp = exp * 10 + digit;
                        p++;
                        continue;
                    }
                    break;
                }
            }

            // the byte isn't a part of the number
            if (isNumberDelimiter(c)) {
                state = LEXEMA_NUMBER_READY;
            } else {
                failed = true;
            }
            break;
        }

        number.setMantissa(mantissa);
        number.setExp(exp);
        numberMantissaExp = mantissaExp;
        currentLexemaState = state;

        if (failed) {
            error(ERROR_INCORRECT_NUMBER_MESSAGE, p - currentStart);
            return -1;
        }
        return p;
    }

    /**
     * End of JSON
     *
     * @return this
     */
    public Utf8JsonParser eoj() {
        doEoj();
        return this;
    }

    public Utf8JsonParser reset() {
        doResetAndNotify();
        return this;
    }
}
