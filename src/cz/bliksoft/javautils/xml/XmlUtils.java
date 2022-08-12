package cz.bliksoft.javautils.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.streams.xml.ElementWriter;

/**
 * https://www.codenotfound.com/2013/07/jaxb-marshal-element-missing-xmlrootelement-annotation.html
 * zabalení do root objektu: QName qName = new
 * QName("com.codenotfound.jaxb.model", "car"); JAXBElement<Car> root = new
 * JAXBElement<>(qName, Car.class, car); Rozbalení: JAXBElement<Car> root =
 * jaxbUnmarshaller.unmarshal(new StreamSource( file), Car.class); Car car =
 * root.getValue();
 * 
 * @author jjelinek
 *
 */
public class XmlUtils {
	private static Logger log = Logger.getLogger(XmlUtils.class.getName());

	private static Marshaller getMarshaller(Object obj) throws JAXBException {
		JAXBContext context;

		context = JAXBContext.newInstance(obj.getClass());
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		return m;
	}

	public static void marshal(Object obj, Writer writer) throws JAXBException {
		getMarshaller(obj).marshal(obj, writer);
	}

	public static void marshal(Object obj, XMLStreamWriter writer) throws JAXBException {
		getMarshaller(obj).marshal(obj, writer);
	}

	public static void marshal(Object obj, OutputStream writer) throws JAXBException {
		getMarshaller(obj).marshal(obj, writer);
	}

	public static String marshal(Object obj) throws JAXBException {
		StringWriter sw = new StringWriter();
		getMarshaller(obj).marshal(obj, sw);
		return sw.toString();
	}

	public static String marshalSimpleElement(Object obj) throws JAXBException, XMLStreamException {
		StringWriter sw = new StringWriter();
		getMarshaller(obj).marshal(obj, ElementWriter.filter(sw));
		return sw.toString();
	}

	@SuppressWarnings("unchecked")
	public static String marshalUnanotated(Object obj) throws JAXBException {
		StringWriter sw = new StringWriter();
		Marshaller m = getMarshaller(obj);
		QName qName = new QName(null, StringUtils.hasTextDefault(obj.getClass().getSimpleName(), "Object"));
		Class<Object> cls = (Class<Object>) obj.getClass();
		JAXBElement<Object> root = new JAXBElement<Object>(qName, cls, obj);
		m.marshal(root, sw);
		return sw.toString();
	}

	public static Object unmarshal(String xml, Class<?> cls) throws XMLStreamException, JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(cls);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader someSource = factory.createXMLEventReader(new StringReader(xml));
		return jaxbUnmarshaller.unmarshal(someSource, cls).getValue();
	}

	public static Object unmarshal(File xml, Class<?> cls)
			throws XMLStreamException, JAXBException, FileNotFoundException {
		JAXBContext jaxbContext = JAXBContext.newInstance(cls);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader someSource = factory.createXMLEventReader(new FileReader(xml));
		return jaxbUnmarshaller.unmarshal(someSource, cls).getValue();
	}

	public static Object unmarshal(InputStream xml, Class<?> cls)
			throws XMLStreamException, JAXBException, FileNotFoundException {
		JAXBContext jaxbContext = JAXBContext.newInstance(cls);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader someSource = factory.createXMLEventReader(xml);
		return jaxbUnmarshaller.unmarshal(someSource, cls).getValue();
	}

	public static Document convertStringToDocument(String xmlStr)
			throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
		return doc;
	}

	public static Document convertStringToDocument(Reader xmlRdr)
			throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(xmlRdr));
		return doc;
	}

	public static Node convertStringToNode(String xmlStr) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
		Node n = doc.getDocumentElement();
		return n;
	}

	public static String getStringFromDocument(Document doc) {
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.transform(domSource, result);
			return writer.toString();
		} catch (TransformerException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static String getStringFromNode(Node doc) {
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.transform(domSource, result);
			return writer.toString();
		} catch (TransformerException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static Document createDocument(String rootName) {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement(rootName);
			doc.appendChild(rootElement);
			return doc;
		} catch (ParserConfigurationException e) {
			log.warning("Unable to create XML document");
			return null;
		}
	}

	public static String prettyPrintXml(String source) throws Exception {
		Document doc = XmlUtils.convertStringToDocument(source);
		return prettyPrintXml(doc);
	}

	public static String prettyPrintXml(Document doc) throws Exception {
		doc.getDocumentElement().normalize();
		XPathExpression xpath;
		NodeList blankTextNodes = null;
		xpath = XPathFactory.newInstance().newXPath().compile("//text()[normalize-space(.) = '']");
		blankTextNodes = (NodeList) xpath.evaluate(doc, XPathConstants.NODESET);
		for (int i = 0; i < blankTextNodes.getLength(); i++) {
			blankTextNodes.item(i).getParentNode().removeChild(blankTextNodes.item(i));
		}

		Transformer transformer = null;
		transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		DOMSource src = new DOMSource(doc.getDocumentElement());
		transformer.transform(src, result);
		return sw.toString();
	}

}
