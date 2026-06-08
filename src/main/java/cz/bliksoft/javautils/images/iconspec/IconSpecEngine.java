package cz.bliksoft.javautils.images.iconspec;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.view.FloatSize;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.barcodes.QRGenerator;
import cz.bliksoft.javautils.images.CssColor;
import cz.bliksoft.javautils.images.PixelOps;
import cz.bliksoft.javautils.images.ico.IcoReader;
import cz.bliksoft.javautils.images.svg.SvgConverter;
import cz.bliksoft.javautils.math.polynomial.PolynomialEvaluator;

/**
 * Toolkit-agnostic icon-spec resolution engine: resolves a compact <em>icon
 * spec string</em> to a single {@link BufferedImage}, working internally on the
 * {@code int[] argb + width + height} representation provided by
 * {@link PixelOps}. It has no JavaFX dependency and is usable directly from
 * Swing UIs and server-side template rendering (e.g. the Freemarker
 * {@code Base64IconSpec} extension), as well as from JavaFX —
 * {@code cz.bliksoft.javautils.fx.tools.ImageUtils} (BSToolbox-jfx) wraps
 * {@link #createImage(String)} and converts the resulting {@link BufferedImage}
 * to {@link javafx.scene.image.Image} at the boundary.
 *
 * <p>
 * An icon spec resolves either directly to a single image, or via postfix
 * composition — {@code #}-separated tokens where non-{@code *} tokens are
 * pushed onto a stack and {@code *}-prefixed tokens are commands operating on
 * the stack and a shared "mode" map (alignment, offsets, drawing colors, …).
 * The result is the top of the stack.
 *
 * <h2>Single-image formats</h2>
 * <ul>
 * <li>{@code name.png} / {@code name.svg} / {@code name.ico} — resolved
 * relative to the branding images root ({@link #setBrandingImagesRoot})</li>
 * <li>{@code /absolute/path.png} — absolute classpath resource</li>
 * <li>{@code [F]:/filesystem/path.png} — explicit file-system path (also
 * supported for {@code .svg} / {@code .ico})</li>
 * <li>{@code name.svg|w|h|scale|stroke|fill} — SVG rendered via
 * {@link SvgConverter}, with optional size and color overrides; {@code w}/
 * {@code h} default to the SVG's natural aspect ratio if omitted, and
 * {@code stroke}/{@code fill} accept bare hex ({@code ff0000},
 * {@code ff000080}), {@code 0xRRGGBB[AA]}, CSS named colors, or
 * {@code rgb(...)}/{@code rgba(...)}</li>
 * <li>{@code name.ico|w|h} — Windows icon, decoded and best-fit scaled via
 * {@link IcoReader}</li>
 * <li>{@code QR|ec|moduleSize|targetSize|data} — QR code rendered via
 * {@link QRGenerator} ({@code ec}: error-correction level name {@code L}/
 * {@code M}/{@code Q}/{@code H})</li>
 * <li>{@code EMPTY|size} / {@code EMPTY|w|h} / {@code EMPTY|w|h|color} —
 * synthetic transparent or solid-color canvas; useful as the base layer in
 * overlay chains</li>
 * <li>{@code [PI]:<pathData>|w|h|scale|style} — inline SVG path data,
 * rasterized by wrapping it in a synthetic {@code <svg><path d="..."/></svg>}
 * document and rendering it through {@link SvgConverter}</li>
 * </ul>
 *
 * <p>
 * {@code [P]:}/{@code [PS]:} (layoutable {@code SVGPath}/{@code Shape}
 * scene-graph nodes) are JavaFX scene-graph concepts handled by the JavaFX
 * adapter before delegating here; {@link #parsePathSpec} is exposed so that
 * adapters can reuse the shared {@code <pathData>|w|h|scale|style} parsing for
 * all three {@code [P]:}/{@code [PI]:}/{@code [PS]:} prefixes.
 *
 * <h2>Postfix composition commands</h2>
 * <p>
 * {@code #}-separated, e.g. a badge overlay at the bottom-right corner:
 * {@code base.svg|24#badge.svg|12#*+}.
 * <ul>
 * <li>{@code *+|canvasMode} / {@code *-|canvasMode} — composite the top of the
 * stack over / cut it out of (SRC_OVER / DST_OUT) the image below it;
 * {@code canvasMode}: {@code C} = crop the result to the base size (default),
 * {@code E} = extend the canvas to the union bounding box</li>
 * <li>{@code *ANCHOR|position|offsetX|offsetY} — sets sticky alignment used by
 * subsequent compose/{@code *TEXT}/{@code *DRAW} operations; positions:
 * {@code TL}, {@code TR}, {@code BL}, {@code BR} (default), {@code C},
 * {@code N} (standalone — no compositing)</li>
 * <li>{@code *EMPTY|w|h|color}, {@code *QR|ec|moduleSize|targetSize|data} —
 * push a synthetic canvas / QR code onto the stack</li>
 * <li>{@code *META|key|value} — toolkit-agnostic sticky metadata side-channel:
 * stashes a key/value pair for the current {@link #createImage} evaluation
 * without affecting the pixels, readable back via {@link #getLastMetadata()};
 * {@code *JFXSTYLE|css} is sugar for {@code *META|jfxStyle|css}, used by the
 * JavaFX adapter to style the resulting {@code ImageView}</li>
 * <li>{@code *COLOR|stroke|fill|width} — sets sticky drawing colors and stroke
 * width consumed by {@code *TEXT}/{@code *DRAW} ({@code none} = no paint)</li>
 * <li>{@code *TEXT|value|size|font|style} — renders text using the current
 * {@code *COLOR} fill/stroke (style flags: {@code B}old, {@code I}talic,
 * {@code U}nderline, {@code S}trikethrough, {@code O}utline; prefix a flag with
 * {@code -} to remove it, a bare {@code -} clears all)</li>
 * <li>{@code *DRAW|shape|...params...|t} — draws onto the top canvas using the
 * current {@code *COLOR} fill/stroke ({@code t}, if present, overrides the
 * stroke width for this call only); shapes: {@code line|x1|y1|x2|y2},
 * {@code circle|cx|cy|r}, {@code square|x|y|side},
 * {@code rectangle|x|y|w|h}</li>
 * <li>{@code *PUSH}, {@code *SWAP}, {@code *COPY}, {@code *PASTE},
 * {@code *POP}, {@code *RESET} — stack and clipboard manipulation;
 * {@code *RESET} also clears the mode map (alignment reverts to
 * {@code BR, 0, 0})</li>
 * <li>{@code *GET_CACHE|key} / {@code *PUT_CACHE|key} /
 * {@code *CLEAR_CACHE|key} — explicit, user-keyed cache for reusing composed
 * sub-images across evaluations: {@code GET} pushes the cached image (if
 * present) and skips to the matching {@code PUT_CACHE}, otherwise falls through
 * so the following tokens rebuild it; {@code PUT} stores the stack top under
 * the key</li>
 * <li>{@code *NOCACHE} — marks this evaluation as one the caller should not
 * store in its own outer cache (see {@link #wasNoCacheRequested()})</li>
 * <li>{@code *FILTER|name|p1|p2|p3} — applies a pixel-level {@link ImageFilter}
 * to the top of the stack in place; {@code **|name|p1|p2|p3} is equivalent to
 * {@code *COPY # *FILTER|name|... # *- # *PASTE # *+} (apply the filter to a
 * copy, cut its silhouette out of the base, then recompose the original top
 * over the result — used by silhouette-expanding filters such as shadow/
 * outline). See {@link ImageFilter} for the full filter reference (shadow,
 * outline, rotate, shift, scale, resize, mask, monochrome, keymask,
 * mirror)</li>
 * <li>{@code *SET|name|value} — registers a numeric variable usable in
 * polynomial size/position expressions via {@link PolynomialEvaluator}</li>
 * </ul>
 *
 * <p>
 * Token substitution (e.g. {@code ${scale}}) and outer-level result caching by
 * resolved spec string are toolkit-specific concerns left to callers — the
 * engine itself provides only the spec-internal {@code *GET_CACHE}/
 * {@code *PUT_CACHE}/{@code *CLEAR_CACHE} cache described above, which is part
 * of the spec language itself.
 */
