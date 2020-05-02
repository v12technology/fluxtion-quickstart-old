# fluxtion-quickstart
5 minute quick start tutorial to demo fluxtion. This example demonstrates implementing a simple unix `wc` like utility with Fluxtion.

## Build:

```bat
C:\tmp> git clone https://github.com/v12technology/fluxtion-quickstart
...
C:\tmp> cd fluxtion-quickstart
C:\tmp> mvn install -P fluxtion
```

## Run

```bat
c:\tmp\fluxtion-quickstart> java -jar dist/wc.jar dist/sample/norvig.txt
 48,698,162 chars
  7,439,040 words
  1,549,801 lines

time: 0.098 sec
```
This shows a Fluxtion generated Static Event Processor processing **48.5 million char events in a 0.1 seconds.** This includes jvm startup and reading all data from the file, for larger files these costs are amortized and we see **750 million events per second** on a single core.

### Zero gc
Fluxtion is careful to allocate no unnecessary objects in the generated solution. The example below demonstrates zero gc in a 5M heap. It is possible to run in 1M heap and a single GC occurs due to class loading in the JVM 
```bat
c:\tmp\fluxtion-quickstart> java -Xmx5M -verbose:gc -jar dist/wc.jar dist/sample/norvig.txt
 48,698,162 chars
  7,439,040 words
  1,549,801 lines

time: 0.106 sec
```

## Development process ##
### Overview ###

To build the Fluxtion wc from scratch a developer follows the five steps below:

