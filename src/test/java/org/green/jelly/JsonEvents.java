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
    public void onObjectStarted() {
        stack.add(new ObjectStart());
    }

    @Override
    public void onObjectMember(final CharSequence name) {
        stack.add(new ObjectMember(name));
    }

    @Override
    public void onObjectEnded() {
        stack.add(new ObjectEnd());
    }

    @Override
    public void onArrayStarted() {
        stack.add(new ArrayStart());
    }

    @Override
    public void onArrayEnded() {
        stack.add(new ArrayEnd());
    }

    @Override
    public void onStringValue(final CharSequence data) {
        stack.add(new StringValue(data));
    }

    @Override
    public void onNumberValue(final JsonNumber number) {
        stack.add(new NumberValue(number));
    }

    public void onNumberValue(final long mantissa, final int exp) {
        stack.add(new NumberValue(mantissa, exp));
    }

    @Override
    public void onTrueValue() {
        stack.add(new TrueValue());
    }

    @Override
    public void onFalseValue() {
        stack.add(new FalseValue());
    }

    @Override
    public void onNullValue() {
        stack.add(new NullValue());
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
            if (!is(clazz)) {
                return null;
            }
            return (T) this;
        }

        public boolean is(final Class clazz) {
            return getClass() == clazz;
        }


        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return is(obj.getClass());
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
