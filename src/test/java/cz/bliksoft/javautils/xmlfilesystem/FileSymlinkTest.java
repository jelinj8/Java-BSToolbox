package cz.bliksoft.javautils.xmlfilesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

import cz.bliksoft.javautils.exceptions.InitializationException;

class FileSymlinkTest {

	/**
	 * parses a single XML fragment ({@code <file .../>} or {@code <symlink .../>})
	 * into its DOM node
	 */
	private static Node parseElement(String xml) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
				.getDocumentElement();
	}

	private static FileSymlink symlink(FileObject parent, String xml) throws Exception {
		return new FileSymlink(parseElement(xml), parent, "test");
	}

	@Test
	void resolvesByTargetIdWhenAlreadyRegistered() throws Exception {
		FileObject root = new FileObject();
		root.importFile(FileObject.createChild(parseElement("<file id=\"test.targetIdOk\" name=\"TargetAction\"/>"),
				null, "test", false));

		FileSymlink link = symlink(root, "<symlink name=\"link1\" target-id=\"test.targetIdOk\"/>");

		assertEquals("test.targetIdOk", link.getSymlinkTargetId());
		assertNotNull(link.getTargetFile());
		assertEquals("TargetAction", link.getTargetFile().getName());
	}

	@Test
	void pathTakesPriorityOverTargetIdWhenBothGiven() throws Exception {
		FileObject root = new FileObject();
		root.importFile(FileObject.createChild(
				parseElement("<file name=\"container\"><file name=\"pathTarget\"/></file>"), null, "test", false));
		root.importFile(FileObject.createChild(parseElement("<file id=\"test.otherId\" name=\"otherTarget\"/>"), null,
				"test", false));

		FileSymlink link = symlink(root,
				"<symlink name=\"link2\" path=\"/container/pathTarget\" target-id=\"test.otherId\"/>");

		assertNotNull(link.getTargetFile());
		assertEquals("pathTarget", link.getTargetFile().getName());
	}

	@Test
	void missingBothPathAndTargetIdThrows() throws Exception {
		FileObject root = new FileObject();
		assertThrows(InitializationException.class, () -> symlink(root, "<symlink name=\"link3\"/>"));
	}

	@Test
	void nameInheritedFromTargetResolvedByTargetId() throws Exception {
		FileObject root = new FileObject();
		root.importFile(FileObject.createChild(parseElement("<file id=\"test.nameFromId\" name=\"InheritedName1\"/>"),
				null, "test", false));

		FileSymlink link = symlink(root, "<symlink target-id=\"test.nameFromId\"/>");

		assertEquals("InheritedName1", link.getName());
	}

	@Test
	void nameInheritedFromTargetResolvedByPath() throws Exception {
		FileObject root = new FileObject();
		root.importFile(FileObject.createChild(
				parseElement("<file name=\"container2\"><file name=\"InheritedName2\"/></file>"), null, "test", false));

		FileSymlink link = symlink(root, "<symlink path=\"/container2/InheritedName2\"/>");

		assertEquals("InheritedName2", link.getName());
	}

	@Test
	void namelessSymlinkWithUnresolvableTargetThrows() throws Exception {
		FileObject root = new FileObject();
		assertThrows(InitializationException.class,
				() -> symlink(root, "<symlink target-id=\"test.doesNotExistYet\"/>"));
	}

	@Test
	void namelessSymlinkMergesCleanlyIntoParentByInheritedName() throws Exception {
		// catalog entry and symlink live in separate folders, mirroring the real
		// core/availableActions vs core/actions split -- they share a name but must
		// not collide as siblings of the same parent
		FileObject root = new FileObject();
		FileObject catalogFolder = FileObject.createChild(parseElement("<file name=\"catalog\"/>"), null, "test",
				false);
		root.importFile(catalogFolder);
		catalogFolder.importFile(FileObject
				.createChild(parseElement("<file id=\"test.mergeTarget\" name=\"MergedName\"/>"), null, "test", false));

		FileObject actionsFolder = FileObject.createChild(parseElement("<file name=\"actions\"/>"), null, "test",
				false);
		root.importFile(actionsFolder);

		FileObject linkFo = FileObject.createChild(parseElement("<symlink target-id=\"test.mergeTarget\"/>"), null,
				"test", false);
		actionsFolder.importFile(linkFo);

		FileObject found = actionsFolder.getFile("MergedName");
		assertNotNull(found);
		assertSame(linkFo, found);
	}

}
