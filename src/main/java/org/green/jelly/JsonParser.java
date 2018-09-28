/**
 * MIT License
 *
 * Copyright (c) 2018 Anatoly Gudkov
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
package org.green.jelly;

/**
 * Parses JSON character stream.
 */
public final class JsonParser {

    private static final int LEXEMA_INITIAL = 0;

    private static final int LEXEMA_TRUE_STARTED_T = LEXEMA_INITIAL + 1;
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
    private static final int EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER
        = EXPRESSION_OBJECT_STARTED_MEMBER_NAME + 1;
    private static final int EXPRESSION_OBJECT_STARTED_MEMBER_VALUE
        = EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER + 1;
    private static final int EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER
        = EXPRESSION_OBJECT_STARTED_MEMBER_VALUE + 1;

    private static final int EXPRESSION_ARRAY_STARTED = EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER + 1;
    private static final int EXPRESSION_ARRAY_STARTED_VALUE = EXPRESSION_ARRAY_STARTED + 1;
    private static final int EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER = EXPRESSION_ARRAY_STARTED_VALUE + 1;

    private static final String ERROR_INCORRECT_STRING_MESSAGE = "Incorrect string";
    private static final String ERROR_INCORRECT_ESCAPING_MESSAGE = "Incorrect character escaping";
    private static final String ERROR_INCORRECT_UNICODE_ESCAPING_MESSAGE = "Incorrect unicode escaping";
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

    private static final String ERROR_INTERNAL_UNEXPECTED_LEXEMA_ERROR_MESSAGE = "Internal error. Unexpected lexema";

    private static final int RESET_REQUIRED = -1;

    private final JsonStringBuilder string;

    private final JsonNumber number = new JsonNumber() {
        @Override
        public long mantissa() {
            return numberMantissa;
        }

        @Override
        public int exp() {
            return numberExp;
        }

        @Override
        public String toString() {
            return mantissa() + "E" + exp();
        }
    };
    private long numberMantissa;
    private int numberMantissaExp;
    private int numberExp;
    private int numberMinuses;

    private int[] scopeStack = new int[8];
    private int scopeStackDepth;

    private JsonParserListener listener;

    private String error;
    private int errorPosition;

    private boolean notifyObjectMemberNameString;

    private int currentLexemaState;
    private int currentLexemaPosition;

    public JsonParser() {
        this(new CopyingStringBuilder());
    }

