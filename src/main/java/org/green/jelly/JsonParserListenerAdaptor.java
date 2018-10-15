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

public class JsonParserListenerAdaptor implements JsonParserListener {

    @Override
    public void onJsonStarted() {
    }

    @Override
    public void onError(final String error, final int position) {
    }

    @Override
    public void onJsonEnded() {
    }

    @Override
    public boolean onObjectStarted() {
        return true;
    }

    @Override
    public boolean onObjectMember(final CharSequence name) {
        return true;
    }

    @Override
    public boolean onObjectEnded() {
        return true;
    }

    @Override
    public boolean onArrayStarted() {
        return true;
    }

    @Override
    public boolean onArrayEnded() {
        return true;
    }

    @Override
    public boolean onStringValue(final CharSequence data) {
        return true;
    }

    @Override
    public boolean onNumberValue(final JsonNumber number) {
        return true;
    }

    @Override
    public boolean onTrueValue() {
        return true;
    }

    @Override
    public boolean onFalseValue() {
        return true;
    }

    @Override
    public boolean onNullValue() {
        return true;
    }

}
