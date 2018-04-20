# [WIP] Moleculer for Java

[![Build Status](https://travis-ci.org/moleculer-java/moleculer-java.svg?branch=master)](https://travis-ci.org/moleculer-java/moleculer-java)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b26c4ff30c6b4cb4a5536b5c1de0c317)](https://www.codacy.com/app/berkesa/moleculer-java?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=moleculer-java/moleculer-java&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/moleculer-java/moleculer-java/branch/master/graph/badge.svg)](https://codecov.io/gh/moleculer-java/moleculer-java)

Java implementation of the [Moleculer microservices framework](http://moleculer.services/).

## For prototyping

**Maven**

```xml
<dependencies>
	<dependency>
		<groupId>com.github.berkesa</groupId>
		<artifactId>moleculer-java</artifactId>
		<version>1.0.3-SNAPSHOT</version>
		<scope>runtime</scope>
	</dependency>
</dependencies>

<repositories>
	<repository>
		<id>snapshot</id>
		<name>Sonatype Snapshots</name>
		<url>https://oss.sonatype.org/content/repositories/snapshots</url>
		<layout>default</layout>
		<snapshots>
			<enabled>true</enabled>
		</snapshots>
	</repository>
</repositories>	
```

**Gradle**

```gradle
repositories {
    maven { url "https://oss.sonatype.org/content/repositories/snapshots" }
}

dependencies {
	compile group: 'com.github.berkesa', name: 'moleculer-java', version: '1.0.3-SNAPSHOT' 
}
```

# License
moleculer-java is available under the [MIT license](https://tldrlegal.com/license/mit-license).
