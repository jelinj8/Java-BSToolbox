package cz.bliksoft.javautils.app.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.app.BSAppMessages;
import cz.bliksoft.javautils.exceptions.ViewableException;

/**
 * {@link Properties} backed by an XML file ({@code loadFromXML} /
 * {@code storeToXML}). Loaded from the given file on construction (missing file
 * = empty properties); {@link #save()} writes atomically via a {@code .tmp}
 * file. Adds typed accessors for {@code Double} and {@code Boolean} values.
 */
public class XmlProperties extends Properties {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static Logger log = null;
	private File path;

	// public final Properties properties = new Properties();

	public XmlProperties(File path) {
		super();
		this.path = path;

		if (path.exists()) {
			try (FileInputStream fis = new FileInputStream(path)) {
				this./* properties. */loadFromXML(fis);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void save() throws ViewableException {
		String dir = this.path.getParent();
		File savedir = new File(dir);
		savedir.mkdirs();
		getLogger().finer("Saving properties file " + this.path);
		try {
			File tmpfile = new File(this.path.getPath() + ".tmp");
			try (FileOutputStream fs = new FileOutputStream(tmpfile)) {
				this./* properties. */storeToXML(fs, null);
			}
			path.delete();
			tmpfile.renameTo(path);
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Error saving XML properties file", e);
			throw new ViewableException(BSAppMessages.getString("XmlProperties.savingError"), e);
		}
	}

	public File getPath() {
		return this.path;
	}

	public boolean isWritable() {
		File cfgFile = getPath();
		if (cfgFile.exists() && cfgFile.canWrite())
			return true;
		return cfgFile.getParentFile().canWrite();
	}

	private static Logger getLogger() {
		if (log == null)
			log = Logger.getLogger(XmlProperties.class.getName());
		return log;
	}

	public Double getDouble(String k) {
		String v = getProperty(k);
		if (v == null)
			return null;
		try {
			return Double.parseDouble(v);
		} catch (Exception e) {
			return null;
		}
	}

	public void putDouble(String k, double v) {
		setProperty(k, Double.toString(v));
	}

	public Boolean getBool(String k) {
		String v = getProperty(k);
		return v == null ? null : Boolean.parseBoolean(v);
	}

	public void putBool(String k, boolean v) {
		setProperty(k, Boolean.toString(v));
	}
}
