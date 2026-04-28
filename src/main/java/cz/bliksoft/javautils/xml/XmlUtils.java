package cz.bliksoft.javautils.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import cz.bliksoft.javautils.StringUtils;
import cz.bliksoft.javautils.streams.xml.ElementWriter;
import cz.bliksoft.javautils.xml.xpath.ChooseXPathFunction;
import cz.bliksoft.javautils.xml.xpath.Default;
import cz.bliksoft.javautils.xml.xpath.First;
import cz.bliksoft.javautils.xml.xpath.FormatXPathFunction;
import cz.bliksoft.javautils.xml.xpath.IfElseIf;
import cz.bliksoft.javautils.xml.xpath.Join;
import cz.bliksoft.javautils.xml.xpath.LogFunction;
import cz.bliksoft.javautils.xml.xpath.MapXPathFunction;
import cz.bliksoft.javautils.xml.xpath.UuidFunction;
import cz.bliksoft.javautils.xml.xpath.XPathVarCache;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 * https://www.codenotfound.com/2013/07/jaxb-marshal-element-missing-xmlrootelement-annotation.html
 * <ul>
 * <li>zabalení do root objektu:
 * {@code qName = new QName("com.codenotfound.jaxb.model", "car");
 * JAXBElement<Car> root = new JAXBElement<>(qName, Car.class, car);}
 * 
 * <li>Rozbalení:
 * {@code JAXBElement<Car> root = jaxbUnmarshaller.unmarshal(new StreamSource(file), Car.class);
 * Car car = root.getValue();}
 * </ul>
 * 
 * @author jelinj8
 *
 */
public class XmlUtils {
	private static Logger log = Logger.getLogger(XmlUtils.class.getName());

	private static String xslUrl = null;
	private static String oneTimeXslUrl = null;

	private static Marshaller getMarshaller(Class<?>... classes) throws JAXBException {
		JAXBContext context;

		context = JAXBContext.newInstance(classes);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		if (oneTimeXslUrl != null) {
			m.setProperty("com.sun.xml.internal.bind.xmlHeaders",
					"<?xml-stylesheet type='text/xsl' href=\"" + oneTimeXslUrl + "\" ?>");
			oneTimeXslUrl = null;
		} else if (xslUrl != null)
			m.setProperty("com.sun.xml.internal.bind.xmlHeaders",
					"<?xml-stylesheet type='text/xsl' href=\"" + xslUrl + "\" ?>");
		return m;
	}

	public static Document getDocument(Object obj) throws JAXBException {
		return getDocument(obj, obj.getClass());
	}

	public static Document getDocument(Object obj, Class<?>... classes) throws JAXBException {
		Marshaller m = getMarshaller(classes);
		DOMResult result = new DOMResult();
		m.marshal(obj, result);
		return (Document) result.getNode();
	}

	public static void marshal(Object obj, Writer writer) throws JAXBException {
		marshal(obj, writer, obj.getClass());
	}

	public static void marshal(Object obj, Writer writer, Class<?>... classes) throws JAXBException {
		getMarshaller(classes).marshal(obj, writer);
	}

	public static void marshal(Object obj, File target) throws JAXBException, IOException {
		marshal(obj, target, obj.getClass());
	}

	public static void marshal(Object obj, File target, Class<?>... classes) throws JAXBException, IOException {
		try (FileWriter fw = new FileWriter(target)) {
			getMarshaller(classes).marshal(obj, fw);
		}
	}

	public static void marshal(Object obj, XMLStreamWriter writer) throws JAXBException {
		marshal(obj, writer, obj.getClass());
	}

	public static void marshal(Object obj, XMLStreamWriter writer, Class<?>... classes) throws JAXBException {
		getMarshaller(classes).marshal(obj, writer);
	}

	public static void marshal(Object obj, OutputStream writer) throws JAXBException {
		marshal(obj, writer, obj.getClass());
	}

