package org.green.jelly;

public interface JsonParserListener extends JsonEventListener {

    void onJsonStarted();

    void onError(String error, int position);

    void onJsonEnded();
}
