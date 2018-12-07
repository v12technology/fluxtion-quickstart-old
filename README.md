# fluxtion-quickstart
5 minute quick start tutorial to demo fluxtion. This example demonstrates implementing
a simple unix wc like utility with Fluxtion.

## Build:
```bat
C:\tmp>git clone https://github.com/v12technology/fluxtion-quickstart
...
C:\tmp>cd fluxtion-quickstart
C:\tmp>mvn install -P fluxtion
```

## Run
```bat
c:\tmp\fluxtion-quickstart>java -jar dist\wc.jar dist\sample\norvig.txt
 48,698,162 chars
  7,439,040 words
  1,549,801 lines

time: 0.098 sec
```
This shows a Fluxtion generated Static Event Processor processing **48.5 million char events in a 0.1 seconds.** This includes jvm startup and reading all data from the file, for larger files these costs are amortized and we see **750 million events per second** on a single core.

### Zero gc
Fluxtion is careful to allocate no unnecessary objects in the generated solution. The example below demonstrates zero gc in a 5M heap. It is possible to run in 1M heap and a single GC occurs due to class loading in the JVM 
```bat
c:\tmp\fluxtion-quickstart>java -Xmx5M -verbose:gc -jar dist\wc.jar dist\sample\norvig.txt
 48,698,162 chars
  7,439,040 words
  1,549,801 lines

time: 0.106 sec
```

## Description

The processing is described [here](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/WordCounter.java)

The Fluxtion generated Static Event Processor is [here](https://github.com/v12technology/fluxtion-quickstart/blob/master/src/main/java/com/fluxtion/sample/wordcount/generated/WcProcessor.java) 
