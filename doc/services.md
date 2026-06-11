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

`loadServices()` is idempotent and thread-safe — a second call is a no-op.
This means it is also safe to trigger lazily: `getService`/`getServices`
(below) call `loadServices()` themselves if it hasn't run yet, so a service
constructor that looks up another service (or a `Singletons` entry that is
itself populated as a side effect) works correctly regardless of which static
initializer runs first, and regardless of which thread gets there first.

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
| `loadServices()` | All `services/` nodes instantiated eagerly; stored in a static map. Idempotent and thread-safe — later calls are no-ops |
| Runtime | Objects remain alive; no framework start/stop beyond construction |
| `cleanup()` | `close()` called on every instance that implements `Closeable` |

### Looking up services by type

```java
// First match assignable to the given type, or null
MyInterface obj = Services.getService(MyInterface.class);

// All matches assignable to the given type
List<MyInterface> all = Services.getServices(MyInterface.class);
```

Both methods trigger `loadServices()` on first use if it hasn't run yet, and
check `cls.isAssignableFrom(value.getClass())` against every loaded instance
— same assignability rule as `Singletons.getSingleton`/`getSingletons` (below).
A service can rely on this for discovery instead of (or in addition to)
self-registering into another system (event bus, scheduler, etc.) during
construction.

`getService`/`getServices` (and `loadServices`/`cleanup`) all synchronize on
the same internal lock, so concurrent calls from multiple threads are safe.
Because the lock is a plain Java intrinsic lock (reentrant per-thread), a
service constructor that itself calls `getService`/`getServices` on the same
thread — e.g. while still being instantiated during `loadServices()` — does
not deadlock; it just re-enters and sees the (partially populated) `services`
map as it stands so far.

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

`loadSingletons()` is idempotent and thread-safe — a second call is a no-op,
so the cached instances from earlier `getSingleton`/`getSingletons` calls (see
below) are never discarded/re-instantiated. `getSingleton`/`getSingletons`
trigger `loadSingletons()` themselves on first use if it hasn't run yet, and
are safe to call even if `singletons/` doesn't exist in the filesystem at all
(returns `null` / an empty list rather than throwing).

`loadSingletons()`, `getSingleton`/`getSingletons`, and `cleanup()` all
synchronize on the same internal lock, and the per-entry
`SingletonContainer.getValue()` (which performs the actual lazy
instantiation) is itself `synchronized`. Together this means:

- Concurrent calls from multiple threads are safe — two threads racing to
  resolve the same singleton type cannot construct two instances of it.
- The lock is a plain Java intrinsic lock (reentrant per-thread), so a
  singleton constructor that itself calls `getSingleton`/`getSingletons` on
  the same thread — e.g. while still being instantiated during another
  lookup — does not deadlock; it re-enters and sees `singletonObjects` as
  populated so far.
- `Services` and `Singletons` use *separate* locks. A constructor that calls
  back across registries (e.g. a `/services` entry's constructor calling
  `Singletons.getSingleton(...)`, as `WebServerCameraSource` does for
  `BSHttpServer`) is fine as long as the corresponding cross-call doesn't
  also happen in the other direction *concurrently from a different thread*
  (which would risk a classic two-lock deadlock). No such cross-registry
  cycle exists in the current codebase.

### Declaring a singleton in XML

```xml
<file name="singletons">
    <file name="myImpl" type="com.example.MyServiceImpl" />
</file>
```

### Implementing a singleton class

Instantiation tries the same two strategies, in the same order, as
`ClassFileLoader` (used by `Services`):

**1. Constructor accepting `FileObject`** *(preferred — attributes available)*

```java
public class MyServiceImpl implements MyInterface, Closeable {
    public MyServiceImpl(FileObject def) {
        String host = def.getAttribute("host", "localhost");
        // …
    }
}
```

**2. No-arg constructor + `IInitializeWithFileObject`**

```java
public class MyServiceImpl implements MyInterface, IInitializeWithFileObject {
    public MyServiceImpl() { /* … */ }

    @Override
    public void initializeWithFileObject(FileObject def) {
        // read attributes here
    }
}
```

A plain no-arg constructor (implementing neither) also works if no
configuration is needed.

---

## Choosing between Services and Singletons

