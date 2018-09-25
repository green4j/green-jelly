package org.green.jelly;

public interface JsonEventListener {

    void onObjectStarted();

    void onObjectMember(CharSequence name);

    void onObjectEnded();

    void onArrayStarted();

    void onArrayEnded();

    void onStringValue(CharSequence data);

    void onNumberValue(JsonNumber number);

    void onTrueValue();

    void onFalseValue();

    void onNullValue();

}
