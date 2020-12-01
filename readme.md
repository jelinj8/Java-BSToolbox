**Freemarker**
To use freemarker parts of this library, add
```
<!-- https://mvnrepository.com/artifact/org.freemarker/freemarker -->
<dependency>
	<groupId>org.freemarker</groupId>
	<artifactId>freemarker</artifactId>
	<version>2.3.30</version>
	<scope>compile</scope>
	<optional>true</optional>
</dependency>
```
to youyr classpath.

Optionally for Java 8 Date+Time API extensions add
```
<!-- https://mvnrepository.com/artifact/no.api.freemarker/freemarker-java8 -->
<dependency>
	<groupId>no.api.freemarker</groupId>
	<artifactId>freemarker-java8</artifactId>
	<version>2.0.0</version>
	<scope>compile</scope>
	<optional>true</optional>
</dependency>
```

**JAXB and Java 9+**

Current version is compatible with Java 1.8.
For compatibility with later versions of Java it is necessary to add this dependency for this library:
```
<!-- https://mvnrepository.com/artifact/javax.xml.bind/jaxb-api -->
<dependency>
	<groupId>javax.xml.bind</groupId>
	<artifactId>jaxb-api</artifactId>
	<version>2.3.1</version>
</dependency>
```

When you actually use JAXB, you will need also some implementation in runtime classpath:
```
<dependency>
	<groupId>org.glassfish.jaxb</groupId>
	<artifactId>jaxb-runtime</artifactId> 
	<version>2.3.1</version>
	<scope>runtime</scope>
</dependency>
```

or for EclipseLink MOXy implementation add a file *jaxb.properties* with ```javax.xml.bind.context.factory=org.eclipse.persistence.jaxb.JAXBContextFactory``` and dependency
```
<dependency> 
	<groupId>org.eclipse.persistence</groupId>
	<artifactId>org.eclipse.persistence.moxy</artifactId>
	<version>2.7.3</version>
	<scope>runtime</scope>
</dependency>
```