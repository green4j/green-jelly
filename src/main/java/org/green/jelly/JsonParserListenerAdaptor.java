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
    public void onObjectStarted() {
    }

    @Override
    public void onObjectMember(final CharSequence name) {
    }

    @Override
    public void onObjectEnded() {
    }

    @Override
    public void onArrayStarted() {
    }

    @Override
    public void onArrayEnded() {
    }

    @Override
    public void onStringValue(final CharSequence data) {
    }

    @Override
    public void onNumberValue(final JsonNumber number) {
    }

    @Override
    public void onTrueValue() {
    }

    @Override
    public void onFalseValue() {
    }

    @Override
    public void onNullValue() {
    }

}
