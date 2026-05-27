# Image Utilities

Package: `cz.bliksoft.javautils.images`

Three classes for loading, transforming, and rendering images.

---

## `ImageUtils`

Static utility methods for `BufferedImage` manipulation.

### `rotate(BufferedImage inputImage, int angle)`

Rotates the image by the specified angle in degrees.

- For 90° and 270°, dimensions are swapped exactly (no interpolation artefacts).
- For other angles, new dimensions are computed from the bounding box of the rotated original.
- The image is centred in the output using an `AffineTransform`.

```java
BufferedImage rotated = ImageUtils.rotate(original, 90);
```

### `scale(BufferedImage inputImage, int scale)`

Scales the image. `scale` is a percentage value where `100` means 1:1.

- Values **above** 100 make the image smaller: `targetSize = originalSize * 100 / scale`.
- Values **below** 100 make the image larger (useful for upscaling).
- Uses `Graphics2D.drawImage` for rendering.

```java
BufferedImage half = ImageUtils.scale(original, 200);  // 50% of original size
BufferedImage double_ = ImageUtils.scale(original, 50); // 200% of original size
```

---

## `ImageLoader`

Abstract registry for extension-specific image loaders.

### Registering loaders

```java
ImageLoader.addLoader("svg", new MySvgLoader());
ImageLoader.setDefault(new MyDefaultLoader()); // fallback for unknown extensions
```

### Loading images

```java
ImageLoader loader = ImageLoader.getLoader("chart.svg"); // selects by extension
Image img = loader.getImage("chart.svg");
Image imgFromBytes = loader.getImage(pngBytes);
```

`getLoader(String fileName)` matches by the file extension (case-insensitive). Falls back to the default loader if no extension-specific loader is registered.

### Implementing a loader

```java
public class MySvgLoader extends ImageLoader {
    @Override
    public List<String> getSupportedExtensions() {
        return List.of("svg");
    }

    @Override
    public Image getImage(String name, String... args) throws Exception { ... }

    @Override
    public Image getImage(byte[] data, String... args) throws Exception { ... }
}
```

### `toBufferedImage(Image img)`

Converts any `Image` to a `BufferedImage` (TYPE_INT_ARGB). If the argument is already a `BufferedImage` it is returned unchanged.

```java
BufferedImage buf = ImageLoader.toBufferedImage(someImage);
```

---

## `HtmlImageRenderer`

Renders an HTML string to a `BufferedImage` using Swing's `JEditorPane`.

```java
BufferedImage img = HtmlImageRenderer.render("<h1>Hello</h1><p>World</p>", 800, 600);
```

### `render(String html, int width, int height)`

- `width` and `height` set the `JEditorPane` size; pass `0, 0` to use the preferred size.
- The image is created with the default screen device's compatible image configuration.
- No display is required on headless systems if AWT headless mode is configured (`java.awt.headless=true`); however `JEditorPane` rendering typically requires a graphics environment.

The returned `BufferedImage` can be passed to `ImageUtils.rotate` / `ImageUtils.scale` or saved with `ImageIO.write`.
