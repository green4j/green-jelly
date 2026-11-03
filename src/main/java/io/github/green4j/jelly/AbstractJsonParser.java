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

/**
 * The part of the parsers, which doesn't depend on the type of the data parsed: the grammar of JSON
 * over the lexemas the parsers read, and the state of the parsing. The methods are final and are called
 * by the parsers only, so the calls are bound statically and inlined the same way as within a single class.
 */
abstract class AbstractJsonParser {

    static final int LEXEMA_TRUE_STARTED_T = 1;
    static final int LEXEMA_TRUE_STARTED_TR = LEXEMA_TRUE_STARTED_T + 1;
    static final int LEXEMA_TRUE_STARTED_TRU = LEXEMA_TRUE_STARTED_TR + 1;

    static final int LEXEMA_FALSE_STARTED_F = LEXEMA_TRUE_STARTED_TRU + 1;
    static final int LEXEMA_FALSE_STARTED_FA = LEXEMA_FALSE_STARTED_F + 1;
    static final int LEXEMA_FALSE_STARTED_FAL = LEXEMA_FALSE_STARTED_FA + 1;
    static final int LEXEMA_FALSE_STARTED_FALS = LEXEMA_FALSE_STARTED_FAL + 1;

    static final int LEXEMA_NULL_STARTED_N = LEXEMA_FALSE_STARTED_FALS + 1;
    static final int LEXEMA_NULL_STARTED_NU = LEXEMA_NULL_STARTED_N + 1;
    static final int LEXEMA_NULL_STARTED_NUL = LEXEMA_NULL_STARTED_NU + 1;

    static final int LEXEMA_STRING_STARTED = LEXEMA_NULL_STARTED_NUL + 1;
    static final int LEXEMA_STRING_STARTED_ESCAPE = LEXEMA_STRING_STARTED + 1;
    static final int LEXEMA_STRING_STARTED_ESCAPE_UNICODE = LEXEMA_STRING_STARTED_ESCAPE + 1;
    static final int LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1 = LEXEMA_STRING_STARTED_ESCAPE_UNICODE + 1;
    static final int LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2 = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1 + 1;
    static final int LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3 = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2 + 1;

    static final int LEXEMA_NUMBER_STARTED_MANTISSA_SIGN = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3 + 1;
    static final int LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART = LEXEMA_NUMBER_STARTED_MANTISSA_SIGN + 1;
    static final int LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART
            = LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART + 1;
    static final int LEXEMA_NUMBER_STARTED_E = LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART + 1;
    static final int LEXEMA_NUMBER_STARTED_E_SIGN = LEXEMA_NUMBER_STARTED_E + 1;
    static final int LEXEMA_NUMBER_STARTED_E_VALUE = LEXEMA_NUMBER_STARTED_E_SIGN + 1;

    static final int LEXEMA_READY = LEXEMA_NUMBER_STARTED_E_VALUE + 1;

    static final int LEXEMA_CURLY_BRACKET_LEFT_READY = LEXEMA_READY + 1;
    static final int LEXEMA_CURLY_BRACKET_RIGHT_READY = LEXEMA_CURLY_BRACKET_LEFT_READY + 1;
    static final int LEXEMA_BOX_BRACKET_LEFT_READY = LEXEMA_CURLY_BRACKET_RIGHT_READY + 1;
    static final int LEXEMA_BOX_BRACKET_RIGHT_READY = LEXEMA_BOX_BRACKET_LEFT_READY + 1;
    static final int LEXEMA_COMMA_READY = LEXEMA_BOX_BRACKET_RIGHT_READY + 1;
    static final int LEXEMA_COLON_READY = LEXEMA_COMMA_READY + 1;
    static final int LEXEMA_STRING_READY = LEXEMA_COLON_READY + 1;
    static final int LEXEMA_NUMBER_READY = LEXEMA_STRING_READY + 1;
    static final int LEXEMA_TRUE_READY = LEXEMA_NUMBER_READY + 1;
    static final int LEXEMA_FALSE_READY = LEXEMA_TRUE_READY + 1;
    static final int LEXEMA_NULL_READY = LEXEMA_FALSE_READY + 1;

    static final int EXPRESSION_INITIAL = 0;

