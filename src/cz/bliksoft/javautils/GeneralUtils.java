package cz.bliksoft.javautils;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeneralUtils {
	private static Logger log = Logger.getLogger(GeneralUtils.class.getName());

	public static boolean browseFile(File f) {
		if (!f.exists()) {
			log.log(Level.SEVERE, Messages.getString("GeneralUtils.FileToBrowseNotFound"), f); //$NON-NLS-1$
			return false;
		}
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			try {
				Desktop.getDesktop().browse(f.toURI());
				return true;
			} catch (IOException e) {
				log.log(Level.SEVERE, Messages.getString("GeneralUtils.FailedToOpenBrowserForFile"), f); //$NON-NLS-1$
			}
		}
		return false;
	}

	public enum OS {
		WINDOWS, LINUX, MAC, SOLARIS
	};// Operating systems.

	private static OS os = null;

	public static OS getOS() {
		if (os == null) {
			String operSys = System.getProperty("os.name").toLowerCase();
			if (operSys.contains("win")) {
				os = OS.WINDOWS;
			} else if (operSys.contains("nix") || operSys.contains("nux") || operSys.contains("aix")) {
				os = OS.LINUX;
			} else if (operSys.contains("mac")) {
				os = OS.MAC;
			} else if (operSys.contains("sunos")) {
				os = OS.SOLARIS;
			}
		}
		return os;
	}

}
