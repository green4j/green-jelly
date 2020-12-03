package org.green.jelly;

public class JsonEventPump implements JsonParserListener {
    protected final JsonGenerator output;
    protected final StringBuilder memberName = new StringBuilder();

    public JsonEventPump(final JsonBufferedWriter output) {
        this.output = new JsonGenerator(output);
    }

    public JsonEventPump(final JsonGenerator output) {
        this.output = output;
    }

    @Override
    public void onJsonStarted() {
    }

    @Override
    public void onError(final String error, final int position) {
    }

    @Override
    public void onJsonEnded() {
        output.eoj();
    }

    @Override
    public boolean onObjectStarted() {
        pushMemberNameIfRequired();
        output.startObject();
        return true;
    }

    @Override
    public boolean onObjectMember(final CharSequence name) {
        memberName.setLength(0);
        memberName.append(name);
        return true;
    }

    @Override
    public boolean onObjectEnded() {
        output.endObject();
        return true;
    }

    @Override
    public boolean onArrayStarted() {
        pushMemberNameIfRequired();
        output.startArray();
        return true;
    }

    @Override
    public boolean onArrayEnded() {
        output.endArray();
        return true;
    }

    @Override
    public boolean onStringValue(final CharSequence data) {
        pushMemberNameIfRequired();
        output.stringValue(data);
        return true;
    }

    @Override
    public boolean onNumberValue(final JsonNumber number) {
        pushMemberNameIfRequired();
        output.numberValue(number);
        return true;
    }

    @Override
    public boolean onTrueValue() {
        pushMemberNameIfRequired();
        output.trueValue();
        return true;
    }

    @Override
    public boolean onFalseValue() {
        pushMemberNameIfRequired();
        output.falseValue();
        return true;
    }

    @Override
    public boolean onNullValue() {
        pushMemberNameIfRequired();
        output.nullValue();
        return true;
    }

    private void pushMemberNameIfRequired() {
        if (memberName.length() == 0) {
            return;
        }
        output.objectMember(memberName);
        memberName.setLength(0);
    }
}
