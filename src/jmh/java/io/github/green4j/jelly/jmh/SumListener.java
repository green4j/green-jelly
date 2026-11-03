/**
 * MIT License
 * <p>
 * Copyright (c) 2018-2026 Anatoly Gudkov and others.
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
package io.github.green4j.jelly.jmh;

import io.github.green4j.jelly.JsonNumber;
import io.github.green4j.jelly.JsonParserListener;

/**
 * Consumes the events the same way a real streaming handler does: it looks at every value, so neither
 * the numbers nor the strings can be optimized away, but it doesn't add any noticeable cost of its own.
 */
final class SumListener implements JsonParserListener {
    long sum;

    @Override
    public void onJsonStarted() {
        sum = 0;
    }

    @Override
    public void onError(final String error, final int position) {
        throw new IllegalStateException(error + " at " + position);
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
        sum += name.length();
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
        final int length = data.length();
        sum += length;
        if (length > 0) {
            sum += data.charAt(length - 1);
        }
        return true;
    }

    @Override
    public boolean onNumberValue(final JsonNumber number) {
        sum += number.mantissa() + number.exp();
        return true;
    }

    @Override
    public boolean onTrueValue() {
        sum++;
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
