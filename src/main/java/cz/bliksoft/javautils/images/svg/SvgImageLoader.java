package cz.bliksoft.javautils.images.svg;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.github.weisj.jsvg.SVGDocument;

import cz.bliksoft.javautils.images.ImageLoader;

/**
 * {@link ImageLoader} for the {@code svg} extension, backed by
 * {@link SvgConverter} (JSVG). Additional args, in order:
 * {@code W, H, scale, stroke, fill} — blank, missing, or the literal string
 * {@code "null"} are treated as unset.
 * <p>
 * JSVG is an optional dependency of this module (see {@code pom.xml}); it is
 * only resolved lazily, the first time an SVG is actually rendered, so
 * registering this loader is safe even when JSVG is absent from the final
 * application's classpath. A missing JSVG surfaces as an {@link IOException} at
 * that point instead of an uncaught {@link LinkageError}.
 */
public class SvgImageLoader extends ImageLoader {

	@Override
	public List<String> getSupportedExtensions() {
		List<String> res = new ArrayList<>();
		res.add("svg");
		return res;
	}

	@Override
	public Image getImage(String name, String... args) throws Exception {
		Float w = parseFloat(args, 0);
		Float h = parseFloat(args, 1);
		Float scale = parseFloat(args, 2);
		String stroke = arg(args, 3);
		String fill = arg(args, 4);
		try {
			return SvgConverter.createImageFromSVG(new File(name), w, h, scale, stroke, fill);
		} catch (LinkageError e) {
			throw jsvgUnavailable(e);
		}
	}

	@Override
	public Image getImage(byte[] data, String... args) throws Exception {
		Float w = parseFloat(args, 0);
		Float h = parseFloat(args, 1);
		Float scale = parseFloat(args, 2);
		try {
			String svgContent = new String(data, StandardCharsets.UTF_8);
			SVGDocument doc = SvgConverter.loadSvgDocumentFromString(svgContent);
			return SvgConverter.createImageFromSVG(doc, w, h, scale);
		} catch (LinkageError e) {
			throw jsvgUnavailable(e);
		}
	}

	private static IOException jsvgUnavailable(Throwable cause) {
		return new IOException(
				"SVG rendering unavailable: JSVG library (com.github.weisj:jsvg) is not on the classpath", cause);
	}

	private static Float parseFloat(String[] args, int idx) {
		String v = arg(args, idx);
		if (v == null)
			return null;
		try {
			return Float.parseFloat(v);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String arg(String[] args, int idx) {
		if (args == null || idx >= args.length)
			return null;
		String v = args[idx];
		if (v == null || v.trim().isEmpty() || "null".equals(v))
			return null;
		return v;
	}

}