    static final int EXPRESSION_OBJECT_STARTED = EXPRESSION_INITIAL + 1;
    static final int EXPRESSION_OBJECT_STARTED_MEMBER_NAME = EXPRESSION_OBJECT_STARTED + 1;
    static final int EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER =
            EXPRESSION_OBJECT_STARTED_MEMBER_NAME + 1;
    static final int EXPRESSION_OBJECT_STARTED_MEMBER_VALUE =
            EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER + 1;
    static final int EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER =
            EXPRESSION_OBJECT_STARTED_MEMBER_VALUE + 1;

    static final int EXPRESSION_ARRAY_STARTED = EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER + 1;
    static final int EXPRESSION_ARRAY_STARTED_VALUE = EXPRESSION_ARRAY_STARTED + 1;
    static final int EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER = EXPRESSION_ARRAY_STARTED_VALUE + 1;

    static final String ERROR_INCORRECT_STRING_MESSAGE = "Incorrect string";
    static final String ERROR_INCORRECT_ESCAPING_MESSAGE = "Incorrect character escaping";
    static final String ERROR_MESSAGE_INCORRECT_UNICODE_ESCAPING = "Incorrect unicode escaping";
    static final String ERROR_INCORRECT_NUMBER_MESSAGE = "Incorrect number";
    static final String ERROR_TRUE_EXPECTED_MESSAGE = "'true' expected";
    static final String ERROR_FALSE_EXPECTED_MESSAGE = "'false' expected";
    static final String ERROR_NULL_EXPECTED_MESSAGE = "'null' expected";
    static final String ERROR_UNEXPECTED_CURLY_BRACKET_LEFT_MESSAGE = "Unexpected '{'";
    static final String ERROR_UNEXPECTED_CURLY_BRACKET_RIGHT_MESSAGE = "Unexpected '}'";
    static final String ERROR_UNEXPECTED_BOX_BRACKET_LEFT_MESSAGE = "Unexpected '['";
    static final String ERROR_UNEXPECTED_BOX_BRACKET_RIGHT_MESSAGE = "Unexpected ']'";
    static final String ERROR_UNEXPECTED_COMMA_MESSAGE = "Unexpected ','";
    static final String ERROR_UNEXPECTED_COLON_MESSAGE = "Unexpected ':'";
    static final String ERROR_UNEXPECTED_STRING_MESSAGE = "Unexpected string";
    static final String ERROR_UNEXPECTED_NUMBER_MESSAGE = "Unexpected number";
    static final String ERROR_UNEXPECTED_TRUE_MESSAGE = "Unexpected 'true'";
    static final String ERROR_UNEXPECTED_FALSE_MESSAGE = "Unexpected 'false'";
    static final String ERROR_UNEXPECTED_NULL_MESSAGE = "Unexpected 'null'";
    static final String ERROR_UNEXPECTED_CHAR_MESSAGE = "Unexpected char";

    static final String ERROR_INTERNAL_UNEXPECTED_LEXEMA_ERROR_MESSAGE = "Internal error. Unexpected lexema";

    static final int JSON_STARTED_NOTIFICATION_REQUIRED = -1;
    static final int RESET_REQUIRED = JSON_STARTED_NOTIFICATION_REQUIRED - 1;

    static final int RESUME_LEXEMA_REQUIRED = 2;

    static final int MAX_SAFE_DIGITS = 18;

    static final char[] LITERAL_EXPECTED_CHARS = new char[LEXEMA_NULL_STARTED_NUL + 1];
    static final boolean[] NUMBER_DELIMITERS = new boolean[128];

