# Web Services

Package: `cz.bliksoft.javautils.ws`

Helpers for JAX-WS SOAP clients: endpoint binding, authentication header injection, and message logging.

## `BaseService<T>`

Abstract base class for SOAP port wrappers. `T` is the JAX-WS port interface.

```java
public class MyService extends BaseService<MyPort> {
    public MyService(String endpoint) {
        super(MyService_Service.create(...), MyPort.class, endpoint);
    }
}
```

### Constructors

| Constructor | Description |
|---|---|
| `BaseService(Service svc, Class<?> cls, String endpoint)` | Creates the port, creates a `SecurityHandler` (auth type `NONE`), and binds both to the endpoint |
| `BaseService(Service svc, Class<?> cls, String endpoint, SecurityHandler securityHandler)` | As above but uses the supplied handler; pass `null` to skip handler installation |

### Methods

| Method | Description |
|---|---|
| `T getService()` | The bound port object |
| `Class<?> getServiceClass()` | The port interface class |
| `SecurityHandler getSecurityHandler()` | The installed security handler |
| `void setCredentials(String user, String pwd)` | Shortcut for `getSecurityHandler().setUser/setPass` |
| `void addLogHandler()` | Install a `SOAPLogHandler` named after the port class |
| `void addLogHandler(String serviceName)` | Install a `SOAPLogHandler` with a custom name |

---

## `SecurityHandler`

`SOAPHandler<SOAPMessageContext>` that injects authentication headers into outbound messages.

### Authentication modes (`AuthTypes` enum)

| Mode | Behaviour |
|---|---|
| `NONE` | No authentication (default) |
| `BASIC` | Adds `Username` and `Password` HTTP request headers |
| `BINDING` | Sets `BindingProvider.USERNAME_PROPERTY` and `PASSWORD_PROPERTY` on the message context |
| `SOAP` | Adds a WS-Security `Security` header with `UsernameToken` (plain-text password), a `Timestamp` (30-second expiry window), and a random 16-byte `Nonce` (Base64-encoded) |

```java
service.getSecurityHandler().authType = SecurityHandler.AuthTypes.SOAP;
service.setCredentials("user", "secret");
```

### Methods

| Method | Description |
|---|---|
| `setCredentials(String user, String password)` | Set both at once |
| `setUser(String user)` / `setPass(String pass)` | Set individually |
| `String getUser()` | Get the configured username |

---

## `Binder`

Static helpers for attaching handlers and setting endpoints.

| Method | Description |
|---|---|
| `bindService(BindingProvider, String endpoint)` | Set the endpoint URL |
| `bindService(BindingProvider, SOAPHandler, String endpoint)` | Set endpoint and install one handler |
| `addHandler(BindingProvider, SOAPHandler)` | Append a handler to the existing handler chain |
| `addHandler(Binding, SOAPHandler)` | Same, taking a `Binding` directly |

---

## Handlers

### `SOAPLogHandler`

Logs raw SOAP XML messages to files via `LogUtils`. File names follow the pattern `SOAP{name}_in.xml`, `SOAP{name}_out.xml`, and `SOAP{name}_in_FAULT.xml` / `SOAP{name}_out_FAULT.xml`.

```java
// Convenience static factory methods
SOAPLogHandler.addLogHandler(serviceObject);
SOAPLogHandler.addLogHandler(serviceObject, "MyService");
SOAPLogHandler.addLogHandler(binding);
SOAPLogHandler.addLogHandler(binding, "MyService");
```

Or attach directly:

```java
service.addLogHandler("MyService");
```

### `DelayHandler`

Inserts a configurable delay before forwarding each message. Useful for simulating slow networks in testing.

### `MacAddressValidationHandlerBase`

Base class for handlers that validate the caller's MAC address. Extend and implement the validation logic.

### `NaiveSSLHelper`

Disables SSL certificate validation for a `BindingProvider`. Use only in controlled test environments.

---

## Typical usage

```java
public class StockService extends BaseService<StockQuotePortType> {

    public StockService(String endpoint) {
        super(new StockQuoteService().getService(), StockQuotePortType.class, endpoint);
        getSecurityHandler().authType = SecurityHandler.AuthTypes.SOAP;
        setCredentials("user", "password");
        addLogHandler("StockService");  // optional
    }

    public String getQuote(String symbol) {
        return getService().getQuote(symbol);
    }
}
```
