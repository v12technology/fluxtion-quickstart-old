# Introduction
5 Minute tutorial to demonstrate processing of streaming data using Fluxtion. 
The goal is to read sensor data for a set of rooms, calculate aggregate values per room and 
notify a user class when a room breaches set criteria.
## Requirements
 - Read room sensor temperature as a csv character stream or as instances of SensorReading events. 
 - Merge csv and SensorReading's into a single event stream for processing
 - The event stream can be infinite
 - For each room calculate the max and average temperature
 - Run a tumbling window, zeroing all room values every 3 readings
 - Register a user class with the stream processor to act as a controller of an external system
 - If a room has an average of > 60 and max of > 90 then
	 - Log a warning
	 - A user class(TempertureController) will attempt to send an SMS listing rooms to investigate
 - Register an SMS endpoint with the controller by sending a String as an event into the processor
## Running the application
Clone the application
Execute the sensorquickstart.jar
```bat
git clone https://github.com/v12technology/fluxtion-quickstart.git
cd 
java  -Dfluxtion.cacheDirectory=fluxtion -jar dist\sensorquickstart.jar
21:40:45.991 [main] INFO  c.f.generator.compiler.SepCompiler - generated sep: C:\Users\gregp\development\projects\fluxtion\open-source\quickstart\fluxtion\source\com\fluxtion\quickstart\roomsensor\generated\RoomSensorSEP.java
 ->     bathroom:45
 ->     living:78
 ->     bed:43
readings in window : [(bathroom  max:45 average:45.0), (living  max:78 average:78.0), (bed  max:43 average:43.0)]
 ->     bed:23
 ->     bathroom:19
 ->     bed:34
readings in window : [(bed  max:34 average:28.5), (bathroom  max:19 average:19.0)]
 ->     living:89
 ->     bed:23
 ->     living:44
readings in window : [(living  max:89 average:66.5), (bed  max:23 average:23.0)]
 ->     living:36
 ->     living:99
 ->     living:56
readings in window : [(living  max:99 average:63.666666666666664)]
**** WARNING **** sensors to investigate:[living]
NO SMS details registered, controller impotent
Temp controller registering sms details:0800-1-HELP-ROOMTEMP
 ->     living:36
 ->     living:99
 ->     living:56
readings in window : [(living  max:99 average:63.666666666666664)]
**** WARNING **** sensors to investigate:[living]
SMS:0800-1-HELP-ROOMTEMP investigate:[living]
```
### Cached compilation
The application generates a solution in the cache directory fluxtion , ready for a second run, The directory contains three sub-directories:
 - classes - compiled classes implementing the stream processing requirements
 - resources - meta-data describing the processor
 - sources - The java source files used to generate the classes 

Executing the jar a second time sees a significant reduction in execution time as the processor has been compiled ahead of time and is executed immediately. Deleting the cache directory will cause the regeneration and compilation of the solution. 

## Code description
The SensorMonitor builds a streaming processing engine in the main method
```java
StaticEventProcessor processor = reuseOrBuild("RoomSensorSEP", "com.fluxtion.quickstart.roomsensor.generated", SensorMonitor::buildSensorProcessor);

```
The actual method that performs the build is: 
```java
public static void buildSensorProcessor(SEPConfig cfg) {
    //merge csv marshller and SensorReading instance events
    Wrapper<SensorReading> sensorData = merge(select(SensorReading.class),
            csvMarshaller(SensorReading.class).build()).console(" -> \t");
    //group by sensor and calculate max, average
    GroupBy<SensorReadingDerived> sensors = groupBy(sensorData, SensorReading::getSensorName, SensorReadingDerived.class)
            .init(SensorReading::getSensorName, SensorReadingDerived::setSensorName)
            .max(SensorReading::getValue, SensorReadingDerived::setMax)
            .avg(SensorReading::getValue, SensorReadingDerived::setAverage)
            .build();
    //tumble window (count=3), warning if avg > 60 && max > 90 in the window for a sensor
    tumble(sensors, 3).console("readings in window : ", GroupBy::collection)
            .map(SensorMonitor::warningSensors, GroupBy::collection)
            .filter(c -> c.size() > 0)
            .console("**** WARNING **** sensors to investigate:")
            .push(new TempertureController()::investigateSensors);
}
```

