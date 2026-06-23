# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Moleculer for Java — a JVM implementation of the [Moleculer microservices framework](https://moleculer.services/).
It is wire-compatible with the Node.js implementation, so Java and Node.js nodes can form one cluster. The default wire-protocol version is **"5"** (Moleculer JS **0.15**); v4 and v5 are wire-compatible for the JSON serializer, so set `ServiceBroker.builder().protocolVersion("4")` (or `ServiceBrokerConfig.setProtocolVersion("4")`) to talk to legacy 0.14 nodes. The library is non-blocking and Promise-based throughout. It is published to Maven Central as `com.github.berkesa:moleculer-java`.

## Build & test

The build uses **Maven** with a **Java 17** bytecode target (`<release>17</release>`) and a JDK 17+ build environment (JDK 25 in use). Minimum consumer runtime: **JDK 17** (forced by Spring Framework 6.2.x).

```powershell
mvn clean verify                 # compile + test + jar (the definition-of-done gate)
mvn clean install                # same, then installs 2.0.0-SNAPSHOT to ~/.m2 for downstream projects
mvn test                         # run tests only
mvn -Prelease deploy             # sources + javadoc + GPG-signed publish to Maven Central (Central Portal)
```

Run a single test class or method (Surefire `-Dtest` filter):

```powershell
mvn test "-Dtest=MemoryCacherTest"
mvn test "-Dtest=ServiceTest#testCall"
```

Gotchas that will bite you:

- **Java 17 baseline** (`maven.compiler.release=17`); compilation is plain **`javac`**. Java 17-era APIs are fine; avoid Java 18–21 APIs (not in the bytecode contract).
- **Integration tests needing an external broker are excluded** in the Surefire `<excludes>` (Kafka, JMS, FileSystem, AMQP, MQTT, NATS, Redis, TCP transporter & stream tests, the Redis cacher test, `ClusterTest`/`GossiperTest`, and `TransporterTestSuite`) so `mvn test` is green offline. Remove a class from `<excludes>` (and start the matching broker) to run it.
- **`PojoTest` (openpojo)** needs the `--add-opens` flags in the Surefire `argLine` to deep-reflect JDK types under the Java module system — keep them.

### Version bumps touch two places

When changing the release version, update **both** or the cluster handshake will be inconsistent:
1. `pom.xml` → `<version>`
2. `ServiceBroker.SOFTWARE_VERSION` (in `src/main/java/services/moleculer/ServiceBroker.java`)

(Maven derives the JAR file name from `<version>`, so the artifact is `moleculer-java-<version>.jar`; the published `groupId:artifactId` is unchanged.)

## Core architecture

Everything lives under the `services.moleculer` package (note the unusual root: `services`, not `com.*`).

### Two foundational types from the `datatree` dependency

These are used *everywhere*; understand them first:
- **`io.datatree.Tree`** — a dynamic, JSON-like document. All action params, responses, event payloads, and node descriptors are `Tree`s. It carries a separate **meta** sub-tree (`tree.getMeta()`) that rides alongside the data across calls.
- **`io.datatree.Promise`** — the async return type of every call/action. Non-blocking; `.then(...)`/`.catchError(...)` to chain. In tests, `.waitFor(timeoutMillis)` blocks for the result.

### The node: `ServiceBroker`

One `ServiceBroker` instance == one node in the cluster. Construct it directly (`new ServiceBroker("node-1")`), via `ServiceBroker.builder()`, or from a `ServiceBrokerConfig`. Lifecycle is `start()` / `stop()`; both walk every internal component calling `started(broker)` / `stopped()` (the `MoleculerComponent` / `MoleculerLifecycle` contract). Services/middlewares added before `start()` are queued and installed during boot; added after, they install immediately and (if clustered) re-broadcast the node's INFO packet.

### `ContextSource` — the call/emit/broadcast surface

`ServiceBroker` and `Context` both extend `ContextSource`, which defines `call()`, `emit()`, `broadcast()`, `broadcastLocal()`, and `createStream()`. Practical consequence: from inside an action you make **nested calls with the same API** via the injected `ctx` (`ctx.call("math.add", ...)`), and the framework propagates `requestID`, `parentID`, call `level`, and meta automatically. `Context` is immutable request state: `id`, `name`, `params` (Tree), `level`, `parentID`, `requestID`, `stream`, `opts`, `nodeID`.

- **emit** → load-balanced to ONE listener per group; **broadcast** → ALL listeners on ALL nodes; **broadcastLocal** → ALL listeners on this node only.
- Event group targeting via `Groups.of(...)`. Calls take `CallOptions` (`CallOptions.nodeID(...)`, timeout, retries) as a trailing vararg.

