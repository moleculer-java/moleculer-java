# [WIP] Moleculer for Java

[![Build Status](https://travis-ci.org/moleculer-java/moleculer-java.svg?branch=master)](https://travis-ci.org/moleculer-java/moleculer-java)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/03f8b9251bde406a9794d7e255859c8a)](https://www.codacy.com/app/mereg-norbert/moleculer-java?utm_source=github.com&utm_medium=referral&utm_content=berkesa/moleculer-java&utm_campaign=badger)

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
