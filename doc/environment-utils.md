# EnvironmentUtils

Class: `cz.bliksoft.javautils.EnvironmentUtils`

Central facility for environment-aware configuration. It loads properties from files, merges them with OS environment variables, and provides `${TOKEN}` substitution for file paths. Applications call `init()` once at startup; everything else reads from the populated property map.

## Lifecycle

```
EnvironmentUtils.setAppName("myapp");          // before init (or pass appName in init(props))
EnvironmentUtils.preinit(preloadProps);         // optional, before init
EnvironmentUtils.init();                        // or init(customDefaults)
// application runs
```

After `init()` returns, `isInitialized()` is `true` and all property-access methods are safe to call.

The application name must be set before `init()` — either via `setAppName(...)` or as the `appName` key of the `Properties` passed to `init(props)`; otherwise `init()` throws `InitializationException`.

## Initialisation methods

| Method | Description |
|---|---|
| `static void init()` | Initialise from environment variables and built-in defaults |
| `static void init(Properties props)` | As above, but `props` supplies additional default values |
| `static void preinit(Properties props)` | Pre-load values that take priority over custom defaults but are overridden by environment variables. Must be called before `init()`. |
| `static void setAppName(String name)` | Set the application name (once). Affects `PATH_APPUSERDIR`. |
| `static void setEnvironmentConfigDirectory(File directory)` | Override the environment config directory (before `init()`) |

### Resolution order (highest to lowest)

1. OS environment variables
2. Properties passed to `init(props)` (custom defaults)
3. Properties passed to `preinit(props)` (pre-loaded values)
4. `env.properties` file loaded from the environment config directory
5. `default.env` file in the working directory (specifies which environment directory to use)
6. Built-in defaults

A key defined **both** by `preinit()` and in `env.properties` is an error — the import detects the duplicity and throws `InitializationException`. A value of `#OPTIONAL#` is skipped entirely; `#EMPTY#` imports an empty string.

## Built-in property keys

### Configuration keys

| Property constant | Key string | Default | Description |
|---|---|---|---|
| `PROP_ENVIRONMENT_CONFIG_DIR` | `environmentConfigDir` | `"env_config"` | Directory containing the active environment's `env.properties` |
| `PROP_GLOBAL_CONFIG_DIR` | `configDir` | `"config"` | Shared global configuration directory |
| `PROP_ENVIRONMENT_PROPERTIES_FILE` | `environmentConfig` | `"env.properties"` | Filename of the environment properties file |
| `PROP_APPNAME` | `appName` | — | Application name |

### Auto-populated keys (set by `init`)

| Property constant | Key string | Description |
|---|---|---|
| `PROP_TIMESTAMP` | `timestamp` | Formatted timestamp at initialisation time |
| `PROP_WORKDIR` | `workdir` | Current working directory path |
| `PROP_APPNAME` | `appName` | Application name |
| `PATH_USERDIR` | `USERDIR` | User home directory |
| `PATH_TEMPDIR` | `TEMPDIR` | System temp directory |
| `PATH_APPUSERDIR` | `APPUSERDIR` | `~/.{appName}` (app-specific user directory) |

The three configuration keys (`configDir`, `environmentConfigDir`, `environmentConfig`) are also placed into the property map with their resolved values.

`LOG_DIR` (`logDir`) is a key **constant** for the configured log directory — it is *not* set by `init()`; the application or its configuration is expected to provide it.

## Directory access

| Method | Description |
|---|---|
| `static File getConfigDir()` | The global configuration directory |
| `static File getConfigDir(String subfile)` | A specific file within the global config directory |
| `static File getEnvironmentConfigDir()` | The active environment configuration directory |
| `static File getEnvironmentConfigDir(String subfile)` | A specific file within the environment config directory |

## Property access

| Method | Description |
|---|---|
| `static Map<String,String> getEnvironmentProperties()` | All public properties (keys without a leading `.`) |
| `static Map<String,String> getAllEnvironmentProperties()` | All properties including hidden ones (keys that start with `.`) |
| `static Map<String,String> tryGetEnvironmentProperties()` | Like `getEnvironmentProperties()` but returns an empty map if not initialised |
| `static Map<String,String> tryGetAllEnvironmentProperties()` | Like `getAllEnvironmentProperties()` but returns an empty map if not initialised |
| `static void setEnvironmentProperty(String name, String value)` | Add or update a property at runtime; a `null` value removes the key. Keys without a leading `.` are public. |
| `static void setEnvironmentPropertyIfInitialized(String name, String value)` | Set only when already initialised |
| `static boolean isInitialized()` | Check whether `init()` has been called |
| `static void checkInit()` | Assert initialised; throws `InitializationException` if not |
| `static String getAppName()` | Return the application name (throws `InitializationException` if not set) |

## Path substitution

`pathReplace(String path)` replaces `${TOKEN}` placeholders in a string.

```java
String resolved = EnvironmentUtils.pathReplace("${configDir}/myapp.properties");
// → "config/myapp.properties"
```

```java
Map<String,String> extra = Map.of("myToken", "someValue");
String resolved = EnvironmentUtils.pathReplace("${myToken}/file.txt", extra);
```

Tokens are resolved from the loaded public properties (whose values already reflect environment-variable overrides applied during `init()`), overlaid by the additional token map, which wins on conflict.

`pathReplace` also works **before** `init()` — in that case only the basic tokens `appName`, `APPUSERDIR`, `TEMPDIR`, and `USERDIR` (plus the additional token map) are available.

## Password encryption

`AbstractDBConnection` uses `CryptUtils.getPwdFromProperties` to decrypt passwords stored under the `dbPassword` key. When a clear-text password (`dbPasswordClear`) is encountered the first time, it is encrypted and saved back to the properties file automatically.

## Hidden properties

Properties whose key starts with `.` are hidden — they are included in `getAllEnvironmentProperties()` but excluded from `getEnvironmentProperties()`. Use this for sensitive values you want available internally but not exposed generally.