1. Create a maven based application and add the [Fluxtion maven build plugin](https://github.com/v12technology/fluxtion-mavenplugin) to the pom.
1. Code an application [event](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/CharEvent.java) that extends [Event](https://github.com/v12technology/fluxtion/blob/master/api/src/main/java/com/fluxtion/api/event/Event.java)
and a [processor](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/WordCounter.java) that will handle the incoming events. 
Event handling methods in the processor are marked with the [@EventHandler](https://github.com/v12technology/fluxtion/blob/master/api/src/main/java/com/fluxtion/api/annotations/EventHandler.java) annotation.
1. Describe the graph construction imperatively in a method marked with a [@SepBuilder](https://github.com/v12technology/fluxtion/blob/master/builder/src/main/java/com/fluxtion/builder/annotation/SepBuilder.java) 
annotation. The Fluxtion generator invokes the builder method as part of the maven build lifecycle.
1. Run the Fluxtion [maven plugin](https://github.com/v12technology/fluxtion-mavenplugin) to generate the [event processor](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/generated/WcProcessor.java) 
1. Integrate the generated processor into the [application](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/Main.java)

### Detailed description ###

#### Step 1 maven build ####

Fluxtion provides a maven plugin that invokes the Fluxtion event stream compiler as part of the normal 
build process with the scan goal. It is good practice to run Fluxtion within a maven profile and skip tests as part
of that profile. Maven struggles to load Fluxtion classes for unit testing in a single build.

Fluxtion requires two dependencies to generate a solution, [api](https://github.com/v12technology/fluxtion/tree/master/api) and 
[generator](https://github.com/v12technology/fluxtion/tree/master/generator). Only the api is required at runtime
so the generator is marked as provided scope. This substantially reduces Fluxtion classes at runtime and has zero
dependencies on external libraries making integration a simple task.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.me.fluxtion.quickstart</groupId>
    <artifactId>wordcount</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>my.fluxtion.quickstart :: wordcount</name>

	
    <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <fluxtion.ver>1.7.25</fluxtion.ver>
    </properties>

    <profiles>
        <profile>
            <id>fluxtion</id>
            <properties>
                <skipTests>true</skipTests>
            </properties>
            <build>
                <plugins>
                    <plugin>
                        <groupId>com.fluxtion</groupId>
                        <artifactId>fluxtion-maven-plugin</artifactId>
                        <version>${fluxtion.ver}</version>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>scan</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>

    <dependencies>
        <dependency>
            <groupId>com.fluxtion</groupId>
            <artifactId>fluxtion-api</artifactId>
            <version>${fluxtion.ver}</version>
        </dependency>
        <dependency>
            <groupId>com.fluxtion</groupId>
            <artifactId>generator</artifactId>
            <version>${fluxtion.ver}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```


#### Step 2 events and processors ####

Define an event, CharEvent, that extends the Fluxtion base class 
[Event](api/src/main/java/com/fluxtion/runtime/event/Event.java). The optional 
filter value of the event is set to the value of the char. This is the event the application will create and feed into the Fluxtion generated SEP
for every character in the file to analyse.

A user written event handler class(WordCounter) receives CharEvents and maintains a set of stateful calculations for chars, words and lines. 
The ```@EventHandler``` annotation attached to a single argument method, marks the method as an entry point for processing. 
Some of the methods are marked with a filter value ```@EventHandler(filterId = '\t')``` signifying 
the methods are only invoked when the Event and the filter value of the event match.

A method is marked with the ```@TearDown``` annotation and will be invoked when teardown is called on the generated static event processor.
 
**[CharEvent:](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/CharEvent.java)** 

```java
public class CharEvent extends Event{
    
    public static final int ID = 1;
    
    public CharEvent(char id) {
        super(ID, id);
        filterId = id;

    }

    public char getCharacter() {
        return (char) filterId;
    }

    /**
     * Setting the character will also make the filterId update as well
     * @param character 
     */
    public void setCharacter(char character) {
        filterId = character;
    }

    @Override
    public String toString() {
        return "CharEvent{" + getCharacter() + '}';
    }
           
}
```

**[WordCounter:](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/WordCounter.java)** 

```java
public class WordCounter {

    public transient int wordCount;
    public transient int charCount;
    public transient int lineCount;
    private int increment = 1;

    @EventHandler
    public void onAnyChar(CharEvent event) {
        charCount++;
    }

    @EventHandler(filterId = '\t')
    public void onTabDelimiter(CharEvent event) {
        increment = 1;
    }

    @EventHandler(filterId = ' ')
    public void onSpaceDelimiter(CharEvent event) {
        increment = 1;
    }

    @EventHandler(filterId = '\n')
    public void onEol(CharEvent event) {
        lineCount++;
        increment = 1;
    }

    @EventHandler(filterId = '\r')
    public void onCarriageReturn(CharEvent event) {
        //do nothing handle \r\n
    }

    @EventHandler(FilterType.unmatched)
    public void onUnmatchedChar(CharEvent event) {
        wordCount += increment;
        increment = 0;
    }

    @TearDown
    public void completed(){
        System.out.println(toString());
    }
    
    @Override
    public String toString() {
        int pad = charCount < 1e6 ? 6 : charCount < 1e9 ? 11 : 14;
        return String.format("%," + pad + "d chars%n%," + pad + "d words%n%," + pad + "d lines %n", charCount, wordCount, lineCount);
    }

    @SepBuilder(name = "WcProcessor",
            packageName = "com.fluxtion.sample.wordcount.generated",
            outputDir = "src/main/java")
    public void buildWcSep(SEPConfig cfg) {
            cfg.addPublicNode(new WordCounter(), "result");
    }

}
```

### Step 3 describe graph ###
Add nodes to a graph at build time in a method annotated with ```@SepBuilder```. Create an instance of WordCounter class
and add it to the supplied [SepConfig](https://github.com/v12technology/fluxtion/blob/master/builder/src/main/java/com/fluxtion/builder/node/SEPConfig.java) 
instance with the addPublicNode method. Package name, generated class name and output directory
are controlled by annotation parameters. There is no need to declare vertices and edges, Fluxtion
carries out all the heavy lifting and calculates the graph structure making the coders' life easier. 

The graph description method:

```java
    @SepBuilder(name = "WcProcessor",
            packageName = "com.fluxtion.sample.wordcount.generated",
            outputDir = "src/main/java")
    public void buildWcSep(SEPConfig cfg) {
            cfg.addPublicNode(new WordCounter(), "result");
    }
```

### Step 4 generate the static event processor ###

Running the maven build will generate the [static event processor](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/generated/WcProcessor.java) 
(SEP) the application will integrate with.

```bat
C:\tmp> mvn install -P fluxtion
```

An instance of the handler class is created and referenced within the generated SEP, the SEP 
will handle all initialisation, lifecycle and event dispatch for managed nodes.

### Step 5 integrate static event processor ###

The generated SEP is the same as any normal java class, it implements the following interfaces:

* [Lifecycle](https://github.com/v12technology/fluxtion/blob/master/api/src/main/java/com/fluxtion/api/lifecycle/Lifecycle.java) - 
call init and teardown methods to notify starting and stopping the processor
* [EventHandler](https://github.com/v12technology/fluxtion/blob/master/api/src/main/java/com/fluxtion/api/lifecycle/EventHandler.java) - 
call onEvent method with an Event, the SEP will handle all dispatching.

 The developer creates an instance of the SEP, calls init() then pushes events with onEvent and finally shuts down with tearDown().

```java

public class Main {

    public static final int SIZE = 4 * 1024;

    public static void main(String[] args) {
        File f = new File(args[0]);
        try {
            streamFromFile(f);
        } catch (IOException ex) {
            System.out.println("error processing file:" + ex.getMessage());
        }
    }

    public static WcProcessor streamFromFile(File file) throws FileNotFoundException, IOException {
        long now = System.nanoTime();
        WcProcessor processor = new WcProcessor();
        processor.init();
        if (file.exists() && file.isFile()) {
            FileChannel fileChannel = new RandomAccessFile(file, "r").getChannel();
            long size = file.length();
            MappedByteBuffer buffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY, 0, size);
            CharEvent charEvent = new CharEvent(' ');

            final byte[] barray = new byte[SIZE];
            int nGet;
            while (buffer.hasRemaining()) {
                nGet = Math.min(buffer.remaining(), SIZE);
                buffer.get(barray, 0, nGet);
                for (int i = 0; i < nGet; i++) {
                    charEvent.setCharacter((char) barray[i]);
                    processor.handleEvent(charEvent);
                }
            }
            double delta = ((int) (System.nanoTime() - now) / 1_000_000) / 1_000.0;
            processor.tearDown();
            System.out.printf("time: %.3f sec %n", delta);
        } else {
            System.out.println("cannot process file file:" + file.getAbsolutePath());
        }
        return processor;
    }
}

```