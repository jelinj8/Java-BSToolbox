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

}
