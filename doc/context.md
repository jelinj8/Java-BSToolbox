# Context System

Package: `cz.bliksoft.javautils.context`

A hierarchical, event-driven state management framework. Contexts form a tree; values stored in a node are visible to that node's listeners and propagate up through parent chains. Listeners are notified when a watched value changes anywhere in the subtree below them.

## Core concepts

- A **Context** is a node in a tree. It holds key→value mappings (via `mapValues`) and typed values (via `listValues`, keyed by `Class`).
- **Listeners** attach to a context and watch a specific key or type. They fire whenever a matching value changes anywhere in the subtree.
- **Level contexts** are boundary nodes that stop propagation from crossing a logical layer boundary.
- **Holder** classes manage which child context is currently active.

## `Context`

Base node class.

### Value storage

| Method | Description |
|---|---|
| `void put(Object key, Object value)` | Store a value under an arbitrary key |
| `void addValue(Object value)` | Store a typed value; the key is `value.getClass()` |
| `ContextSearchResult getValue(Object key)` | Search this node and its children for a value; for `Class` keys the search matches by `instanceof` |

### Tree management

| Method | Description |
|---|---|
| `addContext(Context child)` | Register a child context |
| `removeContext(Context child)` | Unregister a child |
| `removeAllContexts()` | Remove all children |

### Notifications

`void notifyListeners(ContextSearchResult value)` — fires all interested listeners on this node and propagates upward unless a listener calls `event.blockEventPropagation()`.

### Static utilities

| Method | Description |
|---|---|
| `Context.getContextProviderContext(IContextProvider key)` | Get the context associated with a provider |
| `Context.wrapAsLevelContext(Context content, String comment)` | Wrap a context as a level boundary to limit upward propagation |

## `SingleContext<T>`

Typed context holding a single value of type `T`. Optionally binds to a Swing `JList` or `JTree` so that selection changes automatically update the value.

```java
SingleContext<MyItem> ctx = new SingleContext<>();
ctx.bind(myList, MyItem.class);   // selection drives context value
ctx.setValue(item);               // or set programmatically
T current = ctx.getValue();
```

## `ContextSearchResult`

Returned by `Context.getValue(key)`.

| Method | Description |
|---|---|
| `boolean isValid()` | `true` if a value was found |
| `Object getResult()` | The found value; logs an error if not valid |
| `Object getKey()` | The key that was used |
| `Context getContext()` | The context node where the value was found |
| `Integer getLevelsCrossed()` | How many level-context boundaries were crossed |
| `ContextSearchResult.getInvalid(ctx, key)` | Factory for a failed-lookup result |

## `ContextChangedEvent<T>`

Passed to listeners when a value changes.

| Method | Description |
|---|---|
| `T getOldValue()` / `T getNewValue()` | Previous and current values |
| `boolean isOldValid()` / `boolean isNewValid()` | Whether the old/new values exist |
| `boolean isOldNotNull()` / `boolean isNewNotNull()` | Null checks on old/new values |
| `void blockEventPropagation()` | Prevent the event from propagating further up the tree |
| `boolean isPropagationBlocked()` | Query propagation state |

## `AbstractContextListener<T>`

Abstract base for value-change listeners.

```java
AbstractContextListener<MyType> listener = new AbstractContextListener<MyType>(MyType.class, "my-listener") {
    @Override
    public void fired(ContextChangedEvent<MyType> event) {
        // react to value change
    }
};
someContext.contextListeners.add(listener);
```

| Method | Description |
|---|---|
| `Object getKey()` | The key being watched |
| `void setActive(boolean)` | Activate/deactivate; on reactivation replays any missed change |
| `boolean getActive()` | Current activation state |
| `boolean isInterrested(Object resultKey, Integer levelsCrossed)` | Override to filter events by key match or level distance |
| `boolean beforeEvent(T event)` | Hook called before `fired()`; return `false` to skip `fired()` |
| `void afterEvent(Boolean processed, T event)` | Hook called after `fired()` |

Class-typed keys match by `instanceof`, so a listener watching `Animal.class` will fire on changes to `Dog` values.

## Holder classes

Holder classes are context nodes that delegate reads and writes to an inner "active" child context. They let you swap the current context without rewiring listeners.

### `ContextHolder`

Delegates to its first child. `isSet()` returns `true` when a child is attached.

### `SingleContextHolder`

Like `ContextHolder` but generic (`<T extends Context>`).

### `StackedContextHolder`

Extends `SingleContextHolder` with push/pop semantics. The top of the stack is always the active context. `dump()` lists inactive stack entries.

### `MapContextHolder<K, C extends Context>`

Extends `SingleContextHolder`. Stores named contexts in a `LinkedHashMap<K, C>`.

| Method | Description |
|---|---|
| `select(K key)` | Make the named context active |
| `deselect()` | Remove active selection |
| `getSelected()` / `getSelectedKey()` | Query current selection |
| `put(K, C)` / `get(K)` / `removeKey(K)` | Map manipulation |
| `containsKey(K)` / `keySet()` / `values()` / `asMap()` | Map queries |
| `dump()` | Returns a description of non-selected entries |

## Event system (`context.events`)

| Class | Description |
|---|---|
| `EventListener<T>` | Base listener with `beforeEvent()`, `fired()`, `afterEvent()` hooks; handles EDT safety |
| `ConcurrentEventListener<T>` | Thread-safe variant |
| `FirstListenerEvent<T>` | Delivered only to the first registered listener |
| `IConsumableEvent` | Event that can be consumed; once consumed further listeners are skipped |

## Propagation and level contexts

By default, a change in a child context propagates up through all parent contexts, notifying listeners at every level. To create a propagation boundary:

```java
Context boundary = Context.wrapAsLevelContext(innerContext, "UI layer");
```

Listeners can inspect `ContextSearchResult.getLevelsCrossed()` (or the event's level info) to decide whether to act or ignore an event that crossed a boundary.
