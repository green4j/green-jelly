green-jelly
============
GC-free (green) JSON parser/writer library for Java which isn't an object mapper, but aims to be:

* **minimal**: MIT licensed with no dependencies (i.e. just drop the code into your project)
* **reactive styled**: the parser can parse a JSON document part by part (i.e. if you have received the part from a block device), even byte by byte; you don't need to iterate over all the tokens, just handle a callback you are interested in
* **fast**: high performance, more than comparable with other state-of-the-art parsers like Gson and Jackson (see below)
* **lightweight**: the code never recurses or allocates more memory than it needs, the Flightweight patern can be used to wrap a buffer to prevent memory copying

Performance
-----

A JMH test which sums all numbers in the document in the streaming style compared to Gson (v.2.8.5) and Jackson (v.2.9.7):
```
Benchmark                                                   Mode  Cnt      Score     Error  Units
JsonParserPerformanceComparison.greenJellyFlyweightSumTest  avgt   25  17363.530 ± 351.989  ns/op
JsonParserPerformanceComparison.gsonJsonReaderSumTest       avgt   25  23718.733 ±  54.265  ns/op
JsonParserPerformanceComparison.jacksonJsonParserSumTest    avgt   25  18777.392 ± 219.668  ns/op
```
<details>
<summary>Source code of the test</summary>
<p>

```java
import com.fasterxml.jackson.core.JsonFactory;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;
import org.green.jelly.FlyweightStringBuilder;
import org.green.jelly.JsonNumber;
import org.green.jelly.JsonParser;
import org.green.jelly.JsonParserListener;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class JsonParserPerformanceComparison {

    public static final String JSON = "[\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":true},\n"
        + "  {\"property1\":100,\"property2\":200,\"property3\":300,\"property4\":[400,500,600,700],\"property5\":false}\n"
        + "]";

    @State(Scope.Thread)
    public static class JellyFlyweightSum implements JsonParserListener {

        public JsonParser parser;
        public long value;

        @Setup(Level.Invocation)
        public void doSetup() {
            value = 0;
            parser = new JsonParser(new FlyweightStringBuilder());
            parser.setListener(this);
        }

        @TearDown(Level.Invocation)
        public void doTearDown() {
            parser.eoj();
        }

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
            value += number.mantissa();
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

    @State(Scope.Thread)
    public static class GsonSum {
        JsonReader reader;
        long value;

        @Setup(Level.Invocation)
        public void doSetup() {
            value = 0;
            reader = new JsonReader(new StringReader(JSON));
        }

        @TearDown(Level.Invocation)
        public void doTearDown() throws IOException {
            reader.close();
        }
    }

    @State(Scope.Thread)
    public static class JacksonSum {
        com.fasterxml.jackson.core.JsonParser parser;
        long value;

        @Setup(Level.Invocation)
        public void doSetup() throws Exception {
            value = 0;
            JsonFactory factory = new JsonFactory();
            parser = factory.createParser(JSON);
        }

        @TearDown(Level.Invocation)
        public void doTearDown() throws Exception {
            parser.close();
        }
    }

    @Benchmark
    public void greenJellyFlyweightSumTest(final JellyFlyweightSum sum) {
        sum.parser.parse(JSON);
    }
    
    @Benchmark
    public void gsonJsonReaderSumTest(final GsonSum sum) throws IOException {
        final JsonReader reader = sum.reader;
        _end:
        while (true) {
            final JsonToken token = reader.peek();
            switch (token) {
                case BEGIN_ARRAY:
                    reader.beginArray();
                    break;
                case END_ARRAY:
                    reader.endArray();
                    break;
                case BEGIN_OBJECT:
                    reader.beginObject();
                    break;
                case END_OBJECT:
                    reader.endObject();
                    break;
                case NAME:
                    reader.skipValue();
                    break;
                case STRING:
                    reader.skipValue();
                    break;
                case NUMBER:
                    sum.value += reader.nextLong();
                    break;
                case BOOLEAN:
                    reader.skipValue();
                    break;
                case NULL:
                    reader.skipValue();
                    break;
                case END_DOCUMENT:
                    break _end;
            }
        }
    }

    @Benchmark
    public void jacksonJsonParserSumTest(final JacksonSum sum) throws IOException {
        final com.fasterxml.jackson.core.JsonParser parser = sum.parser;
        com.fasterxml.jackson.core.JsonToken token;
        while ((token = parser.nextToken()) != null) {
            if (token == com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT) {
                sum.value += parser.getLongValue();
            }
        }
    }
}
```
</p>
</details>
