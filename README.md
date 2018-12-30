# Moleculer for Java

[![Build Status](https://travis-ci.org/moleculer-java/moleculer-java.svg?branch=master)](https://travis-ci.org/moleculer-java/moleculer-java)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b26c4ff30c6b4cb4a5536b5c1de0c317)](https://www.codacy.com/app/berkesa/moleculer-java?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=moleculer-java/moleculer-java&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/moleculer-java/moleculer-java/branch/master/graph/badge.svg)](https://codecov.io/gh/moleculer-java/moleculer-java)
[![Javadocs](https://www.javadoc.io/badge/com.github.berkesa/moleculer-java.svg)](https://www.javadoc.io/doc/com.github.berkesa/moleculer-java)

Java implementation of the [Moleculer microservices framework](http://moleculer.services/).
The Java-based Moleculer is completely compatible with the NodeJS-based Moleculer.

## Features

* Fast - High-performance and non-blocking API
* Polyglot - Moleculer is implemented under Node.js and Java
* Extensible - All built-in modules (caching, serializer, transporter) are pluggable
* Open source - Moleculer is 100% open source and free of charge
* Fault tolerant - With built-in load balancer &amp; circuit breaker

## Download

**Maven**

```xml
<dependencies>
	<dependency>
		<groupId>com.github.berkesa</groupId>
		<artifactId>moleculer-java</artifactId>
		<version>1.0.8</version>
		<scope>runtime</scope>
	</dependency>
</dependencies>
```

**Gradle**

```gradle
dependencies {
	compile group: 'com.github.berkesa', name: 'moleculer-java', version: '1.0.8' 
}
```

## Usage from code

```java
import io.datatree.Tree;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Action;
import services.moleculer.service.Service;

public class Sample {

	// --- MESSAGE BROKER TEST ---
	
	public static void main(String[] args) throws Exception {
		try {

			// Create Message Broker
			ServiceBroker broker = ServiceBroker.builder().build();

			// Deploy "math" servie
			broker.createService(new Service("math") {

				public Action add = ctx -> {

					return ctx.params.get("a").asInteger()
						 + ctx.params.get("b").asInteger();

				};
			
			});
						
			// Start Message Broker
			broker.start();

			// Create input
			Tree in = new Tree();
			in.put("a", 3);
			in.put("b", 5);

			// Invoke "math" service
			broker.call("math.add", in).then(rsp -> {
				
				// Response
				broker.getLogger().info("Response: " + rsp.asInteger());
				
			});

			// Broadcast event
			broker.broadcast("foo.xyz", "x", 3, "y", 5);

			// Stop Message Broker
			broker.stop();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
```

## Usage with Spring Framework

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:context="http://www.springframework.org/schema/context"
	xsi:schemaLocation="http://www.springframework.org/schema/beans
	   http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
	   http://www.springframework.org/schema/context
	   http://www.springframework.org/schema/context/spring-context-3.0.xsd">

	<!-- ENABLE ANNOTATION PROCESSING -->

	<context:annotation-config />

	<!-- PACKAGE OF THE MOLECULER SERVICES -->
	
	<context:component-scan base-package="my.services" />

	<!-- SPRING REGISTRATOR FOR MOLECULER SERVICES -->

	<bean id="registrator" class="services.moleculer.config.SpringRegistrator" depends-on="broker" />

	<!-- SERVICE BROKER INSTANCE -->

	<bean id="broker" class="services.moleculer.ServiceBroker"
		init-method="start" destroy-method="stop">
		<constructor-arg ref="brokerConfig" />
	</bean>

	<!-- SERVICE BROKER SETTINGS -->

	<bean id="brokerConfig" class="services.moleculer.config.ServiceBrokerConfig">
		<property name="nodeID" value="node-1" />
		<property name="transporter" ref="transporter" />
	</bean>

	<!-- CONFIGURE TRANSPORTER -->

	<bean id="transporter" class="services.moleculer.transporter.TcpTransporter" />

</beans>
```

### Sample Moleculer Service for Spring

```java
package my.service.package;

import services.moleculer.ServiceBroker;
import services.moleculer.cacher.Cache;
import services.moleculer.eventbus.Group;
import services.moleculer.eventbus.Listener;
import services.moleculer.eventbus.Subscribe;
import services.moleculer.service.Action;
import services.moleculer.service.Dependencies;
import services.moleculer.service.Name;
import services.moleculer.service.Service;

@Name("math")
@Dependencies({"loggerService", "storageService"})
public class MathService extends Service {

	@Autowired
	private MySpringBean mySpringBean;

	@Override
	public void started(ServiceBroker broker) throws Exception {
		super.started(broker);
		
		// User-defined init method
		// mySpringBean.method();
	}

	@Cache(keys = { "a", "b" }, ttl = 60000)
	public Action add = ctx -> {

		// Body of the distributed method ("action")
		return ctx.params.get("a").asInteger()
			 + ctx.params.get("b").asInteger();

	};

	@Subscribe("user.created")
	@Group("optionalEventGroup")
	public Listener userCreated = payload -> {
		
		// Body of the distributed event listener method
		System.out.println("Received: " + payload);
	};
	
	@Override
	public void stopped() {
		
		// User-defined destroy method
	}

}
```

# Documentation
Developer documentation is in progress. At present, only JavaDoc and test cases are available.

# Subprojects

* [Interactive Developer Console for Moleculer](https://moleculer-java.github.io/moleculer-java-repl/)

# License
Moleculer-java is available under the [MIT license](https://tldrlegal.com/license/mit-license).
