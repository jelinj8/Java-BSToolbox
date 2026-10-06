package cz.bliksoft.javautils.images.ico;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import cz.bliksoft.javautils.images.iconspec.IconSpecEngine;

/**
 * Renders an icon spec (see {@link IconSpecEngine}) into a multi-size Windows
 * {@code .ico} - e.g. at build time, an application icon file rendered from an
 * icon spec. Headless, no JavaFX.
 * <p>
 * The spec is rendered once per size: a {@code ${size}} token is replaced by
 * the size, a spec without it is a base path completed to
 * {@code <spec><size>.png}.
 * <p>
 * Command line (e.g. {@code exec-maven-plugin}'s {@code java} goal, with the
 * spec's resources on the classpath):
 *
 * <pre>
 * IcoGenerator [--sizes 16,32,48,256] --spec &lt;iconspec&gt; &lt;target.ico&gt;
 * IcoGenerator [--sizes ...] --xml &lt;xml&gt; --attribute &lt;name&gt; &lt;target.ico&gt;
 * </pre>
 *
 * {@code --xml} takes the spec from the {@code value} of the first
 * {@code <attribute name="...">} element (XML-filesystem format) of a classpath
 * resource ({@code /...}) or a file.
 */
public class IcoGenerator {

	/** Default frame sizes. */
	public static final int[] DEFAULT_SIZES = { 16, 24, 32, 48, 64, 128, 256 };

	private IcoGenerator() {
	}

	/**
	 * Renders {@code iconspec} in each of {@code sizes} into {@code target}.
	 *
	 * @throws IOException if a size can't be rendered or the file can't be written
	 */
	public static void generate(String iconspec, int[] sizes, File target) throws IOException {
		List<BufferedImage> frames = new ArrayList<>(sizes.length);
		for (int s : sizes) {
			String spec = specForSize(iconspec, s);
			BufferedImage img = IconSpecEngine.createImage(spec);
			if (img == null)
				throw new IOException("Could not render iconspec: " + spec);
			frames.add(img);
		}
		if (target.getParentFile() != null)
			target.getParentFile().mkdirs();
		IcoWriter.write(target, frames);
	}

	/** The spec for one size ({@code ${size}} token or base path). */
	public static String specForSize(String iconspec, int size) {
		return iconspec.contains("${size}") ? iconspec.replace("${size}", String.valueOf(size)) //$NON-NLS-1$
				: String.format("%s%d.png", iconspec, size); //$NON-NLS-1$
	}

	/**
	 * The {@code value} of the first {@code <attribute name="attribute">} element
	 * of an XML-filesystem XML: a classpath resource when {@code xml} starts with
	 * {@code /} and exists there, else a file.
	 */
	public static String readSpecFromXml(String xml, String attribute) throws Exception {
		InputStream in = xml.startsWith("/") ? IcoGenerator.class.getResourceAsStream(xml) : null; //$NON-NLS-1$
		if (in == null) {
			File f = new File(xml);
			if (!f.isFile())
				throw new IOException("Neither a classpath resource nor a file: " + xml);
			in = new FileInputStream(f);
		}
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			NodeList attributes = dbf.newDocumentBuilder().parse(in).getElementsByTagNameNS("*", "attribute"); //$NON-NLS-1$ //$NON-NLS-2$
			for (int i = 0; i < attributes.getLength(); i++) {
				Element a = (Element) attributes.item(i);
				if (attribute.equals(a.getAttribute("name"))) //$NON-NLS-1$
					return a.getAttribute("value"); //$NON-NLS-1$
			}
		} finally {
			in.close();
		}
		throw new IOException("No " + attribute + " attribute in " + xml);
	}

	public static void main(String[] args) throws Exception {
		System.setProperty("java.awt.headless", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		int[] sizes = DEFAULT_SIZES;
		String spec = null;
		String xml = null;
		String attribute = null;
		String target = null;
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--sizes": //$NON-NLS-1$
				String[] parts = args[++i].split(","); //$NON-NLS-1$
				sizes = new int[parts.length];
				for (int j = 0; j < parts.length; j++)
					sizes[j] = Integer.parseInt(parts[j].trim());
				break;
			case "--spec": //$NON-NLS-1$
				spec = args[++i];
				break;
			case "--xml": //$NON-NLS-1$
				xml = args[++i];
				break;
			case "--attribute": //$NON-NLS-1$
				attribute = args[++i];
				break;
			default:
				target = args[i];
			}
		}
		if (target == null || (spec == null) == (xml == null) || (xml != null && attribute == null))
			throw new IllegalArgumentException(
					"Usage: IcoGenerator [--sizes 16,32,...] (--spec <iconspec> | --xml <resource|file> --attribute <name>) <target.ico>");

		if (xml != null)
			spec = readSpecFromXml(xml, attribute);
		File f = new File(target);
		generate(spec, sizes, f);
		System.out.println("Icon written: " + f + " (" + spec + ")"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
