# BSToolbox — common-java-utils

Universal Java utility library. Compatible with JDK 8+.

## Installation

```xml
<dependency>
    <groupId>cz.bliksoft.java</groupId>
    <artifactId>common-java-utils</artifactId>
    <version>0.6</version>
</dependency>
```

Most heavyweight features are `optional` — the library compiles without them and you add only what you need:

| Feature | Add to your POM |
|---|---|
| Freemarker templating | `dependency-management-8-freemarker` BOM |
| Log4j 2 | `dependency-management-8-log4j` BOM |
| Excel / Word (OOXML) | `dependency-management-8-ooxml` BOM |
| JAXB | `dependency-management-8-jaxb` BOM |
| WS interface + JAXB | `dependency-management-8-servicedef` BOM |
| WS client | `dependency-management-8-client` BOM |
| WS server | `dependency-management-8-service` BOM |
| QR code generation | `com.google.zxing:core` |
| mDNS/Bonjour announcement | `org.jmdns:jmdns` |

## Documentation

Detailed guides in [`doc/`](doc/):
[app](doc/app.md) ·
[context](doc/context.md) ·
[modules](doc/modules.md) ·
[xml-filesystem](doc/xml-filesystem.md) ·
[services](doc/services.md) ·
[freemarker](doc/freemarker.md) ·
[database](doc/database.md) ·
[math](doc/math.md) ·
[ws](doc/ws.md) ·
[classloader](doc/classloader.md) ·
[environment-utils](doc/environment-utils.md) ·
[image-utils](doc/image-utils.md)

---

## Context Framework (`cz.bliksoft.javautils.context`)

Hierarchical, event-driven state container. Values are stored in a tree of `Context` nodes and observed via typed listeners. Changes propagate up the tree automatically.

### Core classes

| Class | Purpose |
|---|---|
| `Context` | Tree node; stores values by type (`addValue`) or key (`put`). Static `getRoot()` / `getCurrentContext()` for global access. |
| `SingleContext<T>` | Leaf node holding one typed value; bindable to a `JList` or `JTree` selection. |
| `EmptyContext` | Plain container node with no value storage. |
| `AbstractContextListener<T>` | Base for observers; implement `fired(ContextChangedEvent<T>)`. Supports enable/disable and level-crossing limits. |
| `ContextBoundary` | Listener that blocks event propagation past its attachment point. |

### Context holders

Holders are special `Context` nodes that manage which child context is currently "active". All three extend `SingleContextHolder` and delegate value reads to the active child.

**`SingleContextHolder`** — holds at most one child; the child can be replaced atomically.
```java
SingleContextHolder holder = new SingleContextHolder("my holder");
holder.replaceContext(myContext);
Context active = holder.getContext();
```

**`StackedContextHolder`** — push/pop stack; the top entry is always the active child.
```java
StackedContextHolder stack = new StackedContextHolder("screens");
stack.push(loginContext);
stack.push(dashboardContext); // dashboard is now active
stack.pop();                  // back to login
```

**`MapContextHolder<T, C>`** — named map of contexts; `select(key)` makes one active.
```java
MapContextHolder<String, EmptyContext> holder = new MapContextHolder<>("tabs");
holder.put("home",     new EmptyContext("home tab"));
holder.put("settings", new EmptyContext("settings tab"));
holder.select("home");                    // home is now active
String current = holder.getSelectedKey(); // "home"
holder.deselect();
```

All three holders include inactive entries in `dump()` output for debugging.

### Listening to value changes

```java
AbstractContextListener<MyService> listener = new AbstractContextListener<MyService>(MyService.class, "svc watcher") {
    @Override
    public void fired(ContextChangedEvent<MyService> event) {
        MyService svc = event.getNewValue(); // null if removed
    }
};
context.addContextListener(listener);
```

### Firing events

```java
context.fireEvent(new MyEvent());     // any thread
context.fireGUIEvent(new MyEvent());  // enforces EDT
```

---

## Module / Plugin System (`cz.bliksoft.javautils.modules`)

SPI-based plugin loader. Modules are discovered via `java.util.ServiceLoader`.

**Implement `IModule`** (or extend `ModuleBase`):
```java
public class MyModule extends ModuleBase {
    @Override public void init()    { /* called first  */ }
    @Override public void install() { /* called second */ }
    @Override public void cleanup() { /* on shutdown   */ }
}
```

Register in `META-INF/services/cz.bliksoft.javautils.modules.IModule`.

**Loading lifecycle:**
```java
Modules.loadModules();    // discover and instantiate
Modules.initModules();    // call init() on each
Modules.installModules(); // call install() on each
```

