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

public class MutableJsonNumber implements JsonNumber {
    private long mantissa;
    private int exp;

    public MutableJsonNumber() {
    }

    public MutableJsonNumber(final long mantissa, final int exp) {
        set(mantissa, exp);
    }

    public void set(final long mantissa, final int exp) {
        setMantissa(mantissa);
        setExp(exp);
    }

    public void setMantissa(final long mantissa) {
        this.mantissa = mantissa;
    }

    public void setExp(final int exp) {
        this.exp = exp;
    }

    @Override
    public long mantissa() {
        return mantissa;
    }

    @Override
    public int exp() {
        return exp;
    }

    public String toString() {
        return mantissa() + "E" + exp();
    }

}
