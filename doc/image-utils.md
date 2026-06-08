# Image Utilities

Package: `cz.bliksoft.javautils.images` (+ `images.iconspec`, `images.ico`, `images.svg`, `barcodes.QRGenerator`)

Toolkit-agnostic image loading, transformation, codec, and icon-spec-resolution support, built entirely on `java.awt.image.BufferedImage`. No JavaFX dependency — usable from Swing UIs and server-side template rendering (Freemarker) alike. JavaFX applications consume this layer through `cz.bliksoft.javautils.fx.tools.ImageUtils` (BSToolbox-jfx, see its `ImageUtils.md`), which converts `BufferedImage` results to `Image`/`ImageView` at the boundary via `SwingFXUtils`.

---

## `IconSpecEngine`

Package: `cz.bliksoft.javautils.images.iconspec`

The central piece of this layer: resolves a compact **icon spec string** to a single `BufferedImage`. This is the same engine that powers JavaFX icon resolution (`ImageUtils.getIconView`/`getImage` and friends) and the `Base64IconSpec` Freemarker extension — the spec language is identical everywhere; only the final `BufferedImage → Image` / `→ Base64 PNG` conversion differs by consumer.

```java
BufferedImage img = IconSpecEngine.createImage("save.svg|24||#badge.svg|12#*+");
```

`createImage` does **not** perform token substitution (e.g. `${scale}`) or look up an outer cache by spec string — those are toolkit-specific concerns left to the caller (jfx `ImageUtils` substitutes tokens and layers an `Image`-keyed cache on top; `Base64IconSpec` calls it directly). The engine retains only the spec-internal `*GET_CACHE`/`*PUT_CACHE`/`*CLEAR_CACHE` cache, addressed by user-chosen keys, which is part of the spec language itself.

### Single-image formats

| Format | Resolves to |
|---|---|
| `name.png` / `name.svg` / `name.ico` | classpath resource relative to the branding images root (`setBrandingImagesRoot`, default `/cz/bliksoft/branding/images/`) |
| `/absolute/path.png` | absolute classpath resource |
| `[F]:/filesystem/path.png` | explicit file-system path (also for `.svg`/`.ico`) |
| `name.svg\|w\|h\|scale\|stroke\|fill` | SVG rendered via `SvgConverter`; `w`/`h` default to the SVG's natural aspect ratio when omitted; `stroke`/`fill` replace `currentColor`/explicit attributes (color syntax: bare hex `ff0000`/`ff000080`, `0xRRGGBB[AA]`, CSS named colors, `rgb()`/`rgba()` — see `CssColor`) |
| `name.ico\|w\|h` | Windows icon, best-fit frame decoded and scaled via `IcoReader` |
| `QR\|ec\|moduleSize\|targetSize\|data` | QR code rendered via `QRGenerator.render` (`ec`: error-correction level `L`/`M`/`Q`/`H`, default `M`) |
| `EMPTY\|size`, `EMPTY\|w\|h`, `EMPTY\|w\|h\|color` | synthetic transparent or solid-color canvas — typically the base layer of an overlay chain |
| `[PI]:<pathData>\|w\|h\|scale\|style` | inline SVG path data, rasterized by wrapping it in a synthetic `<svg><path d="..."/></svg>` and rendering through `SvgConverter` |

`[P]:`/`[PS]:` (which resolve to layoutable `SVGPath`/`Shape` *scene-graph nodes* rather than rasters) are JavaFX-only concepts handled by the jfx adapter before delegating here. `IconSpecEngine.parsePathSpec` is exposed so adapters can reuse the shared `<pathData>|w|h|scale|style` parsing for all three `[P]:`/`[PI]:`/`[PS]:` prefixes.

### Postfix composition

A spec containing `#` is evaluated as a stack machine: `#`-separated tokens where plain tokens push an image and `*`-prefixed tokens are commands operating on the stack and a shared "mode" map (sticky alignment, offsets, drawing colors). The result is the top of the stack at the end.

```java
// Badge overlay at bottom-right
IconSpecEngine.createImage("base.svg|24#badge.svg|12#*+");

// Two badges anchored to opposite corners
IconSpecEngine.createImage(
    "base.svg|24#save.svg|16|||00ff00#*ANCHOR|TL#*+#save.svg|16|||ff0000#*ANCHOR|BR#*+");
```

