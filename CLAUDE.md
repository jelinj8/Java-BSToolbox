# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Universal Java utility library (`cz.bliksoft.java:common-java-utils:0.6-SNAPSHOT`). Must stay compatible with JDK 8. No new hard dependencies; avoid new `provided` dependencies too.

## Build Commands

```bash
mvn clean compile -DskipTests   # compile only
mvn test                         # run tests
mvn package                      # build JAR (deps copied to target/lib/)
mvn test -Dtest=ClassName        # run a single test class
```

## Architecture

### Context Framework (`cz.bliksoft.javautils.context`)

Hierarchical, event-driven state management:

- **`Context`** — tree structure with parent/child relationships; stores values by type (`listValues`) or by key (`mapValues`). Has a static root and thread-local current context via `SingleContextHolder`.
- **`AbstractContextListener<T>`** — observes value changes in a context; tracks old/new values and level (how many levels up the value came from).
- **`EventListener<T>`** — callback base with `beforeEvent()`, `fired()`, `afterEvent()`; handles EDT safety and consumable events (`IConsumableEvent`).
- **`ContextChangedEvent<T>`** — event fired on value mutation; can block upward propagation.
- **`SingleContextHolder`** / **`StackedContextHolder`** — manage the "current" context (switchable vs push/pop stack).

### Modules Framework (`cz.bliksoft.javautils.modules`)

Plugin system using Java SPI (`ServiceLoader`):

- **`IModule`** — interface with lifecycle: `init()` → `install()` → `cleanup()`. Also provides `getFilesystemXml()` for virtual file contributions and `getModuleLoadingOrder()` for priority.
- **`ModuleBase`** — convenient abstract base; auto-discovers `{ClassName}.xml` in its package and integrates git version info.
- **`Modules`** — static registry; loading phases: `loadModules()` → `initModules()` → `installModules()`. Supports enable/disable by class name (`"*"` enables all). Always loads `cz.bliksoft.javautils.app.BaseAppModule`.

### XML Virtual Filesystem (`cz.bliksoft.javautils.xmlfilesystem`)

Modules contribute XML descriptors that are merged into a virtual `FileSystem` of `FileObject` nodes. Classes implementing `IInitializeWithFileObject` are wired automatically during module loading.

### Freemarker (`cz.bliksoft.javautils.freemarker`)

**`FreemarkerGenerator`** is the main entry point. Constructors accept a `File` (filesystem templates), `Class<?>` (classpath-relative), one or more `TemplateLoader`s, or a pre-built `Configuration`. A static `defaultTemplateLoader` can be set once and used by the no-arg constructor.

`generate(String templateName, Object data)` → returns `String`; `data` is exposed as `data` in the template. Overloads write to `File` or `OutputStream`. `setVariable(name, value)` / `setVariables(Map|Properties)` injects additional root variables into every render.

Extensions are split into two maps:
- **`globalExtensions`** — static, shared by all instances; can be modified via `addGlobalExtension` / `removeGlobalExtension`. Safe to expose to external Freemarker configs (e.g. Spring Boot shared variables).
- **`localExtensions`** — per-instance; modified via `addExtension` / `addExtensions`.

**Global extensions** (template variable → what it does):

| Variable | Class | Purpose |
|---|---|---|
| `formatAsHTML` | `HtmlPreformat` | Escapes HTML entities in plain text |
| `identifyObjectType` | `IdentifyObjectType` | Returns class name of a template variable |
| `imgRes` | `ImageResource` | Loads image from classpath resources |
| `code128` | `Code128Encode` | Encodes string as Code128 barcode SVG/data |
| `code128width` | `Code128Width` | Calculates Code128 barcode width |
| `regroup` | `Regroup` | Groups `List<Map>` by key columns → nested `Map` (last level is a `List`) |
| `reindex` | `Reindex` | Like `regroup` but last level is the row `Map` directly (unique key assumed) |
| `prettyXML` | `PrettyPrintXml` | Pretty-prints an XML string |
| `parseXML` | `ParseXml` | Parses XML string into Freemarker DOM node |
| `GUIPrompt` | `GUIPrompt` | Shows Swing dialog to prompt user for input |
| `CMDPrompt` | `CMDPrompt` | Reads user input from stdin; supports option lists |
| `CMDWrite` | `CMDWrite` | Writes text to stdout |
| `Base64File` | `Base64File` | Encodes a file's bytes as Base64 string |
| `Base64QR` | `Base64QR` | Generates QR code image and returns as Base64 (needs ZXing) |
| `LogVariable` | `LogVariable` | Logs a variable's value |
| `LogMessage` | `LogMessage` | Logs a literal message |
| `SystemMessage` | `SystemMessage` | Shows a system message (log or dialog) |
| `DescribeVariable` | `DescribeVariable` | Dumps full description of a variable |
| `StringBuilder` | `StringBuilderDirective` | Captures a template block into a `StringWriter`; also a method returning a new `StringWriter` |
| `ltrim` / `rtrim` | `Trim` | Left / right trims whitespace from a string |
| `TXTTOHTML` | `TextReplacer` | Escapes `& < > " '` and converts `\n` → `<br>` |
| `TXTTOHTML_WHITESPACE` | `TextReplacer` | Same plus space → `&nbsp;` and tab → `&nbsp;&nbsp;&nbsp;` |