    static {
        LITERAL_EXPECTED_CHARS[LEXEMA_TRUE_STARTED_T] = 'r';
        LITERAL_EXPECTED_CHARS[LEXEMA_TRUE_STARTED_TR] = 'u';
        LITERAL_EXPECTED_CHARS[LEXEMA_TRUE_STARTED_TRU] = 'e';
        LITERAL_EXPECTED_CHARS[LEXEMA_FALSE_STARTED_F] = 'a';
        LITERAL_EXPECTED_CHARS[LEXEMA_FALSE_STARTED_FA] = 'l';
        LITERAL_EXPECTED_CHARS[LEXEMA_FALSE_STARTED_FAL] = 's';
        LITERAL_EXPECTED_CHARS[LEXEMA_FALSE_STARTED_FALS] = 'e';
        LITERAL_EXPECTED_CHARS[LEXEMA_NULL_STARTED_N] = 'u';
        LITERAL_EXPECTED_CHARS[LEXEMA_NULL_STARTED_NU] = 'l';
        LITERAL_EXPECTED_CHARS[LEXEMA_NULL_STARTED_NUL] = 'l';

        for (final char c : new char[]{0x09, 0x0a, 0x0d, 0x20, '{', '}', '[', ']', ',', ':'}) {
            NUMBER_DELIMITERS[c] = true;
        }
    }

    static final long MAX_MANTISSA_BASE = 922337203685477579L;

    private final CharSequence stringValue;

    final StringBuilder overflownNumber = new StringBuilder();

    final MutableJsonNumber number = new MutableJsonNumber();

    JsonParserListener listener;

    boolean notifyObjectMemberNameString;

    int currentLen;
    int currentStart;
    int currentPos;
    int currentLexemaState;
    int currentLexemaPosition;

    int numberMantissaExp;
    int numberMinuses;
    boolean numberOverflow;

    String error;
    int errorPosition;

    private int[] scopeStack = new int[8];
    private int scopeStackDepth;

    AbstractJsonParser(final CharSequence stringValue) {
        this.stringValue = stringValue;

        currentLexemaPosition = RESET_REQUIRED;
        currentLexemaState = LEXEMA_READY;
        pushScope(EXPRESSION_INITIAL);
    }

