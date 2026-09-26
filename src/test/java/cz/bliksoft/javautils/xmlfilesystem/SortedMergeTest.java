package cz.bliksoft.javautils.xmlfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

/**
 * The {@code sorted} flag applies on merge (the last explicit value wins), and
 * sorting is deferred until the children are read in order.
 */
class SortedMergeTest {

	private static Node parseElement(String xml) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
				.getDocumentElement();
	}

	/** imports {@code xml} into {@code root} as if from the resource {@code resourceId} */
	private static void importFrom(FileObject root, String resourceId, String xml) throws Exception {
		root.importFile(FileObject.createChild(parseElement(xml), null, resourceId, false));
	}

	private static List<String> names(List<FileObject> files) {
		return files.stream().map(FileObject::getName).collect(Collectors.toList());
	}

	@Test
	void laterModuleSortsAFolderAnotherModuleCreated() throws Exception {
		FileObject root = new FileObject();
		importFrom(root, "first", "<file name=\"credits\"><file name=\"b\"/><file name=\"a\"/></file>");
		importFrom(root, "second", "<file name=\"credits\" sorted=\"true\"><file name=\"c\"/></file>");

		assertEquals(Arrays.asList("a", "b", "c"), names(root.getFile("credits").getChildren()));
	}

	@Test
	void mergeWithoutTheAttributeKeepsTheFlag() throws Exception {
		FileObject root = new FileObject();
		importFrom(root, "first", "<file name=\"credits\" sorted=\"true\"><file name=\"b\"/></file>");
		importFrom(root, "second", "<file name=\"credits\"><file name=\"a\"/></file>");

		assertEquals(Arrays.asList("a", "b"), names(root.getFile("credits").getChildren()));
	}

	@Test
	void lastExplicitValueWins() throws Exception {
		FileObject root = new FileObject();
		importFrom(root, "first", "<file name=\"credits\" sorted=\"true\"><file name=\"b\"/><file name=\"a\"/></file>");
		importFrom(root, "second", "<file name=\"credits\" sorted=\"false\"><file name=\"c\"/></file>");

		// unsorted: insertion order (the sort is stable)
		assertEquals(Arrays.asList("b", "a", "c"), names(root.getFile("credits").getChildren()));
	}

	@Test
	void positionStillComesBeforeTheName() throws Exception {
		FileObject root = new FileObject();
		importFrom(root, "first", "<file name=\"menu\" sorted=\"true\"><file name=\"a\" position=\"20\"/></file>");
		importFrom(root, "second", "<file name=\"menu\"><file name=\"b\" position=\"10\"/><file name=\"c\" position=\"20\"/></file>");

		assertEquals(Arrays.asList("b", "a", "c"), names(root.getFile("menu").getChildren()));
	}

	@Test
	void sortingIsDeferredToTheFirstOrderedRead() throws Exception {
		FileObject root = new FileObject();
		importFrom(root, "first", "<file name=\"credits\" sorted=\"true\"><file name=\"c\"/><file name=\"b\"/></file>");
		importFrom(root, "second", "<file name=\"credits\"><file name=\"a\"/></file>");
		FileObject credits = root.getFile("credits");

		// exact-name lookups (what the merge uses) don't sort
		credits.getFile("a");
		assertEquals(Arrays.asList("c", "b", "a"), names(credits.children));

		assertEquals(Arrays.asList("a", "b", "c"), names(credits.getChildren()));
		assertEquals(Arrays.asList("a", "b", "c"), names(credits.children));
	}

	@Test
	void wildcardLookupSeesTheSortedOrder() throws Exception {
		FileObject root = new FileObject();
		importFrom(root, "first", "<file name=\"f\" sorted=\"true\"><file name=\"xb\"/><file name=\"ab\"/></file>");

		assertEquals("ab", root.getFile("f").getFile("*b").getName());
	}
}
