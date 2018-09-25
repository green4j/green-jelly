package org.green.jelly;

public final class JsonParser {

    public static JsonParser newFlyweightParser() {
        return new JsonParser(new FlyweightStringBuilder());
    }

    public static JsonParser newCopyingParser() {
        return new JsonParser();
    }

    public static JsonParser newCopyingRawParser() {
        final CopyingStringBuilder builder = new CopyingStringBuilder(true);
        return new JsonParser(builder);
    }

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
    private static final int LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART =
        LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART + 1;
    private static final int LEXEMA_NUMBER_STARTED_E = LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART + 1;
    private static final int LEXEMA_NUMBER_STARTED_E_SIGN = LEXEMA_NUMBER_STARTED_E + 1;
    private static final int LEXEMA_NUMBER_STARTED_E_VALUE = LEXEMA_NUMBER_STARTED_E_SIGN + 1;

    private static final int LEXEMA_READY = 100;

    private static final int LEXEMA_CURLY_BRACKET_LEFT_READY = LEXEMA_READY + 1;
    private static final int LEXEMA_CURLY_BRACKET_RIGHT_READY = LEXEMA_CURLY_BRACKET_LEFT_READY + 1;
    private static final int LEXEMA_BOX_BRACKET_LEFT_READY = LEXEMA_CURLY_BRACKET_RIGHT_READY + 1;
    private static final int LEXEMA_BOX_BRACKET_RIGHT_READY = LEXEMA_BOX_BRACKET_LEFT_READY + 1;
    private static final int LEXEMA_COMMA_READY = LEXEMA_BOX_BRACKET_RIGHT_READY + 1;
    private static final int LEXEMA_COLON_READY = LEXEMA_COMMA_READY + 1;
    private static final int LEXEMA_TRUE_READY = LEXEMA_COLON_READY + 1;
    private static final int LEXEMA_FALSE_READY = LEXEMA_TRUE_READY + 1;
    private static final int LEXEMA_NULL_READY = LEXEMA_FALSE_READY + 1;
    private static final int LEXEMA_STRING_READY = LEXEMA_NULL_READY + 1;
    private static final int LEXEMA_NUMBER_READY = LEXEMA_STRING_READY + 1;
    private static final int LEXEMA_EOJ_READY = LEXEMA_NUMBER_READY + 1; // End Of Json
                                                                         // - special lexema to be passed
                                                                         // out from the parser

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

    private static final String ERROR_TRUE_EXPECTED_MESSAGE = "'true' expected";
    private static final String ERROR_FALSE_EXPECTED_MESSAGE = "'false' expected";
    private static final String ERROR_NULL_EXPECTED_MESSAGE = "'null' expected";
    private static final String ERROR_INCORRECT_ESCAPING_MESSAGE = "Incorrect character escaping";
    private static final String ERROR_INCORRECT_UNICODE_ESCAPING_MESSAGE = "Incorrect unicode escaping";
    private static final String ERROR_INCORRECT_STRING_ERROR_MESSAGE = "Incorrect string";
    private static final String ERROR_INCORRECT_NUMBER_ERROR_MESSAGE = "Incorrect number";
    private static final String ERROR_INCORRECT_OBJECT_ERROR_MESSAGE = "Incorrect object";
    private static final String ERROR_INCORRECT_ARRAY_ERROR_MESSAGE = "Incorrect array";
    private static final String ERROR_MULTIPLE_VALUES_ERROR_MESSAGE = "Multiple values are not allowed in JSON;"
        + " only one literal, object or array can be defined. Finish the current JSON with .eoj() or .reset()";

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
    private int errorPosition = -1;

    private boolean notifyObjectMemberNameString;

    private int currentLexemaState;
    private int currentLexemaPosition = RESET_REQUIRED;

    public JsonParser() {
        this(new CopyingStringBuilder());
    }

