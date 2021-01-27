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

/**
 * Parses JSON character sequence.
 */
public final class JsonParser {

    public interface Next {
        Next next();
    }

    private static final int LEXEMA_TRUE_STARTED_T = 1;
    private static final int LEXEMA_TRUE_STARTED_TR = LEXEMA_TRUE_STARTED_T + 1;
    private static final int LEXEMA_TRUE_STARTED_TRU = LEXEMA_TRUE_STARTED_TR + 1;

    private static final int LEXEMA_FALSE_STARTED_F = LEXEMA_TRUE_STARTED_TRU + 1;
    private static final int LEXEMA_FALSE_STARTED_FA = LEXEMA_FALSE_STARTED_F + 1;
    private static final int LEXEMA_FALSE_STARTED_FAL = LEXEMA_FALSE_STARTED_FA + 1;
    private static final int LEXEMA_FALSE_STARTED_FALS = LEXEMA_FALSE_STARTED_FAL + 1;

    private static final int LEXEMA_NULL_STARTED_N = LEXEMA_FALSE_STARTED_FALS + 1;
    private static final int LEXEMA_NULL_STARTED_NU = LEXEMA_NULL_STARTED_N + 1;
    private static final int LEXEMA_NULL_STARTED_NUL = LEXEMA_NULL_STARTED_NU + 1;

    private static final int LEXEMA_STRING_STARTED = LEXEMA_NULL_STARTED_NUL + 1;
    private static final int LEXEMA_STRING_STARTED_ESCAPE = LEXEMA_STRING_STARTED + 1;
    private static final int LEXEMA_STRING_STARTED_ESCAPE_UNICODE = LEXEMA_STRING_STARTED_ESCAPE + 1;
    private static final int LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1 = LEXEMA_STRING_STARTED_ESCAPE_UNICODE + 1;
    private static final int LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2 = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1 + 1;
    private static final int LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3 = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2 + 1;

    private static final int LEXEMA_NUMBER_STARTED_MANTISSA_SIGN = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3 + 1;
    private static final int LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART = LEXEMA_NUMBER_STARTED_MANTISSA_SIGN + 1;
    private static final int LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART
            = LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART + 1;
    private static final int LEXEMA_NUMBER_STARTED_E = LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART + 1;
    private static final int LEXEMA_NUMBER_STARTED_E_SIGN = LEXEMA_NUMBER_STARTED_E + 1;
    private static final int LEXEMA_NUMBER_STARTED_E_VALUE = LEXEMA_NUMBER_STARTED_E_SIGN + 1;

    private static final int LEXEMA_READY = LEXEMA_NUMBER_STARTED_E_VALUE + 1;

    private static final int LEXEMA_CURLY_BRACKET_LEFT_READY = LEXEMA_READY + 1;
    private static final int LEXEMA_CURLY_BRACKET_RIGHT_READY = LEXEMA_CURLY_BRACKET_LEFT_READY + 1;
    private static final int LEXEMA_BOX_BRACKET_LEFT_READY = LEXEMA_CURLY_BRACKET_RIGHT_READY + 1;
    private static final int LEXEMA_BOX_BRACKET_RIGHT_READY = LEXEMA_BOX_BRACKET_LEFT_READY + 1;
    private static final int LEXEMA_COMMA_READY = LEXEMA_BOX_BRACKET_RIGHT_READY + 1;
    private static final int LEXEMA_COLON_READY = LEXEMA_COMMA_READY + 1;
    private static final int LEXEMA_STRING_READY = LEXEMA_COLON_READY + 1;
    private static final int LEXEMA_NUMBER_READY = LEXEMA_STRING_READY + 1;
    private static final int LEXEMA_TRUE_READY = LEXEMA_NUMBER_READY + 1;
    private static final int LEXEMA_FALSE_READY = LEXEMA_TRUE_READY + 1;
    private static final int LEXEMA_NULL_READY = LEXEMA_FALSE_READY + 1;

    private static final int EXPRESSION_INITIAL = 0;

    private static final int EXPRESSION_OBJECT_STARTED = EXPRESSION_INITIAL + 1;
    private static final int EXPRESSION_OBJECT_STARTED_MEMBER_NAME = EXPRESSION_OBJECT_STARTED + 1;
    private static final int EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER =
            EXPRESSION_OBJECT_STARTED_MEMBER_NAME + 1;
    private static final int EXPRESSION_OBJECT_STARTED_MEMBER_VALUE =
            EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER + 1;
    private static final int EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER =
            EXPRESSION_OBJECT_STARTED_MEMBER_VALUE + 1;

    private static final int EXPRESSION_ARRAY_STARTED = EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER + 1;
    private static final int EXPRESSION_ARRAY_STARTED_VALUE = EXPRESSION_ARRAY_STARTED + 1;
    private static final int EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER = EXPRESSION_ARRAY_STARTED_VALUE + 1;