### Authoring a service

Extend `services.moleculer.service.Service`. Actions and listeners are declared as **public fields discovered by reflection** at registration:

```java
broker.createService(new Service("math") {
    @Cache(keys = {"a", "b"}, ttl = 30)        // optional per-action caching
    public Action add = ctx -> ctx.params.get("a").asInteger()
                             + ctx.params.get("b").asInteger();

    @Subscribe("user.*")                        // event listener (Listener field)
    @Group("audit")
    public Listener onUser = ctx -> { /* ... */ };
});
```

- **Action visibility = field visibility.** `public Action` is exported (remotely callable, appears in the `$node` descriptor). Package-private/`private Action` is local-only and hidden from the descriptor — see `ServiceTest` (`add` vs `add2`/`add3`).
- Annotations: `@Name` (override service/component name), `@Version`, `@Cache`, `@Subscribe` + `@Group`, `@Dependencies`, `@Visibility`.
- Use `broker.waitForServices(...)` to depend on services that may not be up yet.

### Pluggable components (the extension model)

`ServiceBrokerConfig` holds one instance of each subsystem; every one is an interface with a default impl and multiple alternatives. Swap them via the builder/config setters. This is the heart of the design — each package = one pluggable concern:

| Concern | Interface | Default | Alternatives (package) |
|---|---|---|---|
| Clustering | `Transporter` | none (single node) | TCP/gossip, Redis, NATS, MQTT, AMQP/RabbitMQ, Kafka, JMS, FileSystem, Internal/Null (`transporter`) |
| Caching | `Cacher` | `MemoryCacher` | Redis, JCache (`cacher`) |
| Serialization | `Serializer` | `JsonSerializer` | MsgPack, CBOR, BSON, Ion, Smile, Java + wrappers `BlockCipher` (encryption), `Deflater` (compression), `Chained` (`serializer`) |
| Load balancing | `StrategyFactory` | `RoundRobin` | Random variants, CpuUsage, NetworkLatency, Shard (`strategy`) |
| CPU monitor | `Monitor` | JMX→Constant (auto-detected) | Command (`monitor`) |
| Metrics | `Metrics` | `DefaultMetrics` (off unless `metricsEnabled`) | Dropwizard, Micrometer reporters (`metrics`) |
| UID gen | `UidGenerator` | `IncrementalUidGenerator` | (`uid`) |

Other internal components: `ServiceRegistry` (`DefaultServiceRegistry`), `ServiceInvoker` (`DefaultServiceInvoker` — retry logic + max call level), `Eventbus` (`DefaultEventbus`).

> Note: optional backend client libraries (Lettuce, jnats, Kafka, RabbitMQ, Paho MQTT, Jakarta JMS + ActiveMQ, JCache RI, Dropwizard/Micrometer registries) are on the compile classpath but marked `<optional>true</optional>` in the POM — downstream users add the client lib for the backend they actually use.

### Middleware

`Middleware.install(Action action, Tree config)` returns a wrapped `Action` (decorator pattern). The **Cacher, CircuitBreaker, and Metrics are themselves middleware** — they wrap actions rather than being special-cased. Register with `broker.use(...)`. The `breaker` package implements the circuit breaker; `DefaultServiceInvoker` drives retries based on `MoleculerError.isRetryable()`.

### Running standalone + Spring

`config.MoleculerRunner` is a `main()` that boots a broker from a **Spring** context — either an XML config (`ClassPathXml`/`FileSystemXml`) or a Spring Boot main class (first CLI arg). It installs a shutdown hook and listens on a **UDP port (default 6786)** for a stop message (`java ... MoleculerRunner stop <port> <secret>`). `SpringRegistrator` auto-registers all Spring beans of type `Service` (and can package-scan) into the broker.

### Internal `$node` service

`internal.NodeService` exposes introspection actions (`$node.list`, `$node.services`, `$node.actions`, `$node.health`, ...). Installed automatically unless `config.setInternalServices(false)`.

## Test conventions

- Tests use **JUnit 5 (Jupiter)**: `@Test`, `@BeforeEach`/`@AfterEach`, and static `Assertions.*` methods (originally JUnit 3 `TestCase` — migrated in 2.0.0).
- Typical pattern: build a broker, `createService(...)`, then `broker.call(...).waitFor(timeout)` and assert on the returned `Tree`.
- `breaker/TestTransporter` and `stream/WrongOrderTransporter` are in-memory fake transporters used to simulate cluster/network behavior and inspect emitted protocol messages without real infrastructure.
