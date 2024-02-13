# common dependency base versions

to inherit all default versions import

```
<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>cz.bliksoft.java</groupId>
			<artifactId>dependency-management-8</artifactId>
			<version>1.0.0-SNAPSHOT</version>
			<type>pom</type>
			<scope>import</scope>
		</dependency>
	</dependencies>
</dependencyManagement>
```

# Freemarker

To use freemarker parts of this library, add

```
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-8-freemarker</artifactId>
	<type>pom</type>
</dependency>
```
to your classpath.

# ZXing for QR code generation

If you want to use QR generator, you'll need to provide ZXing library, e.g.:


```
<!-- https://mvnrepository.com/artifact/com.google.zxing/core -->
<dependency>
	<groupId>com.google.zxing</groupId>
	<artifactId>core</artifactId>
	<version>3.4.0</version>
</dependency>
```

# OOXML

Excel + word OpenDocuments

```
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-8-ooxml</artifactId>
	<type>pom</type>
</dependency>
```

# Log4J2

Optional, but reasonable to include

```
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-8-log4j</artifactId>
	<type>pom</type>
</dependency>
```

# JAXB

Generally use classes from jakarta namespace.

```
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-8-jaxb</artifactId>
	<type>pom</type>
</dependency>
```

 # WS interface

```
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-8-servicedef</artifactId>
	<type>pom</type>
</dependency>
```

Jaxb is already included in WS client / server.

## WS client

```
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-8-client</artifactId>
	<type>pom</type>
</dependency>
```

## WS server

```
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-8-service</artifactId>
	<type>pom</type>
</dependency>
```

For POJO mappings use at least a simple bindings.xml:

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
