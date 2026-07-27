# Application framework (BSApp)

`cz.bliksoft.javautils.app`

Static hub for building headless or GUI applications on top of the modules framework: lifecycle management, layered XML properties, an application event-dispatch thread, and a pluggable permission/session model.

Requires Log4j 2 on the classpath (optional dependency — add the `dependency-management-8-log4j` BOM).

## Typical bootstrap

```java
public static void main(String[] args) throws Exception {
    BSApp.setAppName("myapp");          // before init(); affects settings directories
    // optional: BSApp.setSessionManager(new MySessionManager());
    BSApp.init();                        // load + init + install modules, locale, classpath
    BSApp.startConsole();                // block until shutdown ('q' + Enter quits)
}
```

`init()` must be called once before `start()` / `startConsole()`. It:

1. Adds JARs from the configured module directory (`moduleDir` property) and lib directory (`libDir` property, default folder name `lib`) to the classpath.
2. Applies module enablement from the environment-qualified properties `DisabledModules` and `EnabledModules` (`;`-separated class names; default `"*"` = all).
3. Reads the environment name from the `app.configname` global property (default `"default"`).
4. Sets the default `Locale` from the `{environment}.lang` global property, if present.
5. Installs `DefaultUnrestrictedSessionManager` when no session manager was set.
6. Runs `Modules.loadModules()` → `initModules()` → `installModules()`.

## Lifecycle and shutdown

| Method | Behaviour |
|---|---|
| `start()` | Starts the application EDT (a plain worker thread named `app-edt`) and blocks the caller until shutdown completes. |
| `startConsole()` | Same as `start()`, plus a daemon thread that quits the app when `q` + Enter is typed on stdin. |
| `executeLater(Runnable)` | Queues a task for the application EDT. |
| `initShutdown(reason)` | Graceful shutdown request — fires `TryCloseEvent`, which listeners may veto via `blockClosing(reason)`. If not vetoed, `AppClosedEvent` fires and the app shuts down. |
| `shutdown(reason)` | Forced shutdown — fires `AppClosedEvent` directly, bypassing the veto. |
| `setBeforeExitHook(Runnable)` | Single callback invoked just before framework cleanup. |

On shutdown the framework cleans up `Singletons`, `Services`, and `Modules`.

Events (`cz.bliksoft.javautils.app.events`) are fired on the root `Context`:

- **`TryCloseEvent`** — vetoable close request; listeners call `blockClosing(String reason)` to prevent shutdown.
- **`AppClosedEvent`** — the application is closing; carries the reason.

## Properties

Two layers of XML-format properties (`XmlProperties` extends `java.util.Properties` with `loadFromXML`/`storeToXML` and typed `getDouble`/`getBool` accessors), both stored as `settings.xml`:

| Layer | Location | Accessors |
|---|---|---|
| **Global** (application-level) | `{workingDir}/.{appName}/settings.xml` | `getGlobalProperties()`, `setGlobalProperty`, `removeGlobalProperty`, `saveGlobalProperties()` |
| **Local** (user-level) | `~/.{appName}/settings.xml` | `getLocalProperties()`, `setLocalProperty`, `removeLocalProperty`, `saveLocalProperties()` |

- `getProperty(key[, default])` reads local first, then global, then the default.
- **Environment-qualified** variants (`getEnvironmentProperty`, `setLocalEnvironmentProperty`, …) prefix keys with the active environment name: `{environment}.{key}`. The environment is selected by the `app.configname` global property, so one settings file can hold several configurations.
- `reloadGlobal()` / `reloadLocal()` discard the cached instance and re-read from disk.
- Saving writes atomically through a `.tmp` file; `saveGlobalProperties()` refuses (and logs) when the file is not writable — check `isGlobalPropertiesWritable()`.

**Ad-hoc object properties**: `setObjectProperty(obj, name, value)` / `getObjectProperty(obj, name)` attach values to arbitrary objects in a weak map — entries disappear when the owner is garbage-collected.

## Permissions and sessions

`cz.bliksoft.javautils.app.permissions`

- **`Permission`** — abstract; one subclass per permission, identified by its class. Provides `getName()`, `getAlias()`, and optional category/description.
- **`Permissions`** — static registry. Permissions are discovered lazily from the XML virtual filesystem folder `core/permissions` (children loaded via `FileObjectClassLoader`) and from Java SPI (`META-INF/services/cz.bliksoft.javautils.app.permissions.Permission`). Check with `Permissions.isAllowed(MyPermission.class)`.
- **`SessionManager`** / **`UserInfo`** — supply the current user and their permission set. Install your implementation with `BSApp.setSessionManager(...)` *before* `init()` (it can only be set once); otherwise `DefaultUnrestrictedSessionManager` (all permissions) is used.
- **`NotAllowedPermission`** — sentinel returned by `Permissions.getByName(...)` for unknown names; no user holds it.

## Related pieces

- **`ViewableException`** (`cz.bliksoft.javautils.exceptions`) — generic exception carrying a user-presentable message; thrown e.g. by `XmlProperties.save()`.
- **`DomainLoader`** (`cz.bliksoft.javautils.xmlfilesystem`) — loads typed objects from a virtual-filesystem folder by dispatching each child to the `FileLoader` registered for its `type` attribute (`loadChild`, `loadChildrenList`, `loadChildrenMap`).
- **`BSAppMessages`** — localized framework messages (`BSAppMessages.properties`, Czech variant included); reloaded automatically when `init()` changes the locale.