    private static final String ERROR_INCORRECT_STRING_MESSAGE = "Incorrect string";
    private static final String ERROR_INCORRECT_ESCAPING_MESSAGE = "Incorrect character escaping";
    private static final String ERROR_MESSAGE_INCORRECT_UNICODE_ESCAPING = "Incorrect unicode escaping";
    private static final String ERROR_INCORRECT_NUMBER_MESSAGE = "Incorrect number";
    private static final String ERROR_TRUE_EXPECTED_MESSAGE = "'true' expected";
    private static final String ERROR_FALSE_EXPECTED_MESSAGE = "'false' expected";
    private static final String ERROR_NULL_EXPECTED_MESSAGE = "'null' expected";
    private static final String ERROR_UNEXPECTED_CURLY_BRACKET_LEFT_MESSAGE = "Unexpected '{'";
    private static final String ERROR_UNEXPECTED_CURLY_BRACKET_RIGHT_MESSAGE = "Unexpected '}'";
    private static final String ERROR_UNEXPECTED_BOX_BRACKET_LEFT_MESSAGE = "Unexpected '['";
    private static final String ERROR_UNEXPECTED_BOX_BRACKET_RIGHT_MESSAGE = "Unexpected ']'";
    private static final String ERROR_UNEXPECTED_COMMA_MESSAGE = "Unexpected ','";
    private static final String ERROR_UNEXPECTED_COLON_MESSAGE = "Unexpected ':'";
    private static final String ERROR_UNEXPECTED_STRING_MESSAGE = "Unexpected string";
    private static final String ERROR_UNEXPECTED_NUMBER_MESSAGE = "Unexpected number";
    private static final String ERROR_UNEXPECTED_TRUE_MESSAGE = "Unexpected 'true'";
    private static final String ERROR_UNEXPECTED_FALSE_MESSAGE = "Unexpected 'false'";
    private static final String ERROR_UNEXPECTED_NULL_MESSAGE = "Unexpected 'null'";
    private static final String ERROR_UNEXPECTED_CHAR_MESSAGE = "Unexpected char";

    private static final String ERROR_INTERNAL_UNEXPECTED_LEXEMA_ERROR_MESSAGE = "Internal error. Unexpected lexema";

    private static final int JSON_STARTED_NOTIFICATION_REQUIRED = -1;
    private static final int RESET_REQUIRED = JSON_STARTED_NOTIFICATION_REQUIRED - 1;

    public static void parseNumber(final CharSequence data, final MutableJsonNumber to) {
        int currentLexState = LEXEMA_READY;

        int numberMantissaExp = 0;
        int numberMinuses = 0;

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
                                        to.setMantissa(to.mantissa() * 10 + (c - '0'));
                                        if (++pos == len) {
                                            break _end;
                                        }
                                        continue;
                                    }
                                    switch (c) {
                                        case '.':
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
                            to.setMantissa(to.mantissa() * 10 + (c - '0'));
                            if (++pos == len) {
                                break _end;
                            }
                            c = data.charAt(pos);
                            continue;
                        }
                        switch (c) {
                            case '.':
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
                            to.setMantissa(to.mantissa() * 10 + (c - '0'));
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
                            to.setMantissa(to.mantissa() * 10 + (c - '0'));
                            numberMantissaExp--;
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
        setNumber(to, numberMinuses, numberMantissaExp);
    }

    private final Next next = () -> JsonParser.this.next();

    private final MutableJsonNumber number = new MutableJsonNumber();
    private final JsonStringBuilder string;

    private JsonParserListener listener;

    private boolean notifyObjectMemberNameString;

    private CharSequence currentData;
    private int currentLen;
    private int currentStart;
    private int currentPos;
    private int currentLexemaState;
    private int currentLexemaPosition;

    private int numberMantissaExp;
    private int numberMinuses;

    private String error;
    private int errorPosition;

    private int[] scopeStack = new int[8];
    private int scopeStackDepth;

    public JsonParser() {
        this(new CopyingStringBuilder());
    }

    public JsonParser(final JsonStringBuilder stringBuilder) {
        string = stringBuilder;

        currentLexemaPosition = RESET_REQUIRED;
        currentLexemaState = LEXEMA_READY;
        pushScope(EXPRESSION_INITIAL);
    }

    public boolean isNotifyObjectMemberNameString() {
        return notifyObjectMemberNameString;
    }

    public JsonParser setNotifyObjectMemberNameString(final boolean notifyObjectMemberNameString) {
        this.notifyObjectMemberNameString = notifyObjectMemberNameString;
        return this;
    }

    public JsonParser setListener(final JsonParserListener listener) {
        this.listener = listener;
        return this;
    }

    public String getError() {
        return error;
    }

    public int getErrorPosition() {
        return errorPosition;
    }

    public boolean hasError() {
        return error != null;
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
        return next();
    }

