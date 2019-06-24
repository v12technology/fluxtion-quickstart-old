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

To build the Fluxtion wc from scratch a developer follows the four steps below:

1. Code an [event](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/CharEvent.java) that extend [Event](https://github.com/v12technology/fluxtion/blob/master/api/src/main/java/com/fluxtion/api/event/Event.java)
and a [processor](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/WordCounter.java). Annotation are used to declare event handling methods in the processor with @EventHandler
2. Describe the graph with an @SepBuilder annotated builder method
3. Run the Fluxtion [maven plugin](https://github.com/v12technology/fluxtion-quickstart/blob/master/pom.xml) to generate the [event processor](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/generated/WcProcessor.java) 
4. Integrate the generated processor into the [application](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/Main.java)

### Detailed description ###

#### Events and processors ####

Define events that the application will process by extending the base class Event. The optional 
filter value of the event is set to the value of the char. This is the event the application will create and feed into the generated SEP.

A user written class receives CharEvents and maintains a set of stateful calculations for chars, words and lines. 
The ```@EventHandler``` annotation attached to a single argument method, marks the method as an entry point for processing. 
Some of the methods are marked with a filter value ```@EventHandler(filterId = '\t')``` signifying 
the  methods are only invoked when the Event and the filter value of the event match.

 
An instance of this class is created and referenced within the generated SEP, the SEP will handle all initialisation, lifecycle and event dispatch for managed nodes. 


**[CharEvent:](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/CharEvent.java)** Extends [Event](api/src/main/java/com/fluxtion/runtime/event/Event.java), the content of the CharEvent is the char value. An event is the entry point to a processing cycle in the SEP.

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
            cfg.supportDirtyFiltering = false;
    }

}

```
 