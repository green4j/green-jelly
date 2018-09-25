package org.green.jelly;

public interface JsonBufferedWriter {

    interface Frame extends CharSequence {
        void setCharAt(int index, char c);
    }

    Frame append(int size);

    void append(char c);

    void append(CharSequence data);

    void append(CharSequence data, int start, int len);

    void flush();

}
