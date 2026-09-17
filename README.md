# Moleculer for Java

Moleculer for Java is a JVM implementation of the
[Moleculer microservices framework](https://moleculer.services/). It is **wire-compatible**
with the Node.js version, so Java and Node.js nodes can join the **same cluster** and call each
other's services as if they were local. The whole stack is **non-blocking and Promise-based**,
and built around the universal `Tree` data type, so the same code scales from a single process
to a distributed system.

## Highlights

- **Polyglot clustering** — speaks the Moleculer wire protocol (defaults to v5 / Moleculer JS
  0.15; configurable back to v4) so it interoperates with Node.js nodes out of the box.
- **Pluggable everything** — transporters (TCP/gossip, Redis, NATS, MQTT, AMQP, Kafka, JMS, …),
  cachers, serializers, load-balancing strategies, and metrics are all swappable interfaces.
- **Built-in fault tolerance** — circuit breaker, retries, timeouts, and bulkhead, all as
  composable middleware.
- **Service discovery & introspection** via the built-in `$node` service.

## Documentation

[![Documentation](https://raw.githubusercontent.com/moleculer-java/site/master/docs/docs-button.png)](https://moleculer-java.github.io/site/introduction.html)

## Download

```xml
<dependency>
    <groupId>com.github.berkesa</groupId>
    <artifactId>moleculer-java</artifactId>
    <version>2.1.1</version>
</dependency>
```

## Quick start

A node is a `ServiceBroker`. Register a service, start the broker, and call an action — every
call returns a non-blocking `Promise`:

```java
ServiceBroker broker = new ServiceBroker("node-1");

// A service with one callable action
broker.createService(new Service("math") {
    public Action add = ctx ->
        ctx.params.get("a").asInteger() + ctx.params.get("b").asInteger();
});

broker.start();

Tree params = new Tree();
params.put("a", 5);
params.put("b", 3);

broker.call("math.add", params).then(rsp -> {
    System.out.println("Result = " + rsp.asInteger());   // 8
});
```

Add a `Transporter` (NATS, Redis, TCP, …) and the very same `math.add` action becomes callable
from any other node in the cluster — including Node.js nodes.

## Requirements

Java 17 or newer.

## License

Moleculer for Java is available under the [MIT license](https://tldrlegal.com/license/mit-license).
