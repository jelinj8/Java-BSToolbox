package cz.bliksoft.javautils.xmlfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

/**
 * {@code replace="true"} replaces the existing node (it used to merge), and a
 * trailing wildcard matches the whole prefix.
 */
class ReplaceAndWildcardTest {

	private static Node parseElement(String xml) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
				.getDocumentElement();
	}

	private static void importFrom(FileObject root, String resourceId, String xml) throws Exception {
		root.importFile(FileObject.createChild(parseElement(xml), null, resourceId, false));
	}

	@Test
	void replaceDropsTheExistingNodeAndItsChildren() throws Exception {
		FileObject root = new FileObject();
		importFrom(root, "first", "<file name=\"x\"><file name=\"a\"/></file>");
		importFrom(root, "second", "<file name=\"x\" replace=\"true\"><file name=\"b\"/></file>");

		FileObject x = root.getFile("x");
		assertEquals(Arrays.asList("b"),
				x.getChildren().stream().map(FileObject::getName).collect(Collectors.toList()));
		assertEquals("second", x.getResourceId());
	}

	@Test
	void replaceRegistersTheIdsOfTheNewSubtree() throws Exception {
		FileObject root = new FileObject();
		importFrom(root, "first", "<file name=\"y\"><file name=\"old\" id=\"replaceTest.old\"/></file>");
		importFrom(root, "second",
				"<file name=\"y\" replace=\"true\"><file name=\"new\" id=\"replaceTest.new\"/></file>");

		assertSame(root.getFile("y/new"), FileObject.getFileByID("replaceTest.new"));
	}

	@Test
	void trailingWildcardMatchesTheWholePrefix() throws Exception {
		FileObject root = new FileObject();
		importFrom(root, "first", "<file name=\"f\"><file name=\"axe\"/><file name=\"abc\"/></file>");

		FileObject f = root.getFile("f");
		assertEquals("abc", f.getFile("ab*").getName());
		assertNull(f.getFile("abd*"));
	}
}
