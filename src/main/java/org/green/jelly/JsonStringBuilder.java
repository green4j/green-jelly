package org.green.jelly;

public interface JsonStringBuilder extends CharSequence {
    /**
     * @param data
     * @param position - position of the opening quote
     */
    void start(CharSequence data, int position);

    void append(CharSequence data, int position, char c);

    void appendEscape(CharSequence data, int position);

    void appendEscapedQuotationMark(CharSequence data, int position);

    void appendEscapedReverseSolidus(CharSequence data, int position);

    void appendEscapedSolidus(CharSequence data, int position);

    void appendEscapedBackspace(CharSequence data, int position);

    void appendEscapedFormfeed(CharSequence data, int position);

    void appendEscapedNewLine(CharSequence data, int position);

    void appendEscapedCarriageReturn(CharSequence data, int position);

    void appendEscapedHorisontalTab(CharSequence data, int position);

    void appendEscapedUnicodeU(CharSequence data, int position);

    boolean appendEscapedUnicodeChar1(CharSequence data, int position, char c);

    boolean appendEscapedUnicodeChar2(CharSequence data, int position, char c);

    boolean appendEscapedUnicodeChar3(CharSequence data, int position, char c);

    boolean appendEscapedUnicodeChar4(CharSequence data, int position, char c);
}
