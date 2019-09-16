# Green Jelly

GC-free (green) JSON parser/generator library for Java which isn't an object mapper, but aims to be:

* **minimal**: MIT licensed with no dependencies (i.e. just drop the code into your project)
* **reactive styled**: the parser can parse a JSON document part by part (i.e. if you have received the part from a block device), even byte by byte; you don't need to iterate over all the tokens with verbose `if` or `switch`, just handle a callback you are interested in
* **lightweight**: the code never recurses or allocates more memory than it needs, the Flyweight patern can be used to wrap a receive/send buffer to prevent memory copying
* **fast**: high performance, more than comparable with other state-of-the-art parsers like Gson and Jackson (see [Performance](#performance))
* **robust**: built according to [Ecma-404](https://www.ecma-international.org/publications/standards/Ecma-404.htm) schema with some extensions for the number values (see [Numbers](#numbers))

## How to build

Just run `gradlew` to build the library. But prefer to just drop the code into your own project and modify according to your own needs; don't forget about unit tests.

## Usage

### JsonParser

Generic pattern of the usage if the following:
* create instance of the JsonParser
* set a listener (an instance of the `JsonParserListener` interface) to get all parsing events
* call the `parse` method as much as parts of a JSON document you have
* finish the parsing with a call of the `eoj`(End Of JSON) method
* parse next JSON document with the same instance of the parser

```java
final JsonParser parser = new JsonParser();
parser.setListener(new JsonParserListener() {
...
});

parser.parse("[1,2,3]");
parser.eoj();

parser.parse("\"st");
parser.parse("ring\"");
parser.eoj();
```
#### JsonStringBuilder

While creating an instance of the `JsonParser`, an instance of the `JsonStringBuilder` can be passed to `JsonParser`'s constructor. The responsibility of the `JsonStringBuilder` is to store characters of a string value. There are two implementations of the interface included to the library:

* **CopyingStringBuilder**: copies the character of string values to internal buffer. Should be used when not the whole JSON document can be parsed at once (i.e. you use reusable/mutable buffer to receive the data via `receive` system call). Also, this builder supports *unescaping* on-the-fly.
* **FlyweightStringBuilder**: stores the reference to a CharSequence passed to the `parse` method and knows the length of the string value. This builder prevents you from memory copying, but it requires any string value must be fitted into one instance of CharSequence. Also, *unescaping* isn't supported, since the length of the result string value, passed to the `onStringValue(CharSequence data)` callback, must be the same as the length of the original string.

#### JsonParserListener

While parsing you can be notified about the following events with an instance of the JsonParserListener:

```
void onJsonStarted();

void onError(String error, int position);

void onJsonEnded();

boolean onObjectStarted();

boolean onObjectMember(CharSequence name);

boolean onObjectEnded();

boolean onArrayStarted();

boolean onArrayEnded();

boolean onStringValue(CharSequence data);

boolean onNumberValue(JsonNumber number);

boolean onTrueValue();

boolean onFalseValue();

boolean onNullValue();
```
If any of method returns `false`, the parsing is stopped and can be continued then (see [Parsing with steps](#parsing-with-steps)).
A default implementatation of the listener is included to the library as `JsonParserListenerAdaptor`. All callbacks of the adaptor are empty and just return `true`.

#### Numbers

Supported format of number values is a bit more relaxed than specified in [Ecma-404](https://www.ecma-international.org/publications/standards/Ecma-404.htm):
* the numbers can start with both `+` or `-`
* the leading zeros are allowed for both mantissa and exponent

To prevent memory allocation and unnecessary computations, the library doesn't implement any fixed or floating point arithmetic. Numbers are presented with the following interface:
```java
public interface JsonNumber {

    long mantissa();

    int exp();
}
```
Feel free to use any type of arithmetic like [decimal4j](https://github.com/tools4j/decimal4j), which supports GC-free calculations, out-of-the-box `java.math.BigDecimal`, which a bit slow and allocates new memory, etc. An example of `java.math.BigDecimal` using:

```java
JsonNumber number = ...
BigDecimal decimal = BigDecimal.valueOf(number.mantissa(), -number.exp());
```

#### Error handling

If the parser detects an error while parsing, you receive a notification with the `onError(String error, int position)` callback. Also, after the control is given back from the `parse` method, you can check the error with the following methods:
```
boolean hasError()

String getError()
    
int getErrorPosition()
```
#### Parsing with steps
Sometimes you may need to split the process of parsing into some steps. In this case you return `false` from any of `JsonEventListener`'s callback. The parsing stops with an instance of the `Next` returned back. You call `Next.next()` until it returns `null`. For example, the following code prints each number on new line:

```java
final AtomicLong value = new AtomicLong(0); // a mutable value

final JsonParser parser = new JsonParser().setListener(
   new JsonParserListenerAdaptor() {
       @Override
       public boolean onNumberValue(final JsonNumber number) {
            value.set(number.mantissa());
            return false; // stop the parsing after each number
        }
    }
);

JsonParser.Next nextStep = parser.parse("[0,10,20,30,40,5");
while (nextStep != null) {
    System.out.println(value.get());
    nextStep = nextStep.next();
}

nextStep = parser.parse("0,60,70,80,90,100]");
while (nextStep != null) {
    System.out.println(value.get());
    nextStep = nextStep.next();
}
parser.eoj();
```

### JsonGenerator

An example:
```java
final AppendableWriter<StringBuilder> writer = new AppendableWriter<>(new StringBuilder());
final JsonGenerator generator = new JsonGenerator();
generator.setOutput(writer);
       
generator.startArray();
generator.stringValue("\test1", true); // the value requires to be escaped
generator.stringValue("test2");
generator.stringValue("test3");
generator.endArray();
generator.eoj();

System.out.println(writer.output().toString());
```

### Encodings

The library works over character based abstractions, so, it doesn't implement any encoding functionality. As in case of Gson, for instance, the user has to care about correct bytes-to/from-chars transformation if any required.

## Performance

A JMH test, which sums all numbers in the document in streaming style, compared to Gson (v.2.8.5) and Jackson (v.2.9.7):
```
Benchmark                                                   Mode  Cnt      Score     Error  Units
JsonParserPerformanceComparison.greenJellyFlyweightSumTest  avgt   25  17747.965 ± 257.699  ns/op
JsonParserPerformanceComparison.gsonJsonReaderSumTest       avgt   25  23442.288 ± 109.931  ns/op
JsonParserPerformanceComparison.jacksonJsonParserSumTest    avgt   25  18336.310 ± 220.573  ns/op
```

<details><summary markdown="span"><code>Source code of the test</code></summary>
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
import org.green.jelly.JsonParserListenerAdaptor;
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
    public static class JellyFlyweightSum extends JsonParserListenerAdaptor {

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
        public boolean onNumberValue(final JsonNumber number) {
            value += number.mantissa();
            return true;
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

## License
The code is available under the terms of the [MIT License](http://opensource.org/licenses/MIT).
