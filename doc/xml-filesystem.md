# XML Virtual Filesystem

Package: `cz.bliksoft.javautils.xmlfilesystem`

Modules contribute XML descriptors that are merged at runtime into a single virtual `FileSystem` of `FileObject` nodes. The filesystem is used for configuration, class wiring, and any hierarchical data modules want to share.

## Core types

### `FileSystem`

Singleton. Access via `FileSystem.getDefault()`.

| Method | Description |
|---|---|
| `importXml(InputStream, String resourceId)` | Parse and merge an XML descriptor into the filesystem |
| `getFile(String path)` | Retrieve a `FileObject` by path (e.g. `"config/logging"`) |
| `getFile(String basePath, String... subpaths)` | Retrieve a node by path segments |
| `loadTranslations()` | Walk the `/translations` folder and populate the translation map; call once after all modules are loaded |
| `getTranslation(String translationId)` | Look up a translation by ID; returns `null` if not found |
| `addTranslation(String key, String value)` | Register a single translation entry programmatically; logs an error on duplicate keys |
| `addTranslations(Map<String,String> translations)` | Register multiple translation entries at once |

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
| `String getPath()` | Path from root, e.g. `"config/logging"` |
| `Map<String,FileAttribute> getAttributes()` | Raw attribute map (values and optional translation IDs) |
| `String getAttribute(String name, String def)` | Raw attribute value, or `def` if absent |
| `String getLocalizedAttribute(String name, String def)` | Translated attribute value; falls back to raw value then `def` |
| `String getLocalizedName()` | Translated display name; falls back to `name`, or `<$translationId$>` if key is missing |
| `void importFile(FileObject fo)` | Merge another `FileObject` into this node |
| `boolean isWritable()` | `true` if this node is a `WritableFileObject`, i.e. was loaded from a `mode="rw"` `<include>`/`<require>` |

**XML child elements:**

| Element | Description |
|---|---|
| `<file name="..." type="...">` | Child node |
| `<attribute name="..." value="...">` | Custom attribute (optionally translatable) |
| `<include path="..." mode="ro\|rw">` | Include another descriptor from classpath; silently skipped if missing |
| `<require path="..." mode="ro\|rw">` | Like `include` but throws if the path is not found |
| `<classpath path="...">` | Import a descriptor from a classpath resource (always read-only; `mode="rw"` is ignored with a warning) |
| `<symlink name="..." path="...">` | Symbolic link — resolves lazily to another `FileObject` by path |

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

| Method | Description |
|---|---|
| `String getTargetPath()` | The unresolved `path` from the `<symlink>` element |
| `FileObject getTargetFile()` | Resolves and returns the target node, or `null` if it cannot be found |

### `IInitializeWithFileObject`

Marker interface. Classes that implement it receive their corresponding `FileObject` during module loading via `initializeWithFileObject(FileObject definition)`.

### `FilesystemModule`

`ModuleBase` implementation that registers the XML filesystem itself. It has a loading order of `-10000` so it initialises before all other modules.

## Descriptor XML structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<root xmlns="http://bliksoft.cz/XmlFilesystem">

  <file name="config" type="folder">
    <file name="database" type="dbConfig">
      <attribute name="url" value="${DB_URL}"/>
    </file>
  </file>

  <include path="/etc/optional-extra.xml"/>
  <require path="/etc/mandatory.xml"/>

  <!-- Translations: each child file's name becomes the translation ID.
       Locale codes and the "default" fallback are <attribute> children. -->
  <file name="translations">
    <file name="menu.open">
      <attribute name="default" value="Open"/>
      <attribute name="en"      value="Open"/>
      <attribute name="cs"      value="Otevřít"/>
    </file>
    <file name="menu.save">
      <attribute name="default" value="Save"/>
      <attribute name="cs"      value="Uložit"/>
    </file>
    <file name="config">
      <file name="title">
        <attribute name="default" value="Configuration"/>
        <attribute name="cs"      value="Konfigurace"/>
      </file>
    </file>
  </file>

</root>
```

## Localization

The localization system maps string translation IDs to locale-specific display strings. It is used for `FileObject` node names and attribute values.

### Translation storage

Translations are stored as a regular folder in the filesystem at the path `translations`. Each leaf node in that folder represents one translation entry. The translation ID is the node's path relative to `translations/`, with `/` as the separator. Folder nodes group related translations but have no translation value themselves.

```
translations/
  menu.open        → ID "menu.open"
  menu.save        → ID "menu.save"
  config/
    title          → ID "config/title"
```

The locale-specific string and the `default` fallback are stored as `<attribute>` child nodes of the translation file, named after the locale code (e.g. `en`, `cs`).

```xml
<file name="translations">
  <file name="menu.open">
    <attribute name="default" value="Open"/>
    <attribute name="en"      value="Open"/>
    <attribute name="cs"      value="Otevřít"/>
  </file>
  <file name="config">
    <file name="title">
      <attribute name="default" value="Configuration"/>
      <attribute name="cs"      value="Konfigurace"/>
    </file>
  </file>
