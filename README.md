# Green Jelly

[![Build](https://github.com/green4j/green-jelly/actions/workflows/build.yml/badge.svg)](https://github.com/green4j/green-jelly/actions/workflows/build.yml)

GC-free (green) JSON parser/generator library for Java which isn't an object mapper, but aims to be:

* **one pass for UTF-8 and JSON**: JSON received as UTF-8 bytes is decoded and parsed in a single pass, right from a `byte[]` or a heap or direct `ByteBuffer`, with no copying of the data and no intermediate buffer of chars; only the bytes of string values are decoded (see [Utf8JsonParser](#utf8jsonparser))
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

```shell
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

```java
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
final JsonNumber number = ...
final BigDecimal decimal = BigDecimal.valueOf(number.mantissa(), -number.exp());
```
Sometimes a vendor sends numbers as string values. To parse such a value, use the static `JsonParser.parseNumber` method:
```java
final MutableJsonNumber number = new MutableJsonNumber();
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
final MutableJsonNumber number = new MutableJsonNumber();
final StringBuilder overflown = new StringBuilder();
if (JsonParser.parseNumber("9999999999999999999999.55e-7", number, overflown)) {
    final BigDecimal exact = new BigDecimal(overflown.toString());
}
```
or use the thread local one:
```java
if (JsonParser.parseNumber("9999999999999999999999.55e-7", number)) {
    final BigDecimal exact = new BigDecimal(JsonParser.lastOverflownNumber().toString());
}
```
Note that the text is normalized: leading zeros and a leading `+` aren't reproduced and the exponent is always written with a lowercase `e`. The value is exactly the one of the original number.

#### Error handling

If the parser detects an error while parsing, you receive a notification with the `onError(String error, int position)` callback. Also, after control is given back from the `parse` method, you can check the error with the following methods:
```java
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

### Utf8JsonParser

`Utf8JsonParser` parses JSON encoded in UTF-8 the same way `JsonParser` parses chars, but it reads the bytes as they are,
so decoding to chars and parsing are done in one pass. All the chars of the JSON syntax are ASCII, and a byte of a char
encoded with several bytes is never ASCII, so the structure, the numbers and the literals are parsed right from the bytes,
and only the bytes of string values are decoded, while they are copied into the string builder.

The parser takes the same `JsonParserListener`, so a handler written for `JsonParser` works with it as is. The data can be:
* a `byte[]`
* a `ByteBuffer`, either heap or direct one. The bytes are read at absolute indexes, so neither the position
  nor the limit of the buffer is changed. A direct buffer is read in place, there is no copying of its memory to the heap
* any other storage of bytes wrapped with the `ByteSequence` interface

```java
final Utf8JsonParser parser = new Utf8JsonParser();
parser.setListener(new JsonParserListener() {
...
});

parser.parseAndEoj("[\"gelée\",1]".getBytes(StandardCharsets.UTF_8));

final ByteBuffer received = ...; // e.g. a direct buffer the data is received into
parser.parse(received); // the bytes between the position and the limit
parser.eoj();
```

Like `JsonParser`, the parser can be fed with a document part by part. A part may end in the middle of a char encoded
with several bytes: the bytes read are kept by the string builder, and the char is decoded when the rest of it comes
with the next part. So reusable receive buffers can be passed to the parser as they are.

The positions passed to `onError` and returned by `getErrorPosition()` are indexes of bytes relative to the start
of the data given to the `parse` method.

#### JsonUtf8StringBuilder

The string values are collected by an instance of the `JsonUtf8StringBuilder`, which decodes their bytes to chars.
`CopyingStringBuilder` implements it, as well as `JsonStringBuilder`, so it's used by default and supports unescaping
in the same way. Malformed UTF-8 bytes in a string value are handled according to the builder:
* by default, each malformed sequence of bytes is replaced with U+FFFD, the same way
  `new String(bytes, StandardCharsets.UTF_8)` does, and the parsing goes on
* `new CopyingStringBuilder(false, true)` makes them an error of the parsing reported with `onError`

```java
final Utf8JsonParser strictParser = new Utf8JsonParser(new CopyingStringBuilder(false, true));
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

`JsonParser` and `JsonGenerator` work over character based abstractions. JSON encoded in UTF-8 can be parsed
with no decoding of the data beforehand with `Utf8JsonParser` (see [Utf8JsonParser](#utf8jsonparser)).
For other encodings the user has to take care of the correct bytes-to/from-chars transformation.

There are a few specific classes to support JSON generation in different encodings if required:

| Json Writer                                    | Buffer Type/Encoding                                                                |
|------------------------------------------------|-----------------------------------------------------------------------------------|
| `io.github.green4j.jelly.AsciiByteArrayWriter` | Byte array of ASCII encoded data. All characters outside the 0-127 range are escaped |
| `io.github.green4j.jelly.Utf8ByteArrayWriter`  | Byte array of UTF-8 encoded data                                                    |
| `io.github.green4j.jelly.CharArrayWriter`      | Char array. Can be used as is or converted later to any other encoding              |

Each of the classes has its own internal mutable buffer which can be passed to IO routines after the JSON is generated.

## Performance

The JMH benchmarks live in the `src/jmh` source set, which is built by `./gradlew build`, but doesn't get into the released artifacts.
Run them all, or choose some of them and pass any other JMH options, with:
```shell
./gradlew jmh
./gradlew jmh -PjmhArgs="JsonParserBenchmark -p document=DECIMALS -prof gc"
```

| Benchmark                          | What is measured                                                                                  |
|------------------------------------|---------------------------------------------------------------------------------------------------|
| `JsonParserBenchmark`              | Parsing of a whole document, given as a `String`, of every kind and with both string builders     |
| `JsonParserInputBenchmark`         | Parsing in a process which gives the parser several `CharSequence` implementations                |
| `JsonParserChunkedBenchmark`       | Parsing of a document coming part by part, with lexemas cut by the boundaries of the parts        |
| `Utf8JsonParserBenchmark`          | Parsing of a document received as UTF-8 bytes: `Utf8JsonParser` reading a `byte[]` or a direct `ByteBuffer`, compared to decoding the bytes to chars first and parsing them with `JsonParser` |
| `JsonGeneratorBenchmark`           | Generating of a document of every kind with every writer, with and without indenting              |
| `JsonParserComparisonBenchmark`    | The parser compared to Jackson and Gson: every member name, string and number of a document is read |
| `JsonGeneratorComparisonBenchmark` | The generator compared to Jackson and Gson: the same kinds of documents, the strings are escaped   |

The results below are measured on: Apple M1 Pro (8 performance and 2 efficiency cores), 32 GB of RAM, macOS 26.4,
Amazon Corretto 21.0.3 (OpenJDK 64-Bit Server VM with the default options), JMH 1.37, one thread.
The documents have 40 records. The numbers are ns/op, indicative only, less is better.

Comparison with Jackson 2.21.6 and Gson 2.13.1, a median of 3 forks:

| Document | Parser: green-jelly | Jackson | Gson | Generator: green-jelly | Jackson | Gson |
|----------|--------------------:|--------:|-----:|-----------------------:|--------:|-----:|
| NUMBERS  | 4985                | 11060   | 12717 | 4392                  | 4760    | 17775 |
| DECIMALS | 7387                | 11514   | 14514 | 5043                  | 8409    | 17702 |
| STRINGS  | 5851                | 9807    | 9709  | 6311                  | 6420    | 13356 |

The parser of green-jelly unescapes the strings with `CopyingStringBuilder`, Jackson gives the chars of a string from its buffer
and Gson creates a `String`. A decimal is given by green-jelly as a mantissa and an exponent, while Jackson and Gson parse
it to a `double` and the generators of them format a `double`. Jackson and green-jelly generate UTF-8 bytes, Gson generates chars.

Parsing of a document received as UTF-8 bytes (`Utf8JsonParserBenchmark`), a median of 3 forks (5 for PRETTY):

| Document | `Utf8JsonParser`, `byte[]` | `Utf8JsonParser`, direct `ByteBuffer` | `CharsetDecoder` to `char[]` + `JsonParser` | `new String` + `JsonParser` |
|----------|---------------------------:|--------------------------------------:|--------------------------------------------:|----------------------------:|
| NUMBERS  | 5638                       | 6451                                  | 6450                                        | 5069                        |
| DECIMALS | 7307                       | 8360                                  | 8034                                        | 7705                        |
| STRINGS  | 6027                       | 7740                                  | 8070                                        | 6730                        |
| ESCAPED  | 12743                      | 17678                                 | 14649                                       | 11972                       |
| UNICODE  | 15560                      | 18828                                 | 19823                                       | 17913                       |
| PRETTY   | 14577                      | 14818                                 | 14763                                       | 11427                       |

The strings are copied and unescaped with `CopyingStringBuilder` in all the cases. Decoding with a `CharsetDecoder`
to a reused array of chars is the way to get chars from bytes with no garbage; it needs a buffer of chars
for the whole document and one more pass over the data. `new String` allocates a new string for each document.

A few things to know to get the most out of the library:
* the parser reads the given data in place, with `CharSequence.charAt()`, nothing is copied but the values of the strings. It's the fastest when a process gives the parser one implementation of `CharSequence` only: then the calls are inlined, while with several implementations every call is a virtual one
* with `FlyweightStringBuilder` the strings of a document aren't copied at all, so it's the best choice if the whole document is available and there is no need to unescape the values
* for UTF-8 bytes `Utf8JsonParser` is the fastest with a `byte[]` or a heap `ByteBuffer`. A direct `ByteBuffer` is read in place with no copying, but every single byte read from it pays for the checks of `ByteBuffer.get(int)`; the long runs of plain bytes of strings are read eight bytes at once
* a custom `BufferingWriter` should override `append(char[], int, int)` with a bulk copying, since the generator writes all the numbers with it

## JSON Value

Sometimes we don't worry about memory consumption and CPU utilization, but we need a simple way to work
with JSON documents. And we would prefer not to introduce complex POJO structures and object mapping.
A good example of an approach like that is the `JSON.simple` library. The `Green-Jelly` also provides a similar
package, `io.github.green4j.jelly.simple`.

The `io.github.green4j.jelly.simple.JsonValue` class is the main class to construct a representation of a JSON document:
```java
final JsonValue json = JsonValue.newObject();
final JsonObject user = json.asObject();
user.putString("name", "Mike");
user.putInteger("age", 23);
final JsonArray rates = user.putArray("rates");
rates.addInteger(10);
rates.addInteger(8);
```

A `JsonValue` can be serialized with an `io.github.green4j.jelly.simple.JsonWriter` or with a `java.io.Writer`:
```java
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
```java
final JsonValueParser parser = new JsonValueParser();
final JsonValue json = parser.parseAndEoj("{ \"name\": \"Mike\", \"age\": 23 }");
System.out.println(json);
```

## License
The code is available under the terms of the [MIT License](http://opensource.org/licenses/MIT).