public final class IconSpecEngine {

	private static final Logger log = LogManager.getLogger();

	private IconSpecEngine() {
	}

	// ---- Configuration ----

	private static volatile String brandingImagesRoot = "/cz/bliksoft/branding/images/"; //$NON-NLS-1$

	/**
	 * Sets the classpath root prefix used when resolving relative image names.
	 *
	 * @param path the new root prefix (must end with {@code /})
	 */
	public static void setBrandingImagesRoot(String path) {
		brandingImagesRoot = path;
	}

	/** Returns the current classpath root prefix for relative image names. */
	public static String getBrandingImagesRoot() {
		return brandingImagesRoot;
	}

	// ---- Alignment constants (shared by composition, anchoring, and resize) ----

	/** Overlay alignment: source image is centered over the base. */
	public static final int ALIGN_CENTER = 0;
	/**
	 * Overlay alignment: source image is placed at the bottom-right corner (default
	 * for badge overlays).
	 */
	public static final int ALIGN_BOTTOM_RIGHT = 1;
	/** Overlay alignment: source image is placed at the top-left corner. */
	public static final int ALIGN_TOP_LEFT = 2;
	/** Overlay alignment: source image is placed at the bottom-left corner. */
	public static final int ALIGN_BOTTOM_LEFT = 3;
	/** Overlay alignment: source image is placed at the top-right corner. */
	public static final int ALIGN_TOP_RIGHT = 4;
	/**
	 * Alignment: forces {@code *TEXT} to push a new standalone image without
	 * compositing onto the existing stack canvas.
	 */
	public static final int ALIGN_NEW = 5;

	// ---- Spec-internal cache & per-evaluation thread-local state ----

	private static final Map<String, BufferedImage> iconCache = new HashMap<>();

	private static final ThreadLocal<PolynomialEvaluator> compositionEvaluatorTL = new ThreadLocal<>();

	/** Set by {@code *NOCACHE} during postfix evaluation; read back by callers. */
	private static final ThreadLocal<Boolean> noCacheTL = new ThreadLocal<>();

	/**
	 * Arbitrary metadata accumulated by {@code *META}/{@code *JFXSTYLE} during
	 * postfix evaluation; read back by callers via {@link #getLastMetadata()}.
	 */
	private static final ThreadLocal<Map<String, String>> metadataTL = new ThreadLocal<>();

	/**
	 * Returns whether the most recent {@link #createImage} evaluation on this
	 * thread executed {@code *NOCACHE} — callers should then skip storing the
	 * result in their own outer caches.
	 */
	public static boolean wasNoCacheRequested() {
		return noCacheTL.get() != null;
	}

	/**
	 * Returns the metadata accumulated by {@code *META}/{@code *JFXSTYLE} during
	 * the most recent {@link #createImage} evaluation on this thread (e.g. the
	 * {@code "jfxStyle"} key for jfx {@code ImageView} styling). Never
	 * {@code null}; empty if no metadata commands were executed.
	 */
	public static Map<String, String> getLastMetadata() {
		Map<String, String> m = metadataTL.get();
		return m != null ? m : java.util.Collections.emptyMap();
	}

	// ---- Entry point ----