| Command | Effect |
|---|---|
| `*+\|canvasMode`, `*-\|canvasMode` | Composite top-of-stack over / cut it out of (SRC_OVER / DST_OUT) the image below it. `canvasMode`: `C` = crop to base size (default), `E` = extend canvas to the union bounding box |
| `*ANCHOR\|position\|offsetX\|offsetY` | Sets sticky alignment for subsequent compose/`*TEXT`/`*DRAW`; positions `TL`, `TR`, `BL`, `BR` (default), `C`, `N` (standalone, no compositing) |
| `*EMPTY\|w\|h\|color`, `*QR\|ec\|moduleSize\|targetSize\|data` | Push a synthetic canvas / QR code |
| `*META\|key\|value` | Toolkit-agnostic sticky metadata side-channel — stashes a key/value pair for the current `createImage` evaluation without touching the pixels; read back via `getLastMetadata()`. `*JFXSTYLE\|css` is sugar for `*META\|jfxStyle\|css` (consumed by the jfx adapter to style the resulting `ImageView`) |
| `*COLOR\|stroke\|fill\|width` | Sets sticky drawing colors/width consumed by `*TEXT`/`*DRAW` (`none` = no paint) |
| `*TEXT\|value\|size\|font\|style` | Renders text with the current `*COLOR` fill/stroke; style flags `B`old/`I`talic/`U`nderline/`S`trikethrough/`O`utline (prefix `-` removes a flag, bare `-` clears all) |
| `*DRAW\|shape\|...\|t` | Draws onto the top canvas with the current `*COLOR` fill/stroke (`t` optionally overrides stroke width for this call); shapes: `line\|x1\|y1\|x2\|y2`, `circle\|cx\|cy\|r`, `square\|x\|y\|side`, `rectangle\|x\|y\|w\|h` |
| `*PUSH`, `*SWAP`, `*COPY`, `*PASTE`, `*POP`, `*RESET` | Stack/clipboard manipulation; `*RESET` also clears the mode map (alignment reverts to `BR, 0, 0`) |
| `*GET_CACHE\|key`, `*PUT_CACHE\|key`, `*CLEAR_CACHE\|key` | Explicit, user-keyed cache for reusing composed sub-images: `GET` pushes the cached image and skips to the matching `PUT_CACHE` when present (otherwise falls through so later tokens rebuild it); `PUT` stores the stack top under the key |
| `*NOCACHE` | Marks this evaluation as not to be stored in the caller's outer cache (`wasNoCacheRequested()`) |
| `*FILTER\|name\|p1\|p2\|p3`, `**\|name\|p1\|p2\|p3` | Apply a pixel-level filter (see `ImageFilter`) to the top of the stack in place, or (`**`) equivalently to `*COPY # *FILTER|... # *- # *PASTE # *+` — apply to a copy, cut its silhouette out of the base, recompose the original on top (used by silhouette-expanding filters like shadow/outline) |
| `*SET\|name\|value` | Registers a numeric variable usable in polynomial size/position expressions (`PolynomialEvaluator`) |

### Configuration & state read-back

```java
IconSpecEngine.setBrandingImagesRoot("/myapp/icons/");
boolean skipCache = IconSpecEngine.wasNoCacheRequested();
Map<String, String> meta = IconSpecEngine.getLastMetadata(); // e.g. meta.get("jfxStyle")
```

`wasNoCacheRequested()` and `getLastMetadata()` reflect the most recent `createImage` evaluation **on the calling thread**.

---

## `ImageFilter`

Package: `cz.bliksoft.javautils.images.iconspec`

Enumeration of the pixel-level filters available through the `*FILTER`/`**` postfix commands, each operating on the top-of-stack image and replacing it with the result:

| Filter | Parameters | Effect |
|---|---|---|
| `shadow` | `color, width, fill` | Diffuse fade-out shadow/glow extending `width` px beyond the silhouette (`fill`: `F` filled interior (default), `T` transparent holes preserved) |
| `outline` | `color, width, fill` | Sharp (non-blurred) silhouette expansion — same as `shadow` without the Gaussian falloff |
| `rotate` | `angle` | Clockwise rotation around the canvas centre, canvas size unchanged; multiples of 90° are lossless, others use bilinear interpolation |
| `shift` | `angle, pixels` | Translation in `angle` direction (0°=right, 90°=down); canvas size unchanged |
| `scale` | `w, h, mode` | Scale image; `mode`: `F`=fit, `C`=crop/fill, `%`=percent; omit one dimension to keep aspect ratio |
| `resize` | `w, h` | Change canvas size using the current alignment/offset mode; content is not scaled |
| `mask` | `color, invert` | Replace all pixel colours with `color` (default white), keeping original alpha (`invert`: `Y` inverts alpha) |
| `monochrome` | `color` | Convert to monochrome by tinting with `color` weighted by each pixel's luminance |
| `keymask` | `color` | Replace a specific colour (or the majority corner colour, if omitted) with full transparency |
| `mirror` | `direction` | Flip the image; `H` horizontal (default), `V` vertical |

---

## `PixelOps`

Package: `cz.bliksoft.javautils.images`