    public JsonParser(final JsonStringBuilder stringBuilder) {
        string = stringBuilder;

        currentLexemaPosition = RESET_REQUIRED;
        currentLexemaState = LEXEMA_INITIAL;
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

    public JsonParser parse(final CharSequence data) {
        return parse(data, 0, data.length());
    }

    public JsonParser parse(final CharSequence data, final int start, final int len) {
        final JsonParserListener lnr = listener;

        assert lnr != null;

        int currentLexPos = currentLexemaPosition;

        if (currentLexPos == RESET_REQUIRED) {
            doReset();
            lnr.onJsonStarted();
        }

        int currentLexState = currentLexemaState;

        final JsonStringBuilder stringBuilder = this.string;

        int substringStart = 0;
        int substringSize = 0;

        try {
            for (int i = 0; i < len; i++) {
                final int charAbsPos = start + i;

                final char c = data.charAt(charAbsPos);

                switch (currentLexState) {
                    case LEXEMA_STRING_STARTED:
                        substringSize++;
                        if (c == '"') {
                            substringSize--;
                            stringBuilder.append(data, substringStart, substringSize);
                            currentLexState = LEXEMA_STRING_READY;
                            if (!onStringReady(lnr, currentLexPos)) {
                                //processLexema(currentLexState, currentLexPos)) {
                                return this;
                            }
                        } else if (c == '\\') {
                            substringSize--;
                            stringBuilder.append(data, substringStart, substringSize);
                            stringBuilder.appendEscape();
                            currentLexState = LEXEMA_STRING_STARTED_ESCAPE;
                        }
                        break;
                    case LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART:
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
                                numberMantissa = numberMantissa * 10 + (c - '0');
                                continue;
                            case '.':
                                currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART;
                                continue;
                            case 'e':
                            case 'E':
                                currentLexState = LEXEMA_NUMBER_STARTED_E;
                                continue;
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
                                i--;
                                currentLexState = LEXEMA_NUMBER_READY;
                                if (!onNumber(lnr, currentLexPos)) {
                                    return this;
                                }
                                continue;
                            default:
                                error(ERROR_INCORRECT_NUMBER_MESSAGE, i);
                                return this;
                        }
                    default:
                        switch (currentLexState) {
                            /* ready to start new lexema */
                            case LEXEMA_INITIAL:
                            case LEXEMA_CURLY_BRACKET_LEFT_READY:
                            case LEXEMA_CURLY_BRACKET_RIGHT_READY:
                            case LEXEMA_BOX_BRACKET_LEFT_READY:
                            case LEXEMA_BOX_BRACKET_RIGHT_READY:
                            case LEXEMA_COMMA_READY:
                            case LEXEMA_COLON_READY:
                            case LEXEMA_TRUE_READY:
                            case LEXEMA_FALSE_READY:
                            case LEXEMA_NULL_READY:
                            case LEXEMA_STRING_READY:
                            case LEXEMA_NUMBER_READY: {
                                switch (c) {
                                    case 0x09:
                                    case 0x0a:
                                    case 0x0d:
                                    case 0x20:
                                        continue;
                                    case '{':
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_CURLY_BRACKET_LEFT_READY;
                                        if (!onCurlyBracketLeft(lnr, currentLexPos)) {
                                            return this;
                                        }
                                        continue;
                                    case '}':
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_CURLY_BRACKET_RIGHT_READY;
                                        if (!onCurlyBracketRight(lnr, currentLexPos)) {
                                            return this;
                                        }
                                        continue;
                                    case '[':
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_BOX_BRACKET_LEFT_READY;
                                        if (!onBoxBracketLeft(lnr, currentLexPos)) {
                                            return this;
                                        }
                                        continue;
                                    case ']':
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_BOX_BRACKET_RIGHT_READY;
                                        if (!onBoxBracketRight(lnr, currentLexPos)) {
                                            return this;
                                        }
                                        continue;
                                    case ',':
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_COMMA_READY;
                                        if (!onComma(currentLexPos)) {
                                            return this;
                                        }
                                        continue;
                                    case ':':
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_COLON_READY;
                                        if (!onColon(currentLexPos)) {
                                            return this;
                                        }
                                        continue;
                                    case 't':
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_TRUE_STARTED_T;
                                        continue;
                                    case 'f':
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_FALSE_STARTED_F;
                                        continue;
                                    case 'n':
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_NULL_STARTED_N;
                                        continue;
                                    case '"':
                                        stringBuilder.start(data, charAbsPos);
                                        substringStart = i + 1;
                                        substringSize = 0;
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_STRING_STARTED;
                                        continue;
                                    case '-':
                                        numberMantissa = 0;
                                        numberMantissaExp = 0;
                                        numberExp = 0;
                                        numberMinuses = 2;
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_SIGN;
                                        continue;
                                    case '+':
                                        numberMantissa = 0;
                                        numberMantissaExp = 0;
                                        numberExp = 0;
                                        numberMinuses = 0;
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_SIGN;
                                        continue;
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
                                        numberMantissa = c - '0';
                                        numberMantissaExp = 0;
                                        numberExp = 0;
                                        numberMinuses = 0;
                                        currentLexPos = i;
                                        currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART;
                                        continue;
                                    default:
                                        continue;
                                }
                            }

                            /* constant 'true' - the rest */
                            case LEXEMA_TRUE_STARTED_T: {
                                switch (c) {
                                    case 'r':
                                        currentLexState = LEXEMA_TRUE_STARTED_TR;
                                        continue;
                                    default:
                                        error(ERROR_TRUE_EXPECTED_MESSAGE, i);
                                        return this;
                                }
                            }
                            case LEXEMA_TRUE_STARTED_TR: {
                                switch (c) {
                                    case 'u':
                                        currentLexState = LEXEMA_TRUE_STARTED_TRU;
                                        continue;
                                    default:
                                        error(ERROR_TRUE_EXPECTED_MESSAGE, i);
                                        return this;
                                }
                            }
                            case LEXEMA_TRUE_STARTED_TRU: {
                                switch (c) {
                                    case 'e':
                                        currentLexState = LEXEMA_TRUE_READY;
                                        if (!onTrue(lnr, currentLexPos)) {
                                            return this;
                                        }
                                        continue;
                                    default:
                                        error(ERROR_TRUE_EXPECTED_MESSAGE, i);
                                        return this;
                                }
                            }

                            /* constant 'false' - the rest */
                            case LEXEMA_FALSE_STARTED_F: {
                                switch (c) {
                                    case 'a':
                                        currentLexState = LEXEMA_FALSE_STARTED_FA;
                                        continue;
                                    default:
                                        error(ERROR_FALSE_EXPECTED_MESSAGE, i);
                                        return this;
                                }
                            }
                            case LEXEMA_FALSE_STARTED_FA: {
                                switch (c) {
                                    case 'l':
                                        currentLexState = LEXEMA_FALSE_STARTED_FAL;
                                        continue;
                                    default:
                                        error(ERROR_FALSE_EXPECTED_MESSAGE, i);
                                        return this;
                                }
                            }
                            case LEXEMA_FALSE_STARTED_FAL: {
                                switch (c) {
                                    case 's':
                                        currentLexState = LEXEMA_FALSE_STARTED_FALS;
                                        continue;
                                    default:
                                        error(ERROR_FALSE_EXPECTED_MESSAGE, i);
                                        return this;
                                }
                            }
                            case LEXEMA_FALSE_STARTED_FALS: {
                                switch (c) {
                                    case 'e':
                                        currentLexState = LEXEMA_FALSE_READY;
                                        if (!onFalse(lnr, currentLexPos)) {
                                            return this;
                                        }
                                        continue;
                                    default:
                                        error(ERROR_FALSE_EXPECTED_MESSAGE, i);
                                        return this;
                                }
                            }

                            /* constant 'null' - the rest */
                            case LEXEMA_NULL_STARTED_N: {
                                switch (c) {
                                    case 'u':
                                        currentLexState = LEXEMA_NULL_STARTED_NU;
                                        continue;
                                    default:
                                        error(ERROR_NULL_EXPECTED_MESSAGE, i);
                                        return this;
                                }
                            }
                            case LEXEMA_NULL_STARTED_NU: {
                                switch (c) {
                                    case 'l':
                                        currentLexState = LEXEMA_NULL_STARTED_NUL;
                                        continue;
                                    default:
                                        error(ERROR_NULL_EXPECTED_MESSAGE, i);
                                        return this;
                                }
                            }
                            case LEXEMA_NULL_STARTED_NUL: {
                                switch (c) {
                                    case 'l':
                                        currentLexState = LEXEMA_NULL_READY;
                                        if (!onNull(lnr, currentLexPos)) {
                                            return this;
                                        }
                                        continue;
                                    default:
                                        error(ERROR_NULL_EXPECTED_MESSAGE, i);
                                        return this;
                                }
                            }

                            /* string - the rest */
                            case LEXEMA_STRING_STARTED_ESCAPE: {
                                substringStart = i + 1;
                                substringSize = 0;
                                switch (c) {
                                    case '"':
                                        stringBuilder.appendEscapedQuotationMark();
                                        currentLexState = LEXEMA_STRING_STARTED;
                                        continue;
                                    case '\\':
                                        stringBuilder.appendEscapedReverseSolidus();
                                        currentLexState = LEXEMA_STRING_STARTED;
                                        continue;
                                    case '/':
                                        stringBuilder.appendEscapedSolidus();
                                        currentLexState = LEXEMA_STRING_STARTED;
                                        continue;
                                    case 'b':
                                        stringBuilder.appendEscapedBackspace();
                                        currentLexState = LEXEMA_STRING_STARTED;
                                        continue;
                                    case 'f':
                                        stringBuilder.appendEscapedFormfeed();
                                        currentLexState = LEXEMA_STRING_STARTED;
                                        continue;
                                    case 'n':
                                        stringBuilder.appendEscapedNewLine();
                                        currentLexState = LEXEMA_STRING_STARTED;
                                        continue;
                                    case 'r':
                                        stringBuilder.appendEscapedCarriageReturn();
                                        currentLexState = LEXEMA_STRING_STARTED;
                                        continue;
                                    case 't':
                                        stringBuilder.appendEscapedHorisontalTab();
                                        currentLexState = LEXEMA_STRING_STARTED;
                                        continue;
                                    case 'u':
                                        stringBuilder.appendEscapedUnicodeU();
                                        currentLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE;
                                        continue;
                                    default:
                                        error(ERROR_INCORRECT_ESCAPING_MESSAGE, i);
                                        return this;
                                }
                            }
                            case LEXEMA_STRING_STARTED_ESCAPE_UNICODE: {
                                if (stringBuilder.appendEscapedUnicodeChar1(c)) {
                                    substringStart = i + 1;
                                    currentLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1;
                                    continue;
                                } else {
                                    error(ERROR_INCORRECT_UNICODE_ESCAPING_MESSAGE, i);
                                    return this;
                                }
                            }
                            case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1: {
                                if (stringBuilder.appendEscapedUnicodeChar2(c)) {
                                    currentLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2;
                                    continue;
                                } else {
                                    error(ERROR_INCORRECT_UNICODE_ESCAPING_MESSAGE, i);
                                    return this;
                                }
                            }
                            case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2: {
                                if (stringBuilder.appendEscapedUnicodeChar3(c)) {
                                    currentLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3;
                                    continue;
                                } else {
                                    error(ERROR_INCORRECT_UNICODE_ESCAPING_MESSAGE, i);
                                    return this;
                                }
                            }
                            case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3: {
                                if (stringBuilder.appendEscapedUnicodeChar4(c)) {
                                    currentLexState = LEXEMA_STRING_STARTED;
                                    continue;
                                } else {
                                    error(ERROR_INCORRECT_UNICODE_ESCAPING_MESSAGE, i);
                                    return this;
                                }
                            }

                            /* number - the rest */
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
                                        numberMantissa = numberMantissa * 10 + (c - '0');
                                        currentLexState = LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART;
                                        continue;
                                    default:
                                        error(ERROR_INCORRECT_NUMBER_MESSAGE, i);
                                        return this;
                                }
                            }
                            case LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART: {
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
                                        numberMantissa = numberMantissa * 10 + (c - '0');
                                        numberMantissaExp--;
                                        continue;
                                    case 'e':
                                    case 'E':
                                        currentLexState = LEXEMA_NUMBER_STARTED_E;
                                        continue;
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
                                        i--;
                                        currentLexState = LEXEMA_NUMBER_READY;
                                        if (!onNumber(lnr, currentLexPos)) {
                                            return this;
                                        }
                                        continue;
                                    default:
                                        error(ERROR_INCORRECT_NUMBER_MESSAGE, i);
                                        return this;
                                }
                            }
                            case LEXEMA_NUMBER_STARTED_E: {
                                switch (c) {
                                    case '+':
                                        currentLexState = LEXEMA_NUMBER_STARTED_E_SIGN;
                                        continue;
                                    case '-':
                                        numberMinuses = numberMinuses | 1;
                                        currentLexState = LEXEMA_NUMBER_STARTED_E_SIGN;
                                        continue;
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
                                        numberExp = numberExp * 10 + (c - '0');
                                        currentLexState = LEXEMA_NUMBER_STARTED_E_VALUE;
                                        continue;
                                    default:
                                        error(ERROR_INCORRECT_NUMBER_MESSAGE, i);
                                        return this;
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
                                        numberExp = numberExp * 10 + (c - '0');
                                        currentLexState = LEXEMA_NUMBER_STARTED_E_VALUE;
                                        continue;
                                    default:
                                        error(ERROR_INCORRECT_NUMBER_MESSAGE, i);
                                        return this;
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
                                        numberExp = numberExp * 10 + (c - '0');
                                        currentLexState = LEXEMA_NUMBER_STARTED_E_VALUE;
                                        continue;
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
                                        i--;
                                        currentLexState = LEXEMA_NUMBER_READY;
                                        if (!onNumber(lnr, currentLexPos)) {
                                            return this;
                                        }
                                        continue;
                                    default:
                                        error(ERROR_INCORRECT_NUMBER_MESSAGE, i);
                                        return this;
                                }
                            }

                            /* unknown state */
                            default:
                                error(ERROR_INTERNAL_UNEXPECTED_LEXEMA_ERROR_MESSAGE, i);
                                return this;
                        }
                }
            }

            if (substringSize > 0) {
                stringBuilder.append(data, substringStart, substringSize);
            }
        } finally {
            currentLexemaState = currentLexState;
            currentLexemaPosition = currentLexPos;
        }
        return this;
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
        currentLexemaPosition = RESET_REQUIRED;
        return this;
    }

    private void doReset() {
        currentLexemaState = LEXEMA_INITIAL;
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

    private void setNumber() {
        if ((numberMinuses & 2) != 0) {
            numberMantissa = -numberMantissa;
        }
        if ((numberMinuses & 1) != 0) {
            numberExp = -numberExp;
        }
        numberExp = numberExp + numberMantissaExp;
    }

    private boolean onCurlyBracketLeft(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL:
                lnr.onObjectStarted();
                replaceScope(EXPRESSION_OBJECT_STARTED);
                break;
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER:
                lnr.onObjectStarted();
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                pushScope(EXPRESSION_OBJECT_STARTED);
                break;
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER:
                lnr.onObjectStarted();
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                pushScope(EXPRESSION_OBJECT_STARTED);
                break;
            default:
                error(ERROR_UNEXPECTED_CURLY_BRACKET_LEFT_MESSAGE, lexemaPosition);
                return false;
        }
        return true;
    }

    private boolean onCurlyBracketRight(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_OBJECT_STARTED:
            case EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER:
                lnr.onObjectEnded();
                popScope();
                break;
            case EXPRESSION_OBJECT_STARTED_MEMBER_VALUE:
                lnr.onObjectEnded();
                popScope();
                break;
            default:
                error(ERROR_UNEXPECTED_CURLY_BRACKET_RIGHT_MESSAGE, lexemaPosition);
                return false;
        }
        return true;
    }

    private boolean onBoxBracketLeft(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL:
                lnr.onArrayStarted();
                replaceScope(EXPRESSION_ARRAY_STARTED);
                break;
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER:
                lnr.onArrayStarted();
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                pushScope(EXPRESSION_ARRAY_STARTED);
                break;
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER:
                lnr.onArrayStarted();
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                pushScope(EXPRESSION_ARRAY_STARTED);
                break;
            default:
                error(ERROR_UNEXPECTED_BOX_BRACKET_LEFT_MESSAGE, lexemaPosition);
                return false;
        }
        return true;
    }

    private boolean onBoxBracketRight(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER:
                lnr.onArrayEnded();
                popScope();
                break;
            case EXPRESSION_ARRAY_STARTED_VALUE:
                lnr.onArrayEnded();
                popScope();
                break;
            default:
                error(ERROR_UNEXPECTED_BOX_BRACKET_RIGHT_MESSAGE, lexemaPosition);
                return false;
        }
        return true;
    }

    private boolean onComma(final int lexemaPosition) {
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
                return false;
        }
        return true;
    }

    private boolean onColon(final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME:
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER);
                break;
            default:
                error(ERROR_UNEXPECTED_COLON_MESSAGE, lexemaPosition);
                return false;
        }
        return true;
    }

    private boolean onStringReady(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL:
                lnr.onStringValue(string);
                popScope();
                break;
            case EXPRESSION_OBJECT_STARTED:
            case EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER:
                lnr.onObjectMember(string);
                if (notifyObjectMemberNameString) {
                    lnr.onStringValue(string);
                }
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_NAME);
                break;
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER:
                lnr.onStringValue(string);
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                break;
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER:
                lnr.onStringValue(string);
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                break;
            default:
                error(ERROR_UNEXPECTED_STRING_MESSAGE, lexemaPosition);
                return false;
        }
        return true;
    }

    private boolean onNumber(final JsonParserListener lnr, final int lexemaPosition) {
        setNumber();

        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL:
                lnr.onNumberValue(number);
                popScope();
                break;
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER:
                lnr.onNumberValue(number);
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                break;
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER:
                lnr.onNumberValue(number);
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                break;
            default:
                error(ERROR_UNEXPECTED_NUMBER_MESSAGE, lexemaPosition);
                return false;
        }
        return true;
    }

    private boolean onTrue(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL:
                lnr.onTrueValue();
                popScope();
                break;
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER:
                lnr.onTrueValue();
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                break;
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER:
                lnr.onTrueValue();
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                break;
            default:
                error(ERROR_UNEXPECTED_TRUE_MESSAGE, lexemaPosition);
                return false;
        }
        return true;
    }

    private boolean onFalse(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL:
                lnr.onFalseValue();
                popScope();
                break;
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER:
                lnr.onFalseValue();
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                break;
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER:
                lnr.onFalseValue();
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                break;
            default:
                error(ERROR_UNEXPECTED_FALSE_MESSAGE, lexemaPosition);
                return false;
        }
        return true;
    }

    private boolean onNull(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL:
                lnr.onNullValue();
                popScope();
                break;
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER:
                lnr.onNullValue();
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                break;
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER:
                lnr.onNullValue();
                replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                break;
            default:
                error(ERROR_UNEXPECTED_NULL_MESSAGE, lexemaPosition);
                return false;
        }
        return true;
    }

    private boolean onEoj(final JsonParserListener lnr, final int lexemaPosition) {
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
                        return false;
                    case LEXEMA_FALSE_STARTED_F:
                    case LEXEMA_FALSE_STARTED_FA:
                    case LEXEMA_FALSE_STARTED_FAL:
                    case LEXEMA_FALSE_STARTED_FALS:
                        error(ERROR_FALSE_EXPECTED_MESSAGE, lexemaPosition);
                        return false;
                    case LEXEMA_NULL_STARTED_N:
                    case LEXEMA_NULL_STARTED_NU:
                    case LEXEMA_NULL_STARTED_NUL:
                        error(ERROR_NULL_EXPECTED_MESSAGE, lexemaPosition);
                        return false;
                    case LEXEMA_STRING_STARTED:
                    case LEXEMA_STRING_STARTED_ESCAPE:
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE:
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1:
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2:
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3:
                        error(ERROR_INCORRECT_STRING_MESSAGE, lexemaPosition);
                        return false;
                    case LEXEMA_NUMBER_STARTED_MANTISSA_SIGN:
                    case LEXEMA_NUMBER_STARTED_E:
                    case LEXEMA_NUMBER_STARTED_E_SIGN:
                        error(ERROR_INCORRECT_NUMBER_MESSAGE, lexemaPosition);
                        return false;
                    case LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART:
                    case LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART:
                    case LEXEMA_NUMBER_STARTED_E_VALUE:
                        setNumber(); // try to apply the number
                        lnr.onNumberValue(number);
                        popScope();
                        break;
                }
                break;
            default:
                error(ERROR_INTERNAL_UNEXPECTED_LEXEMA_ERROR_MESSAGE, lexemaPosition);
                return false;
        }
        return true;
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