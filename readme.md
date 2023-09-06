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
to your classpath.

Optionally for Java 8 Date+Time API extensions add

```
<!-- https://mvnrepository.com/artifact/no.api.freemarker/freemarker-java8 -->
<dependency>
	<groupId>no.api.freemarker</groupId>
	<artifactId>freemarker-java8</artifactId>
	<version>2.0.0</version>
</dependency>
```

**ZXing for QR code generation**

If you want to use QR generator, you'll need to provide ZXing library, e.g.:


```
<!-- https://mvnrepository.com/artifact/com.google.zxing/core -->
<dependency>
	<groupId>com.google.zxing</groupId>
	<artifactId>core</artifactId>
	<version>3.4.0</version>
</dependency>
```

**JAXB**


```
<dependency>
	<groupId>jakarta.annotation</groupId>
	<artifactId>jakarta.annotation-api</artifactId>
	<version>2.1.1</version>
</dependency>

<dependency>
	<groupId>jakarta.xml.bind</groupId>
	<artifactId>jakarta.xml.bind-api</artifactId>
	<version>3.0.1</version>
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

**To use webservices add to client/server**

```
<dependency>
	<groupId>org.glassfish.metro</groupId>
	<artifactId>webservices-rt</artifactId>
	<version>3.0.3</version>
</dependency>
<dependency>
	<groupId>jakarta.xml.ws</groupId>
	<artifactId>jakarta.xml.ws-api</artifactId>
	<version>3.0.1</version>
</dependency>
```

WS server will need also

```
<dependency>
	<groupId>jakarta.servlet</groupId>
	<artifactId>jakarta.servlet-api</artifactId>
	<version>5.0.0</version>
</dependency>
```

For POJO mappings use bindings.xml:

```
<jaxb:bindings version="3.0"
	xmlns:jaxb="https://jakarta.ee/xml/ns/jaxb"
	xmlns:xjc="http://java.sun.com/xml/ns/jaxb/xjc"
	xmlns:s="http://www.w3.org/2001/XMLSchema"
	>
	<jaxb:bindings>
		<jaxb:globalBindings
			generateElementProperty="false">
			<xjc:javaType
				adapter="cz.bliksoft.javautils.xml.adapters.LocalDateAdapter"
				name="java.time.LocalDate" xmlType="s:date" />
			<xjc:javaType
				adapter="cz.bliksoft.javautils.xml.adapters.OffsetDateTimeAdapter"
				name="java.time.OffsetDateTime" xmlType="s:dateTime" />
		</jaxb:globalBindings>
	</jaxb:bindings>
</jaxb:bindings>
```
