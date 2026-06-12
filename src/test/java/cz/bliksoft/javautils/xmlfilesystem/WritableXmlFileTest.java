package cz.bliksoft.javautils.xmlfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WritableXmlFileTest {

	private static final String NS = "http://bliksoft.cz/XmlFilesystem";

	@Test
	void standaloneLoadEditSaveReload(@TempDir File tempDir) throws Exception {
		File file = new File(tempDir, "writable.xml");
		Files.write(file.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"a\">\n"
						+ "    <attribute name=\"foo\" value=\"bar\"/>\n" + "    <file name=\"b\"/>\n" + "  </file>\n"
						+ "</root>\n").getBytes(StandardCharsets.UTF_8));

		WritableXmlFile wxf = WritableXmlFile.load(file);
		assertEquals(1, wxf.getRoots().size());

		FileObject a = wxf.getRoots().get(0);
		assertTrue(a.isWritable());
		assertTrue(a instanceof WritableFileObject);
		WritableFileObject wa = (WritableFileObject) a;
		assertEquals("bar", wa.getAttribute("foo"));
		assertNotNull(wa.getFile("b"));

		// mutate: change an attribute and add a new child file
		wa.setAttribute("foo", "baz");
		WritableFileObject c = new WritableFileObject("c", false, wa);
		wa.addChild(c);

		wxf.save();

		// reload from disk and verify the changes round-tripped
		WritableXmlFile reloaded = WritableXmlFile.load(file);
		assertEquals(1, reloaded.getRoots().size());
		FileObject a2 = reloaded.getRoots().get(0);
		assertEquals("baz", a2.getAttribute("foo"));
		assertNotNull(a2.getFile("b"));
		assertNotNull(a2.getFile("c"));

		// original <root> namespace declaration preserved
		String savedXml = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		assertTrue(savedXml.contains("xmlns=\"" + NS + "\""));
	}

	@Test
	void fileSystemModeRwIntegration(@TempDir File tempDir) throws Exception {
		try {
			cz.bliksoft.javautils.EnvironmentUtils.setAppName("WritableXmlFileTest");
		} catch (cz.bliksoft.javautils.exceptions.InitializationException e) {
			// already set by another test in this JVM
		}

		File writableFile = new File(tempDir, "writable.xml");
		Files.write(writableFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"wtest\">\n"
						+ "    <attribute name=\"foo\" value=\"bar\"/>\n" + "  </file>\n" + "</root>\n")
						.getBytes(StandardCharsets.UTF_8));

		File readonlyFile = new File(tempDir, "readonly.xml");
		Files.write(readonlyFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"rotest\"/>\n" + "</root>\n")
						.getBytes(StandardCharsets.UTF_8));

		File filesystemFile = new File(tempDir, "filesystem.xml");
		Files.write(filesystemFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <require path=\"" + writableFile.getAbsolutePath()
						+ "\" mode=\"rw\"/>\n" + "  <require path=\"" + readonlyFile.getAbsolutePath() + "\"/>\n"
						+ "</root>\n").getBytes(StandardCharsets.UTF_8));

		try (FileInputStream stream = new FileInputStream(filesystemFile)) {
			FileSystem.getDefault().importXml(stream, filesystemFile.getPath());
		}

		FileObject wtest = FileSystem.getFile("wtest");
		assertNotNull(wtest);
		assertTrue(wtest.isWritable());
		assertTrue(wtest instanceof WritableFileObject);
		WritableFileObject writableWtest = (WritableFileObject) wtest;
		assertNotNull(writableWtest.getDocument());

		FileObject rotest = FileSystem.getFile("rotest");
		assertNotNull(rotest);
		assertFalse(rotest.isWritable());

		// mutate and persist back to writable.xml via the linked WritableXmlFile
		writableWtest.setAttribute("foo", "changed");
		writableWtest.save();

		WritableXmlFile reloaded = WritableXmlFile.load(writableFile);
		FileObject reloadedRoot = reloaded.getRoots().get(0);
		assertEquals("changed", reloadedRoot.getAttribute("foo"));
	}

	@Test
	void nestedRequireModeRwUnderFile(@TempDir File tempDir) throws Exception {
		try {
			cz.bliksoft.javautils.EnvironmentUtils.setAppName("WritableXmlFileTest-nested");
		} catch (cz.bliksoft.javautils.exceptions.InitializationException e) {
			// already set by another test in this JVM
		}

		// writable.xml contributes children directly (no wrapping "settings" element)
		File writableFile = new File(tempDir, "writable.xml");
		Files.write(writableFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"print\">\n"
						+ "    <attribute name=\"foo\" value=\"bar\"/>\n" + "  </file>\n" + "</root>\n")
						.getBytes(StandardCharsets.UTF_8));

		// filesystem.xml defines a non-writable "settings" folder with the rw require
		// nested inside it
		File filesystemFile = new File(tempDir, "filesystem.xml");
		Files.write(filesystemFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"settings\">\n" + "    <file name=\"other\"/>\n"
						+ "    <require path=\"" + writableFile.getAbsolutePath() + "\" mode=\"rw\"/>\n" + "  </file>\n"
						+ "</root>\n").getBytes(StandardCharsets.UTF_8));

		try (FileInputStream stream = new FileInputStream(filesystemFile)) {
			FileSystem.getDefault().importXml(stream, filesystemFile.getPath());
		}

		FileObject settings = FileSystem.getFile("settings");
		assertNotNull(settings);
		assertFalse(settings.isWritable());
		assertNotNull(settings.getFile("other"));

		FileObject print = settings.getFile("print");
		assertNotNull(print, "nested <require mode=\"rw\"> should import its content under the enclosing <file>");
		assertTrue(print.isWritable());
		assertTrue(print instanceof WritableFileObject);
		assertEquals("bar", print.getAttribute("foo"));

		// mutate and persist back to writable.xml
		WritableFileObject writablePrint = (WritableFileObject) print;
		writablePrint.setAttribute("foo", "changed");
		writablePrint.save();

		WritableXmlFile reloaded = WritableXmlFile.load(writableFile);
		FileObject reloadedPrint = reloaded.getRoots().get(0);
		assertEquals("print", reloadedPrint.getName());
		assertEquals("changed", reloadedPrint.getAttribute("foo"));
	}

	@Test
	void nestedRequireInsideWritableTreeIsRejected(@TempDir File tempDir) throws Exception {
		try {
			cz.bliksoft.javautils.EnvironmentUtils.setAppName("WritableXmlFileTest-nestedRw");
		} catch (cz.bliksoft.javautils.exceptions.InitializationException e) {
			// already set by another test in this JVM
		}

		// inner2.xml: the deepest level, never actually reached
		File inner2 = new File(tempDir, "inner2.xml");
		Files.write(inner2.toPath(), ("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"deepNest\"/>\n" + "</root>\n")
				.getBytes(StandardCharsets.UTF_8));

		// inner1.xml: becomes writable via the outer rw require, but itself
		// contains a further mode="rw" require - not allowed
		File inner1 = new File(tempDir, "inner1.xml");
		Files.write(inner1.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"printNest\">\n" + "    <require path=\""
						+ inner2.getAbsolutePath() + "\" mode=\"rw\"/>\n" + "  </file>\n" + "</root>\n")
						.getBytes(StandardCharsets.UTF_8));

		File filesystemFile = new File(tempDir, "filesystem-nest.xml");
		Files.write(filesystemFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"settingsNest\">\n" + "    <require path=\""
						+ inner1.getAbsolutePath() + "\" mode=\"rw\"/>\n" + "  </file>\n" + "</root>\n")
						.getBytes(StandardCharsets.UTF_8));

		try (FileInputStream stream = new FileInputStream(filesystemFile)) {
			assertThrows(cz.bliksoft.javautils.exceptions.InitializationException.class,
					() -> FileSystem.getDefault().importXml(stream, filesystemFile.getPath()));
		}
	}

	@Test
	void collidingWritableRootIsRejected(@TempDir File tempDir) throws Exception {
		try {
			cz.bliksoft.javautils.EnvironmentUtils.setAppName("WritableXmlFileTest-collide");
		} catch (cz.bliksoft.javautils.exceptions.InitializationException e) {
			// already set by another test in this JVM
		}

		File innerFile = new File(tempDir, "inner-collide.xml");
		Files.write(innerFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"printColl\">\n"
						+ "    <attribute name=\"foo\" value=\"bar\"/>\n" + "  </file>\n" + "</root>\n")
						.getBytes(StandardCharsets.UTF_8));

		// "settings" already has a "printColl" child before the rw require is processed
		File filesystemFile = new File(tempDir, "filesystem-collide.xml");
		Files.write(filesystemFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"settingsColl\">\n"
						+ "    <file name=\"printColl\"/>\n" + "    <require path=\"" + innerFile.getAbsolutePath()
						+ "\" mode=\"rw\"/>\n" + "  </file>\n" + "</root>\n").getBytes(StandardCharsets.UTF_8));

		try (FileInputStream stream = new FileInputStream(filesystemFile)) {
			assertThrows(cz.bliksoft.javautils.exceptions.InitializationException.class,
					() -> FileSystem.getDefault().importXml(stream, filesystemFile.getPath()));
		}
	}

	@Test
	void foreignAttributeOverrideNotPersistedAndUserValuePreserved(@TempDir File tempDir) throws Exception {
		try {
			cz.bliksoft.javautils.EnvironmentUtils.setAppName("WritableXmlFileTest-override");
		} catch (cz.bliksoft.javautils.exceptions.InitializationException e) {
			// already set by another test in this JVM
		}

		File writableFile = new File(tempDir, "writable3.xml");
		Files.write(writableFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"print3\">\n"
						+ "    <attribute name=\"dpi\" value=\"203\"/>\n"
						+ "    <attribute name=\"foo\" value=\"bar\"/>\n" + "  </file>\n" + "</root>\n")
						.getBytes(StandardCharsets.UTF_8));

		// second top-level <file name="settings3"> merges an admin attribute
		// override onto the writable "print3" node added by the first one
		File filesystemFile = new File(tempDir, "filesystem3.xml");
		Files.write(filesystemFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"settings3\">\n" + "    <file name=\"other3\"/>\n"
						+ "    <require path=\"" + writableFile.getAbsolutePath() + "\" mode=\"rw\"/>\n" + "  </file>\n"
						+ "  <file name=\"settings3\">\n" + "    <file name=\"print3\">\n"
						+ "      <attribute name=\"dpi\" value=\"300\"/>\n" + "    </file>\n" + "  </file>\n"
						+ "</root>\n").getBytes(StandardCharsets.UTF_8));

		try (FileInputStream stream = new FileInputStream(filesystemFile)) {
			FileSystem.getDefault().importXml(stream, filesystemFile.getPath());
		}

		FileObject settings3 = FileSystem.getFile("settings3");
		assertNotNull(settings3);
		FileObject print3 = settings3.getFile("print3");
		assertNotNull(print3);
		assertTrue(print3 instanceof WritableFileObject);
		WritableFileObject writablePrint3 = (WritableFileObject) print3;

		// override visible at runtime, user's own "foo" untouched
		assertEquals("300", print3.getAttribute("dpi", null));
		assertEquals("bar", print3.getAttribute("foo", null));

		// override not persisted, user's own value preserved
		writablePrint3.save();
		WritableXmlFile reloaded = WritableXmlFile.load(writableFile);
		FileObject reloadedPrint3 = reloaded.getRoots().get(0);
		assertEquals("203", reloadedPrint3.getAttribute("dpi", null));
		assertEquals("bar", reloadedPrint3.getAttribute("foo", null));

		// user edit persists underneath the override
		writablePrint3.setAttribute("dpi", "150");
		assertEquals("300", print3.getAttribute("dpi", null));
		writablePrint3.save();
		WritableXmlFile reloaded2 = WritableXmlFile.load(writableFile);
		FileObject reloadedPrint3b = reloaded2.getRoots().get(0);
		assertEquals("150", reloadedPrint3b.getAttribute("dpi", null));
	}

	@Test
	void foreignRemoveOfWritableNodeIsRejected(@TempDir File tempDir) throws Exception {
		try {
			cz.bliksoft.javautils.EnvironmentUtils.setAppName("WritableXmlFileTest-remove");
		} catch (cz.bliksoft.javautils.exceptions.InitializationException e) {
			// already set by another test in this JVM
		}

		File writableFile = new File(tempDir, "writable5.xml");
		Files.write(writableFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"print5\">\n"
						+ "    <attribute name=\"foo\" value=\"bar\"/>\n" + "  </file>\n" + "</root>\n")
						.getBytes(StandardCharsets.UTF_8));

		// second top-level <file name="settings5"> tries to remove the writable
		// "print5" node
		File filesystemFile = new File(tempDir, "filesystem5.xml");
		Files.write(filesystemFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"settings5\">\n" + "    <require path=\""
						+ writableFile.getAbsolutePath() + "\" mode=\"rw\"/>\n" + "  </file>\n"
						+ "  <file name=\"settings5\">\n" + "    <file name=\"print5\" remove=\"true\"/>\n"
						+ "  </file>\n" + "</root>\n").getBytes(StandardCharsets.UTF_8));

		try (FileInputStream stream = new FileInputStream(filesystemFile)) {
			assertThrows(cz.bliksoft.javautils.exceptions.InitializationException.class,
					() -> FileSystem.getDefault().importXml(stream, filesystemFile.getPath()));
		}
	}

	@Test
	void targetBasedOverrideAppliesToWritableNode(@TempDir File tempDir) throws Exception {
		try {
			cz.bliksoft.javautils.EnvironmentUtils.setAppName("WritableXmlFileTest-target");
		} catch (cz.bliksoft.javautils.exceptions.InitializationException e) {
			// already set by another test in this JVM
		}

		File writableFile = new File(tempDir, "writable4.xml");
		Files.write(writableFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"print4\" id=\"printDefault4\">\n"
						+ "    <attribute name=\"dpi\" value=\"203\"/>\n" + "  </file>\n" + "</root>\n")
						.getBytes(StandardCharsets.UTF_8));

		// "print4" is contributed as a top-level rw require (no wrapping folder), the
		// second top-level <file target="printDefault4"> overrides it by id
		File filesystemFile = new File(tempDir, "filesystem4.xml");
		Files.write(filesystemFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <require path=\"" + writableFile.getAbsolutePath()
						+ "\" mode=\"rw\"/>\n" + "  <file name=\"override4\" target=\"printDefault4\">\n"
						+ "    <attribute name=\"dpi\" value=\"300\"/>\n" + "  </file>\n" + "</root>\n")
						.getBytes(StandardCharsets.UTF_8));

		try (FileInputStream stream = new FileInputStream(filesystemFile)) {
			FileSystem.getDefault().importXml(stream, filesystemFile.getPath());
		}

		FileObject print4 = FileSystem.getFile("print4");
		assertNotNull(print4);
		assertTrue(print4 instanceof WritableFileObject);
		WritableFileObject writablePrint4 = (WritableFileObject) print4;

		// override visible at runtime
		assertEquals("300", print4.getAttribute("dpi", null));

		// override not persisted
		writablePrint4.save();
		WritableXmlFile reloaded = WritableXmlFile.load(writableFile);
		FileObject reloadedPrint4 = reloaded.getRoots().get(0);
		assertEquals("203", reloadedPrint4.getAttribute("dpi", null));
	}

	@Test
	void nestedRequireModeRwUnderFileWithIdDoesNotDoubleRegister(@TempDir File tempDir) throws Exception {
		try {
			cz.bliksoft.javautils.EnvironmentUtils.setAppName("WritableXmlFileTest-nestedId");
		} catch (cz.bliksoft.javautils.exceptions.InitializationException e) {
			// already set by another test in this JVM
		}

		File writableFile = new File(tempDir, "writable6.xml");
		Files.write(writableFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"print6\" id=\"printDefault6\">\n"
						+ "    <attribute name=\"foo\" value=\"bar\"/>\n" + "  </file>\n" + "</root>\n")
						.getBytes(StandardCharsets.UTF_8));

		// non-writable "settings6" wrapping a mode="rw" require whose root has an id=
		File filesystemFile = new File(tempDir, "filesystem6.xml");
		Files.write(filesystemFile.toPath(),
				("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"settings6\">\n" + "    <file name=\"other6\"/>\n"
						+ "    <require path=\"" + writableFile.getAbsolutePath() + "\" mode=\"rw\"/>\n" + "  </file>\n"
						+ "</root>\n").getBytes(StandardCharsets.UTF_8));

		try (FileInputStream stream = new FileInputStream(filesystemFile)) {
			FileSystem.getDefault().importXml(stream, filesystemFile.getPath());
		}

		FileObject settings6 = FileSystem.getFile("settings6");
		assertNotNull(settings6);
		FileObject print6 = settings6.getFile("print6");
		assertNotNull(print6);
		assertTrue(print6 instanceof WritableFileObject);
		assertEquals(print6, FileObject.getFileByID("printDefault6"));
	}

	@Test
	void getCreateFileCreatesMissingPathSegments(@TempDir File tempDir) throws Exception {
		File file = new File(tempDir, "writable.xml");
		Files.write(file.toPath(), ("<root xmlns=\"" + NS + "\">\n" + "  <file name=\"a\">\n"
				+ "    <file name=\"b\"/>\n" + "  </file>\n" + "</root>\n").getBytes(StandardCharsets.UTF_8));

		WritableXmlFile wxf = WritableXmlFile.load(file);
		WritableFileObject a = (WritableFileObject) wxf.getRoots().get(0);

		// existing path is returned as-is
		WritableFileObject b = a.getCreateFile("b");
		assertNotNull(b);
		assertNotNull(a.getFile("b"));

		// missing intermediate folders and leaf are created
		WritableFileObject d = a.getCreateFile("c/d");
		assertNotNull(d);
		assertEquals("d", d.getName());
		WritableFileObject c = (WritableFileObject) a.getFile("c");
		assertNotNull(c);
		assertNotNull(c.getFile("d"));

		// calling again with the same path returns the same object
		assertEquals(d, a.getCreateFile("c/d"));

		// subpaths overload behaves the same as a single joined path
		WritableFileObject f = a.getCreateFile("e", "f");
		assertNotNull(a.getFile("e"));
		assertNotNull(((WritableFileObject) a.getFile("e")).getFile("f"));
		assertEquals(f, a.getCreateFile("e/f"));

		wxf.save();

		WritableXmlFile reloaded = WritableXmlFile.load(file);
		FileObject a2 = reloaded.getRoots().get(0);
		assertNotNull(a2.getFile("c"));
		assertNotNull(a2.getFile("c").getFile("d"));
		assertNotNull(a2.getFile("e"));
		assertNotNull(a2.getFile("e").getFile("f"));
	}
}
