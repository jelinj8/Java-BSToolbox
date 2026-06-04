# Services and Singletons

Two complementary mechanisms instantiate and keep alive objects whose lifetime
matches the application lifetime. Both read from the XML filesystem and support
`Closeable` cleanup on shutdown.

---

## Services

**Class:** `cz.bliksoft.javautils.xmlfilesystem.singletons.Services`  
**XML path:** `services/`

`Services.loadServices()` walks every node under `services/`, instantiates each
one via `FileLoader`, and stores the result in a static map so it is never
garbage-collected. Call it once during application startup, after all module
descriptors have been merged into the filesystem:

```java
Services.loadServices();   // startup
// ...
Services.cleanup();        // shutdown — calls close() on any Closeable instances
```

### Declaring a service in XML

`Services.loadServices()` uses `FileLoader.loadFile()` to instantiate each node,
so the `type` attribute selects whichever `FileLoader` is registered for that type.
Any custom loader registered under `/fileLoaders/` in the filesystem can be used
here, not just `ClassFileLoader`.

The standard loader is `ClassFileLoader` (`type="Class"`). The `class` attribute
names the FQCN; every other attribute is forwarded as configuration to the
constructor.

```xml
<file name="services">
    <file name="Netum" type="Class">
        <attribute name="class"
            value="com.example.SerialScannerHandler" />
        <attribute name="com"       value="COM7" />
        <attribute name="baud"      value="115200" />
        <attribute name="delimiter" value="CRLF" />
        <!-- <attribute name="timegap" value="200" /> -->
    </file>
</file>
```

The file `name` (`"Netum"` above) is a human-readable label used in log messages;
it does not affect loading.

### Implementing a service class

`ClassFileLoader` tries two instantiation strategies in order:

**1. Constructor accepting `FileObject`** *(preferred)*

```java
public class SerialScannerHandler implements Closeable {

    public SerialScannerHandler(FileObject def) {
        String port      = def.getAttribute("com");
        int    baud      = def.getInt("baud", 9600);
        String delimiter = def.getAttribute("delimiter", "\r\n");
        long   timegap   = def.getLong("timegap", 0L);
        // initialise…
    }

    @Override
    public void close() {
        // release port, stop threads…
    }
}
```

**2. No-arg constructor + `IInitializeWithFileObject`**

```java
public class MyService implements IInitializeWithFileObject, Closeable {

    public MyService() { }

    @Override
    public void initializeWithFileObject(FileObject def) {
        // read attributes here
    }

    @Override
    public void close() { /* cleanup */ }
}
```

### `FileObject` attribute accessors

| Method | Returns |
|---|---|
| `getAttribute(name)` | `String`, or `null` if absent |
| `getAttribute(name, def)` | `String`, or `def` if absent |
| `getInt(name, def)` | `int` |
| `getLong(name, def)` | `long` |
| `getBool(name, def)` | `boolean` |
| `getDouble(name, def)` | `double` |

### Lifecycle

| Phase | What happens |
|---|---|
| `loadServices()` | All `services/` nodes instantiated eagerly; stored in a static map |
| Runtime | Objects remain alive; no framework start/stop beyond construction |
| `cleanup()` | `close()` called on every instance that implements `Closeable` |

`Services` does not expose a way to look up a loaded instance by type; services are
meant to self-register into whatever system they belong to (event bus, scheduler,
etc.) during construction.

---

## Singletons

**Class:** `cz.bliksoft.javautils.xmlfilesystem.singletons.Singletons`  
**XML path:** `singletons/` (default; overridable via `loadSingletons(path)`)

`Singletons` differs from `Services` in three ways:

- The `type` attribute on the XML node is the **FQCN** of the class — no separate
  `class` attribute, no `ClassFileLoader`.
- Instantiation is **lazy** — objects are created on first `getSingleton` /
  `getSingletons` call, not at load time.
- Instances are **retrieved by interface** rather than by name.

```java
// Startup — registers class metadata (no instantiation yet)
Singletons.loadSingletons();

// First call instantiates; subsequent calls return the cached instance
MyInterface obj = Singletons.getSingleton(MyInterface.class);

// Retrieve all registered implementations of an interface
List<MyInterface> all = Singletons.getSingletons(MyInterface.class);

// Shutdown
Singletons.cleanup();   // close() on any Closeable instances
```

### Declaring a singleton in XML

```xml
<file name="singletons">
    <file name="myImpl" type="com.example.MyServiceImpl" />
</file>
```

### Implementing a singleton class

`Singletons` uses only a **no-arg constructor** (unlike `Services`/`ClassFileLoader`,
there is no `FileObject` injection). Use `IInitializeWithFileObject` if you need
access to the XML node at instantiation time — but note that the standard
`Singletons` loader does not call it; configuration attributes are not forwarded.
For attribute-driven configuration, use `Services` instead.

```java
public class MyServiceImpl implements MyInterface {
    public MyServiceImpl() { /* … */ }
}
```

---

## Choosing between Services and Singletons

| | Services | Singletons |
|---|---|---|
| XML declaration | `type="Class"` + `class` attribute | `type` = FQCN |
| Configuration attributes | Forwarded to constructor / `IInitializeWithFileObject` | Not forwarded |
| Instantiation | Eager (`loadServices()`) | Lazy (first `getSingleton()`) |
| Lookup | None (self-register on construction) | By interface (`getSingleton(Class)`) |
| Multiple instances | Yes (one per XML node) | Yes (all matching `getSingletons(Class)`) |
| Closeable cleanup | Yes | Yes |

Use **Services** when an object needs XML-driven configuration and registers itself
independently (e.g. a hardware driver, a background scheduler).  
Use **Singletons** when other parts of the application need to retrieve the instance
by interface without knowing the concrete class.
