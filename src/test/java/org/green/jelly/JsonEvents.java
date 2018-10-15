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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JsonEvents implements JsonParserListener {

    private final List<Event> stack = new ArrayList<>();

    public JsonEvents() {
    }

    public JsonEvents(final Event... events) {
        Collections.addAll(stack, events);
        Collections.reverse(stack);
    }

    @Override
    public boolean onObjectStarted() {
        stack.add(new ObjectStart());
        return true;
    }

    @Override
    public boolean onObjectMember(final CharSequence name) {
        stack.add(new ObjectMember(name));
        return true;
    }

    @Override
    public boolean onObjectEnded() {
        stack.add(new ObjectEnd());
        return true;
    }

    @Override
    public boolean onArrayStarted() {
        stack.add(new ArrayStart());
        return true;
    }

    @Override
    public boolean onArrayEnded() {
        stack.add(new ArrayEnd());
        return true;
    }

    @Override
    public boolean onStringValue(final CharSequence data) {
        stack.add(new StringValue(data));
        return true;
    }

    @Override
    public boolean onNumberValue(final JsonNumber number) {
        stack.add(new NumberValue(number));
        return true;
    }

    public void onNumberValue(final long mantissa, final int exp) {
        stack.add(new NumberValue(mantissa, exp));
    }

    @Override
    public boolean onTrueValue() {
        stack.add(new TrueValue());
        return true;
    }

    @Override
    public boolean onFalseValue() {
        stack.add(new FalseValue());
        return true;
    }

    @Override
    public boolean onNullValue() {
        stack.add(new NullValue());
        return true;
    }

    @Override
    public void onJsonStarted() {
        stack.add(new JsonStart());
    }

    @Override
    public void onError(final String error, final int position) {
        stack.add(new Error(error, position));
    }

    @Override
    public void onJsonEnded() {
        stack.add(new JsonEnd());
    }

    public Event pop() {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.remove(stack.size() - 1);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return stack.size();
    }

    public void clear() {
        stack.clear();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.stack);
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JsonEvents other = (JsonEvents) obj;
        return Objects.equals(this.stack, other.stack);
    }

    @Override
    public String toString() {
        return "[" + stack + ']';
    }

    public abstract class Event {

        protected Event() {
        }

        @SuppressWarnings("unchecked")
        public <T> T as(final Class<T> clazz) {
            if (!isOf(clazz)) {
                return null;
            }
            return (T) this;
        }

        public boolean isOf(final Class clazz) {
            return getClass() == clazz;
        }


        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return isOf(obj.getClass());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    public abstract class StringEvent extends Event {

        private final String string;

        protected StringEvent(final String string) {
            this.string = string;
        }

        public String string() {
            return string;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + Objects.hashCode(this.string);
            return hash;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StringEvent other = (StringEvent) obj;
            return Objects.equals(this.string, other.string);
        }

        @Override
        public String toString() {
            return super.toString() + ": '" + string + '\'';
        }
    }

    public abstract class NumberEvent extends Event {

        private final BigDecimal number;

        protected NumberEvent(final long mantissa, final int exp) {
            this.number = BigDecimal.valueOf(mantissa, -exp);
        }

        public BigDecimal number() {
            return number;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.number);
            return hash;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NumberEvent other = (NumberEvent) obj;
            return Objects.equals(this.number, other.number);
        }

        @Override
        public String toString() {
            return super.toString() + ": " + number;
        }
    }

    public class ObjectStart extends Event {
    }

    public class ObjectMember extends StringEvent {

        public ObjectMember(final CharSequence name) {
            super(name.toString());
        }
    }

    public class ObjectEnd extends Event {
    }

    public class ArrayStart extends Event {
    }

    public class ArrayEnd extends Event {
    }

    public class StringValue extends StringEvent {

        public StringValue(final CharSequence value) {
            super(value.toString());
        }
    }

    public class NumberValue extends NumberEvent {

        public NumberValue(final JsonNumber value) {
            super(value.mantissa(), value.exp());
        }

        public NumberValue(final long mantissa, final int exp) {
            super(mantissa, exp);
        }
    }

    public class TrueValue extends Event {
    }

    public class FalseValue extends Event {
    }

    public class NullValue extends Event {
    }

    public class JsonStart extends Event {
    }

    public class JsonEnd extends Event {
    }

    public class Error extends StringEvent {

        private final int position;

        public Error(final String error, final int position) {
            super(error);
            this.position = position;
        }

        public int position() {
            return position;
        }
    }
}