    public boolean isNotifyObjectMemberNameString() {
        return notifyObjectMemberNameString;
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

    /**
     * The full decimal representation of the last number which overflowed the mantissa.
     *
     * <p>The content is valid only while the {@code onNumberValue(JsonNumber, boolean)} callback,
     * which has been notified with {@code overflow} set to {@code true}, is being handled.
     * It's a well-formed decimal literal, so it can be passed to
     * {@code new java.math.BigDecimal(CharSequence)} as is.
     *
     * @return the buffer with the text of the last overflown number
     */
    public CharSequence overflownNumber() {
        return overflownNumber;
    }

    // Notifies the listener that a JSON has started, if the data given is the first part of it.
    final void onJsonStartedIfRequired(final JsonParserListener lnr) {
        if (currentLexemaPosition < 0) {
            if (currentLexemaPosition < JSON_STARTED_NOTIFICATION_REQUIRED) {
                doReset();
            }
            lnr.onJsonStarted();
            // the data may have no lexemas, the notification must not be repeated with the next data
            currentLexemaPosition = 0;
        }
    }

    final int onIncorrectUnicodeEscaping(final int state, final int p) {
        currentLexemaState = state;
        error(ERROR_MESSAGE_INCORRECT_UNICODE_ESCAPING, p - currentStart);
        return -1;
    }

    final void onMantissaOverflown(
            final long mantissa,
            final boolean fractionalPart,
            final int mantissaExp,
            final char digit) {
        if (numberOverflow) {
            overflownNumber.append(digit);
        } else {
            numberOverflow = true;
            startOverflownNumber(overflownNumber, mantissa, numberMinuses, fractionalPart, mantissaExp, digit);
        }
    }

    final void doEoj() {
        final JsonParserListener lnr = listener;

        if (lnr != null) {
            if (error == null) {
                onEoj(lnr, currentLexemaPosition);
            }
            lnr.onJsonEnded();
        }
        currentLexemaPosition = RESET_REQUIRED;
    }

    final void doResetAndNotify() {
        doReset();
        currentLexemaPosition = JSON_STARTED_NOTIFICATION_REQUIRED;
    }

    final void doReset() {
        currentLexemaState = LEXEMA_READY;
        clearScope();
        pushScope(EXPRESSION_INITIAL);
        error = null;
    }

    final void error(final String error, final int position) {
        final JsonParserListener lnr = listener;

        assert lnr != null;

        this.error = error;
        this.errorPosition = position;
        lnr.onError(error, position);
    }

    static void setNumber(
            final MutableJsonNumber number,
            final int numberMinuses,
            final int numberMantissaExp,
            final boolean numberOverflow,
            final StringBuilder overflownNumber) {
        if (numberOverflow) {
            // number.exp() still holds the magnitude of the explicit exponent only,
            // its sign is in the bit 1 of numberMinuses
            final int explicitExp = number.exp();
            if (explicitExp != 0) {
                overflownNumber.append('e');
                if ((numberMinuses & 1) != 0) {
                    overflownNumber.append('-');
                }
                overflownNumber.append(explicitExp);
            }
        }
        if ((numberMinuses & 2) != 0) {
            number.setMantissa(-number.mantissa());
        }
        if ((numberMinuses & 1) != 0) {
            number.setExp(-number.exp());
        }
        number.setExp(number.exp() + numberMantissaExp);
    }

    final int onCurlyBracketLeft(final JsonParserListener lnr, final int lexemaPosition) {
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

    final int onCurlyBracketRight(final JsonParserListener lnr, final int lexemaPosition) {
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

    final int onBoxBracketLeft(final JsonParserListener lnr, final int lexemaPosition) {
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

    final int onBoxBracketRight(final JsonParserListener lnr, final int lexemaPosition) {
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

    final int onComma(final int lexemaPosition) {
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

    final int onColon(final int lexemaPosition) {
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

    final int onStringReady(final JsonParserListener lnr, final int lexemaPosition) {
        final int currentScope = peekScope();

        final CharSequence value = stringValue;

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

    final int onNumber(final JsonParserListener lnr, final int lexemaPosition) {
        setNumber(number, numberMinuses, numberMantissaExp, numberOverflow, overflownNumber);

        final int currentScope = peekScope();

        switch (currentScope) {
            case EXPRESSION_INITIAL: {
                final boolean r = lnr.onNumberValue(number, numberOverflow);
                popScope();
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER: {
                final boolean r = lnr.onNumberValue(number, numberOverflow);
                replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                if (r) {
                    break;
                }
                return 1;
            }
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER: {
                final boolean r = lnr.onNumberValue(number, numberOverflow);
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

    final int onTrue(final JsonParserListener lnr, final int lexemaPosition) {
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

    final int onFalse(final JsonParserListener lnr, final int lexemaPosition) {
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

    final int onNull(final JsonParserListener lnr, final int lexemaPosition) {
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
                        // try to apply the number
                        setNumber(number, numberMinuses, numberMantissaExp, numberOverflow, overflownNumber);
                        lnr.onNumberValue(number, numberOverflow);
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

    /**
     * Starts a capturing of the full decimal representation of a number which has just overflown
     * the mantissa. The digits accumulated so far are exact, so the head of the number is rendered
     * back from the state of the parsing, and the digit the overflow has happened on is appended to it.
     * All the following digits are appended to the same buffer as is.
     *
     * @param sink the buffer to render into
     * @param mantissa the mantissa accumulated so far. It's still a positive magnitude at this point
     * @param numberMinuses the bit mask of the minuses of the number
     * @param fractionalPart {@code true} if the overflow has happened in the fractional part of the mantissa
     * @param numberMantissaExp the current exponent of the mantissa
     * @param digit the digit the overflow has happened on
     * @return the buffer the number has been rendered into
     */
    static StringBuilder startOverflownNumber(
            final StringBuilder sink,
            final long mantissa,
            final int numberMinuses,
            final boolean fractionalPart,
            final int numberMantissaExp,
            final char digit) {
        final StringBuilder result = sink;
        result.setLength(0);
        if ((numberMinuses & 2) != 0) {
            result.append('-');
        }
        final int digitsStart = result.length();
        result.append(mantissa);
        if (fractionalPart) {
            final int fractionalDigits = -numberMantissaExp;
            final int digits = result.length() - digitsStart;
            if (fractionalDigits < digits) {
                result.insert(digitsStart + digits - fractionalDigits, '.');
            } else { // 0.000...digits
                result.insert(digitsStart, "0.");
                for (int i = digits; i < fractionalDigits; i++) {
                    result.insert(digitsStart + 2, '0');
                }
            }
        }
        result.append(digit);
        return result;
    }
}