    public JsonParser(final JsonStringBuilder stringBuilder) {
        string = stringBuilder;

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

        try {
            int newLexState = -1;
            for (int i = 0; i < len; i++) {
                final int charAbsPos = start + i;

                final char c = data.charAt(charAbsPos);

                check_new_lex_:
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
                                newLexState = LEXEMA_CURLY_BRACKET_LEFT_READY;
                                currentLexPos = i;
                                break check_new_lex_;
                            case '}':
                                newLexState = LEXEMA_CURLY_BRACKET_RIGHT_READY;
                                currentLexPos = i;
                                break check_new_lex_;
                            case '[':
                                newLexState = LEXEMA_BOX_BRACKET_LEFT_READY;
                                currentLexPos = i;
                                break check_new_lex_;
                            case ']':
                                newLexState = LEXEMA_BOX_BRACKET_RIGHT_READY;
                                currentLexPos = i;
                                break check_new_lex_;
                            case ',':
                                newLexState = LEXEMA_COMMA_READY;
                                currentLexPos = i;
                                break check_new_lex_;
                            case ':':
                                newLexState = LEXEMA_COLON_READY;
                                currentLexPos = i;
                                break check_new_lex_;
                            case 't':
                                newLexState = LEXEMA_TRUE_STARTED_T;
                                currentLexPos = i;
                                break check_new_lex_;
                            case 'f':
                                newLexState = LEXEMA_FALSE_STARTED_F;
                                currentLexPos = i;
                                break check_new_lex_;
                            case 'n':
                                newLexState = LEXEMA_NULL_STARTED_N;
                                currentLexPos = i;
                                break check_new_lex_;
                            case '"':
                                string.start(data, charAbsPos);
                                newLexState = LEXEMA_STRING_STARTED;
                                currentLexPos = i;
                                break check_new_lex_;
                            case '-':
                                numberMantissa = 0;
                                numberMantissaExp = 0;
                                numberExp = 0;
                                numberMinuses = 2;
                                newLexState = LEXEMA_NUMBER_STARTED_MANTISSA_SIGN;
                                currentLexPos = i;
                                break check_new_lex_;
                            case '+':
                                numberMantissa = 0;
                                numberMantissaExp = 0;
                                numberExp = 0;
                                numberMinuses = 0;
                                newLexState = LEXEMA_NUMBER_STARTED_MANTISSA_SIGN;
                                currentLexPos = i;
                                break check_new_lex_;
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
                                newLexState = LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART;
                                currentLexPos = i;
                                break check_new_lex_;
                            default:
                                continue;
                        }
                    }

                    /* constant 'true' - the rest */
                    case LEXEMA_TRUE_STARTED_T: {
                        switch (c) {
                            case 'r':
                                newLexState = LEXEMA_TRUE_STARTED_TR;
                                break check_new_lex_;
                            default:
                                error(ERROR_TRUE_EXPECTED_MESSAGE, i);
                                return this;
                        }
                    }
                    case LEXEMA_TRUE_STARTED_TR: {
                        switch (c) {
                            case 'u':
                                newLexState = LEXEMA_TRUE_STARTED_TRU;
                                break check_new_lex_;
                            default:
                                error(ERROR_TRUE_EXPECTED_MESSAGE, i);
                                return this;
                        }
                    }
                    case LEXEMA_TRUE_STARTED_TRU: {
                        switch (c) {
                            case 'e':
                                newLexState = LEXEMA_TRUE_READY;
                                break check_new_lex_;
                            default:
                                error(ERROR_TRUE_EXPECTED_MESSAGE, i);
                                return this;
                        }
                    }

                    /* constant 'false' - the rest */
                    case LEXEMA_FALSE_STARTED_F: {
                        switch (c) {
                            case 'a':
                                newLexState = LEXEMA_FALSE_STARTED_FA;
                                break check_new_lex_;
                            default:
                                error(ERROR_FALSE_EXPECTED_MESSAGE, i);
                                return this;
                        }
                    }
                    case LEXEMA_FALSE_STARTED_FA: {
                        switch (c) {
                            case 'l':
                                newLexState = LEXEMA_FALSE_STARTED_FAL;
                                break check_new_lex_;
                            default:
                                error(ERROR_FALSE_EXPECTED_MESSAGE, i);
                                return this;
                        }
                    }
                    case LEXEMA_FALSE_STARTED_FAL: {
                        switch (c) {
                            case 's':
                                newLexState = LEXEMA_FALSE_STARTED_FALS;
                                break check_new_lex_;
                            default:
                                error(ERROR_FALSE_EXPECTED_MESSAGE, i);
                                return this;
                        }
                    }
                    case LEXEMA_FALSE_STARTED_FALS: {
                        switch (c) {
                            case 'e':
                                newLexState = LEXEMA_FALSE_READY;
                                break check_new_lex_;
                            default:
                                error(ERROR_FALSE_EXPECTED_MESSAGE, i);
                                return this;
                        }
                    }

                    /* constant 'null' - the rest */
                    case LEXEMA_NULL_STARTED_N: {
                        switch (c) {
                            case 'u':
                                newLexState = LEXEMA_NULL_STARTED_NU;
                                break check_new_lex_;
                            default:
                                error(ERROR_NULL_EXPECTED_MESSAGE, i);
                                return this;
                        }
                    }
                    case LEXEMA_NULL_STARTED_NU: {
                        switch (c) {
                            case 'l':
                                newLexState = LEXEMA_NULL_STARTED_NUL;
                                break check_new_lex_;
                            default:
                                error(ERROR_NULL_EXPECTED_MESSAGE, i);
                                return this;
                        }
                    }
                    case LEXEMA_NULL_STARTED_NUL: {
                        switch (c) {
                            case 'l':
                                newLexState = LEXEMA_NULL_READY;
                                break check_new_lex_;
                            default:
                                error(ERROR_NULL_EXPECTED_MESSAGE, i);
                                return this;
                        }
                    }

                    /* string - the rest */
                    case LEXEMA_STRING_STARTED: {
                        switch (c) {
                            case '"':
                                newLexState = LEXEMA_STRING_READY;
                                break check_new_lex_;
                            case '\\':
                                string.appendEscape(data, charAbsPos);
                                newLexState = LEXEMA_STRING_STARTED_ESCAPE;
                                break check_new_lex_;
                            default:
                                string.append(data, charAbsPos, c);
                                break;
                        }
                        continue;
                    }
                    case LEXEMA_STRING_STARTED_ESCAPE: {
                        switch (c) {
                            case '"':
                                string.appendEscapedQuotationMark(data, charAbsPos);
                                newLexState = LEXEMA_STRING_STARTED;
                                break check_new_lex_;
                            case '\\':
                                string.appendEscapedReverseSolidus(data, charAbsPos);
                                newLexState = LEXEMA_STRING_STARTED;
                                break check_new_lex_;
                            case '/':
                                string.appendEscapedSolidus(data, charAbsPos);
                                newLexState = LEXEMA_STRING_STARTED;
                                break check_new_lex_;
                            case 'b':
                                string.appendEscapedBackspace(data, charAbsPos);
                                newLexState = LEXEMA_STRING_STARTED;
                                break check_new_lex_;
                            case 'f':
                                string.appendEscapedFormfeed(data, charAbsPos);
                                newLexState = LEXEMA_STRING_STARTED;
                                break check_new_lex_;
                            case 'n':
                                string.appendEscapedNewLine(data, charAbsPos);
                                newLexState = LEXEMA_STRING_STARTED;
                                break check_new_lex_;
                            case 'r':
                                string.appendEscapedCarriageReturn(data, charAbsPos);
                                newLexState = LEXEMA_STRING_STARTED;
                                break check_new_lex_;
                            case 't':
                                string.appendEscapedHorisontalTab(data, charAbsPos);
                                newLexState = LEXEMA_STRING_STARTED;
                                break check_new_lex_;
                            case 'u':
                                string.appendEscapedUnicodeU(data, charAbsPos);
                                newLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE;
                                break check_new_lex_;
                            default:
                                error(ERROR_INCORRECT_ESCAPING_MESSAGE, i);
                                return this;
                        }
                    }
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE: {
                        if (string.appendEscapedUnicodeChar1(data, charAbsPos, c)) {
                            newLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1;
                            break check_new_lex_;
                        } else {
                            error(ERROR_INCORRECT_UNICODE_ESCAPING_MESSAGE, i);
                            return this;
                        }
                    }
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_1: {
                        if (string.appendEscapedUnicodeChar2(data, charAbsPos, c)) {
                            newLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2;
                            break check_new_lex_;
                        } else {
                            error(ERROR_INCORRECT_UNICODE_ESCAPING_MESSAGE, i);
                            return this;
                        }
                    }
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_2: {
                        if (string.appendEscapedUnicodeChar3(data, charAbsPos, c)) {
                            newLexState = LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3;
                            break check_new_lex_;
                        } else {
                            error(ERROR_INCORRECT_UNICODE_ESCAPING_MESSAGE, i);
                            return this;
                        }
                    }
                    case LEXEMA_STRING_STARTED_ESCAPE_UNICODE_3: {
                        if (string.appendEscapedUnicodeChar4(data, charAbsPos, c)) {
                            newLexState = LEXEMA_STRING_STARTED;
                            break check_new_lex_;
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
                                newLexState = LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART;
                                break check_new_lex_;
                            default:
                                error(ERROR_INCORRECT_NUMBER_ERROR_MESSAGE, i);
                                return this;
                        }
                    }
                    case LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART: {
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
                                newLexState = LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART;
                                break check_new_lex_;
                            case 'e':
                            case 'E':
                                newLexState = LEXEMA_NUMBER_STARTED_E;
                                break check_new_lex_;
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
                                setNumber();
                                i--;
                                newLexState = LEXEMA_NUMBER_READY;
                                break check_new_lex_;
                            default:
                                error(ERROR_INCORRECT_NUMBER_ERROR_MESSAGE, i);
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
                                newLexState = LEXEMA_NUMBER_STARTED_E;
                                break check_new_lex_;
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
                                setNumber();
                                i--;
                                newLexState = LEXEMA_NUMBER_READY;
                                break check_new_lex_;
                            default:
                                error(ERROR_INCORRECT_NUMBER_ERROR_MESSAGE, i);
                                return this;
                        }
                    }
                    case LEXEMA_NUMBER_STARTED_E: {
                        switch (c) {
                            case '+':
                                newLexState = LEXEMA_NUMBER_STARTED_E_SIGN;
                                break check_new_lex_;
                            case '-':
                                numberMinuses = numberMinuses | 1;
                                newLexState = LEXEMA_NUMBER_STARTED_E_SIGN;
                                break check_new_lex_;
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
                                newLexState = LEXEMA_NUMBER_STARTED_E_VALUE;
                                break check_new_lex_;
                            default:
                                error(ERROR_INCORRECT_NUMBER_ERROR_MESSAGE, i);
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
                                newLexState = LEXEMA_NUMBER_STARTED_E_VALUE;
                                break check_new_lex_;
                            default:
                                error(ERROR_INCORRECT_NUMBER_ERROR_MESSAGE, i);
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
                                newLexState = LEXEMA_NUMBER_STARTED_E_VALUE;
                                break check_new_lex_;
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
                                setNumber();
                                i--;
                                newLexState = LEXEMA_NUMBER_READY;
                                break check_new_lex_;
                            default:
                                error(ERROR_INCORRECT_NUMBER_ERROR_MESSAGE, i);
                                return this;
                        }
                    }

