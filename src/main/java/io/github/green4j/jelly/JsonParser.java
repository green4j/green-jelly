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

import java.util.Objects;

/**
 * Parses JSON character sequence.
 */
public final class JsonParser extends AbstractJsonParser {

    public interface Next {
        Next next();
    }

    public static final long MAX_MANTISSA_VALUE = MAX_MANTISSA_BASE * 10 + 9;

    private static final ThreadLocal<StringBuilder> LAST_OVERFLOWN_NUMBER =
            ThreadLocal.withInitial(StringBuilder::new);

    /**
     * The full decimal representation of the last number which overflowed the mantissa while
     * being parsed with {@link #parseNumber(CharSequence, MutableJsonNumber)} by the current thread.
     *
     * <p>The content is valid only right after a call of {@link #parseNumber(CharSequence, MutableJsonNumber)}
     * which returned {@code true}. It's a well-formed decimal literal, so it can be passed to
     * {@code new java.math.BigDecimal(CharSequence)} as is.
     *
     * @return the buffer with the text of the last overflown number
     */
    public static CharSequence lastOverflownNumber() {
        return LAST_OVERFLOWN_NUMBER.get();
    }

    /**
     * Parses a number value and stores the result into the given {@link MutableJsonNumber}.
     *
     * <p>If the mantissa of the number doesn't fit into a {@code long}, the number is truncated
     * (see {@link #MAX_MANTISSA_VALUE}) and the full decimal representation of the number is stored
     * into a thread local buffer available with {@link #lastOverflownNumber()}.
     *
     * @param data the text of the number
     * @param to the result
     * @return {@code true} if the mantissa has been overflown
     */
    public static boolean parseNumber(final CharSequence data, final MutableJsonNumber to) {
        return parseNumber(data, to, null);
    }