**Local extensions** (per-instance, added by default):

| Variable | Class | Purpose |
|---|---|---|
| `registerVariable` | `VariableRegistrator` | Registers a variable on the generator for *subsequent* renders (not current); useful in SQL query templates |
| `anchorNumberer` | `AnchorNumberer` | Auto-incrementing counter for anchors/headings |
| `variableCache` | `VariableCache` | Dynamic in-template storage: `set`, `get`, `add` (list), `put` (map), `remove`, `clear` |

**Extensions that must be added manually** (not registered by default):

- **`Query`** — executes a SQL query via `IDBConnectionProvider`; returns `List<Map<String,Object>>` and sets `LastQuery` map with `columns`, `columnTypes`, `SQL`, `parameters`, `resultCount`. Use with `QueryListArgs` to format IN-clause parameter lists.
- **`RegexMatcher`** — apply a compiled regex to strings; returns list of match maps with group values; supports named groups via `addGroup(name, index)`.
- **`ListCollector`** — accumulates values into a `List<Object>` during template execution; read back from Java after render.
- **`MapCollector`** — accumulates key-value pairs into a `Map<String,String>` during template execution.
- **`StringCollector`** — accumulates string fragments.
- **`LocalDateTimeFormatter`** — formats `LocalDateTime` with a given `DateTimeFormatter` pattern (requires `freemarker-java8` library on classpath).

**`ObjectWrapperRegister`** (wrappers): singleton `DefaultObjectWrapper` used by all `FreemarkerGenerator` instances. Built-in conversions: `Optional<T>` → unwrapped value or null; `File` → path string; `TimestampedObject` → map with `millis`, `timestamp` (LocalDateTime), `value`. Register additional type converters with `ObjectWrapperRegister.addConverter(Class, Function)` before the singleton is created. If `no.api.freemarker.java8.Java8ObjectWrapper` is on the classpath it is loaded reflectively to handle Java 8 time types. Call `useToString()` to enable a last-resort `toString()` fallback for unknown types.

**`BuiltinTemplateLoader`** (includes): exposes built-in templates bundled in the library. Use `BuiltinTemplateLoader.getTemplateLoader(yourLoader)` to create a `MultiTemplateLoader` where your loader takes precedence; or pass `getBuiltinTemplateLoader()` directly.

Other generator options: `disableLocalizedTemplateLookup()`, `storeEnvironment()` + `getLastEnvironment()` for post-render environment inspection, `setNumberFormat(NumberFormats)`, `useJaxenXPathSupport()` for XPath in XML templates.

### Optional Feature Dependencies

These are `optional` in `pom.xml`; callers must provide their own dependency:

| Feature | Dependency to add |
|---|---|
| Freemarker templates | `dependency-management-8-freemarker` BOM |
| QR code generation | `com.google.zxing:core` |
| Excel/Word (OOXML) | `dependency-management-8-ooxml` BOM |
| Log4j 2 | `dependency-management-8-log4j` BOM |
| JAXB | `dependency-management-8-jaxb` BOM |
| WS interface/JAXB | `dependency-management-8-servicedef` BOM |
| WS client | `dependency-management-8-client` BOM |
| WS server | `dependency-management-8-service` BOM |

For JAXB/WS POJO mappings use `LocalDateAdapter` and `OffsetDateTimeAdapter` in `cz.bliksoft.javautils.xml.adapters` via a `bindings.xml`.

## Testing

One test exists: `UnusedMessagesKeysTest` — validates that all keys in message `.properties` files are actually used in source code. Add new message keys carefully; unused ones will fail this test.
