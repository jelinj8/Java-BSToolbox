package cz.bliksoft.javautils.modules;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

import cz.bliksoft.javautils.StringUtils;

/**
 *
 */
public abstract class ModuleBase implements IModule {

	private boolean moduleEnabled = true;

	@Override
	public InputStream getFilesystemXml() {
		String path = getClass().getPackage().getName().replace('.', '/');
		return ClassLoader.getSystemResourceAsStream(path + "/" + getClass().getSimpleName() + ".xml"); //$NON-NLS-1$
	}

	@Override
	public void init() {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void cleanup() {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void install() {
		// throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public HashMap<String, String> getTranslations() {
		return null;
	}

	@Override
	public boolean isEnabled() {
		return moduleEnabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		moduleEnabled = enabled;
	}

	/**
	 * default read git.properties from module root, alternatively use IVersionInfo
	 * with a template-generated implementation.
	 */
	@Override
	public String getVersionInfo() {
		return readVersionFor(this.getClass());
	}

	@Override
	public int getModuleLoadingOrder() {
		return 0;
	}

	protected static String readVersionFor(Class<?> anchor) {
		try {
			// URL to the .class inside its container (dir or jar)
			String classRes = anchor.getSimpleName() + ".class";
			URL classUrl = anchor.getResource(classRes);
			if (classUrl == null)
				return "Unknown version (class URL not found)";

			String s = classUrl.toString();

			// If inside a jar: jar:file:/path/plugin.jar!/pkg/MyClass.class
			if (s.startsWith("jar:")) {
				int bang = s.indexOf("!/");
				if (bang < 0)
					return "Unknown version (bad jar URL)";

				String jarRoot = s.substring(0, bang + 2); // includes "!/"
				URL propsUrl = new java.net.URI(jarRoot + "git.properties").toURL();

				try (InputStream is = propsUrl.openStream()) {
					return formatVersion(load(is));
				}
			}

			// If in exploded classes dir: file:/.../target/classes/pkg/MyClass.class
			if (s.startsWith("file:")) {
				// walk up from .../pkg/MyClass.class to .../ (classes root)
				java.nio.file.Path classFile = java.nio.file.Paths.get(classUrl.toURI());
				String pkg = anchor.getPackage().getName();
				int depth = (pkg == null || !StringUtils.hasText(pkg)) ? 0 : pkg.split("\\.").length;

				java.nio.file.Path classesRoot = classFile.getParent();
				for (int i = 0; i < depth; i++)
					classesRoot = classesRoot.getParent();

				java.nio.file.Path propsFile = classesRoot.resolve("git.properties");
				try (InputStream is = java.nio.file.Files.newInputStream(propsFile)) {
					return formatVersion(load(is));
				}
			}

			return "Unknown version (unsupported URL scheme: " + classUrl + ")";
		} catch (Exception e) {
			return "Failed to read version info: " + e.getMessage();
		}
	}

	private static java.util.Properties load(java.io.InputStream is) throws java.io.IOException {
		Properties p = new java.util.Properties();
		p.load(is);
		return p;
	}

	private static String formatVersion(java.util.Properties props) {
		String version = props.getProperty("git.build.version", "?");
		String closestTagCommitCount = props.getProperty("git.closest.tag.commit.count", "");
		String closestTag = props.getProperty("git.closest.tag.name", "");
		String commitId = props.getProperty("git.commit.id.abbrev", "");
		String branch = props.getProperty("git.branch", "");
		String tags = props.getProperty("git.tags", "");

		StringBuilder sb = new StringBuilder();
		sb.append(version);

		if (StringUtils.hasText(closestTag)) {
			sb.append(" (").append(closestTag);
			if (StringUtils.hasText(closestTagCommitCount))
				sb.append("+").append(closestTagCommitCount);
			sb.append(")");
		}

		if (StringUtils.hasText(branch) || !StringUtils.hasText(commitId)) {
			sb.append(" [").append(branch).append(":").append(commitId).append("]");
		}

		if (StringUtils.hasText(tags))
			sb.append(" ").append(tags);

		return sb.toString();
	}
}
