package cz.bliksoft.javautils.images;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Parses CSS-style color specifications into ARGB ints — the non-JavaFX
 * equivalent of {@code javafx.scene.paint.Color.web(String)}, scoped to the
 * forms accepted by the icon-spec language: hex (3/4/6/8 hex digits, with or
 * without a leading {@code #} or {@code 0x}), {@code rgb()}/{@code rgba()}
 * functional notation, and the standard CSS3/SVG named-color keywords.
 */
public final class CssColor {

	private static final Logger log = LogManager.getLogger();

	private CssColor() {
	}

	/**
	 * Parses {@code spec} to an ARGB int, returning {@code defaultArgb} if
	 * {@code spec} is {@code null}/empty or cannot be parsed (a warning is logged
	 * in the latter case).
	 */
	public static int parse(String spec, int defaultArgb) {
		if (spec == null || spec.isEmpty())
			return defaultArgb;
		try {
			return parseWeb(normalize(spec));
		} catch (Exception e) {
			log.warn("Invalid color '{}': {}", spec, e.getMessage());
			return defaultArgb;
		}
	}

	/**
	 * Parses {@code spec} to an ARGB int, returning {@code null} if {@code spec} is
	 * {@code null}/empty, the literal {@code "none"}, or cannot be parsed — used
	 * where "no color" is a meaningful, distinct outcome (e.g.
	 * {@code *DRAW}/{@code *TEXT} fill/stroke).
	 */
	public static Integer parseOrNone(String spec) {
		if (spec == null || spec.isEmpty() || "none".equalsIgnoreCase(spec))
			return null;
		try {
			return parseWeb(normalize(spec));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Normalizes bare hex digits ({@code "f68"}, {@code "ff6688"}, …) and
	 * {@code 0x}-prefixed hex to {@code #}-prefixed CSS hex notation; everything
	 * else (named colors, {@code rgb()}/{@code rgba()}, already-{@code #}-prefixed
	 * hex) passes through unchanged.
	 */
	private static String normalize(String s) {
		if (s.startsWith("0x") || s.startsWith("0X"))
			return "#" + s.substring(2);
		if (s.matches("[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8}"))
			return "#" + s;
		return s;
	}

	private static int parseWeb(String colorString) {
		String color = colorString.toLowerCase(Locale.ROOT);

		if (color.startsWith("#")) {
			color = color.substring(1);
		} else if (color.startsWith("0x")) {
			color = color.substring(2);
		} else if (color.startsWith("rgb")) {
			if (color.startsWith("(", 3))
				return parseRgb(color, 4, false);
			if (color.startsWith("a(", 3))
				return parseRgb(color, 5, true);
		} else {
			Integer named = NamedColors.get(color);
			if (named != null)
				return named;
		}

		try {
			switch (color.length()) {
			case 3: {
				int r = Integer.parseInt(color.substring(0, 1), 16) * 17;
				int g = Integer.parseInt(color.substring(1, 2), 16) * 17;
				int b = Integer.parseInt(color.substring(2, 3), 16) * 17;
				return 0xFF000000 | (r << 16) | (g << 8) | b;
			}
			case 4: {
				int r = Integer.parseInt(color.substring(0, 1), 16) * 17;
				int g = Integer.parseInt(color.substring(1, 2), 16) * 17;
				int b = Integer.parseInt(color.substring(2, 3), 16) * 17;
				int a = Integer.parseInt(color.substring(3, 4), 16) * 17;
				return (a << 24) | (r << 16) | (g << 8) | b;
			}
			case 6: {
				int r = Integer.parseInt(color.substring(0, 2), 16);
				int g = Integer.parseInt(color.substring(2, 4), 16);
				int b = Integer.parseInt(color.substring(4, 6), 16);
				return 0xFF000000 | (r << 16) | (g << 8) | b;
			}
			case 8: {
				int r = Integer.parseInt(color.substring(0, 2), 16);
				int g = Integer.parseInt(color.substring(2, 4), 16);
				int b = Integer.parseInt(color.substring(4, 6), 16);
				int a = Integer.parseInt(color.substring(6, 8), 16);
				return (a << 24) | (r << 16) | (g << 8) | b;
			}
			default:
				break;
			}
		} catch (NumberFormatException e) {
			// fall through to the exception below
		}

		throw new IllegalArgumentException("Invalid color specification: " + colorString);
	}

	private static int parseRgb(String color, int off, boolean hasAlpha) {
		int rend = color.indexOf(',', off);
		int gend = rend < 0 ? -1 : color.indexOf(',', rend + 1);
		int bend = gend < 0 ? -1 : color.indexOf(hasAlpha ? ',' : ')', gend + 1);
		int aend = hasAlpha ? (bend < 0 ? -1 : color.indexOf(')', bend + 1)) : bend;
		if (aend < 0)
			throw new IllegalArgumentException("Invalid color specification: " + color);

		int r = toByte(parseComponent(color.substring(off, rend)));
		int g = toByte(parseComponent(color.substring(rend + 1, gend)));
		int b = toByte(parseComponent(color.substring(gend + 1, bend)));
		int a = hasAlpha ? toByte(clamp01(Double.parseDouble(color.substring(bend + 1, aend).trim()))) : 255;
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/**
	 * Parses an {@code rgb()}/{@code rgba()} component: an integer 0-255, or a
	 * percentage.
	 */
	private static double parseComponent(String s) {
		s = s.trim();
		if (s.endsWith("%"))
			return clamp01(Double.parseDouble(s.substring(0, s.length() - 1).trim()) / 100.0);
		double c = Integer.parseInt(s);
		return (c <= 0.0) ? 0.0 : ((c >= 255.0) ? 1.0 : (c / 255.0));
	}

	private static double clamp01(double v) {
		return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
	}

	private static int toByte(double v01) {
		return (int) Math.round(v01 * 255.0);
	}

	/** Standard CSS3/SVG named-color keywords, mapped to their ARGB values. */
	private static final class NamedColors {

		private static final Map<String, Integer> COLORS = build();

		private NamedColors() {
		}

		static Integer get(String name) {
			return COLORS.get(name);
		}

		private static Map<String, Integer> build() {
			Map<String, Integer> colors = new HashMap<>(256);
			colors.put("aliceblue", 0xFFF0F8FF);
			colors.put("antiquewhite", 0xFFFAEBD7);
			colors.put("aqua", 0xFF00FFFF);
			colors.put("aquamarine", 0xFF7FFFD4);
			colors.put("azure", 0xFFF0FFFF);
			colors.put("beige", 0xFFF5F5DC);
			colors.put("bisque", 0xFFFFE4C4);
			colors.put("black", 0xFF000000);
			colors.put("blanchedalmond", 0xFFFFEBCD);
			colors.put("blue", 0xFF0000FF);
			colors.put("blueviolet", 0xFF8A2BE2);
			colors.put("brown", 0xFFA52A2A);
			colors.put("burlywood", 0xFFDEB887);
			colors.put("cadetblue", 0xFF5F9EA0);
			colors.put("chartreuse", 0xFF7FFF00);
			colors.put("chocolate", 0xFFD2691E);
			colors.put("coral", 0xFFFF7F50);
			colors.put("cornflowerblue", 0xFF6495ED);
			colors.put("cornsilk", 0xFFFFF8DC);
			colors.put("crimson", 0xFFDC143C);
			colors.put("cyan", 0xFF00FFFF);
			colors.put("darkblue", 0xFF00008B);
			colors.put("darkcyan", 0xFF008B8B);
			colors.put("darkgoldenrod", 0xFFB8860B);
			colors.put("darkgray", 0xFFA9A9A9);
			colors.put("darkgreen", 0xFF006400);
			colors.put("darkgrey", 0xFFA9A9A9);
			colors.put("darkkhaki", 0xFFBDB76B);
			colors.put("darkmagenta", 0xFF8B008B);
			colors.put("darkolivegreen", 0xFF556B2F);
			colors.put("darkorange", 0xFFFF8C00);
			colors.put("darkorchid", 0xFF9932CC);
			colors.put("darkred", 0xFF8B0000);
			colors.put("darksalmon", 0xFFE9967A);
			colors.put("darkseagreen", 0xFF8FBC8F);
			colors.put("darkslateblue", 0xFF483D8B);
			colors.put("darkslategray", 0xFF2F4F4F);
			colors.put("darkslategrey", 0xFF2F4F4F);
			colors.put("darkturquoise", 0xFF00CED1);
			colors.put("darkviolet", 0xFF9400D3);
			colors.put("deeppink", 0xFFFF1493);
			colors.put("deepskyblue", 0xFF00BFFF);
			colors.put("dimgray", 0xFF696969);
			colors.put("dimgrey", 0xFF696969);
			colors.put("dodgerblue", 0xFF1E90FF);
			colors.put("firebrick", 0xFFB22222);
			colors.put("floralwhite", 0xFFFFFAF0);
			colors.put("forestgreen", 0xFF228B22);
			colors.put("fuchsia", 0xFFFF00FF);
			colors.put("gainsboro", 0xFFDCDCDC);
			colors.put("ghostwhite", 0xFFF8F8FF);
			colors.put("gold", 0xFFFFD700);
			colors.put("goldenrod", 0xFFDAA520);
			colors.put("gray", 0xFF808080);
			colors.put("green", 0xFF008000);
			colors.put("greenyellow", 0xFFADFF2F);
			colors.put("grey", 0xFF808080);
			colors.put("honeydew", 0xFFF0FFF0);
			colors.put("hotpink", 0xFFFF69B4);
			colors.put("indianred", 0xFFCD5C5C);
			colors.put("indigo", 0xFF4B0082);
			colors.put("ivory", 0xFFFFFFF0);
			colors.put("khaki", 0xFFF0E68C);
			colors.put("lavender", 0xFFE6E6FA);
			colors.put("lavenderblush", 0xFFFFF0F5);
			colors.put("lawngreen", 0xFF7CFC00);
			colors.put("lemonchiffon", 0xFFFFFACD);
			colors.put("lightblue", 0xFFADD8E6);
			colors.put("lightcoral", 0xFFF08080);
			colors.put("lightcyan", 0xFFE0FFFF);
			colors.put("lightgoldenrodyellow", 0xFFFAFAD2);
			colors.put("lightgray", 0xFFD3D3D3);
			colors.put("lightgreen", 0xFF90EE90);
			colors.put("lightgrey", 0xFFD3D3D3);
			colors.put("lightpink", 0xFFFFB6C1);
			colors.put("lightsalmon", 0xFFFFA07A);
			colors.put("lightseagreen", 0xFF20B2AA);
			colors.put("lightskyblue", 0xFF87CEFA);
			colors.put("lightslategray", 0xFF778899);
			colors.put("lightslategrey", 0xFF778899);
			colors.put("lightsteelblue", 0xFFB0C4DE);
			colors.put("lightyellow", 0xFFFFFFE0);
			colors.put("lime", 0xFF00FF00);
			colors.put("limegreen", 0xFF32CD32);
			colors.put("linen", 0xFFFAF0E6);
			colors.put("magenta", 0xFFFF00FF);
			colors.put("maroon", 0xFF800000);
			colors.put("mediumaquamarine", 0xFF66CDAA);
			colors.put("mediumblue", 0xFF0000CD);
			colors.put("mediumorchid", 0xFFBA55D3);
			colors.put("mediumpurple", 0xFF9370DB);
			colors.put("mediumseagreen", 0xFF3CB371);
			colors.put("mediumslateblue", 0xFF7B68EE);
			colors.put("mediumspringgreen", 0xFF00FA9A);
			colors.put("mediumturquoise", 0xFF48D1CC);
			colors.put("mediumvioletred", 0xFFC71585);
			colors.put("midnightblue", 0xFF191970);
			colors.put("mintcream", 0xFFF5FFFA);
			colors.put("mistyrose", 0xFFFFE4E1);
			colors.put("moccasin", 0xFFFFE4B5);
			colors.put("navajowhite", 0xFFFFDEAD);
			colors.put("navy", 0xFF000080);
			colors.put("oldlace", 0xFFFDF5E6);
			colors.put("olive", 0xFF808000);
			colors.put("olivedrab", 0xFF6B8E23);
			colors.put("orange", 0xFFFFA500);
			colors.put("orangered", 0xFFFF4500);
			colors.put("orchid", 0xFFDA70D6);
			colors.put("palegoldenrod", 0xFFEEE8AA);
			colors.put("palegreen", 0xFF98FB98);
			colors.put("paleturquoise", 0xFFAFEEEE);
			colors.put("palevioletred", 0xFFDB7093);
			colors.put("papayawhip", 0xFFFFEFD5);
			colors.put("peachpuff", 0xFFFFDAB9);
			colors.put("peru", 0xFFCD853F);
			colors.put("pink", 0xFFFFC0CB);
			colors.put("plum", 0xFFDDA0DD);
			colors.put("powderblue", 0xFFB0E0E6);
			colors.put("purple", 0xFF800080);
			colors.put("red", 0xFFFF0000);
			colors.put("rosybrown", 0xFFBC8F8F);
			colors.put("royalblue", 0xFF4169E1);
			colors.put("saddlebrown", 0xFF8B4513);
			colors.put("salmon", 0xFFFA8072);
			colors.put("sandybrown", 0xFFF4A460);
			colors.put("seagreen", 0xFF2E8B57);
			colors.put("seashell", 0xFFFFF5EE);
			colors.put("sienna", 0xFFA0522D);
			colors.put("silver", 0xFFC0C0C0);
			colors.put("skyblue", 0xFF87CEEB);
			colors.put("slateblue", 0xFF6A5ACD);
			colors.put("slategray", 0xFF708090);
			colors.put("slategrey", 0xFF708090);
			colors.put("snow", 0xFFFFFAFA);
			colors.put("springgreen", 0xFF00FF7F);
			colors.put("steelblue", 0xFF4682B4);
			colors.put("tan", 0xFFD2B48C);
			colors.put("teal", 0xFF008080);
			colors.put("thistle", 0xFFD8BFD8);
			colors.put("tomato", 0xFFFF6347);
			colors.put("transparent", 0x00000000);
			colors.put("turquoise", 0xFF40E0D0);
			colors.put("violet", 0xFFEE82EE);
			colors.put("wheat", 0xFFF5DEB3);
			colors.put("white", 0xFFFFFFFF);
			colors.put("whitesmoke", 0xFFF5F5F5);
			colors.put("yellow", 0xFFFFFF00);
			colors.put("yellowgreen", 0xFF9ACD32);
			return colors;
		}
	}
}
