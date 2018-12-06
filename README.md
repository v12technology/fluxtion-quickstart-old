# fluxtion-quickstart
5 minute quick start tutorial to demo fluxtion. This example demonstrates implementing
a simple unix wc like utility with Fluxtion.

## Build:
mvn install -P fluxtion

## Run
java -jar dist\wc.jar dist\sample\norvig.txt example below:

    C:\projects\quickstart>java -jar dist\wc.jar dist\sample\norvig.txt
    48,698,162 chars
     7,439,040 words
     1,549,801 lines

    time: 0.098 sec

this shows Fluxtion processing 48.5 million char events in a 0.1 seconds.

## Description