    /**
     * Parses a number value and stores the result into the given {@link MutableJsonNumber}.
     *
     * <p>If the mantissa of the number doesn't fit into a {@code long}, the number is truncated
     * (see {@link #MAX_MANTISSA_VALUE}) and the full decimal representation of the number is stored
     * into {@code overflownNumber}. The text is a well-formed decimal literal, so it can be passed to
     * {@code new java.math.BigDecimal(CharSequence)} as is. The buffer is touched only when the
     * overflow happens, so the normal path of the parsing doesn't pay for this fallback.
     *
     * @param data the text of the number
     * @param to the result
     * @param overflownNumber the buffer to store the text of the number into if the mantissa is overflown.
     *                        If {@code null}, the thread local buffer, available with
     *                        {@link #lastOverflownNumber()}, is used
     * @return {@code true} if the mantissa has been overflown
     */
    public static boolean parseNumber(final CharSequence data,
                                      final MutableJsonNumber to,
                                      final StringBuilder overflownNumber) {
        int currentLexState = LEXEMA_READY;

        int numberMantissaExp = 0;
        int numberMinuses = 0;
        boolean numberOverflow = false;
        StringBuilder overflown = overflownNumber;

        final int len = data.length();

        _end:
        for (int pos = 0; pos < len; pos++) {
            char c = data.charAt(pos);

            _next_char:
            switch (currentLexState) {
                case LEXEMA_READY: {
                    switch (c) {
                        case '-': {
                            to.setMantissa(0);
                            to.setExp(0);
                            numberMantissaExp = 0;
                            numberMinuses = 2;
                            currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_SIGN;
                            break _next_char;
                        }
                        case '+': {
                            to.setMantissa(0);
                            to.setExp(0);
                            numberMantissaExp = 0;
                            numberMinuses = 0;
                            currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_SIGN;
                            break _next_char;
                        }
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
                            to.setMantissa(c - '0');
                            to.setExp(0);
                            numberMantissaExp = 0;
                            numberMinuses = 0;

                            if (len - pos > 1) { // try to read available part of the number value's mantissa
                                pos++;
                                while (true) {
                                    c = data.charAt(pos);
                                    if (c >= '0' && c <= '9') {
                                        final long m = to.mantissa();
                                        if (!numberOverflow && m <= MAX_MANTISSA_BASE) {
                                            to.setMantissa(m * 10 + (c - '0'));
                                        } else if (!numberOverflow) {
                                            numberOverflow = true;
                                            overflown = startOverflownNumber(overflownSink(overflown), m, numberMinuses,
                                                    false, numberMantissaExp, c);
                                            numberMantissaExp++;
                                        } else {
                                            overflown.append(c);
                                            numberMantissaExp++;
                                        }
                                        if (++pos == len) {
                                            break _end;
                                        }
                                        continue;
                                    }
                                    switch (c) {
                                        case '.':
                                            if (numberOverflow) {
                                                overflown.append('.');
                                            }
                                            currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART;
                                            break _next_char;
                                        case 'e':
                                        case 'E':
                                            currentLexState = LEXEMA_NUMBER_STARTED_E;
                                            break _next_char;
                                        default:
                                            throw new IllegalArgumentException(ERROR_INCORRECT_NUMBER_MESSAGE
                                                    + ". Unexpected char '" + c + "' at the position " + pos);
                                    }
                                }
                            }
                            break _end;
                        }
                        default:
                            throw new IllegalArgumentException(ERROR_INCORRECT_NUMBER_MESSAGE
                                    + ". Unexpected char '" + c + "' at the position " + pos);
                    }
                }
                case LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART: {
                    while (true) {
                        if (c >= '0' && c <= '9') {
                            final long m = to.mantissa();
                            if (!numberOverflow && m <= MAX_MANTISSA_BASE) {
                                to.setMantissa(m * 10 + (c - '0'));
                            } else if (!numberOverflow) {
                                numberOverflow = true;
                                overflown = startOverflownNumber(overflownSink(overflown), m, numberMinuses,
                                        false, numberMantissaExp, c);
                                numberMantissaExp++;
                            } else {
                                overflown.append(c);
                                numberMantissaExp++;
                            }
                            if (++pos == len) {
                                break _end;
                            }
                            c = data.charAt(pos);
                            continue;
                        }
                        switch (c) {
                            case '.':
                                if (numberOverflow) {
                                    overflown.append('.');
                                }
                                currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART;
                                break _next_char;
                            case 'e':
                            case 'E':
                                currentLexState = LEXEMA_NUMBER_STARTED_E;
                                break _next_char;
                            default:
                                throw new IllegalArgumentException(ERROR_INCORRECT_NUMBER_MESSAGE
                                        + ". Unexpected char '" + c + "' at the position " + pos);
                        }
                    }
                }
                case LEXEMA_NUMBER_STARTED_MANTISSA_SIGN: {
                    switch (c) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            to.setMantissa(c - '0');
                            currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART;
                            break _next_char;
                        default:
                            throw new IllegalArgumentException(ERROR_INCORRECT_NUMBER_MESSAGE
                                    + ". Unexpected char '" + c + "' at the position " + pos);
                    }
                }
                case LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART: {
                    while (true) {
                        if (c >= '0' && c <= '9') {
                            final long m = to.mantissa();
                            if (!numberOverflow && m <= MAX_MANTISSA_BASE) {
                                to.setMantissa(m * 10 + (c - '0'));
                                numberMantissaExp--;
                            } else if (!numberOverflow) {
                                numberOverflow = true;
                                overflown = startOverflownNumber(overflownSink(overflown), m, numberMinuses,
                                        true, numberMantissaExp, c);
                            } else {
                                overflown.append(c);
                            }

                            if (++pos == len) {
                                break _end;
                            }
                            c = data.charAt(pos);
                            continue;
                        }
                        switch (c) {
                            case 'e':
                            case 'E':
                                currentLexState = LEXEMA_NUMBER_STARTED_E;
                                break _next_char;
                            default:
                                throw new IllegalArgumentException(ERROR_INCORRECT_NUMBER_MESSAGE
                                        + ". Unexpected char '" + c + "' at the position " + pos);
                        }
                    }
                }
                case LEXEMA_NUMBER_STARTED_E: {
                    switch (c) {
                        case '+':
                            currentLexState = LEXEMA_NUMBER_STARTED_E_SIGN;
                            break _next_char;
                        case '-':
                            numberMinuses = numberMinuses | 1;
                            currentLexState = LEXEMA_NUMBER_STARTED_E_SIGN;
                            break _next_char;
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            to.setExp(to.exp() * 10 + (c - '0'));
                            currentLexState = LEXEMA_NUMBER_STARTED_E_VALUE;
                            break _next_char;
                        default:
                            throw new IllegalArgumentException(ERROR_INCORRECT_NUMBER_MESSAGE
                                    + ". Unexpected char '" + c + "' at the position " + pos);
                    }
                }
                case LEXEMA_NUMBER_STARTED_E_SIGN: {
                    switch (c) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            to.setExp(to.exp() * 10 + (c - '0'));
                            currentLexState = LEXEMA_NUMBER_STARTED_E_VALUE;
                            break _next_char;
                        default:
                            throw new IllegalArgumentException(ERROR_INCORRECT_NUMBER_MESSAGE
                                    + ". Unexpected char '" + c + "' at the position " + pos);
                    }
                }
                case LEXEMA_NUMBER_STARTED_E_VALUE: {
                    switch (c) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            to.setExp(to.exp() * 10 + (c - '0'));
                            break _next_char;
                        default:
                            throw new IllegalArgumentException(ERROR_INCORRECT_NUMBER_MESSAGE
                                    + ". Unexpected char '" + c + "' at the position " + pos);
                    }
                }
                default:
                    throw new IllegalStateException();
            }
        }
        setNumber(to, numberMinuses, numberMantissaExp, numberOverflow, overflown);
        return numberOverflow;
    }

    private static StringBuilder overflownSink(final StringBuilder overflownNumber) {
        return overflownNumber != null ? overflownNumber : LAST_OVERFLOWN_NUMBER.get();
    }

    private final Next next = () -> JsonParser.this.next();

    private final JsonStringBuilder string;

    private CharSequence currentData;

    public JsonParser() {
        this(new CopyingStringBuilder());
    }

    public JsonParser(final JsonStringBuilder stringBuilder) {
        super(stringBuilder);
        string = stringBuilder;
    }

    public JsonParser setNotifyObjectMemberNameString(final boolean notifyObjectMemberNameString) {
        this.notifyObjectMemberNameString = notifyObjectMemberNameString;
        return this;
    }

    public JsonParser setListener(final JsonParserListener listener) {
        this.listener = listener;
        return this;
    }

    public void parseAndEoj(final CharSequence data) {
        parse(data);
        eoj();
    }

    public Next parse(final CharSequence data) {
        return parse(data, 0, data.length());
    }

    public Next parse(final CharSequence data, final int start, final int len) {
        this.currentData = data;
        this.currentStart = start;
        this.currentLen = len;
        this.currentPos = 0;
        Objects.checkFromIndexSize(start, len, data.length());
        return next();
    }

    private Next next() {
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
        final CharSequence data = currentData;
        final int base = currentStart;
        final int end = base + currentLen;

        int p = base + currentPos;
        int lexPos = currentLexemaPosition;
        int lexState = currentLexemaState;
        boolean failed = false;

        try {
            _end:
            while (p < end) {
                final char c = data.charAt(p);
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
                            if (data.charAt(p + 1) == 'r' && data.charAt(p + 2) == 'u' && data.charAt(p + 3) == 'e') {
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
                            if (data.charAt(p + 1) == 'a' && data.charAt(p + 2) == 'l'
                                    && data.charAt(p + 3) == 's' && data.charAt(p + 4) == 'e') {
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
                            if (data.charAt(p + 1) == 'u' && data.charAt(p + 2) == 'l' && data.charAt(p + 3) == 'l') {
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
                        final JsonStringBuilder stringBuilder = string;
                        stringBuilder.start(data, p);

                        final int plainStart = p + 1;
                        final int plainEnd = skipPlainChars(data, plainStart, end);
                        stringBuilder.append(data, plainStart, plainEnd - plainStart);
                        if (plainEnd == end) { // no more data
                            p = end;
                            lexState = LEXEMA_STRING_STARTED;
                            break _end;
                        }
                        if (data.charAt(plainEnd) == '"') { // the most frequent case
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
                            if (digitsEnd < end && isNumberDelimiter(data.charAt(digitsEnd))) {
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
        final CharSequence data = currentData;
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

    private int resumeLiteral(final JsonParserListener lnr, final CharSequence data, final int from, final int end) {
        int state = currentLexemaState;

        for (int p = from; p < end; p++) {
            if (data.charAt(p) != LITERAL_EXPECTED_CHARS[state]) {
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
    // Returns the index of the char next to the last one read, or -1 in the case of an error.
    private int scanString(final int fromState, final CharSequence data, final int from, final int end) {
        final JsonStringBuilder stringBuilder = string;

        int state = fromState;
        int p = from;

        while (p < end) {
            if (state == LEXEMA_STRING_STARTED) {
                final int plainStart = p;
                p = skipPlainChars(data, p, end);
                stringBuilder.append(data, plainStart, p - plainStart);
                if (p == end) {
                    break;
                }
                if (data.charAt(p++) == '"') {
                    state = LEXEMA_STRING_READY;
                    break;
                }
                stringBuilder.appendEscape();
                state = LEXEMA_STRING_STARTED_ESCAPE;
                continue;
            }

            final char c = data.charAt(p);

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
                    if (stringBuilder.appendEscapedUnicodeChar1(c)) {
                        state = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1;
                        break;
                    }
                    return onIncorrectUnicodeEscaping(state, p);
                }
                case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1: {
                    if (stringBuilder.appendEscapedUnicodeChar2(c)) {
                        state = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2;
                        break;
                    }
                    return onIncorrectUnicodeEscaping(state, p);
                }
                case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2: {
                    if (stringBuilder.appendEscapedUnicodeChar3(c)) {
                        state = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3;
                        break;
                    }
                    return onIncorrectUnicodeEscaping(state, p);
                }
                default: { // LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3
                    if (stringBuilder.appendEscapedUnicodeChar4(c)) {
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

    private static int skipWhitespaces(final CharSequence data, final int from, final int end) {
        int p = from;
        while (p < end) {
            final char c = data.charAt(p);
            if (c != 0x20 && c != 0x0a && c != 0x0d && c != 0x09) {
                break;
            }
            p++;
        }
        return p;
    }

    private static int skipPlainChars(final CharSequence data, final int from, final int end) {
        int p = from;
        while (p < end) {
            final char c = data.charAt(p);
            if (c == '"' || c == '\\') {
                break;
            }
            p++;
        }
        return p;
    }

    // Reads as many digits of a number, which can't overflow the mantissa, as there are in a row.
    // The result is left in the number, so scanNumber() can continue from the index returned.
    private int scanDigits(final CharSequence data, final int from, final int end) {
        final int limit = Math.min(end, from + MAX_SAFE_DIGITS);
        long mantissa = 0;
        int p = from;
        while (p < limit) {
            final int digit = data.charAt(p) - '0';
            if (digit < 0 || digit > 9) {
                break;
            }
            mantissa = mantissa * 10 + digit;
            p++;
        }
        number.setMantissa(mantissa);
        return p;
    }

    private static boolean isNumberDelimiter(final char c) {
        return c < NUMBER_DELIMITERS.length && NUMBER_DELIMITERS[c];
    }

    // Reads a number value from the given state up to the delimiter next to the number or the end
    // of the data. The state the reading has finished with is stored to currentLexemaState.
    // Returns the index of the first char which hasn't been read, or -1 in the case of an error.
    private int scanNumber(final int fromState, final CharSequence data, final int from, final int end) {
        long mantissa = number.mantissa();
        int mantissaExp = numberMantissaExp;
        int exp = number.exp();

        int state = fromState;
        int p = from;
        boolean failed = false;

        _end:
        while (p < end) {
            char c = data.charAt(p);

            switch (state) {
                case LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART: {
                    int digit = c - '0';
                    while (digit >= 0 && digit <= 9) {
                        if (mantissa <= MAX_MANTISSA_BASE) {
                            mantissa = mantissa * 10 + digit;
                        } else {
                            onMantissaOverflown(mantissa, false, mantissaExp, c);
                            mantissaExp++;
                        }
                        if (++p == end) {
                            break _end;
                        }
                        c = data.charAt(p);
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
                            onMantissaOverflown(mantissa, true, mantissaExp, c);
                        }
                        if (++p == end) {
                            break _end;
                        }
                        c = data.charAt(p);
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

            // the char isn't a part of the number
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
    public JsonParser eoj() {
        doEoj();
        return this;
    }

    public JsonParser reset() {
        doResetAndNotify();
        return this;
    }
}
