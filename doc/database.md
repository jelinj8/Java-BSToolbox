# Database

Package: `cz.bliksoft.javautils.database`

JDBC connection management with file-based configuration, optional password encryption, and a registry for multi-database applications.

## Key interfaces

### `IDBConnectionFactory`

Simplest connection source; creates a new connection per call.

| Method | Description |
|---|---|
| `Connection getConnection(String reason)` | Open and return a new JDBC connection; `reason` is logged |
| `void setAutoCommit(Boolean autoCommit)` | Configure auto-commit for all connections from this factory |

### `IDBConnectionProvider`

Pooling-friendly wrapper. Callers claim a connection with a lock object and release it when done. Extends `Closeable`.

| Method | Description |
|---|---|
| `Connection getConnection(Object lockObject, String reason)` | Acquire the connection; throws if already claimed by another lock object |
| `void releaseConnection(Object lockObject)` | Release the connection back to the provider |
| `void setName(String name)` / `String getName()` | Optional provider name for logging |
| `void close()` | Tear down the underlying connection or pool |

---

## `AbstractDBConnection`

Abstract `IDBConnectionFactory` that loads JDBC settings from a `.properties` file.

### Configuration properties

| Constant | Key | Description |
|---|---|---|
| `PROP_USER` | `dbUserName` | Database user |
| `PROP_CLEAR_PWD` | `dbPasswordClear` | Plain-text password (written back encrypted after first use) |
| `PROP_PWD` | `dbPassword` | Encrypted password |
| `PROP_PWD_ENC` | `dbPassword_enc` | Marker flag set when the password has been encrypted |
| `PROP_TIMEZONE` | `dbTimezone` | Session timezone |
| `PROP_ADDR` | `dbAddr` | Server host |
| `PROP_PORT` | `dbServerPort` | Server port (default `3306`) |
| `PROP_DB` | `dbName` | Database / schema name |

Properties are loaded through `PropertiesUtils.loadFromFile`, which performs `${TOKEN}` substitution using `EnvironmentUtils.tryGetAllEnvironmentProperties()`.

### Password encryption

If `dbPasswordClear` is present, `CryptUtils` encrypts it and saves the updated properties file back to disk, replacing `dbPasswordClear` with `dbPassword` + `dbPassword_enc`. Subsequent connections use the encrypted form.

### Subclass extension points

| Method | Description |
|---|---|
| `protected abstract String getDriverName()` | JDBC driver class name (e.g. `"com.mysql.cj.jdbc.Driver"`) |
| `protected abstract String getServerString()` | Full JDBC connection URL |
| `protected abstract void sessionSetup(Connection c)` | Called after each new connection is opened; set timezone, NLS settings, etc. |
| `protected void processOptions()` | Override to parse additional properties from the config file |
| `protected void afterProcessOptions()` | Override for post-processing (proxy configuration, etc.) |

---

## Concrete implementations

### `MySQLConnection`

| Property | Value |
|---|---|
| Driver | `com.mysql.cj.jdbc.Driver` |
| Default port | `3306` |
| URL | `jdbc:mysql://{dbAddr}:{dbServerPort}/{dbName}` |

Session setup sets `time_zone` if `dbTimezone` is specified.

### `MariaDbConnection`

| Property | Value |
|---|---|
| Driver | `org.mariadb.jdbc.Driver` |
| Default port | `3306` |

Identical to `MySQLConnection` but uses the MariaDB driver.

### `OracleDbConnection`

| Property | Value |
|---|---|
| Driver | `oracle.jdbc.driver.OracleDriver` |
| Default port | `1521` |

Session setup configures timezone and NLS settings.

---

## `DBConnectionProviderFactory`

Static factory that reads a `databaseType` property from the config file and returns the appropriate `IDBConnectionFactory`.

```java
IDBConnectionFactory factory = DBConnectionProviderFactory.getConnectionProvider(
    new File("config/db.properties")
);
```

Supported `databaseType` values (case-insensitive): `MYSQL`, `MARIADB`, `ORACLE`.

Throws `InitializationException` if the file is missing, `databaseType` is absent, or the type is unknown.

---

## `SingleSharedConnectionProvider`

`IDBConnectionProvider` that wraps a single JDBC connection and serialises access with a `Semaphore`. Use for single-threaded or sequential workflows; not suitable for concurrent access.

### Constructors

| Constructor | Description |
|---|---|
| `SingleSharedConnectionProvider(Connection connection)` | Wraps an already-open connection |
| `SingleSharedConnectionProvider(Connection connection, String name)` | Named variant |
| `SingleSharedConnectionProvider(IDBConnectionFactory factory)` | Connection created lazily on first `getConnection` call |
| `SingleSharedConnectionProvider(IDBConnectionFactory factory, String name)` | Named lazy variant |
| `SingleSharedConnectionProvider(Supplier<Connection> factory)` | Lambda-based lazy connection |
| `SingleSharedConnectionProvider(Supplier<Connection> factory, String name)` | Named lambda variant |

`getConnection` throws if the connection is already claimed or is closed. `releaseConnection` must be called with the same lock object used for `getConnection`.

---

## `DBConnectionProvidersRegister`

Named registry of `IDBConnectionProvider` instances for multi-database applications. Implements `Closeable` — closing the register closes all registered providers.

```java
DBConnectionProvidersRegister register = new DBConnectionProvidersRegister();
register.addDefault("main", new SingleSharedConnectionProvider(mainFactory, "main"));
register.add("reporting", new SingleSharedConnectionProvider(reportFactory, "reporting"));

IDBConnectionProvider main = register.getProvider("main");
register.close(); // closes all providers
```

| Method | Description |
|---|---|
| `add(String name, IDBConnectionProvider)` | Register; throws if name already used |
| `addDefault(String name, IDBConnectionProvider)` | Register and set as the default provider |
| `getProvider(String name)` | Look up by name |
| `removeProvider(String name)` | Remove and return |
| `getProviders()` | All providers as a `Map<String,IDBConnectionProvider>` |
| `close()` | Close all registered providers |

---

## Typical usage

```java
// create factory from config file
IDBConnectionFactory factory = DBConnectionProviderFactory.getConnectionProvider(
    EnvironmentUtils.getEnvironmentConfigDir("db.properties")
);

// wrap as a shared provider (for Freemarker Query extension, etc.)
IDBConnectionProvider provider = new SingleSharedConnectionProvider(factory, "main");

// use with Freemarker Query
Query query = new Query(provider, new FileQueryProvider(queriesFileObject));
generator.addExtension("query", query);

// close when done
provider.close();
```
