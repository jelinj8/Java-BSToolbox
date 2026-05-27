# XML Virtual Filesystem

Package: `cz.bliksoft.javautils.xmlfilesystem`

Modules contribute XML descriptors that are merged at runtime into a single virtual `FileSystem` of `FileObject` nodes. The filesystem is used for configuration, class wiring, and any hierarchical data modules want to share.

## Core types

### `FileSystem`

Singleton. Access via `FileSystem.getDefault()`.

| Method | Description |
|---|---|
| `importXml(InputStream, String resourceId)` | Parse and merge an XML descriptor into the filesystem |
| `getFile(String path)` | Retrieve a `FileObject` by absolute path (e.g. `"/config/logging"`) |
| `getFile(String basePath, String fileName)` | Retrieve a child relative to a base path |
| `loadTranslations()` | Load localised display names from `translations` sections |
| `getTranslation(String translationId)` | Look up a translation by ID |
| `setLocaleCode(String locale)` | Override the active locale |

### `FileObject`

Represents a file or folder node. JAXB-serialisable; maps to `<file>` in the XML descriptor.

**XML attributes:**

| Attribute | Description |
|---|---|
| `name` | Node name |
| `type` | Arbitrary type tag used by `FileLoader` to select a loader |
| `id` | Unique identifier for symlink targets |
| `position` | Sort order among siblings |
| `sorted` | Sort children alphabetically when `true` |
| `locked` | Prevent children from being added or replaced when `true` |
| `translation` | Translation key for the display name |
| `mark` | Arbitrary string marker |
| `remove` | Remove an existing node with this name on merge |
| `replace` | Replace an existing node with this name on merge |
| `target` | Redirect an import to a node with this ID |

**Java API:**

| Method | Description |
|---|---|
| `boolean isDirectory()` | `true` when the node has children |
| `FileObject getFile(String name)` | Get a direct child by name |
| `List<FileObject> getChildFiles()` | All direct children |
| `String getFullPath()` | Absolute path, e.g. `"/folder/file"` |
| `Map<String,FileAttribute> getAttributes()` | Custom attribute map |
| `void importFile(FileObject fo)` | Merge another `FileObject` into this node |

**XML child elements:**

| Element | Description |
|---|---|
| `<file name="..." type="...">` | Child node |
| `<attribute name="..." value="...">` | Custom attribute (optionally translatable) |
| `<include path="...">` | Include another descriptor from classpath; silently skipped if missing |
| `<require path="...">` | Like `include` but throws if the path is not found |
| `<classpath path="...">` | Import a descriptor from a classpath resource |
| `<symlink path="...">` | Symbolic link — resolves lazily to another `FileObject` by path |

### `FileLoader`

Abstract registry for type-specific object loaders. Subclass and implement:

- `String getSupportedType()` — the `type` attribute value this loader handles.
- `Object loadObject(FileObject file)` — load and return the object represented by the node.

Loaders register themselves by placing `FileLoader` implementations under the `/fileLoaders/` path in the filesystem (discovered during module loading).

Static helpers:

| Method | Description |
|---|---|
| `FileLoader.loadFile(FileObject)` | Load the object for a node using the registered loader for its type |
| `FileLoader.loadFile(FileObject parent, String fileName)` | Load a named child |
| `FileLoader.getLoader(FileObject)` | Return the loader for a node's type |
| `FileLoader.getLoader(String fileType)` | Return the loader for a type name |

### `FileObjectClassLoader<T>`

Generic loader that instantiates Java classes referenced by `FileObject` nodes.

| Method | Description |
|---|---|
| `T loadFile(FileObject fo)` | Load and instantiate the class named in the node |
| `List<T> loadFiles(FileObject fo)` | Instantiate all classes in a folder node |

### `FileSymlink`

A `FileObject` that acts as a symbolic link. The target is resolved lazily on first access. Children and attributes are transparently delegated to the target.

### `IInitializeWithFileObject`

Marker interface. Classes that implement it receive their corresponding `FileObject` during module loading via `initializeWithFileObject(FileObject definition)`.

### `FilesystemModule`

`ModuleBase` implementation that registers the XML filesystem itself. It has a loading order of `-10000` so it initialises before all other modules.

## Descriptor XML structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<file name="root">
  <file name="config" type="folder">
    <file name="database" type="dbConfig">
      <attribute name="url" value="${DB_URL}"/>
    </file>
    <include path="/META-INF/optional-extra.xml"/>
    <require path="/META-INF/mandatory.xml"/>
  </file>

  <translations>
    <translation id="menu.open" default="Open"/>
  </translations>
</file>
```

## Module integration

A module contributes its descriptor by returning it from `IModule.getFilesystemXml()`:

```java
@Override
public InputStream getFilesystemXml() {
    return getClass().getResourceAsStream("MyModule.xml");
}
```

`Modules.loadModules()` calls this method on every loaded module and passes the stream to `FileSystem.getDefault().importXml(...)`.