	/**
	 * Creates an image from a raw spec string (no cache look-up; no token
	 * substitution — callers are expected to have already resolved {@code ${...}}
	 * placeholders). If the spec contains {@code #}, it is evaluated as a postfix
	 * expression; otherwise it is treated as a single file spec.
	 *
	 * @param spec the icon spec string; may be {@code null}
	 *
	 * @return the composed image, or {@code null} if the spec is {@code null} or
	 *         loading fails
	 */
	public static BufferedImage createImage(String spec) {
		if (spec == null) {
			log.debug("NULL image spec requested");
			return null;
		}

		noCacheTL.remove();
		metadataTL.set(new LinkedHashMap<>());

		if (!spec.contains("#")) //$NON-NLS-1$
			return createSingleImage(spec);

		Deque<BufferedImage> stack = new ArrayDeque<>();
		Map<String, Object> mode = new HashMap<>();
		String skipUntilPutCacheKey = null; // non-null = skip tokens until matching *PUT_CACHE

		try {
			for (String token : spec.split("#", -1)) { //$NON-NLS-1$
				// Skip mode: bypass all tokens until the matching *PUT_CACHE|key is seen
				if (skipUntilPutCacheKey != null) {
					if (token.startsWith("*PUT_CACHE")) { //$NON-NLS-1$
						String[] p = token.split("\\|", -1); //$NON-NLS-1$
						if (skipUntilPutCacheKey.equals(p.length > 1 ? p[1] : "")) //$NON-NLS-1$
							skipUntilPutCacheKey = null;
					}
					continue;
				}

				if (token.startsWith("*GET_CACHE")) { //$NON-NLS-1$
					String[] p = token.split("\\|", -1); //$NON-NLS-1$
					String key = p.length > 1 ? p[1] : ""; //$NON-NLS-1$
					BufferedImage cached = iconCache.get(key);
					if (cached != null) {
						stack.push(cached);
						skipUntilPutCacheKey = key; // activate skip
					}
					// else: key not cached — fall through, let subsequent tokens build the image
				} else if (token.startsWith("*")) { //$NON-NLS-1$
					executeCommand(token, stack, mode, spec);
				} else if (!token.trim().isEmpty()) {
					BufferedImage img = createSingleImage(token);
					stack.push(img != null ? img : new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
				}
			}
			return stack.isEmpty() ? null : stack.peek();
		} finally {
			compositionEvaluatorTL.remove();
		}
	}

	/**
	 * Loads a single non-composite image from a spec string. Handles {@code EMPTY},
	 * {@code QR}, {@code [PI]:} inline paths, SVG, ICO, and raster images. The SVG
	 * spec format is {@code file.svg|w|h|scale|stroke|fill}.
	 */
	private static BufferedImage createSingleImage(String spec) {
		// Inline SVG path data, rasterized via a synthetic <svg><path .../></svg>
		if (spec.startsWith(PREFIX_PATH_IMAGE)) {
			PathSpec ps = parsePathSpec(spec, PREFIX_PATH_IMAGE);
			String svgContent = "<svg xmlns=\"http://www.w3.org/2000/svg\"><path d=\"" //$NON-NLS-1$
					+ escapeXmlAttribute(ps.pathData) + "\"" + fxStyleToSvgAttributes(ps.style) + "/></svg>"; //$NON-NLS-1$ //$NON-NLS-2$
			try {
				SVGDocument doc = SvgConverter.loadSvgDocumentFromString(svgContent);
				Float outW = ps.w;
				Float outH = ps.h;
				if (outW == null && outH == null) {
					FloatSize natural = doc.size();
					if (natural.width <= 0 || natural.height <= 0) {
						outW = 16f;
						outH = 16f;
					}
				}
				return SvgConverter.createImageFromSVG(doc, outW, outH, ps.s);
			} catch (Exception e) {
				log.error("Failed to rasterize inline SVG path for spec '{}': {}", spec, e.getMessage());
				return null;
			}
		}

		String[] params = spec.split("\\|", -1);
		String filePath = params[0];

		// Synthetic canvas: EMPTY|size EMPTY|w|h EMPTY|w|h|color
		if (filePath.equals("EMPTY")) { //$NON-NLS-1$
			try {
				int w = Math.max(1, (int) Math.round(evalNum(params[1])));
				int h = (params.length > 2 && StringUtils.hasLength(params[2]))
						? Math.max(1, (int) Math.round(evalNum(params[2])))
						: w;
				int argb = 0;
				if (params.length > 3 && StringUtils.hasLength(params[3]))
					argb = CssColor.parse(params[3], 0);
				BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				if (argb != 0) {
					int[] pixels = new int[w * h];
					java.util.Arrays.fill(pixels, argb);
					img.setRGB(0, 0, w, h, pixels, 0, w);
				}
				return img;
			} catch (Exception e) {
				log.warn("Invalid EMPTY canvas spec '{}': {}", spec, e.getMessage());
				return null;
			}
		}

		// QR code: QR|ec|moduleSize|targetSize|data
		if (filePath.equals("QR")) { //$NON-NLS-1$
			String data = params.length > 4 ? params[4] : ""; //$NON-NLS-1$
			if (!StringUtils.hasLength(data)) {
				log.warn("Invalid QR spec '{}': missing data", spec);
				return null;
			}
			try {
				String ec = params.length > 1 ? params[1] : null;
				Integer moduleSize = (params.length > 2 && StringUtils.hasLength(params[2]))
						? (int) Math.round(evalNum(params[2]))
						: null;
				Integer targetSize = (params.length > 3 && StringUtils.hasLength(params[3]))
						? (int) Math.round(evalNum(params[3]))
						: null;
				return QRGenerator.render(data, ec, moduleSize, targetSize);
			} catch (Exception e) {
				log.warn("Failed to render QR code for spec '{}': {}", spec, e.getMessage());
				return null;
			}
		}

		// SVG: file.svg|w|h|scale|stroke|fill
		if (filePath.toLowerCase().endsWith(".svg")) { //$NON-NLS-1$
			Float w = null;
			Float h = null;
			Float svgScale = null;

			try {
				if (params.length > 1 && StringUtils.hasLength(params[1]))
					w = (float) evalNum(params[1]);
				if (params.length > 2 && StringUtils.hasLength(params[2]))
					h = (float) evalNum(params[2]);
				if (params.length > 3 && StringUtils.hasLength(params[3]))
					svgScale = (float) evalNum(params[3]);
			} catch (Exception e) {
				log.error("Failed to evaluate SVG size params for spec '{}': {}", spec, e.getMessage());
				return null;
			}

			String strokeColor = null;
			String fillColor = null;
			if (params.length > 4 && StringUtils.hasLength(params[4]))
				strokeColor = resolveSpecColor(params[4]);
			if (params.length > 5 && StringUtils.hasLength(params[5]))
				fillColor = resolveSpecColor(params[5]);

			try {
				if (filePath.startsWith(PREFIX_FILE)) {
					File f = new File(filePath.substring(4));
					if (f.exists() && f.isFile())
						return SvgConverter.createImageFromSVG(f, w, h, svgScale, strokeColor, fillColor);
					return null;
				}
				String res = filePath.startsWith("/") ? filePath : (brandingImagesRoot + filePath); //$NON-NLS-1$
				return SvgConverter.createImageFromSVGResource(res, w, h, svgScale, strokeColor, fillColor);
			} catch (IllegalArgumentException e) {
				log.error("Failed to load SVG image: {} - {}", spec, e.getMessage());
				return null;
			} catch (Exception e) {
				log.error("Failed to load SVG image: {}", spec, e);
				return null;
			}
		}

		// ICO
		if (filePath.toLowerCase().endsWith(".ico")) { //$NON-NLS-1$
			Integer icoW = null;
			Integer icoH = null;
			try {
				if (params.length > 1 && StringUtils.hasLength(params[1]))
					icoW = (int) Math.round(evalNum(params[1]));
				if (params.length > 2 && StringUtils.hasLength(params[2]))
					icoH = (int) Math.round(evalNum(params[2]));
			} catch (Exception e) {
				log.error("Failed to evaluate ICO size params for spec '{}': {}", spec, e.getMessage());
				return null;
			}
			if (icoW != null && icoH == null)
				icoH = icoW;
			if (icoH != null && icoW == null)
				icoW = icoH;
			try {
				if (filePath.startsWith(PREFIX_FILE)) {
					File f = new File(filePath.substring(4));
					if (f.exists() && f.isFile())
						return IcoReader.loadFromFile(f, icoW, icoH);
					return null;
				}
				String res = filePath.startsWith("/") ? filePath : (brandingImagesRoot + filePath); //$NON-NLS-1$
				return IcoReader.loadFromResource(res, icoW, icoH);
			} catch (Exception e) {
				log.error("Failed to load ICO image: {}", spec, e);
				return null;
			}
		}

		// Raster images
		try {
			if (filePath.startsWith(PREFIX_FILE)) {
				File f = new File(filePath.substring(4));
				if (f.exists() && f.isFile()) {
					try (InputStream in = new FileInputStream(f)) {
						return ImageIO.read(in);
					}
				}
				log.error("Image file not found: {} (path: {})", spec, f.getAbsolutePath());
				return null;
			}
			String res = filePath.startsWith("/") ? filePath : (brandingImagesRoot + filePath); //$NON-NLS-1$
			URL url = IconSpecEngine.class.getResource(res);
			if (url != null) {
				try (InputStream in = url.openStream()) {
					return ImageIO.read(in);
				}
			}
			log.error("Image resource not found: {} (resolved: {})", spec, res);
		} catch (Exception e) {
			log.error("Failed to load raster image: {}", spec, e);
		}
		return null;
	}

	// ---- Inline path specs: [PI]: (rasterized here); [P]:/[PS]: parsed here for
	// toolkit-side node creation ----

	public static final String PREFIX_FILE = "[F]:";
	public static final String PREFIX_PATH = "[P]:";
	public static final String PREFIX_PATH_IMAGE = "[PI]:";
	public static final String PREFIX_PATH_SHAPE = "[PS]:";

	/**
	 * Parsed {@code [P]:}/{@code [PI]:}/{@code [PS]:} inline path spec:
	 * {@code <pathData>|w|h|scale|style}.
	 */
	public static final class PathSpec {
		public final String pathData;
		public final Float w;
		public final Float h;
		public final Float s;
		public final String style;

		PathSpec(String pathData, Float w, Float h, Float s, String style) {
			this.pathData = pathData;
			this.w = w;
			this.h = h;
			this.s = s;
			this.style = style;
		}
	}

	/**
	 * Parses an inline path spec of the form
	 * {@code <prefix><pathData>|w|h|scale|style}. Shared by the engine
	 * ({@code [PI]:} rasterization) and toolkit adapters
	 * ({@code [P]:}/{@code [PS]:} scene-graph node creation).
	 */
	public static PathSpec parsePathSpec(String spec, String prefix) {
		String rest = spec.substring(prefix.length());
		String[] p = rest.split("\\|", -1);

		String pathData = (p.length >= 1) ? p[0] : "";
		Float w = (p.length > 1 && StringUtils.hasLength(p[1])) ? Float.valueOf(p[1]) : null;
		Float h = (p.length > 2 && StringUtils.hasLength(p[2])) ? Float.valueOf(p[2]) : null;
		Float s = (p.length > 3 && StringUtils.hasLength(p[3])) ? Float.valueOf(p[3]) : null;
		String style = (p.length > 4 && StringUtils.hasLength(p[4])) ? p[4] : null;

		return new PathSpec(pathData, w, h, s, style);
	}

	// ---- Postfix command dispatch ----

	/** Dispatches a {@code *CMD} postfix token against the stack and mode map. */
	private static void executeCommand(String token, Deque<BufferedImage> stack, Map<String, Object> mode,
			String spec) {
		String[] parts = token.split("\\|", -1); //$NON-NLS-1$
		String cmd = parts[0].substring(1); // strip leading '*'

		switch (cmd) {
		case "+": { // IconspecCommand.COMPOSE_OVER //$NON-NLS-1$
			boolean extend = parts.length > 1 && "E".equalsIgnoreCase(parts[1].trim()); //$NON-NLS-1$
			BufferedImage overlay = stack.isEmpty() ? null : stack.pop();
			BufferedImage base = stack.isEmpty() ? null : stack.pop();
			BufferedImage result = postfixCompose(base, overlay, modeInt(mode, "align", ALIGN_BOTTOM_RIGHT), //$NON-NLS-1$
					modeInt(mode, "offsetX", 0), modeInt(mode, "offsetY", 0), false, extend); //$NON-NLS-1$ //$NON-NLS-2$
			if (result != null)
				stack.push(result);
			break;
		}
		case "-": { // IconspecCommand.COMPOSE_OUT //$NON-NLS-1$
			boolean extend = parts.length > 1 && "E".equalsIgnoreCase(parts[1].trim()); //$NON-NLS-1$
			BufferedImage overlay = stack.isEmpty() ? null : stack.pop();
			BufferedImage base = stack.isEmpty() ? null : stack.pop();
			BufferedImage result = postfixCompose(base, overlay, modeInt(mode, "align", ALIGN_BOTTOM_RIGHT), //$NON-NLS-1$
					modeInt(mode, "offsetX", 0), modeInt(mode, "offsetY", 0), true, extend); //$NON-NLS-1$ //$NON-NLS-2$
			if (result != null)
				stack.push(result);
			break;
		}
		case "ANCHOR": { // IconspecCommand.ANCHOR //$NON-NLS-1$
			String pos = (parts.length > 1 && StringUtils.hasLength(parts[1])) ? parts[1].trim().toUpperCase() : "BR"; //$NON-NLS-1$
			int align;
			switch (pos) {
			case "TL": //$NON-NLS-1$
				align = ALIGN_TOP_LEFT;
				break;
			case "TR": //$NON-NLS-1$
				align = ALIGN_TOP_RIGHT;
				break;
			case "BL": //$NON-NLS-1$
				align = ALIGN_BOTTOM_LEFT;
				break;
			case "C": //$NON-NLS-1$
				align = ALIGN_CENTER;
				break;
			case "N": //$NON-NLS-1$
				align = ALIGN_NEW;
				break;
			default:
				align = ALIGN_BOTTOM_RIGHT;
				break;
			}
			mode.put("align", align); //$NON-NLS-1$
			if (parts.length > 2 && StringUtils.hasLength(parts[2])) {
				try {
					mode.put("offsetX", Integer.parseInt(parts[2].trim())); //$NON-NLS-1$
				} catch (NumberFormatException ignored) {
				}
			}
			if (parts.length > 3 && StringUtils.hasLength(parts[3])) {
				try {
					mode.put("offsetY", Integer.parseInt(parts[3].trim())); //$NON-NLS-1$
				} catch (NumberFormatException ignored) {
				}
			}
			break;
		}
		case "EMPTY": { // IconspecCommand.EMPTY //$NON-NLS-1$
			StringBuilder sb = new StringBuilder("EMPTY"); //$NON-NLS-1$
			for (int i = 1; i < parts.length; i++)
				sb.append("|").append(parts[i]); //$NON-NLS-1$
			BufferedImage img = createSingleImage(sb.toString());
			if (img != null)
				stack.push(img);
			break;
		}
		case "QR": { // IconspecCommand.QR //$NON-NLS-1$
			StringBuilder sb = new StringBuilder("QR"); //$NON-NLS-1$
			for (int i = 1; i < parts.length; i++)
				sb.append("|").append(parts[i]); //$NON-NLS-1$
			BufferedImage img = createSingleImage(sb.toString());
			if (img != null)
				stack.push(img);
			break;
		}
		case "META": // *META|key|value — toolkit-agnostic metadata side channel //$NON-NLS-1$
			if (parts.length > 1 && StringUtils.hasLength(parts[1]))
				metadataTL.get().put(parts[1], parts.length > 2 ? parts[2] : "");
			break;
		case "JFXSTYLE": // sugar for *META|jfxStyle|css //$NON-NLS-1$
			metadataTL.get().put("jfxStyle", parts.length > 1 ? parts[1] : ""); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		case "RESET": // IconspecCommand.RESET //$NON-NLS-1$
			mode.clear();
			break;
		case "PUSH": // IconspecCommand.PUSH //$NON-NLS-1$
			if (!stack.isEmpty())
				stack.push(stack.peek());
			break;
		case "COPY": // IconspecCommand.COPY //$NON-NLS-1$
			if (!stack.isEmpty())
				mode.put("_temp", stack.peek()); //$NON-NLS-1$
			break;
		case "PASTE": { // IconspecCommand.PASTE //$NON-NLS-1$
			BufferedImage temp = (BufferedImage) mode.get("_temp"); //$NON-NLS-1$
			if (temp != null)
				stack.push(copyImage(temp));
			break;
		}
		case "POP": // IconspecCommand.POP //$NON-NLS-1$
			if (!stack.isEmpty())
				stack.pop();
			break;
		case "SWAP": // IconspecCommand.SWAP //$NON-NLS-1$
			if (stack.size() >= 2) {
				BufferedImage top = stack.pop();
				BufferedImage second = stack.pop();
				stack.push(top);
				stack.push(second);
			}
			break;
		case "FILTER": // IconspecCommand.FILTER //$NON-NLS-1$
			if (!stack.isEmpty() && parts.length > 1) {
				ImageFilter filter = ImageFilter.fromName(parts[1]);
				if (filter == null) {
					log.warn("Unknown filter name in: {}", token); //$NON-NLS-1$
				} else {
					BufferedImage top = stack.pop();
					BufferedImage result = applyFilter(filter, top, parts, 2, mode);
					if (result != null)
						stack.push(result);
				}
			}
			break;
		case "*": // IconspecCommand.COMPOSE_FILTER — **|filtername|p1|p2|p3 //$NON-NLS-1$
			// Equivalent to: *COPY # *FILTER|… # *- # *PASTE # *+
			if (stack.size() >= 2 && parts.length > 1) {
				ImageFilter filter = ImageFilter.fromName(parts[1]);
				if (filter == null) {
					log.warn("Unknown filter name in ** command: {}", token); //$NON-NLS-1$
				} else {
					int align = modeInt(mode, "align", ALIGN_BOTTOM_RIGHT); //$NON-NLS-1$
					int offsetX = modeInt(mode, "offsetX", 0); //$NON-NLS-1$
					int offsetY = modeInt(mode, "offsetY", 0); //$NON-NLS-1$
					BufferedImage top = stack.pop();
					BufferedImage base = stack.pop();
					BufferedImage topFiltered = applyFilter(filter, top, parts, 2, mode);
					if (topFiltered != null) {
						BufferedImage baseCutout = postfixCompose(base, topFiltered, align, offsetX, offsetY, true,
								false);
						BufferedImage result = postfixCompose(baseCutout, top, align, offsetX, offsetY, false, false);
						if (result != null)
							stack.push(result);
					}
				}
			}
			break;
		case "TEXT": { // IconspecCommand.TEXT //$NON-NLS-1$
			String value = parts.length > 1 ? parts[1] : ""; //$NON-NLS-1$
			if (parts.length > 2 && StringUtils.hasLength(parts[2])) {
				try {
					mode.put("textSize", Double.parseDouble(parts[2].trim())); //$NON-NLS-1$
				} catch (NumberFormatException ignored) {
				}
			}
			if (parts.length > 3 && StringUtils.hasLength(parts[3]))
				mode.put("textFont", parts[3]); //$NON-NLS-1$
			if (parts.length > 4 && StringUtils.hasLength(parts[4]))
				mode.put("textStyle", mergeStyleFlags((String) mode.get("textStyle"), parts[4])); //$NON-NLS-1$
			if (StringUtils.hasLength(value)) {
				int fillArgb = resolveModeArgb(mode.get("fillColor"), 0xFF000000); //$NON-NLS-1$
				Integer strokeArgb = resolveModeArgbOrNone(mode.get("strokeColor")); //$NON-NLS-1$
				double sw = ((Number) mode.getOrDefault("strokeWidth", 1.0)).doubleValue(); //$NON-NLS-1$
				BufferedImage img = textToImage(value, fillArgb, strokeArgb, sw,
						((Number) mode.getOrDefault("textSize", 12.0)).doubleValue(), //$NON-NLS-1$
						(String) mode.get("textFont"), //$NON-NLS-1$
						(String) mode.get("textStyle")); //$NON-NLS-1$
				if (img != null) {
					int align = modeInt(mode, "align", ALIGN_BOTTOM_RIGHT); //$NON-NLS-1$
					if (!stack.isEmpty() && align != ALIGN_NEW) {
						BufferedImage base = stack.pop();
						int cw = base.getWidth();
						int ch = base.getHeight();
						int tw = img.getWidth();
						int th = img.getHeight();
						int ox = modeInt(mode, "offsetX", 0); //$NON-NLS-1$
						int oy = modeInt(mode, "offsetY", 0); //$NON-NLS-1$
						double dx, dy;
						switch (align) {
						case ALIGN_TOP_LEFT:
							dx = ox;
							dy = oy;
							break;
						case ALIGN_TOP_RIGHT:
							dx = cw - tw + ox;
							dy = oy;
							break;
						case ALIGN_BOTTOM_LEFT:
							dx = ox;
							dy = ch - th + oy;
							break;
						case ALIGN_CENTER:
							dx = (cw - tw) / 2.0 + ox;
							dy = (ch - th) / 2.0 + oy;
							break;
						default: // ALIGN_BOTTOM_RIGHT
							dx = cw - tw + ox;
							dy = ch - th + oy;
							break;
						}
						BufferedImage cv = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
						Graphics2D g = cv.createGraphics();
						try {
							g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
							g.drawImage(base, 0, 0, null);
							g.drawImage(img, (int) Math.round(dx), (int) Math.round(dy), null);
						} finally {
							g.dispose();
						}
						img = cv;
					}
					stack.push(img);
				}
			}
			break;
		}
		case "COLOR": // IconspecCommand.COLOR //$NON-NLS-1$
			if (parts.length > 1 && StringUtils.hasLength(parts[1]))
				mode.put("strokeColor", parts[1]); //$NON-NLS-1$
			if (parts.length > 2 && StringUtils.hasLength(parts[2]))
				mode.put("fillColor", parts[2]); //$NON-NLS-1$
			if (parts.length > 3 && StringUtils.hasLength(parts[3])) {
				try {
					mode.put("strokeWidth", Double.parseDouble(parts[3].trim())); //$NON-NLS-1$
				} catch (NumberFormatException ignored) {
				}
			}
			break;
		case "DRAW": // IconspecCommand.DRAW //$NON-NLS-1$
			if (stack.isEmpty() || parts.length < 2) {
				log.warn("DRAW requires a canvas on the stack and a shape name: {}", token); //$NON-NLS-1$
			} else {
				Integer fillArgb = resolveModeArgbOrNone(mode.get("fillColor")); //$NON-NLS-1$
				Integer strokeArgb = resolveModeArgbOrNone(mode.get("strokeColor")); //$NON-NLS-1$
				double sw = ((Number) mode.getOrDefault("strokeWidth", 1.0)).doubleValue(); //$NON-NLS-1$
				String shape = parts[1];
				// Fixed geometry param counts per shape; optional t override follows
				int geomCount;
				switch (shape) {
				case "line": //$NON-NLS-1$
					geomCount = 4; // x1 y1 x2 y2
					break;
				case "circle": //$NON-NLS-1$
					geomCount = 3; // cx cy r
					break;
				case "square": //$NON-NLS-1$
					geomCount = 3; // x y side
					break;
				case "rectangle": //$NON-NLS-1$
					geomCount = 4; // x y w h
					break;
				default:
					geomCount = -1;
					break;
				}
				if (geomCount < 0) {
					log.warn("Unknown DRAW shape '{}': {}", shape, token); //$NON-NLS-1$
				} else {
					// t override: parts[2+geomCount] if present
					if (parts.length > 2 + geomCount && StringUtils.hasLength(parts[2 + geomCount]))
						sw = parseDouble(parts[2 + geomCount], sw);
					double[] p = new double[geomCount];
					for (int i = 0; i < geomCount; i++)
						p[i] = parseDouble(get(parts, 2 + i), 0.0);
					BufferedImage base = stack.pop();
					int cw = base.getWidth();
					int ch = base.getHeight();
					int drawAlign = modeInt(mode, "align", ALIGN_BOTTOM_RIGHT); //$NON-NLS-1$
					int ox = modeInt(mode, "offsetX", 0); //$NON-NLS-1$
					int oy = modeInt(mode, "offsetY", 0); //$NON-NLS-1$
					double refX;
					switch (drawAlign) {
					case ALIGN_TOP_LEFT:
					case ALIGN_BOTTOM_LEFT:
						refX = ox;
						break;
					case ALIGN_TOP_RIGHT:
					case ALIGN_BOTTOM_RIGHT:
						refX = cw + ox;
						break;
					case ALIGN_NEW:
						refX = 0;
						break;
					default: // ALIGN_CENTER
						refX = cw / 2.0 + ox;
						break;
					}
					double refY;
					switch (drawAlign) {
					case ALIGN_TOP_LEFT:
					case ALIGN_TOP_RIGHT:
						refY = oy;
						break;
					case ALIGN_BOTTOM_LEFT:
					case ALIGN_BOTTOM_RIGHT:
						refY = ch + oy;
						break;
					case ALIGN_NEW:
						refY = 0;
						break;
					default: // ALIGN_CENTER
						refY = ch / 2.0 + oy;
						break;
					}
					BufferedImage result = drawOnImage(base, shape, p, fillArgb, strokeArgb, sw, refX, refY);
					if (result != null)
						stack.push(result);
				}
			}
			break;
		case "PUT_CACHE": // IconspecCommand.PUT_CACHE //$NON-NLS-1$
			if (!stack.isEmpty() && parts.length > 1 && StringUtils.hasLength(parts[1]))
				iconCache.put(parts[1], copyImage(stack.peek()));
			break;
		case "CLEAR_CACHE": // IconspecCommand.CLEAR_CACHE //$NON-NLS-1$
			if (parts.length > 1 && StringUtils.hasLength(parts[1]))
				iconCache.remove(parts[1]);
			break;
		case "SET": //$NON-NLS-1$
			if (parts.length > 2 && StringUtils.hasLength(parts[1])) {
				double val = parseDouble(parts[2], 0.0);
				PolynomialEvaluator ev = compositionEvaluatorTL.get();
				if (ev == null) {
					ev = new PolynomialEvaluator();
					compositionEvaluatorTL.set(ev);
				}
				ev.registerVariable(parts[1], val);
			}
			break;
		case "NOCACHE": // IconspecCommand.NOCACHE //$NON-NLS-1$
			noCacheTL.set(Boolean.TRUE);
			break;
		default:
			log.warn("Unknown postfix command: {} in spec: {}", token, spec); //$NON-NLS-1$
			break;
		}
	}

	private static BufferedImage applyFilter(ImageFilter filter, BufferedImage img, String[] parts, int offset,
			Map<String, Object> mode) {
		switch (filter) {
		case SHADOW:
			return filterShadow(img, CssColor.parse(get(parts, offset), 0xFF000000),
					parseInt(get(parts, offset + 1), 5), !"T".equalsIgnoreCase(get(parts, offset + 2))); //$NON-NLS-1$
		case OUTLINE:
			return filterOutline(img, CssColor.parse(get(parts, offset), 0xFF000000),
					parseInt(get(parts, offset + 1), 1), !"T".equalsIgnoreCase(get(parts, offset + 2))); //$NON-NLS-1$
		case ROTATE:
			return filterRotate(img, parseDouble(get(parts, offset), 0.0));
		case SHIFT:
			return filterShift(img, parseDouble(get(parts, offset), 0.0), parseDouble(get(parts, offset + 1), 0.0));
		case SCALE:
			return filterScale(img, get(parts, offset), get(parts, offset + 1), get(parts, offset + 2));
		case RESIZE:
			return filterResize(img, parseInt(get(parts, offset), img.getWidth()),
					parseInt(get(parts, offset + 1), img.getHeight()), modeInt(mode, "align", ALIGN_BOTTOM_RIGHT), //$NON-NLS-1$
					modeInt(mode, "offsetX", 0), modeInt(mode, "offsetY", 0)); //$NON-NLS-1$ //$NON-NLS-2$
		case MASK:
			return filterMask(img, CssColor.parse(get(parts, offset), 0xFFFFFFFF),
					"Y".equalsIgnoreCase(get(parts, offset + 1))); //$NON-NLS-1$
		case MONOCHROME:
			return filterMonochrome(img, CssColor.parse(get(parts, offset), 0xFFFFFFFF));
		case KEYMASK:
			return filterKeymask(img, get(parts, offset));
		case MIRROR:
			return mirrorImage(img, !"V".equalsIgnoreCase(get(parts, offset))); //$NON-NLS-1$
		default:
			throw new IllegalStateException("Unhandled image filter: " + filter); //$NON-NLS-1$
		}
	}

	// ---- Mode helpers ----

	private static int modeInt(Map<String, Object> mode, String key, int defaultVal) {
		Object v = mode.get(key);
		return (v instanceof Number) ? ((Number) v).intValue() : defaultVal;
	}

	/**
	 * Resolves a {@code mode} color spec (a {@link String} or {@code null}) to an
	 * ARGB int, with a default.
	 */
	private static int resolveModeArgb(Object colorValue, int defaultArgb) {
		return colorValue != null ? CssColor.parse(colorValue.toString(), defaultArgb) : defaultArgb;
	}

	/**
	 * Resolves a {@code mode} color spec to an ARGB int, or {@code null} for "no
	 * paint" ({@code none}/unset/invalid).
	 */
	private static Integer resolveModeArgbOrNone(Object colorValue) {
		return colorValue != null ? CssColor.parseOrNone(colorValue.toString()) : null;
	}

	private static String mergeStyleFlags(String current, String update) {
		if ("-".equals(update))
			return "";
		java.util.Set<Character> flags = new java.util.LinkedHashSet<>();
		if (current != null)
			for (char c : current.toCharArray())
				flags.add(c);
		boolean remove = false;
		for (char c : update.toCharArray()) {
			if (c == '-')
				remove = true;
			else if (c == '+')
				remove = false;
			else if (remove)
				flags.remove(c);
			else
				flags.add(c);
		}
		StringBuilder sb = new StringBuilder();
		for (char c : flags)
			sb.append(c);
		return sb.toString();
	}

	private static double evalNum(String s) {
		PolynomialEvaluator ev = compositionEvaluatorTL.get();
		return ev != null ? ev.evaluate(s) : PolynomialEvaluator.eval(s);
	}

	private static int parseInt(String s, int defaultVal) {
		if (s == null || s.trim().isEmpty())
			return defaultVal;
		try {
			return (int) Math.round(evalNum(s.trim()));
		} catch (Exception e) {
			log.warn("parseInt: failed to evaluate '{}': {}", s, e.getMessage());
			return defaultVal;
		}
	}

	private static double parseDouble(String s, double defaultVal) {
		if (s == null || s.trim().isEmpty())
			return defaultVal;
		try {
			return evalNum(s.trim());
		} catch (Exception e) {
			log.warn("parseDouble: failed to evaluate '{}': {}", s, e.getMessage());
			return defaultVal;
		}
	}

	/** Null-safe positional access into a split-parts array. */
	private static String get(String[] parts, int index) {
		return index < parts.length ? parts[index] : null;
	}

	// ---- Compositing ----

	/**
	 * Composes {@code overlay} onto {@code base} using alignment, optional pixel
	 * offsets, and normal (SRC_OVER) or subtract (DST_OUT) mode.
	 *
	 * <p>
	 * When {@code extend} is {@code true} the result canvas is the union bounding
	 * box of both images (the overlay may grow the canvas). When {@code false} the
	 * result is clipped to the base image dimensions (default for
	 * {@code *+}/{@code *-}).
	 */
	private static BufferedImage postfixCompose(BufferedImage base, BufferedImage overlay, int align, int offsetX,
			int offsetY, boolean subtract, boolean extend) {
		if (base == null)
			return overlay;
		if (overlay == null)
			return base;

		int bW = base.getWidth();
		int bH = base.getHeight();
		int oW = overlay.getWidth();
		int oH = overlay.getHeight();

		int ox, oy;
		switch (align) {
		case ALIGN_TOP_LEFT:
			ox = offsetX;
			oy = offsetY;
			break;
		case ALIGN_TOP_RIGHT:
			ox = bW - oW + offsetX;
			oy = offsetY;
			break;
		case ALIGN_BOTTOM_LEFT:
			ox = offsetX;
			oy = bH - oH + offsetY;
			break;
		case ALIGN_CENTER:
			ox = (bW - oW) / 2 + offsetX;
			oy = (bH - oH) / 2 + offsetY;
			break;
		default: // ALIGN_BOTTOM_RIGHT
			ox = bW - oW + offsetX;
			oy = bH - oH + offsetY;
			break;
		}

		int rW, rH, baseOffX, baseOffY, overlayOffX, overlayOffY;
		if (extend) {
			int minX = Math.min(0, ox);
			int minY = Math.min(0, oy);
			int maxX = Math.max(bW, ox + oW);
			int maxY = Math.max(bH, oy + oH);
			rW = maxX - minX;
			rH = maxY - minY;
			baseOffX = -minX;
			baseOffY = -minY;
			overlayOffX = ox - minX;
			overlayOffY = oy - minY;
		} else {
			rW = bW;
			rH = bH;
			baseOffX = 0;
			baseOffY = 0;
			overlayOffX = ox;
			overlayOffY = oy;
		}

		if (rW <= 0 || rH <= 0)
			return base;

		int[] out = new int[rW * rH];
		PixelOps.alphaCompositeInto(out, rW, rH, readPixels(base), bW, bH, baseOffX, baseOffY);

		int[] ov = readPixels(overlay);
		if (subtract)
			PixelOps.alphaSubtractInto(out, rW, rH, ov, oW, oH, overlayOffX, overlayOffY);
		else
			PixelOps.alphaCompositeInto(out, rW, rH, ov, oW, oH, overlayOffX, overlayOffY);
		return writePixels(out, rW, rH);
	}

	// ---- BufferedImage <-> int[] boundary (PixelOps representation: row-major
	// ARGB) ----

	private static int[] readPixels(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		return img.getRGB(0, 0, w, h, null, 0, w);
	}

	private static BufferedImage writePixels(int[] pixels, int w, int h) {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		img.setRGB(0, 0, w, h, pixels, 0, w);
		return img;
	}

	private static BufferedImage copyImage(BufferedImage src) {
		return writePixels(readPixels(src), src.getWidth(), src.getHeight());
	}

	private static BufferedImage mirrorImage(BufferedImage img, boolean flipLeftRight) {
		int w = img.getWidth();
		int h = img.getHeight();
		return writePixels(PixelOps.mirror(readPixels(img), w, h, flipLeftRight), w, h);
	}

	private static BufferedImage rotateImage(BufferedImage img, int degrees) {
		int w = img.getWidth();
		int h = img.getHeight();
		if (degrees != 90 && degrees != 180 && degrees != 270)
			return img;
		int[] dst = PixelOps.rotateLossless(readPixels(img), w, h, degrees);
		return writePixels(dst, PixelOps.rotatedWidth(w, h, degrees), PixelOps.rotatedHeight(w, h, degrees));
	}

	// ---- Filters (thin PixelOps wrappers) ----

	private static BufferedImage filterShadow(BufferedImage img, int argbColor, int radius, boolean filled) {
		int w = img.getWidth();
		int h = img.getHeight();
		return writePixels(PixelOps.shadow(readPixels(img), w, h, argbColor, radius, filled), w, h);
	}

	private static BufferedImage filterOutline(BufferedImage img, int argbColor, int radius, boolean filled) {
		int w = img.getWidth();
		int h = img.getHeight();
		return writePixels(PixelOps.outline(readPixels(img), w, h, argbColor, radius, filled), w, h);
	}

	private static BufferedImage filterRotate(BufferedImage img, double angleDeg) {
		int norm = (int) (((angleDeg % 360) + 360) % 360);
		if (angleDeg == Math.floor(angleDeg) && norm % 90 == 0) {
			if (norm == 0)
				return img;
			return rotateImage(img, norm);
		}

		int w = img.getWidth();
		int h = img.getHeight();
		return writePixels(PixelOps.rotateBilinear(readPixels(img), w, h, angleDeg), w, h);
	}

	private static BufferedImage filterShift(BufferedImage img, double angleDeg, double pixels) {
		int w = img.getWidth();
		int h = img.getHeight();

		double rad = Math.toRadians(angleDeg);
		double dx = pixels * Math.cos(rad);
		double dy = pixels * Math.sin(rad);

		int[] src = readPixels(img);

		// Lossless path: axis-aligned integer shift
		int norm = (int) (((angleDeg % 360) + 360) % 360);
		boolean isAxisAligned = (angleDeg == Math.floor(angleDeg)) && (norm % 90 == 0);
		boolean isWholePixel = (pixels == Math.floor(pixels));
		if (isAxisAligned && isWholePixel)
			return writePixels(PixelOps.shiftAxisAligned(src, w, h, (int) dx, (int) dy), w, h);

		return writePixels(PixelOps.shiftBilinear(src, w, h, dx, dy), w, h);
	}

	private static BufferedImage filterScale(BufferedImage img, String wStr, String hStr, String modeStr) {
		int srcW = img.getWidth();
		int srcH = img.getHeight();
		if (srcW <= 0 || srcH <= 0)
			return img;

		boolean hasW = StringUtils.hasLength(wStr);
		boolean hasH = StringUtils.hasLength(hStr);
		String mode = (modeStr != null) ? modeStr.trim() : ""; //$NON-NLS-1$

		if ("%".equals(mode) && hasW) { //$NON-NLS-1$
			double pct;
			try {
				pct = Double.parseDouble(wStr.trim()) / 100.0;
			} catch (NumberFormatException e) {
				return img;
			}
			int dstW = Math.max(1, (int) Math.round(srcW * pct));
			int dstH = Math.max(1, (int) Math.round(srcH * pct));
			return applyScaleToImage(img, srcW, srcH, dstW, dstH);
		}

		if ("F".equalsIgnoreCase(mode) && hasW && hasH) { //$NON-NLS-1$
			int targetW, targetH;
			try {
				targetW = Integer.parseInt(wStr.trim());
				targetH = Integer.parseInt(hStr.trim());
			} catch (NumberFormatException e) {
				return img;
			}
			double factor = Math.min((double) targetW / srcW, (double) targetH / srcH);
			int scaledW = Math.max(1, (int) Math.round(srcW * factor));
			int scaledH = Math.max(1, (int) Math.round(srcH * factor));
			int[] scaled = PixelOps.scale(readPixels(img), srcW, srcH, scaledW, scaledH);
			// Fit: scaled image centred on target canvas
			int offX = (targetW - scaledW) / 2;
			int offY = (targetH - scaledH) / 2;
			return writePixels(PixelOps.place(scaled, scaledW, scaledH, targetW, targetH, offX, offY), targetW,
					targetH);
		}

		if ("C".equalsIgnoreCase(mode) && hasW && hasH) { //$NON-NLS-1$
			int targetW, targetH;
			try {
				targetW = Integer.parseInt(wStr.trim());
				targetH = Integer.parseInt(hStr.trim());
			} catch (NumberFormatException e) {
				return img;
			}
			double factor = Math.max((double) targetW / srcW, (double) targetH / srcH);
			int scaledW = Math.max(1, (int) Math.round(srcW * factor));
			int scaledH = Math.max(1, (int) Math.round(srcH * factor));
			int[] scaled = PixelOps.scale(readPixels(img), srcW, srcH, scaledW, scaledH);
			// Crop to targetW×targetH centred
			int offX = (scaledW - targetW) / 2;
			int offY = (scaledH - targetH) / 2;
			return writePixels(PixelOps.place(scaled, scaledW, scaledH, targetW, targetH, -offX, -offY), targetW,
					targetH);
		}

		// Default: proportional (one dimension) or stretch (both)
		int dstW, dstH;
		if (hasW && hasH) {
			try {
				dstW = Math.max(1, Integer.parseInt(wStr.trim()));
				dstH = Math.max(1, Integer.parseInt(hStr.trim()));
			} catch (NumberFormatException e) {
				return img;
			}
		} else if (hasW) {
			try {
				dstW = Math.max(1, Integer.parseInt(wStr.trim()));
			} catch (NumberFormatException e) {
				return img;
			}
			dstH = Math.max(1, (int) Math.round((double) srcH * dstW / srcW));
		} else if (hasH) {
			try {
				dstH = Math.max(1, Integer.parseInt(hStr.trim()));
			} catch (NumberFormatException e) {
				return img;
			}
			dstW = Math.max(1, (int) Math.round((double) srcW * dstH / srcH));
		} else {
			return img;
		}
		return applyScaleToImage(img, srcW, srcH, dstW, dstH);
	}

	private static BufferedImage applyScaleToImage(BufferedImage img, int srcW, int srcH, int dstW, int dstH) {
		int[] dst = PixelOps.scale(readPixels(img), srcW, srcH, dstW, dstH);
		return writePixels(dst, dstW, dstH);
	}

	private static BufferedImage filterResize(BufferedImage img, int newW, int newH, int align, int offsetX,
			int offsetY) {
		int srcW = img.getWidth();
		int srcH = img.getHeight();

		int dstX, dstY;
		switch (align) {
		case ALIGN_TOP_LEFT:
			dstX = offsetX;
			dstY = offsetY;
			break;
		case ALIGN_TOP_RIGHT:
			dstX = newW - srcW + offsetX;
			dstY = offsetY;
			break;
		case ALIGN_BOTTOM_LEFT:
			dstX = offsetX;
			dstY = newH - srcH + offsetY;
			break;
		case ALIGN_CENTER:
			dstX = (newW - srcW) / 2 + offsetX;
			dstY = (newH - srcH) / 2 + offsetY;
			break;
		default: // ALIGN_BOTTOM_RIGHT
			dstX = newW - srcW + offsetX;
			dstY = newH - srcH + offsetY;
			break;
		}

		int[] src = readPixels(img);
		return writePixels(PixelOps.place(src, srcW, srcH, newW, newH, dstX, dstY), newW, newH);
	}

	private static BufferedImage filterMask(BufferedImage img, int argbColor, boolean invert) {
		int w = img.getWidth();
		int h = img.getHeight();
		return writePixels(PixelOps.mask(readPixels(img), argbColor, invert), w, h);
	}

	private static BufferedImage filterMonochrome(BufferedImage img, int argbColor) {
		int w = img.getWidth();
		int h = img.getHeight();
		return writePixels(PixelOps.monochrome(readPixels(img), argbColor), w, h);
	}

	private static BufferedImage filterKeymask(BufferedImage img, String colorSpec) {
		int w = img.getWidth();
		int h = img.getHeight();
		int[] src = readPixels(img);
		int keyRgb = StringUtils.hasLength(colorSpec) ? (CssColor.parse(colorSpec, 0) & 0x00FFFFFF)
				: PixelOps.cornerMajorityRgb(src, w, h);
		return writePixels(PixelOps.keymask(src, keyRgb), w, h);
	}

	// ---- *TEXT (AWT Font/TextLayout — replaces JavaFX Text/Canvas) ----

	private static BufferedImage textToImage(String value, int fillArgb, Integer strokeArgb, double strokeWidth,
			double size, String fontName, String style) {
		boolean bold = style != null && style.indexOf('B') >= 0;
		boolean italic = style != null && style.indexOf('I') >= 0;
		boolean underline = style != null && style.indexOf('U') >= 0;
		boolean strikethrough = style != null && style.indexOf('S') >= 0;
		boolean outline = style != null && style.indexOf('O') >= 0 && strokeArgb != null;

		Map<TextAttribute, Object> attrs = new HashMap<>();
		attrs.put(TextAttribute.FAMILY, StringUtils.hasLength(fontName) ? fontName : Font.SANS_SERIF);
		attrs.put(TextAttribute.SIZE, (float) size);
		attrs.put(TextAttribute.WEIGHT, bold ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR);
		attrs.put(TextAttribute.POSTURE, italic ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR);
		if (underline)
			attrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		if (strikethrough)
			attrs.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		Font font = Font.getFont(attrs);

		BufferedImage measure = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D mg = measure.createGraphics();
		FontRenderContext frc;
		Shape textShape;
		try {
			mg.setFont(font);
			frc = mg.getFontRenderContext();
			TextLayout layout = new TextLayout(value, font, frc);
			java.awt.Rectangle pixelBounds = layout.getPixelBounds(frc, 0, 0);
			textShape = layout.getOutline(AffineTransform.getTranslateInstance(-pixelBounds.x, -pixelBounds.y));
			int w = Math.max(1, pixelBounds.width);
			int h = Math.max(1, pixelBounds.height);
			BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(new Color(fillArgb, true));
				g.fill(textShape);
				if (outline) {
					g.setStroke(new BasicStroke((float) strokeWidth));
					g.setColor(new Color(strokeArgb, true));
					g.draw(textShape);
				}
			} finally {
				g.dispose();
			}
			return img;
		} finally {
			mg.dispose();
		}
	}

