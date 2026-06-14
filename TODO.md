# TODO — Modernize `moleculer-java` (core) to 2.0.0

> **You are the per-project Claude Code instance for `moleculer-java`.** Self-contained file.
> Goal: Gradle/Java 8 → **Maven + JDK 21**, upgrade/replace/drop dependencies, resolve
> `javax`→`jakarta`, tests green on **JUnit 5**, legacy files removed, version **2.0.0**.
> This is the **framework core** (broker, services, transporters, cachers, serializers, metrics).
> Packages live under `services.moleculer.*`. The Tree/Promise data+async types come from datatree.

## Coordinates & facts
- Maven: `com.github.berkesa:moleculer-java`, `jar`, license **MIT**.
- `name`: *Moleculer Microservices Framework* · `inceptionYear`: 2018
- `url`: https://moleculer-java.github.io/moleculer-java/ · `scm`: https://github.com/moleculer-java/moleculer-java.git
- developer: `berkesa` / Andras Berkes / andras.berkes@programmer.net
- **Version → `2.0.0` in THREE places:** `pom.xml` `<version>`, **and**
  `services.moleculer.ServiceBroker.SOFTWARE_VERSION` (the old Gradle `jar { version }` collapses
  into the single Maven `<version>`). Keep `SOFTWARE_VERSION` in sync — it affects the cluster handshake.

## Inter-project dependencies (PIN to 2.0.0)
- `com.github.berkesa:datatree-adapters:2.0.0` (was 1.0.15)
- `com.github.berkesa:datatree-promise:2.0.0` (was 1.0.10)

Build `datatree` → `datatree-adapters` + `datatree-promise` first so their `2.0.0-SNAPSHOT` are in
your local `~/.m2`.

