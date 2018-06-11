# Moleculer for Java

[![Build Status](https://travis-ci.org/moleculer-java/moleculer-java.svg?branch=master)](https://travis-ci.org/moleculer-java/moleculer-java)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b26c4ff30c6b4cb4a5536b5c1de0c317)](https://www.codacy.com/app/berkesa/moleculer-java?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=moleculer-java/moleculer-java&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/moleculer-java/moleculer-java/branch/master/graph/badge.svg)](https://codecov.io/gh/moleculer-java/moleculer-java)

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
		<version>1.0.2</version>
		<scope>runtime</scope>
	</dependency>
</dependencies>
```

**Gradle**

```gradle
dependencies {
	compile group: 'com.github.berkesa', name: 'moleculer-java', version: '1.0.2' 
}
```

## Usage

```java
import io.datatree.Tree;
import services.moleculer.cacher.Cache;
import services.moleculer.eventbus.*;
import services.moleculer.service.*;

public class Sample {

	// --- MESSAGE BROKER TEST ---
	
	public static void main(String[] args) throws Exception {
		try {

			// Create Message Broker
			ServiceBroker broker = ServiceBroker.builder().build();

			// Deploy servie
			broker.createService(new MathService());
			
			// Start Message Broker
			broker.start();

			// Create input
			Tree in = new Tree();
			in.put("a", 3);
			in.put("b", 5);

			// Local or remote method call
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

	// --- SAMPLE SERVICE ---
	
	@Name("math")
	public static class MathService extends Service {

		@Cache(keys = { "a", "b" }, ttl = 5000)
		public Action add = ctx -> {
			return ctx.params.get("a").asInteger() + ctx.params.get("b").asInteger();
		};

		@Subscribe("foo.*")
		public Listener listener = payload -> {
			logger.info("Event received: " + payload);
		};

	};

}
```

# Documentation
Developer documentation is in progress. At present, only JavaDoc and test cases are available.

# Subprojects

* [Interactive Developer Console for Moleculer](https://moleculer-java.github.io/moleculer-java-repl/)

# License
Moleculer-java is available under the [MIT license](https://tldrlegal.com/license/mit-license).