	// ---- *DRAW (AWT Graphics2D primitives — replaces JavaFX
	// Canvas/GraphicsContext) ----

	private static BufferedImage drawOnImage(BufferedImage base, String shape, double[] p, Integer fillArgb,
			Integer strokeArgb, double strokeWidth, double refX, double refY) {
		int w = base.getWidth();
		int h = base.getHeight();
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.drawImage(base, 0, 0, null);
			g.setStroke(new BasicStroke((float) strokeWidth));
			switch (shape) {
			case "line": { // p: x1 y1 x2 y2 (relative to refX/refY) //$NON-NLS-1$
				if (strokeArgb != null) {
					g.setColor(new Color(strokeArgb, true));
					g.draw(new Line2D.Double(refX + p[0], refY + p[1], refX + p[2], refY + p[3]));
				}
				break;
			}
			case "circle": { // p: cx cy r (cx/cy relative to refX/refY) //$NON-NLS-1$
				double cx = refX + p[0], cy = refY + p[1], r = p[2];
				Ellipse2D.Double oval = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
				if (fillArgb != null) {
					g.setColor(new Color(fillArgb, true));
					g.fill(oval);
				}
				if (strokeArgb != null) {
					g.setColor(new Color(strokeArgb, true));
					g.draw(oval);
				}
				break;
			}
			case "square": { // p: x y side (top-left relative to refX/refY) //$NON-NLS-1$
				Rectangle2D.Double rect = new Rectangle2D.Double(refX + p[0], refY + p[1], p[2], p[2]);
				if (fillArgb != null) {
					g.setColor(new Color(fillArgb, true));
					g.fill(rect);
				}
				if (strokeArgb != null) {
					g.setColor(new Color(strokeArgb, true));
					g.draw(rect);
				}
				break;
			}
			case "rectangle": { // p: x y w h (top-left relative to refX/refY) //$NON-NLS-1$
				Rectangle2D.Double rect = new Rectangle2D.Double(refX + p[0], refY + p[1], p[2], p[3]);
				if (fillArgb != null) {
					g.setColor(new Color(fillArgb, true));
					g.fill(rect);
				}
				if (strokeArgb != null) {
					g.setColor(new Color(strokeArgb, true));
					g.draw(rect);
				}
				break;
			}
			default:
				log.warn("Unknown DRAW shape: {}", shape); //$NON-NLS-1$
				break;
			}
		} finally {
			g.dispose();
		}
		return img;
	}

	// ---- Color spec helpers ----

	/**
	 * Normalizes bare hex color values ({@code "f68"}, {@code "ff6688"}, …) and
	 * {@code 0x}-prefixed hex to {@code #}-prefixed CSS notation, for passthrough
	 * to {@link SvgConverter} (which expects CSS color strings, not parsed ARGB
	 * ints, since it substitutes colors directly into SVG source text).
	 */
	private static String resolveSpecColor(String s) {
		if (s == null || s.trim().isEmpty())
			return null;
		if (s.startsWith("0x") || s.startsWith("0X"))
			return "#" + s.substring(2);
		if (s.matches("[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8}"))
			return "#" + s;
		return s;
	}

	private static final Pattern FX_STYLE_PROPERTY = Pattern.compile("(-fx-[a-zA-Z-]+)\\s*:\\s*([^;]+);?");

	/**
	 * Translates the small subset of JavaFX {@code SVGPath} CSS properties used by
	 * {@code [P]:}/{@code [PI]:}/{@code [PS]:} {@code style} parameters
	 * ({@code -fx-fill}, {@code -fx-stroke}, {@code -fx-stroke-width}) into SVG
	 * presentation attributes, for the synthetic {@code <svg><path .../></svg>}
	 * envelope used to rasterize {@code [PI]:} specs. Returns a string of
	 * space-prefixed XML attributes ready to splice into the {@code <path>} tag.
	 */
	private static String fxStyleToSvgAttributes(String fxStyle) {
		if (fxStyle == null || fxStyle.trim().isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		Matcher m = FX_STYLE_PROPERTY.matcher(fxStyle);
		while (m.find()) {
			String value = m.group(2).trim();
			String prop = m.group(1);
			if ("-fx-fill".equals(prop))
				sb.append(" fill=\"").append(escapeXmlAttribute(value)).append('"');
			else if ("-fx-stroke".equals(prop))
				sb.append(" stroke=\"").append(escapeXmlAttribute(value)).append('"');
			else if ("-fx-stroke-width".equals(prop))
				sb.append(" stroke-width=\"").append(escapeXmlAttribute(value)).append('"');
			// other -fx- properties have no SVG presentation-attribute equivalent
		}
		return sb.toString();
	}

	private static String escapeXmlAttribute(String s) {
		if (s == null)
			return "";
		return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