## Dependency actions (confirm newest at execution)
### Upgrade
| Dependency | Current | Target |
|---|---|---|
| `org.slf4j:slf4j-api`, `slf4j-jdk14`, `log4j-over-slf4j`, `jcl-over-slf4j` | 1.7.30 | **2.0.18** |
| `de.undercouch:bson4jackson` | 2.12.0 | **2.18.0** (🔒 lockstep — locked by datatree-adapters; keep identical) |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-cbor` / `-smile` | 2.12.3 | **via `jackson-bom` 2.19.0** (🔒 lockstep — locked by datatree-adapters) |
| `org.springframework:spring-context` | 5.3.7 | **6.2.x** ⚠ jakarta, Java 17+ |
| `org.springframework.boot:spring-boot-starter` | 2.5.0 | **3.5.x** ⚠ jakarta |
| `io.nats:jnats` | 2.11.3 | **2.20.x** |
| `org.eclipse.paho:org.eclipse.paho.client.mqttv3` | 1.2.5 | keep (last v3) — mark `optional` |
| `com.rabbitmq:amqp-client` | 5.12.0 | **5.2x** |
| `org.apache.kafka:kafka-clients` | 2.8.0 | **4.x** ⚠ big jump |
| `io.micrometer:micrometer-core` + registries (new-relic/jmx/datadog) | 1.7.0 | **1.14.x** |
| `javax.cache:cache-api` + `org.jsr107.ri:cache-ri-impl` | 1.1.1 | keep 1.1.1 (JCache stays `javax.cache`) |

### Re-coordinate (groupId/package changed — not just a bump)
| Old | New | Touches |
|---|---|---|
| `biz.paluch.redis:lettuce:4.5.0.Final` | **`io.lettuce:lettuce-core:6.x`** | Redis transporter + Redis cacher (⚠ API changed) |
| `software.amazon.ion:ion-java:1.5.1` | **`com.amazon.ion:ion-java:1.11.10`** (🔒 lockstep — locked by datatree-adapters) | Ion serializer |
| `com.codahale.metrics:metrics-core:3.0.2` | **`io.dropwizard.metrics:metrics-core:4.2.x`** | Dropwizard metrics reporter |
| `com.diogonunes:JCDP:3.0.4` | **`com.diogonunes:JColor:5.x`** | colored logger output (⚠ package `com.diogonunes.jcdp` → `com.diogonunes.jcolor`) |
| `javax.jms:javax.jms-api:2.0.1` + `org.apache.activemq:activemq-client:5.16.2` | **`jakarta.jms:jakarta.jms-api:3.x` + ActiveMQ 6.x (jakarta) / Artemis** | JMS transporter (⚠ jakarta) |
| msgpack `org.msgpack:msgpack:0.6.12` + `jackson-dataformat-msgpack:0.8.24` | **`org.msgpack:msgpack-core:0.9.x` + `jackson-dataformat-msgpack:0.9.9`** (dataformat 🔒 lockstep — locked by datatree-adapters) | MsgPack serializer |

### DROP (dead) — remove dep + adapt the code path
| Drop | Replacement / action |
|---|---|
| `org.fusesource:sigar:1.6.4` **and the `native/` directory** | Sigar is dead JNI and breaks on modern JDKs. Replace `SigarMonitor` with **OSHI** (`com.github.oshi:oshi-core:6.x`) or just let the monitor auto-fallback to the **JMX monitor** (the code already falls back Sigar→JMX→Constant). Easiest correct path: remove Sigar + the OSHI option, keep JMX/Constant. If you want CPU-based load balancing back, add OSHI. |
| `org.caffinitas.ohc:ohc-core-j8:0.6.1` | abandoned. Drop the OHC off-heap cacher or reimplement on **Caffeine** (`com.github.ben-manes.caffeine:caffeine:3.x`). `MemoryCacher` remains the default, so dropping OHC is low-risk. |
| `io.nats:java-nats-streaming:2.2.3` | NATS Streaming is EOL (→ JetStream). Drop the NATS-Streaming transporter (its tests are already excluded). |

## ⚠ javax → jakarta cluster
Spring 5→6 + Boot 2→3 forces it. Update imports across the affected components:
`javax.annotation.*` → `jakarta.annotation.*`; `javax.jms.*` → `jakarta.jms.*` (JMS transporter).
`javax.cache.*` stays (JCache has no jakarta rename). Recompile against Spring 6 / Boot 3 and fix
`MoleculerRunner` / `SpringRegistrator` for the new Spring APIs.

## Optional backends
Most transporter/cacher/serializer client libs are optional at runtime (the user adds the one they
use). In Maven, mark them **`<optional>true</optional>`** (lettuce, jnats, paho, jakarta.jms+ActiveMQ,
amqp-client, kafka-clients, the off-heap cache lib, the micrometer registries, dropwizard metrics).
Keep `datatree-adapters`, `datatree-promise`, `slf4j-api`, jackson serializer formats, and spring-context
as normal `compile` deps (they're needed for the default broker to run).

## Steps
1. **`pom.xml`** (metadata + MIT license + `release=21`). Import `jackson-bom` in
   `<dependencyManagement>`. Apply the four dependency tables above. Build plugins: compiler 3.14.0,
   surefire 3.5.3 (replicate the test excludes — step 4), (release profile) sources/javadoc/gpg +
   central-publishing 0.9.0.
2. **Remove ECJ** → javac. The old build excluded `**/moleculer/logger/**` from coverage — ignore
   (JaCoCo optional).
3. **Apply dependency upgrades / re-coordinates / drops** and fix the code: `SigarMonitor` (drop or
   OSHI), OHC cacher (drop/Caffeine), Redis transporter+cacher (lettuce 4→6 API), JCDP→JColor
   import+API, Ion/Dropwizard/msgpack re-coordinates, NATS-Streaming removal, Spring 6 / Boot 3
   (`javax`→`jakarta`), JMS transporter (jakarta.jms).
4. **Tests → JUnit 5.** Tests currently extend JUnit 3 `junit.framework.TestCase` *and* use `@Test`;
   convert to Jupiter (`@Test`, `Assertions.*`, `setUp`→`@BeforeEach`). Keep the broker→createService
   →`call(...).waitFor(timeout)` pattern. Replicate the old surefire excludes (infra-dependent tests):
   `**/KafkaTransporterTest`, `**/JmsTransporterTest`, `**/FileSystemTransporterTest`,
   `**/NatsStreamingTransporterTest`, `**/KafkaStreamTest`, `**/JmsStreamTest`,
   `**/NatsStreamingStreamTest`, `**/TransporterTestSuite`. Also set
   `systemPropertyVariables` `java.util.logging.SimpleFormatter.format` as before. Note: other
   integration tests (Redis/AMQP/MQTT/NATS/TCP) need a broker and will fail offline — run targeted
   tests or `@Disabled`/`@Tag("integration")` them so `mvn test` is green offline. Update
   `breaker/TestTransporter` and `stream/WrongOrderTransporter` fakes if transporter APIs changed.
   `com.openpojo:openpojo` test dep → **0.9.1** (🔒 lockstep — locked by datatree-templates).
5. **Version → 2.0.0** in `pom.xml` **and** `ServiceBroker.SOFTWARE_VERSION`.
6. **Cleanup — delete:** `build.gradle`, `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/`,
   `.gradle/`, `.codacy.yaml`, `old-travis-config.yml`, `.classpath`, `.project`, `.settings/`, and
   (if Sigar dropped) `native/`. Keep `.git/`, `LICENSE`, `README.md`, `CLAUDE.md`, `src/`.
7. **VSCode + .gitignore.** Add `.vscode/launch.json` for the runnable entry point
   `services.moleculer.config.MoleculerRunner` (it boots a broker from a Spring context).
8. **Build & install:** `mvn clean install`, then `mvn clean verify`.
9. **Update `CLAUDE.md`:** Maven commands; the version now lives in 2 places (pom + SOFTWARE_VERSION,
   not 3); list dropped backends (Sigar/OHC/NATS-Streaming) and the lettuce/JCDP/ion/metrics changes.

## Definition of done
- `mvn clean verify` green on JDK 21 (offline-safe unit tests; infra tests excluded/disabled).
- Spring 6 / Boot 3 with `javax`→`jakarta` resolved; lettuce 6, JColor, com.amazon ion, dropwizard
  4.2, msgpack-core building; Sigar/OHC/NATS-Streaming dropped.
- Optional backends marked `<optional>true</optional>`; jackson unified via BOM.
- JUnit 5; legacy files (+`native/` if Sigar gone) removed; VSCode + .gitignore.
- Version `2.0.0` in pom **and** `ServiceBroker.SOFTWARE_VERSION`; deps on `datatree-adapters:2.0.0`
  + `datatree-promise:2.0.0`; installed to local `~/.m2`; publishing configured.