	public static void marshal(Object obj, OutputStream writer, Class<?>... classes) throws JAXBException {
		getMarshaller(classes).marshal(obj, writer);
	}

	public static Document marshall(Object obj) throws JAXBException {
		Marshaller m = getMarshaller(obj.getClass());
		DOMResult result = new DOMResult();
		m.marshal(obj, result);
		return (Document) result.getNode();
	}

	public static String marshal(Object obj) throws JAXBException {
		return marshal(obj, obj.getClass());
	}

	public static String marshal(Object obj, Class<?>... classes) throws JAXBException {
		StringWriter sw = new StringWriter();
		getMarshaller(classes).marshal(obj, sw);
		return sw.toString();
	}

	public static String marshalSimpleElement(Object obj) throws JAXBException, XMLStreamException {
		return marshalSimpleElement(obj, obj.getClass());
	}

	public static String marshalSimpleElement(Object obj, Class<?>... classes)
			throws JAXBException, XMLStreamException {
		StringWriter sw = new StringWriter();
		getMarshaller(classes).marshal(obj, ElementWriter.filter(sw));
		return sw.toString();
	}

	public static String marshalUnanotated(Object obj) throws JAXBException {
		return marshalUnanotated(obj, obj.getClass());
	}

	@SuppressWarnings("unchecked")
	public static String marshalUnanotated(Object obj, Class<?>... classes) throws JAXBException {
		StringWriter sw = new StringWriter();
		Marshaller m = getMarshaller(classes);
		QName qName = new QName(null, StringUtils.hasTextDefault(obj.getClass().getSimpleName(), "Object"));
		Class<Object> cls = (Class<Object>) obj.getClass();
		JAXBElement<Object> root = new JAXBElement<Object>(qName, cls, obj);
		m.marshal(root, sw);
		return sw.toString();
	}

	public static Document getDocumentUnanotated(Object obj) throws JAXBException {
		return getDocumentUnanotated(obj, obj.getClass());
	}

	@SuppressWarnings("unchecked")
	public static Document getDocumentUnanotated(Object obj, Class<?>... classes) throws JAXBException {
		Marshaller m = getMarshaller(classes);
		QName qName = new QName(null, StringUtils.hasTextDefault(obj.getClass().getSimpleName(), "Object"));
		Class<Object> cls = (Class<Object>) obj.getClass();
		JAXBElement<Object> root = new JAXBElement<Object>(qName, cls, obj);

		DOMResult result = new DOMResult();
		m.marshal(root, result);
		return (Document) result.getNode();
	}

	public static Object unmarshal(String xml, Class<?> cls) throws XMLStreamException, JAXBException {
		return unmarshal(xml, cls, cls);
	}

