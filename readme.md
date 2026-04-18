# BSToolbox

Universal Java utility library. JDK 8 compatible. All optional features are `optional`/`provided` — you bring only what you use.

## Dependency management BOM

To inherit managed dependency versions, import:

```xml
<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>cz.bliksoft.java</groupId>
			<artifactId>dependency-management</artifactId>
			<version>${bliksoft.version}</version>
			<type>pom</type>
			<scope>import</scope>
		</dependency>
	</dependencies>
</dependencyManagement>
```

## Required transitive dependencies

These are `provided` scope in BSToolbox and must be present at runtime:

```xml
<dependency>
	<groupId>commons-io</groupId>
	<artifactId>commons-io</artifactId>
</dependency>
<dependency>
	<groupId>commons-codec</groupId>
	<artifactId>commons-codec</artifactId>
</dependency>
```

## Freemarker

To use the `FreemarkerGenerator` and template extensions:

```xml
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-freemarker</artifactId>
	<type>pom</type>
</dependency>
```

For Java 8 time type support in templates (`LocalDate`, `LocalDateTime`, etc.), optionally add:

```xml
<dependency>
	<groupId>no.api.freemarker</groupId>
	<artifactId>freemarker-java8</artifactId>
</dependency>
```

## ZXing for QR code generation

Required for `Base64QR` Freemarker extension and `BarcodeGenerator`:

```xml
<dependency>
	<groupId>com.google.zxing</groupId>
	<artifactId>core</artifactId>
</dependency>
```

## OOXML

Excel + Word OpenDocuments:

```xml
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-ooxml</artifactId>
	<type>pom</type>
</dependency>
```

## Log4J2

Optional, but reasonable to include:

```xml
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-log4j</artifactId>
	<type>pom</type>
</dependency>
```

## JAXB

Generally use classes from the `jakarta` namespace:

```xml
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-jaxb</artifactId>
	<type>pom</type>
</dependency>
```

## HTTP client

Required for `BasicHttpClient` and proxy utilities (`cz.bliksoft.javautils.net.http`):

```xml
<dependency>
	<groupId>org.apache.httpcomponents.client5</groupId>
	<artifactId>httpclient5</artifactId>
</dependency>
```

## Jakarta Mail

Required for `cz.bliksoft.javautils.net.Mail`:

```xml
<dependency>
	<groupId>com.sun.mail</groupId>
	<artifactId>jakarta.mail</artifactId>
</dependency>
```

## JSON

Required for utilities using `org.json`:

```xml
<dependency>
	<groupId>org.json</groupId>
	<artifactId>json</artifactId>
</dependency>
```

## WS interface

```xml
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-servicedef</artifactId>
	<type>pom</type>
</dependency>
```

JAXB is already included in WS client / server.

### WS client

```xml
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-client</artifactId>
	<type>pom</type>
</dependency>
```

### WS server

```xml
<dependency>
	<groupId>cz.bliksoft.java</groupId>
	<artifactId>dependency-management-service</artifactId>
	<type>pom</type>
</dependency>
```

For POJO mappings use at least a simple `bindings.xml`:

```xml
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