Pure `int[]` ARGB pixel-math core shared by `IconSpecEngine`/`ImageFilter`: geometry transforms (mirror, rotate, shift, scale, resize), compositing (alpha-over / alpha-subtract with alignment & offsets), and the building blocks the filters compose from (silhouette/dilate/Gaussian-blur on the alpha channel, bilinear sampling, corner-majority colour detection, …).

Pixels are row-major `int[] argb` (`y * width + x`) — exactly the layout JavaFX's `PixelReader.getPixels`/`PixelWriter.setPixels(..., PixelFormat.getIntArgbInstance(), ...)` and AWT's `BufferedImage.getRGB`/`setRGB` already produce/consume, so moving this logic to the base library cost no extra conversions on the JavaFX side. All functions are pure (return a new array) unless documented otherwise.

This is a low-level building block — most callers should use `IconSpecEngine`/`ImageFilter` rather than `PixelOps` directly.

---

## `CssColor`

Package: `cz.bliksoft.javautils.images`

```java
int argb = CssColor.parse("rgba(51, 153, 255, 0.5)", 0xFF000000);
```

Parses CSS-style colour strings to ARGB ints — the non-JavaFX equivalent of `javafx.scene.paint.Color.web(String)`, scoped to what the icon-spec language needs: hex (3/4/6/8 digits, with or without leading `#` or `0x`), `rgb()`/`rgba()` functional notation, and standard CSS3/SVG named-colour keywords. Returns the supplied default (and logs a warning) for `null`/empty/unparseable input.

> **Gotcha:** a literal `#` anywhere in an icon spec string triggers postfix-composition parsing (the spec is split on `#`) — this happens *before* any single-image dispatch. Colours embedded in spec strings (SVG `stroke`/`fill` slots, `EMPTY|...|color`, etc.) must therefore avoid `#`-prefixed hex notation; use bare hex (`3399ff`), `0x`-prefixed hex (`0x3399ff`), or `rgb()`/`rgba()` instead.

---

## `images.ico` — `IcoReader` / `IcoWriter`

Package: `cz.bliksoft.javautils.images.ico`

```java
BufferedImage frame = IcoReader.loadFromResource("/icons/app.ico", 32, 32);
IcoWriter.write(new File("app.ico"), List.of(icon16, icon32, icon48, icon256));
```

- **`IcoReader`** reads multi-frame Windows ICO files and decodes the best-matching frame for the requested target size to a `BufferedImage`. Supports PNG-in-ICO (Vista+), 32-bpp BGRA DIB, 24-bpp BGR DIB, and indexed (≤8-bpp) DIB frames. `loadFromFile`/`loadFromResource` take optional `targetW`/`targetH` hints used to pick and scale the closest frame.
- **`IcoWriter`** writes multi-frame ICO files using PNG-in-ICO encoding (the same format `IcoReader` reads), one frame per supplied image — typical icon sets use 16/32/48/256 px frames. `write(File/OutputStream, List<BufferedImage>)`.

---

## `images.svg` — `SvgConverter`

Package: `cz.bliksoft.javautils.images.svg`

JSVG-based SVG rasterization to `BufferedImage`, with optional `currentColor`/`stroke`/`fill` colour injection (`createImageFromSVG(..., strokeColor, fillColor)` overloads — same colour syntax as `CssColor`) and configurable defaults (`setDefaultStrokeColor`/`setDefaultFillColor`). Loads from `File`, classpath resource path, or an in-memory SVG string (`loadSvgDocumentFromString` — used by `IconSpecEngine` to rasterize `[PI]:` inline path data via a synthetic `<svg><path .../></svg>` wrapper document).

```java
BufferedImage img = SvgConverter.createImageFromSVGResource("/icons/save.svg", 24f, 24f, null, "0x3399ff", null);
```

---

## `QRGenerator`

Package: `cz.bliksoft.javautils.barcodes`

ZXing-based QR code rendering to `BufferedImage`. `render(data, errorCorrectionLevel, moduleSize, targetSize)` is the entry point used by `IconSpecEngine`'s `QR`/`*QR` spec formats: it encodes `data` with no quiet zone, defaults the error-correction level to `M` (warning + fallback on an unrecognized name), and either uses a fixed `moduleSize` (default 2 px/module) or derives the per-module pixel size from the matrix's module count to best fit a given `targetSize`. Lower-level helpers (`createQRBitMatrix`, `createQR`, `createQRImg`, `rasterize`) remain available for direct use outside the icon-spec context.

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

> Not to be confused with `IconSpecEngine`/`ImageFilter`'s `scale`/`resize` filters, which operate within the icon-spec composition pipeline and support fit/crop/percent modes and alignment-aware canvas resizing.

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
