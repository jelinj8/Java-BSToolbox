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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;

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

	public static Document marshall(Object obj) throws JAXBException {
		Marshaller m = getMarshaller(obj);
		DOMResult result = new DOMResult();
		m.marshal(obj, result);
		return (Document) result.getNode();
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

	public static boolean checkIfNodeExists(Document document, String xpathExpression) throws XPathExpressionException {
		// Create XPathExpression object
		XPathExpression expr = compileXPath(xpathExpression);
		return checkIfNodeExists(document, expr);
	}

	public static boolean checkIfNodeExists(Document document, XPathExpression expr) throws XPathExpressionException {
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
	 * @author jjelinek
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

			if (StringUtils.hasLength(fname.getPrefix())) {
				res = functions.get(fname.getPrefix()).get(selector + ":" + arity);
				if (res != null)
					return res;
				return functions.get(fname.getPrefix()).get(selector);
			} else {
				res = functions.get(ctx.getPrefix(fname.getNamespaceURI())).get(selector + ":" + arity);
				if (res != null)
					return res;
				return functions.get(ctx.getPrefix(fname.getNamespaceURI())).get(selector);
			}
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
			functionResolver.addFunction(nsPrefix, "map:*", new MapXPathFunction());
		}
	}

	public static class ChooseXPathFunction implements XPathFunction {
		@SuppressWarnings("rawtypes")
		@Override
		public Object evaluate(List args) throws XPathFunctionException {
			boolean val = (boolean) args.get(0);
			if (val)
				return args.get(1);
			else
				return args.get(2);
		}
	}

	public static class MapXPathFunction implements XPathFunction {
		@SuppressWarnings("rawtypes")
		@Override
		public Object evaluate(List args) throws XPathFunctionException {
			if (args.size() < 2)
				throw new XPathFunctionException(
						"XPath map: minimal signature: map(inpud, default) or map(input, val1, res1, val2, res2...) or map(input, val1, res1, val2, res2..., default) ");

			Iterator i = args.iterator();

			Object src = i.next();
			Object val = null;
			Object res = null;
			while (i.hasNext()) {
				val = i.next();
				if (!i.hasNext())
					return val;

				res = i.next();

				if (src.equals(val))
					return res;
			}

			throw new XPathFunctionException(
					"XPath map: No value matched and no default value (with even args count the last one is used as default)");
		}
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

}