</file>
```

### Locale selection

The active locale code defaults to `Locale.getDefault().getLanguage()` at class-load time (e.g. `"en"`, `"cs"`). It can be overridden by writing to `FileSystem.localeCode` before `loadTranslations()` is called.

### Loading translations

Call `FileSystem.loadTranslations()` once, after all module descriptors have been imported. It walks the `translations` folder and populates an internal map. Translations can also be registered programmatically with `addTranslation` / `addTranslations`, which is useful for test fixtures or dynamic entries.

### Using translations on nodes

Set the `translation` attribute on a `<file>` element to the translation ID for that node's display name:

```xml
<file name="openItem" type="menuItem" translation="menu.open">
  <attribute name="tooltip" value="Open a file" translation="menu.open"/>
</file>
```

- `getLocalizedName()` returns the translated string for the node's `translation` key. If no translation is found it returns the raw `name`; if the key is registered but has no matching string it returns `<$translationId$>`.
- `getLocalizedAttribute(name, def)` resolves an attribute's `translation` key from the same map. If the key yields no result it falls back to the raw attribute value; if the attribute is absent it returns `def`.
- `getAttribute(name, def)` always returns the raw value, ignoring any translation key.

### `FileAttribute` structure

Each entry in `getAttributes()` is a `FileAttribute` with two fields:

| Field | Description |
|---|---|
| `value` | Raw string value from the `value` XML attribute or the element's text content |
| `translationID` | Optional translation ID from the `translation` XML attribute; `null` for non-translatable attributes |

## Writable XML files

Normally, the filesystem is read-only: descriptors are merged in memory and never written back. Setting `mode="rw"` on a top-level `<include>` or `<require>` element loads that descriptor's `<file>`/`<symlink>` roots (and all their descendants) as `WritableFileObject`s, which can be modified and saved back to their source file.

```xml
<require path="/etc/user-config.xml" mode="rw"/>
```

`mode="rw"` is only valid on top-level `<include>`/`<require>` elements processed by `FileSystem.importXml`; it is rejected on `<include>`/`<require>`/`<classpath>` elements nested inside a `<file>` (an `InitializationException` is thrown), and ignored (with a warning) on `<classpath>`.

### `WritableFileObject`

A `FileObject` subclass whose `isWritable()` returns `true`. In addition to the read-only API it offers:

| Method | Description |
|---|---|
| `setAttribute(String key, String value)` / `setAttribute(String key, String value, String translationId)` | Set or replace an attribute |
| `removeAttribute(String key)` | Remove an attribute |
| `addChild(WritableFileObject child)` | Add a child node and link it to the same source document |
| `removeChild(FileObject child)` | Remove a child node |
| `setName/setType/setOrder/setSorted/setTranslation(...)` | Modify the corresponding node properties |
| `WritableFileObject getCreateFile(String path)` / `getCreateFile(String path, String... subpaths)` | Like `getFile`, but creates missing path segments (and the target node) as empty `WritableFileObject`s; throws `IllegalStateException` if the path passes through a non-writable node |
| `WritableXmlFile getDocument()` | The source document this node belongs to |
| `void save()` | Shortcut for `getDocument().save()` |

Each mutating call marks the owning `WritableXmlFile` as dirty (`isDirty()`); call `save()` to persist.

### `WritableXmlFile`

Represents one source XML document and the `WritableFileObject` roots loaded from it.

| Method | Description |
|---|---|
| `static WritableXmlFile load(File source)` | Load a document standalone, outside of `FileSystem` |
| `List<FileObject> getRoots()` | Top-level nodes loaded from this document |
| `File getSourceFile()` | The backing file |
| `boolean isDirty()` / `void markDirty()` | Whether there are unsaved changes |
| `void save()` | Rebuild the document from the current state of `getRoots()` and write it via the configured `IWritableXmlStorage`, preserving the original root element's namespace/attributes and any non-`<file>`/`<symlink>` content |

### `IWritableXmlStorage` / `FileXmlStorage`

`IWritableXmlStorage` is the storage strategy used by `WritableXmlFile.save()` — `write(Document document, File source)`. `FileXmlStorage` is the default implementation, writing to a plain `java.io.File`. Alternative implementations (e.g. backed by a database) can be passed to `WritableXmlFile`'s constructor or `load(...)`.

### Merge semantics for writable nodes

- A read-only `<include>`/`<require>` (from a different `resourceId`) can still merge attributes onto an existing writable node, but those attributes are kept separately as "override attributes": they are visible via `getAttribute`/`getLocalizedAttribute`/`getAttributeTranslationId`, take precedence over the node's own attributes, but are never written back by `save()`.
- A foreign `remove`/`replace` of a writable node (i.e. from a different `resourceId`) is rejected with an `InitializationException` — only the document that owns a writable node may remove or replace it.
- For `mode="rw"`, each top-level `<file>`/`<symlink>` root must not collide with an existing node of the same name (or `target` ID) — a collision throws `InitializationException`.

## Module integration

A module contributes its descriptor by returning it from `IModule.getFilesystemXml()`:

```java
@Override
public InputStream getFilesystemXml() {
    return getClass().getResourceAsStream("MyModule.xml");
}
```

`Modules.loadModules()` calls this method on every loaded module and passes the stream to `FileSystem.getDefault().importXml(...)`.