    private Next next() {
        final JsonParserListener lnr = listener;

        assert lnr != null;

        int currentLexPos = currentLexemaPosition;

        if (currentLexPos < 0) {
            if (currentLexPos < JSON_STARTED_NOTIFICATION_REQUIRED) {
                doReset();
            }
            lnr.onJsonStarted();
        }

        int currentLexState = currentLexemaState;

        final JsonStringBuilder stringBuilder = this.string;

        final CharSequence data = currentData;
        final int start = currentStart;
        final int len = currentLen;

        int pos = currentPos;

        try {
            _end:
            while (pos < len) {
                char c = data.charAt(start + pos);

                if (currentLexState >= LEXEMA_READY) {
                    _next_char:
                    switch (c) {
                        case 0x09:
                        case 0x0a:
                        case 0x0d:
                        case 0x20:
                            break;
                        case '{': {
                            currentLexPos = pos;
                            currentLexState = LEXEMA_CURLY_BRACKET_LEFT_READY;
                            final int r = onCurlyBracketLeft(lnr, currentLexPos);
                            if (r == 0) {
                                break;
                            }
                            if (r > 0) {
                                pos++;
                                break _end;
                            }
                            return null;
                        }
                        case '}': {
                            currentLexPos = pos;
                            currentLexState = LEXEMA_CURLY_BRACKET_RIGHT_READY;
                            final int r = onCurlyBracketRight(lnr, currentLexPos);
                            if (r == 0) {
                                break;
                            }
                            if (r > 0) {
                                pos++;
                                break _end;
                            }
                            return null;
                        }
                        case '[': {
                            currentLexPos = pos;
                            currentLexState = LEXEMA_BOX_BRACKET_LEFT_READY;
                            final int r = onBoxBracketLeft(lnr, currentLexPos);
                            if (r == 0) {
                                break;
                            }
                            if (r > 0) {
                                pos++;
                                break _end;
                            }
                            return null;
                        }
                        case ']': {
                            currentLexPos = pos;
                            currentLexState = LEXEMA_BOX_BRACKET_RIGHT_READY;
                            final int r = onBoxBracketRight(lnr, currentLexPos);
                            if (r == 0) {
                                break;
                            }
                            if (r > 0) {
                                pos++;
                                break _end;
                            }
                            return null;
                        }
                        case ',': {
                            currentLexPos = pos;
                            currentLexState = LEXEMA_COMMA_READY;
                            final int r = onComma(currentLexPos);
                            if (r == 0) {
                                break;
                            }
                            return null;
                        }
                        case ':': {
                            currentLexPos = pos;
                            currentLexState = LEXEMA_COLON_READY;
                            final int r = onColon(currentLexPos);
                            if (r == 0) {
                                break;
                            }
                            return null;
                        }
                        case 't': {
                            if (currentLexState > LEXEMA_NUMBER_READY) {
                                error(ERROR_TRUE_EXPECTED_MESSAGE, pos);
                                return null;
                            }
                            currentLexPos = pos;
                            if (len - pos > 3) { // try to read the whole 'true' value
                                if (data.charAt(++pos) == 'r'
                                        && data.charAt(++pos) == 'u'
                                        && data.charAt(++pos) == 'e') {

                                    currentLexState = LEXEMA_TRUE_READY;
                                    final int r = onTrue(lnr, currentLexPos);
                                    if (r == 0) {
                                        break _next_char;
                                    }
                                    if (r > 0) {
                                        pos++;
                                        break _end;
                                    }
                                    return null;
                                }
                                error(ERROR_TRUE_EXPECTED_MESSAGE, currentLexPos);
                                return null;
                            }
                            currentLexState = LEXEMA_TRUE_STARTED_T;
                            break;
                        }
                        case 'f': {
                            if (currentLexState > LEXEMA_NUMBER_READY) {
                                error(ERROR_FALSE_EXPECTED_MESSAGE, pos);
                                return null;
                            }
                            currentLexPos = pos;
                            if (len - pos > 4) { // try to read the whole 'false' value
                                if (data.charAt(++pos) == 'a'
                                        && data.charAt(++pos) == 'l'
                                        && data.charAt(++pos) == 's'
                                        && data.charAt(++pos) == 'e') {

                                    currentLexState = LEXEMA_FALSE_READY;
                                    final int r = onFalse(lnr, currentLexPos);
                                    if (r == 0) {
                                        break _next_char;
                                    }
                                    if (r > 0) {
                                        pos++;
                                        break _end;
                                    }
                                    return null;
                                }
                                error(ERROR_FALSE_EXPECTED_MESSAGE, currentLexPos);
                                return null;
                            }
                            currentLexState = LEXEMA_FALSE_STARTED_F;
                            break;
                        }
                        case 'n': {
                            if (currentLexState > LEXEMA_NUMBER_READY) {
                                error(ERROR_NULL_EXPECTED_MESSAGE, pos);
                                return null;
                            }
                            currentLexPos = pos;
                            if (len - pos > 3) { // try to read the whole 'null' value
                                if (data.charAt(++pos) == 'u'
                                        && data.charAt(++pos) == 'l'
                                        && data.charAt(++pos) == 'l') {

                                    currentLexState = LEXEMA_NULL_READY;
                                    final int r = onNull(lnr, currentLexPos);
                                    if (r == 0) {
                                        break _next_char;
                                    }
                                    if (r > 0) {
                                        pos++;
                                        break _end;
                                    }
                                    return null;
                                }
                                error(ERROR_NULL_EXPECTED_MESSAGE, currentLexPos);
                                return null;
                            }
                            currentLexState = LEXEMA_NULL_STARTED_N;
                            break;
                        }
                        case '"': {
                            stringBuilder.start(data, start + pos);
                            currentLexPos = pos;
                            if (len - pos > 1) { // try to read available part of the string value
                                pos++;
                                final int startStringPos = pos;
                                while (true) {
                                    c = data.charAt(start + pos);
                                    switch (c) {
                                        case '"':
                                            stringBuilder.append(data, startStringPos, pos - startStringPos);
                                            currentLexState = LEXEMA_STRING_READY;
                                            final int r = onStringReady(lnr, currentLexPos);
                                            if (r == 0) {
                                                break _next_char;
                                            }
                                            if (r > 0) {
                                                pos++;
                                                break _end;
                                            }
                                            return null;
                                        case '\\':
                                            stringBuilder.append(data, startStringPos, pos - startStringPos);
                                            stringBuilder.appendEscape();
                                            currentLexState = LEXEMA_STRING_STARTED_ESCAPE;
                                            break _next_char;
                                        default:
                                            break;
                                    }
                                    if (++pos == len) {
                                        stringBuilder.append(data, startStringPos, pos - startStringPos);
                                        break;
                                    }
                                }
                            }
                            currentLexState = LEXEMA_STRING_STARTED;
                            break;
                        }
                        case '-': {
                            number.setMantissa(0);
                            number.setExp(0);
                            numberMantissaExp = 0;
                            numberMinuses = 2;
                            currentLexPos = pos;
                            currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_SIGN;
                            break;
                        }
                        case '+': {
                            number.setMantissa(0);
                            number.setExp(0);
                            numberMantissaExp = 0;
                            numberMinuses = 0;
                            currentLexPos = pos;
                            currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_SIGN;
                            break;
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
                            number.setMantissa(c - '0');
                            number.setExp(0);
                            numberMantissaExp = 0;
                            numberMinuses = 0;
                            currentLexPos = pos;
                            if (len - pos > 1) { // try to read available part of the number value's mantissa
                                pos++;
                                while (true) {
                                    c = data.charAt(start + pos);
                                    if (c >= '0' && c <= '9') {
                                        number.setMantissa(number.mantissa() * 10 + (c - '0'));
                                        if (++pos == len) {
                                            break;
                                        }
                                        continue;
                                    }
                                    switch (c) {
                                        case '.':
                                            currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART;
                                            break _next_char;
                                        case 'e':
                                        case 'E':
                                            currentLexState = LEXEMA_NUMBER_STARTED_E;
                                            break _next_char;
                                        case 0x09:
                                        case 0x0a:
                                        case 0x0d:
                                        case 0x20:
                                        case '{':
                                        case '}':
                                        case '[':
                                        case ']':
                                        case ',':
                                        case ':':
                                            pos--;
                                            currentLexState = LEXEMA_NUMBER_READY;
                                            final int r = onNumber(lnr, currentLexPos);
                                            if (r == 0) {
                                                break _next_char;
                                            }
                                            if (r > 0) {
                                                pos++;
                                                break _end;
                                            }
                                            return null;
                                        default:
                                            error(ERROR_INCORRECT_NUMBER_MESSAGE, pos);
                                            return null;
                                    }
                                }
                            }
                            currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART;
                            break;
                        }
                        default:
                            error(ERROR_UNEXPECTED_CHAR_MESSAGE, pos);
                            return null;
                    }
                } else {
                    _next_char:
                    switch (currentLexState) {

                        /* string value - the web */
                        case LEXEMA_STRING_STARTED: {
                            final int startStringPos = pos;
                            while (true) {
                                switch (c) {
                                    case '"':
                                        stringBuilder.append(data, startStringPos, pos - startStringPos);
                                        currentLexState = LEXEMA_STRING_READY;
                                        final int r = onStringReady(lnr, currentLexPos);
                                        if (r == 0) {
                                            break _next_char;
                                        }
                                        if (r > 0) {
                                            pos++;
                                            break _end;
                                        }
                                        return null;
                                    case '\\':
                                        stringBuilder.append(data, startStringPos, pos - startStringPos);
                                        stringBuilder.appendEscape();
                                        currentLexState = LEXEMA_STRING_STARTED_ESCAPE;
                                        break _next_char;
                                    default:
                                        break;
                                }
                                if (++pos == len) {
                                    stringBuilder.append(data, startStringPos, pos - startStringPos);
                                    break;
                                }
                                c = data.charAt(start + pos);
                            }
                            break;
                        }
                        case LEXEMA_STRING_STARTED_ESCAPE: {
                            switch (c) {
                                case '"':
                                    stringBuilder.appendEscapedQuotationMark();
                                    currentLexState = LEXEMA_STRING_STARTED;
                                    break _next_char;
                                case '\\':
                                    stringBuilder.appendEscapedReverseSolidus();
                                    currentLexState = LEXEMA_STRING_STARTED;
                                    break _next_char;
                                case '/':
                                    stringBuilder.appendEscapedSolidus();
                                    currentLexState = LEXEMA_STRING_STARTED;
                                    break _next_char;
                                case 'b':
                                    stringBuilder.appendEscapedBackspace();
                                    currentLexState = LEXEMA_STRING_STARTED;
                                    break _next_char;
                                case 'f':
                                    stringBuilder.appendEscapedFormfeed();
                                    currentLexState = LEXEMA_STRING_STARTED;
                                    break _next_char;
                                case 'n':
                                    stringBuilder.appendEscapedNewLine();
                                    currentLexState = LEXEMA_STRING_STARTED;
                                    break _next_char;
                                case 'r':
                                    stringBuilder.appendEscapedCarriageReturn();
                                    currentLexState = LEXEMA_STRING_STARTED;
                                    break _next_char;
                                case 't':
                                    stringBuilder.appendEscapedHorisontalTab();
                                    currentLexState = LEXEMA_STRING_STARTED;
                                    break _next_char;
                                case 'u':
                                    stringBuilder.appendEscapedUnicodeU();
                                    currentLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE;
                                    break _next_char;
                                default:
                                    error(ERROR_INCORRECT_ESCAPING_MESSAGE, pos);
                                    return null;
                            }
                        }
                        case LEXEMA_STRING_STARTED_ESCAPE_UNICODE: {
                            if (stringBuilder.appendEscapedUnicodeChar1(c)) {
                                currentLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1;
                                break;
                            } else {
                                error(ERROR_MESSAGE_INCORRECT_UNICODE_ESCAPING, pos);
                                return null;
                            }
                        }
                        case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1: {
                            if (stringBuilder.appendEscapedUnicodeChar2(c)) {
                                currentLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2;
                                break;
                            } else {
                                error(ERROR_MESSAGE_INCORRECT_UNICODE_ESCAPING, pos);
                                return null;
                            }
                        }
                        case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2: {
                            if (stringBuilder.appendEscapedUnicodeChar3(c)) {
                                currentLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3;
                                break;
                            } else {
                                error(ERROR_MESSAGE_INCORRECT_UNICODE_ESCAPING, pos);
                                return null;
                            }
                        }
                        case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3: {
                            if (stringBuilder.appendEscapedUnicodeChar4(c)) {
                                currentLexState = LEXEMA_STRING_STARTED;
                                break;
                            } else {
                                error(ERROR_MESSAGE_INCORRECT_UNICODE_ESCAPING, pos);
                                return null;
                            }
                        }

                        /* number value - the web */
                        case LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART: {
                            while (true) {
                                if (c >= '0' && c <= '9') {
                                    number.setMantissa(number.mantissa() * 10 + (c - '0'));
                                    if (++pos == len) {
                                        break _end;
                                    }
                                    c = data.charAt(start + pos);
                                    continue;
                                }
                                switch (c) {
                                    case '.':
                                        currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART;
                                        break _next_char;
                                    case 'e':
                                    case 'E':
                                        currentLexState = LEXEMA_NUMBER_STARTED_E;
                                        break _next_char;
                                    case 0x09:
                                    case 0x0a:
                                    case 0x0d:
                                    case 0x20:
                                    case '{':
                                    case '}':
                                    case '[':
                                    case ']':
                                    case ',':
                                    case ':':
                                        pos--;
                                        currentLexState = LEXEMA_NUMBER_READY;
                                        final int r = onNumber(lnr, currentLexPos);
                                        if (r == 0) {
                                            break _next_char;
                                        }
                                        if (r > 0) {
                                            pos++;
                                            break _end;
                                        }
                                        return null;
                                    default:
                                        error(ERROR_INCORRECT_NUMBER_MESSAGE, pos);
                                        return null;
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
                                    number.setMantissa(number.mantissa() * 10 + (c - '0'));
                                    currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART;
                                    break _next_char;
                                default:
                                    error(ERROR_INCORRECT_NUMBER_MESSAGE, pos);
                                    return null;
                            }
                        }
                        case LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART: {
                            while (true) {
                                if (c >= '0' && c <= '9') {
                                    number.setMantissa(number.mantissa() * 10 + (c - '0'));
                                    numberMantissaExp--;
                                    if (++pos == len) {
                                        break _end;
                                    }
                                    c = data.charAt(start + pos);
                                    continue;
                                }
                                switch (c) {
                                    case 'e':
                                    case 'E':
                                        currentLexState = LEXEMA_NUMBER_STARTED_E;
                                        break _next_char;
                                    case 0x09:
                                    case 0x0a:
                                    case 0x0d:
                                    case 0x20:
                                    case '{':
                                    case '}':
                                    case '[':
                                    case ']':
                                    case ',':
                                    case ':':
                                        pos--;
                                        currentLexState = LEXEMA_NUMBER_READY;
                                        final int r = onNumber(lnr, currentLexPos);
                                        if (r == 0) {
                                            break _next_char;
                                        }
                                        if (r > 0) {
                                            pos++;
                                            break _end;
                                        }
                                        return null;
                                    default:
                                        error(ERROR_INCORRECT_NUMBER_MESSAGE, pos);
                                        return null;
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
                                    number.setExp(number.exp() * 10 + (c - '0'));
                                    currentLexState = LEXEMA_NUMBER_STARTED_E_VALUE;
                                    break _next_char;
                                default:
                                    error(ERROR_INCORRECT_NUMBER_MESSAGE, pos);
                                    return null;
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
                                    number.setExp(number.exp() * 10 + (c - '0'));
                                    currentLexState = LEXEMA_NUMBER_STARTED_E_VALUE;
                                    break _next_char;
                                default:
                                    error(ERROR_INCORRECT_NUMBER_MESSAGE, pos);
                                    return null;
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
                                    number.setExp(number.exp() * 10 + (c - '0'));
                                    currentLexState = LEXEMA_NUMBER_STARTED_E_VALUE;
                                    break _next_char;
                                case 0x09:
                                case 0x0a:
                                case 0x0d:
                                case 0x20:
                                case '{':
                                case '}':
                                case '[':
                                case ']':
                                case ',':
                                case ':':
                                    pos--;
                                    currentLexState = LEXEMA_NUMBER_READY;
                                    final int r = onNumber(lnr, currentLexPos);
                                    if (r == 0) {
                                        break _next_char;
                                    }
                                    if (r > 0) {
                                        pos++;
                                        break _end;
                                    }
                                    return null;
                                default:
                                    error(ERROR_INCORRECT_NUMBER_MESSAGE, pos);
                                    return null;
                            }
                        }

                        /* 'true' value - the web */
                        case LEXEMA_TRUE_STARTED_T: {
                            switch (c) {
                                case 'r':
                                    currentLexState = LEXEMA_TRUE_STARTED_TR;
                                    break _next_char;
                                default:
                                    error(ERROR_TRUE_EXPECTED_MESSAGE, pos);
                                    return null;
                            }
                        }
                        case LEXEMA_TRUE_STARTED_TR: {
                            switch (c) {
                                case 'u':
                                    currentLexState = LEXEMA_TRUE_STARTED_TRU;
                                    break _next_char;
                                default:
                                    error(ERROR_TRUE_EXPECTED_MESSAGE, pos);
                                    return null;
                            }
                        }
                        case LEXEMA_TRUE_STARTED_TRU: {
                            switch (c) {
                                case 'e':
                                    currentLexState = LEXEMA_TRUE_READY;
                                    final int r = onTrue(lnr, currentLexPos);
                                    if (r == 0) {
                                        break _next_char;
                                    }
                                    if (r > 0) {
                                        pos++;
                                        break _end;
                                    }
                                    return null;
                                default:
                                    error(ERROR_TRUE_EXPECTED_MESSAGE, pos);
                                    return null;
                            }
                        }

                        /* 'false' value - the web */
                        case LEXEMA_FALSE_STARTED_F: {
                            switch (c) {
                                case 'a':
                                    currentLexState = LEXEMA_FALSE_STARTED_FA;
                                    break _next_char;
                                default:
                                    error(ERROR_FALSE_EXPECTED_MESSAGE, pos);
                                    return null;
                            }
                        }
                        case LEXEMA_FALSE_STARTED_FA: {
                            switch (c) {
                                case 'l':
                                    currentLexState = LEXEMA_FALSE_STARTED_FAL;
                                    break _next_char;
                                default:
                                    error(ERROR_FALSE_EXPECTED_MESSAGE, pos);
                                    return null;
                            }
                        }
                        case LEXEMA_FALSE_STARTED_FAL: {
                            switch (c) {
                                case 's':
                                    currentLexState = LEXEMA_FALSE_STARTED_FALS;
                                    break _next_char;
                                default:
                                    error(ERROR_FALSE_EXPECTED_MESSAGE, pos);
                                    return null;
                            }
                        }
                        case LEXEMA_FALSE_STARTED_FALS: {
                            switch (c) {
                                case 'e':
                                    currentLexState = LEXEMA_FALSE_READY;
                                    final int r = onFalse(lnr, currentLexPos);
                                    if (r == 0) {
                                        break _next_char;
                                    }
                                    if (r > 0) {
                                        pos++;
                                        break _end;
                                    }
                                    return null;
                                default:
                                    error(ERROR_FALSE_EXPECTED_MESSAGE, pos);
                                    return null;
                            }
                        }

                        /* 'null' value - the web */
                        case LEXEMA_NULL_STARTED_N: {
                            switch (c) {
                                case 'u':
                                    currentLexState = LEXEMA_NULL_STARTED_NU;
                                    break _next_char;
                                default:
                                    error(ERROR_NULL_EXPECTED_MESSAGE, pos);
                                    return null;
                            }
                        }
                        case LEXEMA_NULL_STARTED_NU: {
                            switch (c) {
                                case 'l':
                                    currentLexState = LEXEMA_NULL_STARTED_NUL;
                                    break _next_char;
                                default:
                                    error(ERROR_NULL_EXPECTED_MESSAGE, pos);
                                    return null;
                            }
                        }
                        case LEXEMA_NULL_STARTED_NUL: {
                            switch (c) {
                                case 'l':
                                    currentLexState = LEXEMA_NULL_READY;
                                    final int r = onNull(lnr, currentLexPos);
                                    if (r == 0) {
                                        break _next_char;
                                    }
                                    if (r > 0) {
                                        pos++;
                                        break _end;
                                    }
                                    return null;
                                default:
                                    error(ERROR_NULL_EXPECTED_MESSAGE, pos);
                                    return null;
                            }
                        }
                        /* unknown state */
                        default:
                            error(ERROR_INTERNAL_UNEXPECTED_LEXEMA_ERROR_MESSAGE, pos);
                            return null;
                    }
                }
                pos++;
            }
            return pos < len ? next : null;
        } finally {
            currentPos = pos;
            currentLexemaState = currentLexState;
            currentLexemaPosition = currentLexPos;
        }
    }

    /**
     * End of JSON
     *
     * @return this
     */
    public JsonParser eoj() {
        final JsonParserListener lnr = listener;

        if (lnr != null) {
            if (error == null) {
                onEoj(lnr, currentLexemaPosition);
            }
            lnr.onJsonEnded();
        }
        currentLexemaPosition = RESET_REQUIRED;
        return this;
    }

    public JsonParser reset() {
        doReset();
        currentLexemaPosition = JSON_STARTED_NOTIFICATION_REQUIRED;
        return this;
    }

    private void doReset() {
        currentLexemaState = LEXEMA_READY;
        clearScope();
        pushScope(EXPRESSION_INITIAL);
        error = null;
    }

    private void error(final String error, final int position) {
        final JsonParserListener lnr = listener;

        assert lnr != null;

        this.error = error;
        this.errorPosition = position;
        lnr.onError(error, position);
    }

    private static void setNumber(final MutableJsonNumber number,
                                  final int numberMinuses,
                                  final int numberMantissaExp) {
        if ((numberMinuses & 2) != 0) {
            number.setMantissa(-number.mantissa());
        }
        if ((numberMinuses & 1) != 0) {
            number.setExp(-number.exp());
        }
        number.setExp(number.exp() + numberMantissaExp);
    }

    private int onCurlyBracketLeft(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL: {
                final boolean r = lnr.onObjectStarted();
                replaceScope(EXPRESSION_OBJECT_STARTED);
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER: {
                final boolean r = lnr.onObjectStarted();
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                pushScope(EXPRESSION_OBJECT_STARTED);
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER: {
                final boolean r = lnr.onObjectStarted();
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                pushScope(EXPRESSION_OBJECT_STARTED);
                if (r) {
                    break;
                }
                return 1;
            }
            default:
                error(ERROR_UNEXPECTED_CURLY_BRACKET_LEFT_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private int onCurlyBracketRight(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_OBJECT_STARTED:
            case EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER: {
                final boolean r = lnr.onObjectEnded();
                popScope();
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_OBJECT_STARTED_MEMBER_VALUE: {
                final boolean r = lnr.onObjectEnded();
                popScope();
                if (r) {
                    break;
                }
                return 1;
            }
            default:
                error(ERROR_UNEXPECTED_CURLY_BRACKET_RIGHT_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private int onBoxBracketLeft(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL: {
                final boolean r = lnr.onArrayStarted();
                replaceScope(EXPRESSION_ARRAY_STARTED);
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER: {
                final boolean r = lnr.onArrayStarted();
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                pushScope(EXPRESSION_ARRAY_STARTED);
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER: {
                final boolean r = lnr.onArrayStarted();
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                pushScope(EXPRESSION_ARRAY_STARTED);
                if (r) {
                    break;
                }
                return 1;
            }
            default:
                error(ERROR_UNEXPECTED_BOX_BRACKET_LEFT_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private int onBoxBracketRight(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER: {
                final boolean r = lnr.onArrayEnded();
                popScope();
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_ARRAY_STARTED_VALUE: {
                final boolean r = lnr.onArrayEnded();
                popScope();
                if (r) {
                    break;
                }
                return 1;
            }
            default:
                error(ERROR_UNEXPECTED_BOX_BRACKET_RIGHT_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private int onComma(final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_OBJECT_STARTED_MEMBER_VALUE:
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER);
                break;
            case EXPRESSION_ARRAY_STARTED_VALUE:
                replaceScope(EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER);
                break;
            default:
                error(ERROR_UNEXPECTED_COMMA_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private int onColon(final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME:
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER);
                break;
            default:
                error(ERROR_UNEXPECTED_COLON_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private int onStringReady(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        final JsonStringBuilder value = string;

        switch (currentScope) {
            case EXPRESSION_INITIAL: {
                final boolean r = lnr.onStringValue(value);
                popScope();
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_OBJECT_STARTED:
            case EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER: {
                final boolean r = lnr.onObjectMember(value);
                if (notifyObjectMemberNameString) {
                    lnr.onStringValue(value);
                }
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_NAME);
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER: {
                final boolean r = lnr.onStringValue(value);
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER: {
                final boolean r = lnr.onStringValue(value);
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                if (r) {
                    break;
                }
                return 1;
            }
            default:
                error(ERROR_UNEXPECTED_STRING_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private int onNumber(final JsonParserListener lnr, final int lexemaPosition) {
        setNumber(number, numberMinuses, numberMantissaExp);

        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL: {
                final boolean r = lnr.onNumberValue(number);
                popScope();
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER: {
                final boolean r = lnr.onNumberValue(number);
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER: {
                final boolean r = lnr.onNumberValue(number);
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                if (r) {
                    break;
                }
                return 1;
            }
            default:
                error(ERROR_UNEXPECTED_NUMBER_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private int onTrue(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL: {
                final boolean r = lnr.onTrueValue();
                popScope();
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER: {
                final boolean r = lnr.onTrueValue();
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER: {
                final boolean r = lnr.onTrueValue();
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                if (r) {
                    break;
                }
                return 1;
            }
            default:
                error(ERROR_UNEXPECTED_TRUE_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private int onFalse(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL: {
                final boolean r = lnr.onFalseValue();
                popScope();
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER: {
                final boolean r = lnr.onFalseValue();
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER: {
                final boolean r = lnr.onFalseValue();
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                if (r) {
                    break;
                }
                return 1;
            }
            default:
                error(ERROR_UNEXPECTED_FALSE_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private int onNull(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL: {
                final boolean r = lnr.onNullValue();
                popScope();
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER: {
                final boolean r = lnr.onNullValue();
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER: {
                final boolean r = lnr.onNullValue();
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                if (r) {
                    break;
                }
                return 1;
            }
            default:
                error(ERROR_UNEXPECTED_NULL_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private int onEoj(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case -1: // no scope
                break;
            case EXPRESSION_INITIAL:
                /* check we are in progress of parsing of the first lexema */
                switch (currentLexemaState) {
                    case LEXEMA_TRUE_STARTED_T:
                    case LEXEMA_TRUE_STARTED_TR:
                    case LEXEMA_TRUE_STARTED_TRU:
                        error(ERROR_TRUE_EXPECTED_MESSAGE, lexemaPosition);
                        return -1;
                    case LEXEMA_FALSE_STARTED_F:
                    case LEXEMA_FALSE_STARTED_FA:
                    case LEXEMA_FALSE_STARTED_FAL:
                    case LEXEMA_FALSE_STARTED_FALS:
                        error(ERROR_FALSE_EXPECTED_MESSAGE, lexemaPosition);
                        return -1;
                    case LEXEMA_NULL_STARTED_N:
                    case LEXEMA_NULL_STARTED_NU:
                    case LEXEMA_NULL_STARTED_NUL:
                        error(ERROR_NULL_EXPECTED_MESSAGE, lexemaPosition);
                        return -1;
                    case LEXEMA_STRING_STARTED:
                    case LEXEMA_STRING_STARTED_ESCAPE:
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE:
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1:
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2:
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3:
                        error(ERROR_INCORRECT_STRING_MESSAGE, lexemaPosition);
                        return -1;
                    case LEXEMA_NUMBER_STARTED_MANTISSA_SIGN:
                    case LEXEMA_NUMBER_STARTED_E:
                    case LEXEMA_NUMBER_STARTED_E_SIGN:
                        error(ERROR_INCORRECT_NUMBER_MESSAGE, lexemaPosition);
                        return -1;
                    case LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART:
                    case LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART:
                    case LEXEMA_NUMBER_STARTED_E_VALUE:
                        setNumber(number, numberMinuses, numberMantissaExp); // try to apply the number
                        lnr.onNumberValue(number);
                        popScope();
                        break;
                }
                break;
            default:
                error(ERROR_INTERNAL_UNEXPECTED_LEXEMA_ERROR_MESSAGE, lexemaPosition);
                return -1;
        }
        return 0;
    }

    private void clearScope() {
        scopeStackDepth = 0;
    }

    private void pushScope(final int expression) {
        int[] stack = scopeStack;
        int depth = scopeStackDepth;
        if (depth == stack.length) {
            stack = new int[stack.length << 1];
            System.arraycopy(scopeStack, 0, stack, 0, scopeStack.length);
            scopeStack = stack;
        }
        stack[depth++] = expression;
        scopeStackDepth = depth;
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

    private void replaceScope(final int expression) {
        assert scopeStackDepth > 0;

        scopeStack[scopeStackDepth - 1] = expression;
    }
}
