# Freemarker

Package: `cz.bliksoft.javautils.freemarker`

A wrapper around Freemarker 2.3.x that adds a library of built-in template extensions, a pluggable object wrapper, and a shared template loader facility.

## `FreemarkerGenerator`

Main entry point. Create one instance per template root; reuse it for multiple renders.

### Constructors

| Constructor | Template source |
|---|---|
| `FreemarkerGenerator()` | Uses the globally configured `defaultTemplateLoader` |
| `FreemarkerGenerator(Configuration config)` | Pre-built Freemarker `Configuration` |
| `FreemarkerGenerator(TemplateLoader loader)` | Single loader |
| `FreemarkerGenerator(TemplateLoader... loaders)` | Multiple loaders wrapped in a `MultiTemplateLoader` |
| `FreemarkerGenerator(File basePath)` | Filesystem directory |
| `FreemarkerGenerator(Class<?> templateLoaderClass)` | Classpath relative to the given class |
| `FreemarkerGenerator(File templatesBasePath, Class<?> templateLoaderClass)` | Filesystem + classpath fallback |

### Configuring the global default loader

```java
FreemarkerGenerator.setDefaultTemplateLoader(new File("templates/"));
FreemarkerGenerator.setDefaultTemplateLoader(MyApp.class);
FreemarkerGenerator.setDefaultTemplateLoader(myLoader);
```

Must be set before the first no-arg constructor call.

### Generating output

```java
String html  = gen.generate("report.ftl", dataObject);
gen.generateToFile("report.ftl", dataObject, new File("out/report.html"));
gen.generateToStream(template, dataObject, outputStream);
```

The `data` argument is available in the template as the `data` variable.

### Additional root variables

```java
gen.setVariable("title", "My Report");
gen.setVariables(propertiesOrMap);
```

These are available in every render alongside `data`.

### Other options

| Method | Description |
|---|---|
| `disableLocalizedTemplateLookup()` | Disable `.locale`-variant template resolution |
| `storeEnvironment()` / `getLastEnvironment()` | Retain the Freemarker `Environment` after the last render for post-processing |
| `getConfiguration()` | Access the underlying `Configuration` |
| `setNumberFormat(NumberFormats)` | Override the default number format |
| `useJaxenXPathSupport()` | Enable Jaxen-based XPath in XML templates |

---

## Global extensions

Registered automatically in every `FreemarkerGenerator` instance. Available in all templates.

