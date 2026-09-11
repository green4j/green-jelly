# Green Jelly

[![Build](https://github.com/green4j/green-jelly/actions/workflows/build.yml/badge.svg)](https://github.com/green4j/green-jelly/actions/workflows/build.yml)

GC-free (green) JSON parser/generator library for Java which isn't an object mapper, but aims to be:

* **minimal**: MIT licensed with no dependencies
* **reactive styled**: the parser can parse a JSON document part by part (e.g. if you have received the part from a block device), even byte by byte; you don't need to iterate over all the tokens with a verbose `if` or `switch`, just handle the callback you are interested in
* **lightweight**: the code never recurses or allocates more memory than it needs; the Flyweight pattern can be used to wrap a receive/send buffer to prevent memory copying
* **fast**: high performance, more than comparable with other state-of-the-art parsers like Gson and Jackson (see [Performance](#performance)). No additional GC pauses are introduced, since the code doesn't allocate new memory in its main/critical path
* **IO buffering oriented**: the parser can be fed with several parts of one single JSON document, even byte by byte, so it's easy to use it with buffered reads
* **robust**: built according to [Ecma-404](https://www.ecma-international.org/publications/standards/Ecma-404.htm) with some extensions for the number values (see [Numbers](#numbers))

## Binaries

Binaries for Maven, Ivy, Gradle, and others can be found at
[https://central.sonatype.com/search?q=green-jelly](https://central.sonatype.com/search?q=green-jelly).

Example for Maven:

```xml
<dependency>
    <groupId>io.github.green4j</groupId>
    <artifactId>green-jelly</artifactId>
    <version>${greenJelly.version}</version>
</dependency>
```

## How to build

Just run the standard Gradle build and install process:

```
    ./gradlew
```

## Usage

### JsonParser

The generic pattern of the usage is the following:
* create an instance of the `JsonParser`
* set a listener (an instance of the `JsonParserListener` interface) to get all parsing events
* call the `parse` method as many times as many parts of a JSON document you have
* finish the parsing with a call of the `eoj` (End Of JSON) method (or use the `parseAndEoj` method)
* parse the next JSON document with the same instance of the parser

```java
final JsonParser parser = new JsonParser();
parser.setListener(new JsonParserListener() {
...
});

parser.parse("\"st");
parser.parse("ring\"");
parser.eoj();

parser.parseAndEoj("[1,2,3]");
```
#### JsonStringBuilder

While creating an instance of the `JsonParser`, an instance of the `JsonStringBuilder` can be passed to `JsonParser`'s constructor. The responsibility of the `JsonStringBuilder` is to store the characters of a string value. There are two implementations of the interface included in the library:

* **CopyingStringBuilder**: copies the characters of string values into an internal buffer. Should be used when the whole JSON document cannot be parsed at once (e.g. you use a reusable/mutable buffer to receive the data via the `receive` system call). Also, this builder supports *unescaping* on-the-fly.
* **FlyweightStringBuilder**: stores the reference to the `CharSequence` passed to the `parse` method and knows the length of the string value. This builder saves you from memory copying, but it requires that any string value fits into a single instance of `CharSequence`. Also, *unescaping* isn't supported, since the length of the resulting string value, passed to the `onStringValue(CharSequence data)` callback, must be the same as the length of the original string.

#### JsonParserListener

While parsing you can be notified about the following events with an instance of the `JsonParserListener`:

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

boolean onNumberValue(JsonNumber number, boolean overflow); // optional, has a default implementation

boolean onTrueValue();

boolean onFalseValue();

boolean onNullValue();
```
If any of the methods returns `false`, the parsing stops and can be continued later (see [Parsing with steps](#parsing-with-steps)).
The library provides a default implementation of the listener, `JsonParserListenerAdapter`. All of its callbacks are empty and just return `true`.

#### Numbers

The supported format of number values is a bit more relaxed than the one specified in [Ecma-404](https://www.ecma-international.org/publications/standards/Ecma-404.htm):
* the numbers can start with either `+` or `-`
* leading zeros are allowed for both the mantissa and the exponent
* the mantissa is represented by Java's signed `long`, which is a bit wider than JavaScript numbers [-(2^53)+1, (2^53)-1]

To prevent memory allocation and unnecessary computations, the library doesn't implement any fixed or floating point arithmetic. Numbers are represented by the following interface:
```java
public interface JsonNumber {

    long mantissa();

    int exp();

}
```
Feel free to use any kind of arithmetic: [decimal4j](https://github.com/tools4j/decimal4j), which supports GC-free calculations, the out-of-the-box `java.math.BigDecimal`, which is a bit slow and allocates new memory, etc. An example of using `java.math.BigDecimal`:

```java
JsonNumber number = ...
BigDecimal decimal = BigDecimal.valueOf(number.mantissa(), -number.exp());
```
Sometimes a vendor sends numbers as string values. To parse such a value, use the static `JsonParser.parseNumber` method:
```java
MutableJsonNumber number = new MutableJsonNumber();
JsonParser.parseNumber("134.4455", number);
System.out.println(number);
```

##### Overflow

Some vendors, cryptocurrency exchanges in particular, publish numbers whose mantissa doesn't fit into a `long`. Such a number is *truncated*: the extra digits of the integer part are dropped and the exponent is adjusted accordingly, while the extra digits of the fractional part are dropped as is. There is no rounding. The largest mantissa the parser can hold is `JsonParser.MAX_MANTISSA_VALUE`.

The truncation is reported with the `overflow` argument of the `onNumberValue(JsonNumber number, boolean overflow)` callback. To let you recover the lost digits, the parser also keeps the **full decimal representation** of such a number and makes it available with the `overflownNumber()` method. The text is a well-formed decimal literal, so it can be passed to `java.math.BigDecimal` as is:
```java
final JsonParser parser = new JsonParser();
parser.setListener(new JsonParserListenerAdapter() {
    @Override
    public boolean onNumberValue(final JsonNumber number, final boolean overflow) {
        if (overflow) {
            final BigDecimal exact = new BigDecimal(parser.overflownNumber().toString());
            ...
        }
        return true;
    }
});
```
The buffer is touched only when an overflow actually happens, so the fallback doesn't add any cost to the normal path of the parsing. The content of the buffer is valid only while a callback notified with `overflow == true` is being handled.

The static `parseNumber` supports the same. Either pass your own buffer:
```java
MutableJsonNumber number = new MutableJsonNumber();
StringBuilder overflown = new StringBuilder();
if (JsonParser.parseNumber("9999999999999999999999.55e-7", number, overflown)) {
    BigDecimal exact = new BigDecimal(overflown.toString());
}
```
or use the thread local one:
```java
if (JsonParser.parseNumber("9999999999999999999999.55e-7", number)) {
    BigDecimal exact = new BigDecimal(JsonParser.lastOverflownNumber().toString());
}
```
Note that the text is normalized: leading zeros and a leading `+` aren't reproduced and the exponent is always written with a lowercase `e`. The value is exactly the one of the original number.

#### Error handling

If the parser detects an error while parsing, you receive a notification with the `onError(String error, int position)` callback. Also, after control is given back from the `parse` method, you can check the error with the following methods:
```
boolean hasError()

String getError()

int getErrorPosition()
```
#### Parsing with steps
Sometimes you may need to split the process of parsing into several steps. In this case, return `false` from any of `JsonEventListener`'s callbacks. The parsing stops and an instance of the `Next` is returned back. Call `Next.next()` until it returns `null`. For example, the following code prints each number on a new line:

```java
final var valueHolder = new Object() {
    long value;
}; // mutable value with the final reference

final JsonParser parser = new JsonParser().setListener(
   new JsonParserListenerAdapter() {
       @Override
       public boolean onNumberValue(final JsonNumber number) {
            valueHolder.value = number.mantissa();
            return false; // stop the parsing after each number
        }
    }
);

JsonParser.Next nextStep = parser.parse("[0,10,20,30,40,5");
while (nextStep != null) {
    System.out.println(valueHolder.value);
    nextStep = nextStep.next();
}

nextStep = parser.parse("0,60,70,80,90,100]");
while (nextStep != null) {
    System.out.println(valueHolder.value);
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
generator.stringValue("\test1", true); // the value requires escaping
generator.stringValue("test2");
generator.stringValue("test3");
generator.endArray();
generator.eoj();

System.out.println(writer.output().toString());
```

### Encodings

The library works over character based abstractions, so it doesn't implement any encoding functionality. As in the case of Gson, for instance, the user has to take care of the correct bytes-to/from-chars transformation if one is required.

At the same time, there are a few specific classes to support JSON generation in different encodings if required:

| Json Writer                                    | Buffer Type/Encoding                                                                |
|------------------------------------------------|-----------------------------------------------------------------------------------|
| `io.github.green4j.jelly.AsciiByteArrayWriter` | Byte array of ASCII encoded data. All characters outside the 0-127 range are escaped |
| `io.github.green4j.jelly.Utf8ByteArrayWriter`  | Byte array of UTF-8 encoded data                                                    |
| `io.github.green4j.jelly.CharArrayWriter`      | Char array. Can be used as is or converted later to any other encoding              |

Each of the classes has its own internal mutable buffer which can be passed to IO routines after the JSON is generated.

## Performance

A JMH test, which sums all numbers in the document in streaming style, compared to Gson (v.2.8.5) and Jackson (v.2.9.7):
```
Benchmark                                                   Mode  Cnt      Score     Error  Units
JsonParserPerformanceComparison.greenJellyFlyweightSumTest  avgt   25  17747.965 +- 257.699  ns/op
JsonParserPerformanceComparison.gsonJsonReaderSumTest       avgt   25  23442.288 +- 109.931  ns/op
JsonParserPerformanceComparison.jacksonJsonParserSumTest    avgt   25  18336.310 +- 220.573  ns/op
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
import io.github.green4j.jelly.FlyweightStringBuilder;
import io.github.green4j.jelly.JsonNumber;
import io.github.green4j.jelly.JsonParser;
import io.github.green4j.jelly.JsonParserListenerAdapter;
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
    public static class JellyFlyweightSum extends JsonParserListenerAdapter {

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
            final JsonFactory factory = new JsonFactory();
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

## JSON Value

Sometimes we don't worry about memory consumption and CPU utilization, but we need a simple way to work
with JSON documents. And we would prefer not to introduce complex POJO structures and object mapping.
A good example of an approach like that is the `JSON.simple` library. The `Green-Jelly` also provides a similar
package, `io.github.green4j.jelly.simple`.

The `io.github.green4j.jelly.simple.JsonValue` class is the main class to construct a representation of a JSON document:
```
final JsonValue json = JsonValue.newObject();
final JsonObject user = json.asObject();
user.putString("name", "Mike");
user.putInteger("age", 23);
final JsonArray rates = user.putArray("rates");
rates.addInteger(10);
rates.addInteger(8);
```

A `JsonValue` can be serialized with an `io.github.green4j.jelly.simple.JsonWriter` or with a `java.io.Writer`:
```
final JsonValue json = ...

final StringBuilder output = new StringBuilder();
final JsonWriter jsonWriter = new JsonWriterGenerator(new JsonGenerator(output));
json.toJsonAndEoj(jsonWriter);
System.out.println(output);

final StringWriter stringWriter = new StringWriter();
json.toJsonAndEoj(stringWriter);
System.out.println(stringWriter);
```

To parse JSON and build a `JsonValue`, use the `JsonValueParser` class:
```
final JsonValueParser parser = new JsonValueParser();
final JsonValue json = parser.parseAndEoj("{ \"name\": \"Mike\", \"age\": 23 }");
System.out.println(json);
```

## License
The code is available under the terms of the [MIT License](http://opensource.org/licenses/MIT).