| | Services | Singletons |
|---|---|---|
| XML declaration | `type="Class"` + `class` attribute | `type` = FQCN |
| Configuration attributes | Forwarded to constructor / `IInitializeWithFileObject` | Same — forwarded to constructor / `IInitializeWithFileObject` |
| Instantiation | Eager (`loadServices()`) | Lazy (first `getSingleton()`/`getSingletons()`) |
| Lookup | By interface (`getService(Class)`/`getServices(Class)`) | By interface (`getSingleton(Class)`/`getSingletons(Class)`) |
| Multiple instances | Yes (one per XML node) | Yes (all matching `getSingletons(Class)`) |
| Closeable cleanup | Yes | Yes |

Both registries now support lookup-by-type with the same assignability
semantics, so the choice mainly comes down to **when** the object needs to
exist:

Use **Services** when the object must be alive and ready from application
startup — e.g. it owns a listener/socket that an external client could hit
immediately (a serial port reader, an HTTP upload endpoint).  
Use **Singletons** when the object can be created on first use — e.g. it's
only needed once some UI is opened, and creating it eagerly would do
unnecessary work (open a device, bind a port) that may never be used.

---

## Worked example: a shared `BSHttpServer` singleton with eager handlers

`cz.bliksoft.javautils.net.http.BSHttpServer` is a small embeddable HTTP
server (`com.sun.net.httpserver.HttpServer` wrapper) that can be registered
once as a `Singletons` entry and shared by multiple `Services`/`Singletons`
that each attach their own handler to a different path.

```xml
<file name="singletons">
    <!-- Eagerly bound by the first lookup below; lazy if untouched -->
    <file name="webserver" type="cz.bliksoft.javautils.net.http.BSHttpServer">
        <attribute name="port"    value="8090" />
        <!-- optional: bind to a single interface, e.g. localhost-only -->
        <!-- <attribute name="address" value="127.0.0.1" /> -->
    </file>
</file>

<file name="services">
    <!-- Eager: must be ready to receive uploads before any UI opens -->
    <file name="phone-pusher" type="Class">
        <attribute name="class" value="cz.bliksoft.javautils.fx.controls.images.cam.WebServerCameraSource" />
        <attribute name="path"  value="/upload" />
        <attribute name="name"  value="Remote camera" />
    </file>
</file>
```

- `BSHttpServer(FileObject)` reads the mandatory `port` attribute (and
  optional `address`) and starts the server immediately, so the singleton is
  fully usable as soon as it's looked up — `BSHttpServer.getSingleton()` is a
  thin wrapper over `Singletons.getSingleton(BSHttpServer.class)`.
- `WebServerCameraSource`, instantiated eagerly via `services/`, calls
  `BSHttpServer.getSingleton()` in its constructor and attaches its upload
  handler with `server.addHandler(path, handler)`. This is what triggers
  `Singletons.loadSingletons()`/instantiation of `webserver` the first time —
  works correctly regardless of whether `Services.loadServices()` or
  `Singletons.loadSingletons()` runs first, because both are idempotent.
- Other modules can attach further endpoints to the same server later, e.g.:
  ```java
  BSHttpServer server = BSHttpServer.getSingleton();
  if (server != null) {
      SystemReportHTTPHandler.addEndpoint(server);   // adds "/systeminfo"
  }
  ```
  Checking for `null` lets an app expose `/systeminfo` only when a shared
  `BSHttpServer` singleton is actually configured.

### `BSHttpServer` lifecycle and handler management

| Method | Notes |
|---|---|
| `BSHttpServer(FileObject)` | Reads `port` (mandatory) / `address` (optional), starts the server |
| `static getSingleton()` | `Singletons.getSingleton(BSHttpServer.class)` |
| `addHandler(path, handler)` | Registers a handler; live-attaches if the server is already running |
| `removeHandler(path)` | Idempotent — safe to call even if the path was never registered or the server is already stopped |
| `close()` (`Closeable`) | Calls `stop()`; safe to call multiple times. Invoked by `Singletons.cleanup()` |

Because `removeHandler` and `close()`/`stop()` are both idempotent and
order-independent, a `Closeable` that owns a handler on the shared server
(e.g. `WebServerCameraSource.close()`) can always call
`server.removeHandler(path)` first, even if `Singletons.cleanup()` already
closed the shared `BSHttpServer` — there's no dependency on cleanup order
between singletons/services.
