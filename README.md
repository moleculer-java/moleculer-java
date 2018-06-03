# Moleculer for Java

[![Build Status](https://travis-ci.org/moleculer-java/moleculer-java.svg?branch=master)](https://travis-ci.org/moleculer-java/moleculer-java)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b26c4ff30c6b4cb4a5536b5c1de0c317)](https://www.codacy.com/app/berkesa/moleculer-java?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=moleculer-java/moleculer-java&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/moleculer-java/moleculer-java/branch/master/graph/badge.svg)](https://codecov.io/gh/moleculer-java/moleculer-java)

Java implementation of the [Moleculer microservices framework](http://moleculer.services/).

## Download

**Maven**

```xml
<dependencies>
	<dependency>
		<groupId>com.github.berkesa</groupId>
		<artifactId>moleculer-java</artifactId>
		<version>1.0.1</version>
		<scope>runtime</scope>
	</dependency>
</dependencies>
```

**Gradle**

```gradle
dependencies {
	compile group: 'com.github.berkesa', name: 'moleculer-java', version: '1.0.1' 
}
```

## Usage

```java
import io.datatree.Tree;
import services.moleculer.cacher.Cache;
import services.moleculer.eventbus.*;
import services.moleculer.service.*;

public class Sample {

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

	@Name("math")
	public static class MathService extends Service {

		@Cache(keys = { "a", "b" }, ttl = 5000)
		public Action add = ctx -> {
			return ctx.params.get("a", 0) + ctx.params.get("b", 0);
		};

		@Subscribe("foo.*")
		public Listener listener = payload -> {
			logger.info("Event received: " + payload);
		};

	};

}
```

# License
moleculer-java is available under the [MIT license](https://tldrlegal.com/license/mit-license).