Modules can contribute an XML virtual filesystem descriptor via `getFilesystemXml()` and control load order via `getModuleLoadingOrder()`.

---

## Application Framework (`cz.bliksoft.javautils.app`)

`BSApp` builds an application skeleton on top of the modules framework: lifecycle (`init()` → `start()`/`startConsole()`, vetoable shutdown via `TryCloseEvent`), layered XML properties (global `{workingDir}/.{appName}/settings.xml` + local `~/.{appName}/settings.xml`, with per-environment key prefixes), an application event-dispatch thread (`executeLater`), and a pluggable permission/session model (`Permissions`, `SessionManager`, `UserInfo`). Requires Log4j 2.

```java
BSApp.setAppName("myapp");
BSApp.init();
BSApp.startConsole(); // blocks; 'q' + Enter quits
```

See [`doc/app.md`](doc/app.md) for the full reference.

---

## XML Virtual Filesystem (`cz.bliksoft.javautils.xmlfilesystem`)

Modules contribute XML descriptors that are merged at runtime into a single virtual `FileSystem` of `FileObject` nodes — used for configuration, class wiring, and translations.

```java
FileSystem.getDefault().importXml(myModule.getFilesystemXml(), "MyModule");
FileSystem.loadTranslations(); // once, after all modules are loaded

FileObject config = FileSystem.getFile("config/database");
String url = config.getAttribute("url", "jdbc:default");
```

Descriptors are plain XML (`META-INF/XmlFilesystem.xsd`):
```xml
<root xmlns="http://bliksoft.cz/XmlFilesystem">
  <file name="config" type="folder">
    <file name="database" type="dbConfig">
      <attribute name="url" value="${DB_URL}"/>
    </file>
  </file>
  <include path="/etc/optional-extra.xml"/>
  <require path="/etc/mandatory.xml"/>
  <symlink name="db" path="config/database"/>
</root>
```

**Writable nodes:** adding `mode="rw"` to a top-level `<include>`/`<require>` loads its `<file>`/`<symlink>` roots as `WritableFileObject`s, which support `setAttribute`, `addChild`, `getCreateFile`, etc., and can be persisted back to their source file with `save()`.

See [`doc/xml-filesystem.md`](doc/xml-filesystem.md) for the full reference, including localization and the writable-filesystem extension.

---

## Freemarker Templating (`cz.bliksoft.javautils.freemarker`)

`FreemarkerGenerator` wraps Apache Freemarker with a set of built-in extensions and a consistent API.

```java
// from classpath (relative to MyClass)
FreemarkerGenerator gen = new FreemarkerGenerator(MyClass.class);
String result = gen.generate("report.ftl", dataObject); // data exposed as ${data}

// to file
gen.generate("report.ftl", dataObject, new File("output.html"));

// inject extra variables
gen.setVariable("title", "My Report");
```

**Selected built-in template variables** (always available):

| Variable | What it does |
|---|---|
| `regroup` | Groups a `List<Map>` by key columns into a nested `Map` |
| `reindex` | Like `regroup` but assumes unique keys (last level is the row directly) |
| `code128` / `code128width` | Code128 barcode SVG encoding and width calculation |
| `Base64QR` | QR code as Base64 image (requires ZXing) |
| `prettyXML` / `parseXML` | Pretty-print or parse XML strings |
| `formatAsHTML` / `TXTTOHTML` | HTML-escape plain text |
| `StringBuilder` | Capture a template block into a string variable |
| `variableCache` | In-template key-value store (`set`/`get`/`add`/`put`) |
| `GUIPrompt` / `CMDPrompt` | Prompt user for input (Swing dialog or stdin) |

**SQL queries in templates** (add manually):
```java
gen.addExtension("Query", new Query(connectionProvider));
```
Then in the template: `<#assign rows = Query("SELECT ...")>`.

**Custom type wrappers:**
```java
// before the first FreemarkerGenerator is created:
ObjectWrapperRegister.addConverter(MyType.class, obj -> obj.toString());
```

---

## Database Utilities (`cz.bliksoft.javautils.database`)

```java
// Implement IDBConnectionProvider and register it:
DBConnectionProvidersRegister.register("mydb", myProvider);

// Retrieve:
IDBConnectionProvider p = DBConnectionProvidersRegister.get("mydb");
try (Connection c = p.getConnection()) { ... }
```

Built-in implementations: `MySQLConnection`, `MariaDbConnection`, `OracleDbConnection` (require the respective JDBC driver on the classpath). See [`doc/database.md`](doc/database.md).