                    /* unknown state */
                    default:
                        error(ERROR_INTERNAL_UNEXPECTED_LEXEMA_ERROR_MESSAGE, i);
                        return this;
                }

                assert newLexState > -1;

                currentLexState = newLexState;
                if (newLexState > LEXEMA_READY) {
                    if (!processLexema(newLexState, currentLexPos)) {
                        return this;
                    }
                }
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
     * @param lnr
     * @return
     */
    public JsonParser eoj() {
        final JsonParserListener lnr = listener;

        if (lnr != null) {
            if (error == null) {
                processLexema(LEXEMA_EOJ_READY, currentLexemaPosition);
            }
            lnr.onJsonEnded();
        }
        currentLexemaPosition = RESET_REQUIRED;
        return this;
    }

    public JsonParser reset() {
        doReset();
        currentLexemaPosition = -1;
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

    private boolean processLexema(final int lexema, final int lexemaPosition) {
        final JsonParserListener lnr = listener;

        assert lnr != null;

        final int currentScope = peekScope();

        switch (currentScope) {
            /* no scope */
            case -1:
                switch (lexema) {
                    case LEXEMA_EOJ_READY:
                        break;
                    default:
                        error(ERROR_MULTIPLE_VALUES_ERROR_MESSAGE, lexemaPosition);
                        return false;
                }
                break;

            /* first lexema */
            case EXPRESSION_INITIAL:
                switch (lexema) {
                    case LEXEMA_CURLY_BRACKET_LEFT_READY:
                        lnr.onObjectStarted();
                        replaceScope(EXPRESSION_OBJECT_STARTED);
                        break;
                    case LEXEMA_BOX_BRACKET_LEFT_READY:
                        lnr.onArrayStarted();
                        replaceScope(EXPRESSION_ARRAY_STARTED);
                        break;
                    case LEXEMA_TRUE_READY:
                        lnr.onTrueValue();
                        popScope();
                        break;
                    case LEXEMA_FALSE_READY:
                        lnr.onFalseValue();
                        popScope();
                        break;
                    case LEXEMA_NULL_READY:
                        lnr.onNullValue();
                        popScope();
                        break;
                    case LEXEMA_STRING_READY:
                        lnr.onStringValue(string);
                        popScope();
                        break;
                    case LEXEMA_NUMBER_READY:
                        lnr.onNumberValue(number);
                        popScope();
                        break;
                    case LEXEMA_EOJ_READY:
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
                                error(ERROR_INCORRECT_STRING_ERROR_MESSAGE, lexemaPosition);
                                return false;
                            case LEXEMA_NUMBER_STARTED_MANTISSA_SIGN:
                            case LEXEMA_NUMBER_STARTED_E:
                            case LEXEMA_NUMBER_STARTED_E_SIGN:
                                error(ERROR_INCORRECT_NUMBER_ERROR_MESSAGE, lexemaPosition);
                                return false;
                            case LEXEMA_NUMBER_STARTED_MANTISSA_INTEGER_PART:
                            case LEXEMA_NUMBER_STARTED_MANTISSA_FRACTIONAL_PART:
                            case LEXEMA_NUMBER_STARTED_E_VALUE:
                                setNumber();
                                lnr.onNumberValue(number);
                                popScope();
                                break;
                        }
                        break;
                    default:
                        error(ERROR_INTERNAL_UNEXPECTED_LEXEMA_ERROR_MESSAGE, lexemaPosition);
                        return false;
                }
                break;

            /* object */
            case EXPRESSION_OBJECT_STARTED:
            case EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER:
                switch (lexema) {
                    case LEXEMA_CURLY_BRACKET_RIGHT_READY:
                        lnr.onObjectEnded();
                        popScope();
                        break;
                    case LEXEMA_STRING_READY:
                        lnr.onObjectMember(string);
                        if (notifyObjectMemberNameString) {
                            lnr.onStringValue(string);
                        }
                        replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_NAME);
                        break;
                    default:
                        error(ERROR_INCORRECT_OBJECT_ERROR_MESSAGE, lexemaPosition);
                        return false;
                }
                break;
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME:
                switch (lexema) {
                    case LEXEMA_COLON_READY:
                        replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER);
                        break;
                    default:
                        error(ERROR_INCORRECT_OBJECT_ERROR_MESSAGE, lexemaPosition);
                        return false;
                }
                break;
            case EXPRESSION_OBJECT_STARTED_MEMBER_NAME_VALUE_COLON_DELIMITER:
                switch (lexema) {
                    case LEXEMA_CURLY_BRACKET_LEFT_READY:
                        lnr.onObjectStarted();
                        replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                        pushScope(EXPRESSION_OBJECT_STARTED);
                        break;
                    case LEXEMA_BOX_BRACKET_LEFT_READY:
                        lnr.onArrayStarted();
                        replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                        pushScope(EXPRESSION_ARRAY_STARTED);
                        break;
                    case LEXEMA_TRUE_READY:
                        lnr.onTrueValue();
                        replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                        break;
                    case LEXEMA_FALSE_READY:
                        lnr.onFalseValue();
                        replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                        break;
                    case LEXEMA_NULL_READY:
                        lnr.onNullValue();
                        replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                        break;
                    case LEXEMA_STRING_READY:
                        lnr.onStringValue(string);
                        replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                        break;
                    case LEXEMA_NUMBER_READY:
                        lnr.onNumberValue(number);
                        replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_VALUE);
                        break;
                    default:
                        error(ERROR_INCORRECT_OBJECT_ERROR_MESSAGE, lexemaPosition);
                        return false;
                }
                break;
            case EXPRESSION_OBJECT_STARTED_MEMBER_VALUE:
                switch (lexema) {
                    case LEXEMA_COMMA_READY:
                        replaceScope(EXPRESSION_OBJECT_STARTED_MEMBER_COMMA_DELIMITER);
                        break;
                    case LEXEMA_CURLY_BRACKET_RIGHT_READY:
                        lnr.onObjectEnded();
                        popScope();
                        break;
                    default:
                        error(ERROR_INCORRECT_OBJECT_ERROR_MESSAGE, lexemaPosition);
                        return false;
                }
                break;

            /* array */
            case EXPRESSION_ARRAY_STARTED:
            case EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER:
                switch (lexema) {
                    case LEXEMA_BOX_BRACKET_RIGHT_READY:
                        lnr.onArrayEnded();
                        popScope();
                        break;
                    case LEXEMA_CURLY_BRACKET_LEFT_READY:
                        lnr.onObjectStarted();
                        replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                        pushScope(EXPRESSION_OBJECT_STARTED);
                        break;
                    case LEXEMA_BOX_BRACKET_LEFT_READY:
                        lnr.onArrayStarted();
                        replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                        pushScope(EXPRESSION_ARRAY_STARTED);
                        break;
                    case LEXEMA_TRUE_READY:
                        lnr.onTrueValue();
                        replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                        break;
                    case LEXEMA_FALSE_READY:
                        lnr.onFalseValue();
                        replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                        break;
                    case LEXEMA_NULL_READY:
                        lnr.onNullValue();
                        replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                        break;
                    case LEXEMA_STRING_READY:
                        lnr.onStringValue(string);
                        replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                        break;
                    case LEXEMA_NUMBER_READY:
                        lnr.onNumberValue(number);
                        replaceScope(EXPRESSION_ARRAY_STARTED_VALUE);
                        break;
                    default:
                        error(ERROR_INCORRECT_ARRAY_ERROR_MESSAGE, lexemaPosition);
                        return false;
                }
                break;
            case EXPRESSION_ARRAY_STARTED_VALUE:
                switch (lexema) {
                    case LEXEMA_COMMA_READY:
                        replaceScope(EXPRESSION_ARRAY_STARTED_COMMA_DELIMITER);
                        break;
                    case LEXEMA_BOX_BRACKET_RIGHT_READY:
                        lnr.onArrayEnded();
                        popScope();
                        break;
                    default:
                        error(ERROR_INCORRECT_ARRAY_ERROR_MESSAGE, lexemaPosition);
                        return false;
                }
                break;

            /* unknown lexema */
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
        if (scopeStackDepth == scopeStack.length) {
            final int[] newScopeStack = new int[scopeStack.length << 1];
            System.arraycopy(scopeStack, 0, newScopeStack, 0, scopeStack.length);
            scopeStack = newScopeStack;
        }
        scopeStack[scopeStackDepth++] = expression;
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