	public static Object unmarshal(String xml, Class<?> cls, Class<?>... classes)
			throws XMLStreamException, JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(classes);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader someSource = factory.createXMLEventReader(new StringReader(xml));
		return jaxbUnmarshaller.unmarshal(someSource, cls).getValue();
	}

	public static Object unmarshal(Node xml, Class<?> cls) throws XMLStreamException, JAXBException {
		return unmarshal(xml, cls, cls);
	}

	public static Object unmarshal(Node xml, Class<?> cls, Class<?>... classes)
			throws XMLStreamException, JAXBException {
		DOMSource xmlSource = new DOMSource(xml);
		JAXBContext jaxbContext = JAXBContext.newInstance(classes);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		return jaxbUnmarshaller.unmarshal(xmlSource, cls).getValue();
	}

	public static Object unmarshal(File xml, Class<?> cls)
			throws XMLStreamException, JAXBException, FileNotFoundException {
		return unmarshal(xml, cls, cls);
	}

	public static Object unmarshal(File xml, Class<?> cls, Class<?>... classes)
			throws XMLStreamException, JAXBException, FileNotFoundException {
		JAXBContext jaxbContext = JAXBContext.newInstance(classes);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader someSource = null;
		FileReader rdr = null;
		try {
			rdr = new FileReader(xml);
			someSource = factory.createXMLEventReader(rdr);
			return jaxbUnmarshaller.unmarshal(someSource, cls).getValue();
		} finally {
			if (someSource != null)
				someSource.close();
			if (rdr != null)
				try {
					rdr.close();
				} catch (IOException e) {
				}
		}
	}

	public static Object unmarshal(InputStream xml, Class<?> cls)
			throws XMLStreamException, JAXBException, FileNotFoundException {
		return unmarshal(xml, cls, cls);
	}

	public static Object unmarshal(InputStream xml, Class<?> cls, Class<?>... classes)
			throws XMLStreamException, JAXBException, FileNotFoundException {
		JAXBContext jaxbContext = JAXBContext.newInstance(classes);
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
		builder.setErrorHandler(new XMLErrorHandler());
		Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
		return doc;
	}

	public static Document convertStringToDocument(Reader xmlRdr)
			throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		builder.setErrorHandler(new XMLErrorHandler());
		Document doc = builder.parse(new InputSource(xmlRdr));
		return doc;
	}

	public static Node convertStringToNode(String xmlStr) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		builder.setErrorHandler(new XMLErrorHandler());
		Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
		Node n = doc.getDocumentElement();
		return n;
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

	public static Document createDocument(InputStream xmlStream)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// optional, but recommended
		// process XML securely, avoid attacks like XML External Entities (XXE)
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

		// parse XML file
		DocumentBuilder db = dbf.newDocumentBuilder();
		db.setErrorHandler(new XMLErrorHandler());
		Document doc = db.parse(xmlStream);
		return doc;
	}

	public static Document createDocument(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// optional, but recommended
		// process XML securely, avoid attacks like XML External Entities (XXE)
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

		// parse XML file
		DocumentBuilder db = dbf.newDocumentBuilder();
		db.setErrorHandler(new XMLErrorHandler());
		Document doc = db.parse(xmlFile);
		return doc;
	}

	public static void writeNode(Node doc, File output) throws IOException {
		try {
			DOMSource domSource = new DOMSource(doc);
			try (FileWriter writer = new FileWriter(output)) {
				StreamResult result = new StreamResult(writer);
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer transformer = tf.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.transform(domSource, result);
			}
		} catch (TransformerException ex) {
			ex.printStackTrace();
		}
	}

	public static Document createDocument(String rootName) {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			docBuilder.setErrorHandler(new XMLErrorHandler());
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

	public static String prettyPrintXml(Node doc) throws Exception {
		doc.normalize();
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
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		DOMSource src = new DOMSource((doc instanceof Document ? ((Document) doc).getDocumentElement() : doc));
		transformer.transform(src, result);
		return sw.toString();
	}

	public static boolean checkIfNodeExists(Node document, String xpathExpression) throws XPathExpressionException {
		// Create XPathExpression object
		XPathExpression expr = compileXPath(xpathExpression);
		return checkIfNodeExists(document, expr);
	}

	public static boolean checkIfNodeExists(Node document, XPathExpression expr) throws XPathExpressionException {
		// Evaluate expression result on XML document
		NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);

		if (nodes != null && nodes.getLength() > 0) {
			return true;
		}
		return false;
	}

	public static XPathExpression compileXPath(String expression) throws XPathExpressionException {
		// Create XPathFactory object
		XPathFactory xpathFactory = XPathFactory.newInstance();

		// Create XPath object
		XPath xpath = xpathFactory.newXPath();

		if (variableResolver != null)
			xpath.setXPathVariableResolver(variableResolver);

		if (namespaceContext != null)
			xpath.setNamespaceContext(namespaceContext);

		if (defaultFunctionResolver != null)
			xpath.setXPathFunctionResolver(defaultFunctionResolver);

		return xpath.compile(expression);
	}

	private static MapVariableResolver variableResolver = null;

	public static MapVariableResolver getVariableResolver() {
		if (variableResolver == null)
			variableResolver = new MapVariableResolver();
		return variableResolver;
	}

	public static class MapVariableResolver extends HashMap<String, Object> implements XPathVariableResolver {
		private static final long serialVersionUID = -5882477376800172743L;

		@Override
		public Object resolveVariable(QName varName) {
			// if using namespaces, there's more to do here
			String key = varName.getLocalPart();
			return get(key);
		}
	}

	private static BasicNamespaceContext namespaceContext = null;

	public static BasicNamespaceContext getDefaultNamespaceContext() {
		if (namespaceContext == null)
			namespaceContext = new BasicNamespaceContext();
		return namespaceContext;
	}

	public static class BasicNamespaceContext extends HashMap<String, String> implements NamespaceContext {
		private static final long serialVersionUID = 1L;

		public String getNamespaceURI(String prefix) {
			String res = get(prefix);
			if (res != null)
				return res;
			else
				return XMLConstants.NULL_NS_URI;
		}

		public String getPrefix(String namespace) {
			for (Entry<String, String> ns : entrySet()) {
				if (namespace.equals(ns.getValue()))
					return ns.getKey();
			}
			return null;
		}

		public Iterator<String> getPrefixes(String namespace) {
			List<String> prefixes = new ArrayList<>();
			for (Entry<String, String> e : entrySet())
				if (namespace.equals(e.getValue()))
					prefixes.add(e.getKey());
			return prefixes.iterator();
		}
	}

	private static BasicFunctionResolver defaultFunctionResolver = null;

	public static BasicFunctionResolver getDefaultFunctionResolver() {
		if (defaultFunctionResolver == null)
			defaultFunctionResolver = new BasicFunctionResolver();
		return defaultFunctionResolver;
	}

	/**
	 * XPath custom functionsregister
	 * 
	 * @author jelinj8
	 *
	 */
	public static class BasicFunctionResolver implements XPathFunctionResolver {
		BasicNamespaceContext ctx = null;

		Map<String, Map<String, XPathFunction>> functions = new HashMap<>();

		/**
		 * register a function
		 * 
		 * @param prefix       namespace prefix
		 * @param fName        function name, for specific arity add e.g. "fname:3"
		 *                     (attempt for specific first, non-specific if not found)
		 * @param functionImpl implementing object
		 */
		public void addFunction(String prefix, String fName, XPathFunction functionImpl) {
			Map<String, XPathFunction> pFunctions = functions.get(prefix);
			if (pFunctions == null) {
				pFunctions = new HashMap<>();
				functions.put(prefix, pFunctions);
			}

			pFunctions.put(fName, functionImpl);
		}

		public XPathFunction resolveFunction(QName fname, int arity) {
			if (fname == null)
				throw new NullPointerException("The XPath function name cannot be null.");
			String selector = fname.getLocalPart();
			XPathFunction res = null;
			Map<String, XPathFunction> reg;

			if (StringUtils.hasLength(fname.getPrefix())) {
				reg = functions.get(fname.getPrefix());
				if (reg == null)
					return null;
				res = reg.get(selector + ":" + arity);
				if (res != null)
					return res;
				res = reg.get(selector);
				if (res != null)
					return res;
				log.severe(MessageFormat.format("Failed to get function {0}:{1} with arity {2}", fname.getPrefix(),
						selector, arity));
			} else {
				reg = functions.get(ctx.getPrefix(fname.getNamespaceURI()));
				if (reg == null)
					return null;
				res = reg.get(selector + ":" + arity);
				if (res != null)
					return res;
				res = functions.get(ctx.getPrefix(fname.getNamespaceURI())).get(selector);
				if (res != null)
					return res;
				log.severe(MessageFormat.format("Failed to get function {0}:{1} with arity {2}",
						ctx.getPrefix(fname.getNamespaceURI()), selector, arity));
			}
			return null;
		}

		public void registerXPathExtensions() {
			registerXPathExtensions(getDefaultNamespaceContext(), "bsExt");
		}

		public void registerXPathExtensions(BasicNamespaceContext ctx, String nsPrefix) {
			registerXPathExtensions(ctx, nsPrefix, getDefaultFunctionResolver());
		}

		public void registerXPathExtensions(BasicNamespaceContext ctx, String nsPrefix,
				BasicFunctionResolver functionResolver) {
			this.ctx = ctx;
			ctx.put(nsPrefix, "http://bliksoft.cz");
			functionResolver.addFunction(nsPrefix, "choose:3", new ChooseXPathFunction());
			functionResolver.addFunction(nsPrefix, "var:2", new XPathVarCache());
			functionResolver.addFunction(nsPrefix, "default:2", new Default());
			functionResolver.addFunction(nsPrefix, "log", new LogFunction());
			functionResolver.addFunction(nsPrefix, "map", new MapXPathFunction());
			functionResolver.addFunction(nsPrefix, "format", new FormatXPathFunction());
			functionResolver.addFunction(nsPrefix, "ifElseIf", new IfElseIf());
			functionResolver.addFunction(nsPrefix, "first", new First());
			functionResolver.addFunction(nsPrefix, "join", new Join());
			functionResolver.addFunction(nsPrefix, "uuid", new UuidFunction());
		}
	}

	/**
	 * return text representation of object
	 * 
	 * @param xRes
	 * @return
	 * @throws XPathException when input is an empty node list (e.g. XPath query
	 *                        returned no nodes)
	 */
	public static String getResultText(Object xRes) throws XPathException {
		if (xRes == null)
			return null;
		if (xRes instanceof String)
			return (String) xRes;

		Object xRes2 = xRes;
		while (xRes2 instanceof NodeList) {
			if (xRes2 instanceof Text)
				return ((Text) xRes2).getNodeValue();

			NodeList nl = (NodeList) xRes2;
			xRes2 = nl.item(0);
			if (xRes2 == null)
				throw new XPathException("Can't get string from an empty node list");
		}

		return ((Node) xRes2).getNodeValue();
	}

	/**
	 * Returns first node
	 * 
	 * @param xRes
	 * @return
	 * @throws XPathException if source is an empty NodeList
	 */
	public static Node getFirstResultNode(Object xRes) throws XPathException {
		if (xRes == null)
			return null;
		Object xRes2 = xRes;
		while (xRes2 instanceof NodeList) {
			if (xRes2 instanceof Node)
				return ((Node) xRes2);

			xRes2 = ((NodeList) xRes2).item(0);

			if (xRes2 == null)
				throw new XPathException("Can't get string from an empty node list");
		}

		return (Node) xRes2;
	}

	/**
	 * register XPath functions with default "bsExt" namespace prefix, using default
	 * global namespace context implementation
	 */
	public static void registerXPathExtensions() {
		registerXPathExtensions("bsExt");
	}

	/**
	 * register XPath extensions with custom namespace prefix, using default global
	 * namespace context implementation
	 * 
	 * @param nsPrefix
	 */
	public static void registerXPathExtensions(String nsPrefix) {
		getDefaultFunctionResolver().registerXPathExtensions(getDefaultNamespaceContext(), nsPrefix);
	}

	/**
	 * register XPath extensions with custom namespace prefix and provided namespace
	 * context implementation
	 * 
	 * @param ctx
	 * @param nsPrefix
	 */
	public static void registerXPathExtensions(BasicNamespaceContext ctx, String nsPrefix) {
		getDefaultFunctionResolver().registerXPathExtensions(ctx, nsPrefix);
	}

	public static void removeWhitespaces(Node doc) throws XPathExpressionException {
		doc.normalize();
		XPathFactory xpathFactory = XPathFactory.newInstance();
		// XPath to find empty text nodes.
		XPathExpression xpathExp = xpathFactory.newXPath().compile("//text()[normalize-space(.) = '']");
		NodeList emptyTextNodes = (NodeList) xpathExp.evaluate(doc, XPathConstants.NODESET);

		// Remove each empty text node from document.
		for (int i = 0; i < emptyTextNodes.getLength(); i++) {
			Node emptyTextNode = emptyTextNodes.item(i);
			emptyTextNode.getParentNode().removeChild(emptyTextNode);
		}
	}

	public static void transformDocument(Node doc, StreamSource template, StreamResult target)
			throws TransformerException {
		transformDocument(doc, template, target, null);
	}

	public static void transformDocument(Node doc, StreamSource template, StreamResult target,
			Map<String, Object> variables) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Templates templates = transformerFactory.newTemplates(template);
		Transformer transformer = templates.newTransformer();
		if (variables != null) {
			for (Entry<String, Object> p : variables.entrySet())
				transformer.setParameter(p.getKey(), p.getValue());
		}
		transformer.transform(new DOMSource(doc), target);
	}

	public static void transform(StreamSource source, StreamSource template, StreamResult target)
			throws TransformerException {
		transform(source, template, target, null);
	}

	public static void transform(StreamSource source, StreamSource template, StreamResult target,
			Map<String, Object> variables) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Templates templates = transformerFactory.newTemplates(template);
		Transformer transformer = templates.newTransformer();
		if (variables != null) {
			for (Entry<String, Object> p : variables.entrySet())
				transformer.setParameter(p.getKey(), p.getValue());
		}
		transformer.transform(source, target);
	}

	public static String innerXml(Node node) {
		DOMImplementationLS lsImpl = (DOMImplementationLS) node.getOwnerDocument().getImplementation().getFeature("LS", //$NON-NLS-1$
				"3.0"); //$NON-NLS-1$
		LSSerializer lsSerializer = lsImpl.createLSSerializer();
		lsSerializer.getDomConfig().setParameter("xml-declaration", false); //$NON-NLS-1$
		NodeList childNodes = node.getChildNodes();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < childNodes.getLength(); i++) {
			sb.append(lsSerializer.writeToString(childNodes.item(i)));
		}
		return sb.toString();
	}

	public static String outerXml(Node node) {
		DOMImplementationLS lsImpl = (DOMImplementationLS) node.getOwnerDocument().getImplementation().getFeature("LS", //$NON-NLS-1$
				"3.0"); //$NON-NLS-1$
		LSSerializer lsSerializer = lsImpl.createLSSerializer();
		lsSerializer.getDomConfig().setParameter("xml-declaration", false); //$NON-NLS-1$
		return lsSerializer.writeToString(node);
	}

	/**
	 * set XSLT url for next "marshall" operation, cleared afterwards (single use).
	 * 
	 * @param url
	 */
	public static void setXslUrlOnce(String url) {
		oneTimeXslUrl = url;
	}

	/**
	 * set XSLT url for "marshall" operations, set to null to clear.
	 * 
	 * @param url
	 */
	public static void setXslUrl(String url) {
		xslUrl = url;
	}

	/**
	 * set system property to allow downloading external DTD
	 */
	public static void allowExternalDTD() {
		System.setProperty("javax.xml.accessExternalDTD", "all");
	}

	/**
	 * set system property to allow downloading external schemas
	 */
	public static void allowExternalSchema() {
		System.setProperty("javax.xml.accessExternalSchema", "all");
	}

	/**
	 * set system property to allow downloading external stylesheets
	 */
	public static void allowExternalStylesheet() {
		System.setProperty("javax.xml.accessExternalStylesheet", "all");
	}

	/**
	 * get child node by name
	 * 
	 * @param node
	 * @param name
	 * @return
	 */
	public static Node getChildByName(Node node, String name) {
		if (node == null)
			return null;
		if (node instanceof Element) {
			NodeList result = ((Element) node).getElementsByTagName(name);
			if (result.getLength() > 0)
				return result.item(0);
		} else {
			Node n = node.getFirstChild();
			while (n != null) {
				if (name.equals(n.getNodeName()))
					return n;
				n = n.getNextSibling();
			}
		}
		return null;
	}

}
