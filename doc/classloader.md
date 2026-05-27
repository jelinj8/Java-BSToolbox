# Classloader Utilities

Package: `cz.bliksoft.javautils.classloader`

Two complementary mechanisms: **source classloaders** that compile Java source code at runtime using the JDK's `javax.tools` API, and **`FolderClassLoader`** that loads pre-compiled `.class` files from a directory.

## `AbstractSourceClassLoader<T, U>`

Generic base class. `T` is the source representation (String, File, InputStream…); `U` is the object type to instantiate.

Requires a JDK on the classpath (not a JRE): `ToolProvider.getSystemJavaCompiler()` throws if unavailable.

### Constructors

| Constructor | Description |
|---|---|
| `AbstractSourceClassLoader()` | Uses the system classloader as the parent |
| `AbstractSourceClassLoader(ClassLoader parent)` | Custom parent classloader |

### Abstract method to implement

```java
public abstract SimpleJavaFileObject getSource(String className, T src);
```

Return a `SimpleJavaFileObject` whose `getCharContent` (or equivalent) provides the Java source. Example:

```java
return new SimpleJavaFileObject(
    URI.create("file:///" + className.replace('.', '/') + ".java"), Kind.SOURCE) {
    @Override
    public CharSequence getCharContent(boolean ignoreEncErrors) {
        return (String) src;
    }
};
```

### Key methods

| Method | Description |
|---|---|
| `Class<?> compileAndLoad(String className, T javaSource)` | Compile the source in-memory and define the class in this classloader. Throws with the compiler error output if compilation fails. |
| `U createNewInstance(String className, T input)` | Compile (if `input != null`) or look up via `Class.forName` (if `input == null`), cache the result, and return a new instance via the no-arg constructor. Subsequent calls with the same `className` return a new instance of the cached class without recompiling. |

To force recompilation, create a **new** `AbstractSourceClassLoader` instance.

---

## Concrete source classloaders

### `StringSourceClassLoader<U>`

Source is a `String` containing the full Java source code.

```java
StringSourceClassLoader<MyInterface> loader = new StringSourceClassLoader<>();
MyInterface instance = loader.createNewInstance(
    "com.example.GeneratedClass",
    "package com.example; public class GeneratedClass implements MyInterface { ... }"
);
```

### `InputStreamSourceClassLoader<U>`

Source is an `InputStream`.

### `FileSourceClassLoader<U>`

Source is a `File` pointing to a `.java` file.

---

## `FolderClassLoader`

Loads pre-compiled `.class` files from a filesystem directory. Used for dynamic class reloading without compilation.

```java
FolderClassLoader loader = new FolderClassLoader(new File("build/classes"));
Class<?> cls = loader.loadClass("com.example.MyClass");
```

Class files are resolved by mapping the dotted class name to a path:  
`com.example.MyClass` → `{rootDir}/com/example/MyClass.class`

If the `.class` file is not found in the root directory, the call falls back to `super.loadClass(s)` (parent classloader delegation).

**Reloading**: each `FolderClassLoader` instance maintains its own class definitions. To reload a class, create a new `FolderClassLoader` instance.

---

## Choosing the right class

| Scenario | Recommended class |
|---|---|
| Source code generated at runtime as a `String` | `StringSourceClassLoader` |
| Source code in a file on disk (`.java`) | `FileSourceClassLoader` |
| Source code from an `InputStream` | `InputStreamSourceClassLoader` |
| Pre-compiled `.class` files in a directory | `FolderClassLoader` |
| Custom source representation | Subclass `AbstractSourceClassLoader` |
