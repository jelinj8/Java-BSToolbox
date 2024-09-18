package cz.bliksoft.javautils.files;

import java.io.File;
import java.net.URI;

/**
 * a File extension that makes File AutoClosable (deleted when not needed any
 * more...)
 */
public class DeleteOnCloseFile extends File implements AutoCloseable {

	private static final long serialVersionUID = -4343249899385955378L;

	public DeleteOnCloseFile(File parent, String child) {
		super(parent, child);
	}

	public DeleteOnCloseFile(String parent, String child) {
		super(parent, child);
	}

	public DeleteOnCloseFile(URI uri) {
		super(uri);
	}

	public DeleteOnCloseFile(String pathname) {
		super(pathname);
	}

	@Override
	public void close() throws Exception {
		if (exists()) {
			delete();
		}
	}

}
