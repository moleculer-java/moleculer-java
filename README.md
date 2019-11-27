[![Build Status](https://travis-ci.org/moleculer-java/moleculer-java.svg?branch=master)](https://travis-ci.org/moleculer-java/moleculer-java)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a18722bf26f14e72946d1ae761fa7b5b)](https://www.codacy.com/manual/berkesa/moleculer-java?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=moleculer-java/moleculer-java&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/moleculer-java/moleculer-java/branch/master/graph/badge.svg)](https://codecov.io/gh/moleculer-java/moleculer-java)
[![Javadocs](https://www.javadoc.io/badge/com.github.berkesa/moleculer-java.svg)](https://www.javadoc.io/doc/com.github.berkesa/moleculer-java)

# Moleculer for Java

Java implementation of the [Moleculer microservices framework](http://moleculer.services/). Moleculer Ecosystem is designed to facilitate the development of multilingual distributed applications. The Java-based Moleculer is completely compatible with the Node.js-based Moleculer.

## Features

*  Fast - High-performance and non-blocking API
*  Polyglot - Moleculer is implemented under Node.js and Java
*  Extensible - All built-in modules (caching, serializer, transporter) are pluggable
*  Open source - Moleculer is 100% open source and free of charge
*  Fault tolerant - With built-in load balancer &amp; circuit breaker

## Download

**Maven**

```xml
<dependencies>
	<dependency>
		<groupId>com.github.berkesa</groupId>
		<artifactId>moleculer-java</artifactId>
		<version>1.2.3</version>
		<scope>runtime</scope>
	</dependency>
</dependencies>
```

**Gradle**

```gradle
dependencies {
	compile group: 'com.github.berkesa', name: 'moleculer-java', version: '1.2.3' 
}
```

#### Changes in Version 1.2.x:

The Event Listener implementation of Moleculer-Java has become similar to the latest (V1.4) Node.js implementation.
In the previous versions, Event Listeners received only the data (~= JSON) block:

```java
@Subscribe("test.*")
Listener evt = payload -> {
    logger.info("Received data: " + payload);
};
```
From version 1.2 onwards, Event Listeners receive the same Context object as the Actions
and the `ctx.params` contains data corresponding to the previous `payload`.
Just change the "payload" variable to "ctx.params" to migrate:

```java
@Subscribe("test.*")
Listener evt = ctx -> {
    logger.info("Received data: " + ctx.params);
};
```

The Context object contains information about the event source,
and allows you to initiate chained calls where call depth is limited.

## Short example of creating and using a Service

```java
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.*;

public class Sample {

    public static void main(String[] args) {
        try {

            // Create Message Broker
            ServiceBroker broker = ServiceBroker.builder().build();

            // Deploy "math" servie
            broker.createService(new Service("math") {
                Action add = ctx -> {
                    return ctx.params.get("a").asInteger()
                         + ctx.params.get("b").asInteger();
                };           
            });
                        
            // Start Message Broker
            broker.start();

            // Create input "JSON"
            Tree in = new Tree();
            in.put("a", 3);
            in.put("b", 5);

            // Invoke "math" service
            broker.call("math.add", in).then(rsp -> {
                
                // Print the response
                broker.getLogger().info("Response: " + rsp.asInteger());
            });

            // Stop Message Broker
            broker.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

# Documentation
Developer documentation is in progress. At present, only JavaDoc and test cases are available.

# Sample project

*  [Moleculer Java demo project with Gradle](https://moleculer-java.github.io/moleculer-spring-boot-demo/)

# Subprojects

*  [High-performance Web API for Moleculer Apps](https://moleculer-java.github.io/moleculer-java-web/)
*  [Interactive Developer Console](https://moleculer-java.github.io/moleculer-java-repl/)
*  [JMX Service for Moleculer](https://moleculer-java.github.io/moleculer-java-jmx/)
*  [MongoDB API for Moleculer](https://moleculer-java.github.io/moleculer-java-mongo/)

# License
Moleculer-java is available under the [MIT license](https://tldrlegal.com/license/mit-license).