---

## Network / HTTP (`cz.bliksoft.javautils.net`)

Lightweight embedded HTTP server and handler helpers:

- **`BSHttpServer`** — embeddable HTTP(S) server (`com.sun.net.httpserver` wrapper); handlers attachable at runtime, optional thread pool, TLS/mutual TLS from code or XML config, mDNS announcement via `registerMdnsService(name)`. Can be shared app-wide as a singleton — see [`doc/services.md`](doc/services.md).
- **`MdnsRegistrar`** — announces services on the LAN via mDNS/Bonjour (requires `org.jmdns:jmdns`).
- **`DefaultFileHTTPHandler`** — serves files from a directory.
- **`DefaultResourceHTTPHandler`** — serves classpath resources.
- **`SystemReportHTTPHandler`** — exposes a simple system-info endpoint.
- **`MultiPart`** — builds multipart/form-data request bodies.
- **`IPUtils`** — local IP address discovery.

---

## XML Utilities (`cz.bliksoft.javautils.xml`)

**JAXB date/time adapters** (reference in `bindings.xml`):

| Adapter | Java type |
|---|---|
| `LocalDateAdapter` | `LocalDate` |
| `LocalDateTimeAdapter` | `LocalDateTime` |
| `LocalTimeAdapter` | `LocalTime` |
| `OffsetDateTimeAdapter` | `OffsetDateTime` |
| `ZonedDateTimeAdapter` | `ZonedDateTime` |
| `BigDecimalAdapter` | `BigDecimal` |
| `SimpleObjectMapAdapter` | `Map<String,Object>` as typed `<integer/string/boolean/float/local-date/…>` elements with `name`/`value` attributes; custom types via `registerTypeHandler` |

**XPath extensions** (`cz.bliksoft.javautils.xml.xpath`) — custom functions usable in XPath expressions. Register once with `XmlUtils.registerXPathExtensions()` (binds namespace prefix `bsExt` → `http://bliksoft.cz`; overloads accept a custom prefix or namespace context), then compile expressions via `XmlUtils.compileXPath(...)`:

```java
XmlUtils.registerXPathExtensions();
String s = XmlUtils.getResultText(
    XmlUtils.compileXPath("bsExt:formatDate(bsExt:now(), 'yyyy-MM-dd')").evaluate(doc));
```

| Function | Purpose |
|---|---|
| `choose(cond, ifTrue, ifFalse)` | Ternary operator |
| `ifElseIf(cond1, res1 [, cond2, res2, …][, elseRes])` | Chained conditions; odd trailing argument is the else value |
| `map(input, val1, res1 [, val2, res2, …][, default])` | Value-to-value mapping; even trailing argument is the default |
| `default(value, fallback)` | Fallback when the value is empty or missing |
| `first(a, b, …)` | First non-empty argument as text |
| `join(separator, a, b, …)` | Joins non-empty text values with a separator |
| `format(pattern, args…)` | `MessageFormat`-style formatting |
| `sprintf(format, args…)` | `String.format`-style formatting |
| `formatNumber(value, pattern)` | `DecimalFormat` number formatting |
| `formatDate(value, pattern)` | `DateTimeFormatter` formatting; accepts temporal objects, `Date`, epoch millis, or ISO strings |
| `now()` | Current time as epoch milliseconds (feed into `formatDate`) |
| `uuid()` | Random UUID string |
| `log(...)` | Logs a message and passes a value through |
| `var(namespace, name)` | Reads a value from the static `XPathVarCache` (set from Java) |

---

## General Utilities

| Class | Highlights |
|---|---|
| `StringUtils` | `hasText`, `format` (MessageFormat shorthand), `ellipsis` |
| `NumericUtils` | Numeric parsing and conversion helpers |
| `BooleanUtils` | Lenient `toBoolean(Object)` coercion (numbers, strings, null-safe) |
| `DateUtils` | Date/time formatting and parsing |
| `GeneralUtils` | Miscellaneous object utilities |
| `ClasspathUtils` | Classpath resource loading |
| `Base64Utils` | Base64 encode/decode |
| `CryptUtils` | AES / hash helpers |
| `SystemUtils` | OS and JVM info |
| `PropertiesUtils` | Properties file loading |
| `TimestampedObject<T>` | Value wrapper with a `LocalDateTime` timestamp |
| `LimitedList<T>` | `ArrayList` capped at a configurable maximum size |
| `HashUUIDCreator` | Deterministic UUID from arbitrary input |

---

## License

[GNU Lesser General Public License v2.1](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html)
