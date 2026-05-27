# Modules System

Package: `cz.bliksoft.javautils.modules`

The modules system is a plugin framework built on Java SPI (`ServiceLoader`). It provides a structured lifecycle for discovering, initialising, and installing application components.

## Core types

### `IModule`

Interface every module must implement.

| Method | Description |
|---|---|
| `String getModuleName()` | Human-readable module name |
| `InputStream getFilesystemXml()` | Root XML descriptor contributed to the virtual filesystem (may return `null`) |
| `Map<String,String> getTranslations()` | Optional i18n key-value pairs the module contributes |
| `boolean isEnabled()` / `void setEnabled(boolean)` | Enable/disable flag |
| `int getModuleLoadingOrder()` | Sort key; lower values load first |
| `void init()` | Called during `Modules.initModules()` |
| `void install()` | Called during `Modules.installModules()`, after all modules are initialised |
| `void cleanup()` | Called during `Modules.cleanup()` before application shutdown |
| `String getVersionInfo()` | Free-form version string |

### `ModuleBase`

Convenient abstract base class. Subclass this instead of implementing `IModule` directly.

- Automatically loads `{ClassName}.xml` from the same package as the module class and returns it from `getFilesystemXml()`.
- Reads version information from `git.properties` (works both in an exploded directory and inside a JAR).  The protected static helper `readVersionFor(Class<?> anchor)` does this lookup and can be called from non-`ModuleBase` classes.
- Default `init()`, `install()`, and `cleanup()` implementations are no-ops.

### `Modules`

Static registry that manages the module lifecycle.

#### Lifecycle phases

```
Modules.loadModules()    // discover & register via ServiceLoader
Modules.initModules()    // call init() on every enabled module
Modules.installModules() // call install() on every enabled module
...
Modules.cleanup()        // call cleanup() on every enabled module
```

`BaseAppModule` (`cz.bliksoft.javautils.app`) is always loaded automatically regardless of the SPI configuration.

#### Key methods

| Method | Description |
|---|---|
| `loadModules()` | Discover all `IModule` implementations via `ServiceLoader` and run `autoloadModule` registrations |
| `initModules()` | Initialise all enabled modules in `getModuleLoadingOrder()` order |
| `installModules()` | Install all enabled modules |
| `cleanup()` | Cleanup all modules |
| `autoloadModule(Class<? extends IModule>)` | Register a module class to be instantiated during `loadModules()` |
| `forceEnableModule(String className)` | Force-enable a module by fully-qualified class name |
| `enableModule(String className)` | Enable a named module |
| `disableModule(String className)` | Disable a named module; `"*"` enables all |
| `getModules()` | Returns the live `Map<String,IModule>` registry |

### `IVersionInfo`

Interface for structured version information, typically implemented by `ModuleBase`.

| Method | Description |
|---|---|
| `getArtifactId()` / `getGroupId()` | Maven coordinates |
| `getVersion()` | Version string |
| `getBranch()` | Git branch |
| `getCommitIdAbbrev()` | Short commit hash |
| `getTags()` / `getClosestTag()` | Git tag information |
| `getClosestTagCommitCount()` | Commits since the nearest tag |
| `getDisplayVersion()` | Convenience default: `"artifactId version [branch:commit]"` |

### `@FunctionInfo`

Runtime annotation for tagging callable functions with a human-readable identification and description.

```java
@FunctionInfo(identification = "myFunc", description = "Does something useful")
public void myMethod() { ... }
```

## SPI registration

Register a module by creating:

```
src/main/resources/META-INF/services/cz.bliksoft.javautils.modules.IModule
```

containing the fully-qualified class name of each module implementation, one per line.

## Typical module skeleton

```java
public class MyModule extends ModuleBase {
    @Override
    public String getModuleName() { return "My Module"; }

    @Override
    public void init() {
        // one-time setup
    }

    @Override
    public void install() {
        // wiring that depends on other modules being initialised
    }
}
```