| Template variable | Class | Description |
|---|---|---|
| `formatAsHTML` | `HtmlPreformat` | Escapes HTML entities in plain text |
| `identifyObjectType` | `IdentifyObjectType` | Returns the class name of a template variable |
| `imgRes` | `ImageResource` | Loads an image from classpath resources |
| `code128` | `Code128Encode` | Encodes a string as a Code128 barcode SVG/data |
| `code128width` | `Code128Width` | Calculates Code128 barcode width |
| `regroup` | `Regroup` | Groups `List<Map>` by key columns → nested `Map`; last level is a `List` |
| `reindex` | `Reindex` | Like `regroup` but last level is the row `Map` directly (assumes unique key) |
| `prettyXML` | `PrettyPrintXml` | Pretty-prints an XML string |
| `parseXML` | `ParseXml` | Parses an XML string into a Freemarker DOM node |
| `GUIPrompt` | `GUIPrompt` | Shows a Swing dialog to prompt the user for input |
| `CMDPrompt` | `CMDPrompt` | Reads input from stdin; supports option lists |
| `CMDWrite` | `CMDWrite` | Writes text to stdout |
| `Base64File` | `Base64File` | Encodes a file's bytes as a Base64 string |
| `Base64QR` | `Base64QR` | Generates a QR code image and returns it as Base64 (requires ZXing) |
| `Base64IconSpec` | `Base64IconSpec` | Resolves an icon-spec string (see [`IconSpecEngine`](image-utils.md#iconspecengine)) to a PNG and returns it as Base64 — no JavaFX required |
| `LogVariable` | `LogVariable` | Logs a variable's value |
| `LogMessage` | `LogMessage` | Logs a literal message |
| `SystemMessage` | `SystemMessage` | Shows a system message (log or dialog) |
| `DescribeVariable` | `DescribeVariable` | Dumps a full description of a variable |
| `StringBuilder` | `StringBuilderDirective` | Captures a template block into a `StringWriter`; also a method returning a new `StringWriter` |
| `ltrim` / `rtrim` | `Trim` | Left / right trims whitespace from a string |
| `TXTTOHTML` | `TextReplacer` | Escapes `& < > " '` and converts `\n` → `<br>` |
| `TXTTOHTML_WHITESPACE` | `TextReplacer` | Same plus space → `&nbsp;` and tab → `&nbsp;&nbsp;&nbsp;` |

Modify the global set:

```java
FreemarkerGenerator.addGlobalExtension("myFunc", new MyTemplateMethod());
FreemarkerGenerator.removeGlobalExtension("myFunc");
```

These changes affect all generator instances, including those shared with Spring Boot or other frameworks.

---

## Local extensions

Registered per-instance. Available in every render of that instance.

| Template variable | Class | Description |
|---|---|---|
| `registerVariable` | `VariableRegistrator` | Registers a variable on the generator for *subsequent* renders (not the current one); useful in SQL query templates |
| `anchorNumberer` | `AnchorNumberer` | Auto-incrementing counter for anchors or headings |
| `variableCache` | `VariableCache` | Dynamic in-template storage: `set`, `get`, `add` (list append), `put` (map put), `remove`, `clear` |

Add per-instance extensions:

```java
gen.addExtension("myExt", new MyExtension());
gen.addExtensions(myExtensionMap);
```

---

## Extensions added manually

These are not registered by default; add them when needed.

### `Query`

Executes SQL queries from templates. Requires `IDBConnectionProvider` and `IQueryProvider`.

```java
Query query = new Query(connectionProvider, queryProvider);
gen.addExtension("query", query);
```

Template usage:
```ftl
<#assign rows = query("myQueryId", param1, param2)>
<#list rows as row>
  ${row.columnName}
</#list>
```

After each call the template variable `lastQuery` is set:

| Key | Value |
|---|---|
| `columns` | `List<String>` of column names |
| `columnTypes` | `List<String>` of SQL type names |
| `SQL` | The executed SQL string |
| `parameters` | `List` of parameter values |
| `resultCount` | Row count (non-iterable mode only) |

For large result sets construct the `Query` with `iterable = true`; the result implements `Iterator<Map<String,Object>>` and `Closeable`.

**`IQueryProvider`** supplies the SQL for each query ID:

| Method | Description |
|---|---|
| `createQuery(String queryID)` | Prepare the query |
| `String getSql(String queryID)` | Return the SQL string |
| `List<Integer> getArgumentTypes(String queryID)` | Return `java.sql.Types` constants for each parameter |

Concrete implementations: `FileQueryProvider` (loads from `FileObject`), `TemplatedQueryProvider` (queries are themselves Freemarker templates).

Use `QueryListArgs` to format SQL `IN`-clause parameter lists.

### `RegexMatcher`

```java
RegexMatcher matcher = new RegexMatcher("(\\d+)-(\\w+)");
matcher.addGroup("number", 1);
matcher.addGroup("word", 2);
gen.addExtension("matchItem", matcher);
```

Returns `List<Map>` per match with keys `match`, `groups` (list), and `namedGroups` (map, when groups are defined via `addGroup`).

### Collectors (read back from Java after render)

| Class | Method to read result | Description |
|---|---|---|
| `ListCollector` | `getValues()` → `List<Object>` | Accumulates values during template execution |
| `MapCollector` | `getValues()` → `Map<String,String>` | Accumulates key-value pairs |
| `StringCollector` | — | Accumulates string fragments |

```java
ListCollector collector = new ListCollector();
gen.addExtension("collect", collector);
gen.generate("template.ftl", data);
List<Object> results = collector.getValues();
```

### `LocalDateTimeFormatter`

Formats `LocalDateTime` values with a `DateTimeFormatter` pattern. Requires `freemarker-java8` on the classpath.

---

## Object wrapper

`ObjectWrapperRegister` holds a singleton `DefaultObjectWrapper` shared by all generator instances. Built-in conversions:

| Java type | Freemarker representation |
|---|---|
| `Optional<T>` | Unwrapped value or `null` |
| `File` | Path string |
| `TimestampedObject` | Map with keys `millis`, `timestamp` (LocalDateTime), `value` |

Register custom converters before the singleton is first created:

```java
ObjectWrapperRegister.addConverter(MyType.class, obj -> obj.toString());
```

Call `ObjectWrapperRegister.useToString()` to enable a last-resort `toString()` fallback for types without a registered converter.

If `no.api.freemarker.java8.Java8ObjectWrapper` is on the classpath it is loaded reflectively to handle `java.time` types.

---

## Built-in template loader

`BuiltinTemplateLoader` exposes a library of helper templates bundled in the JAR.

```java
// use built-in templates only
TemplateLoader builtins = BuiltinTemplateLoader.getBuiltinTemplateLoader();

// let application templates take precedence; fall back to built-ins
TemplateLoader combined = BuiltinTemplateLoader.getTemplateLoader(myLoader);
```
