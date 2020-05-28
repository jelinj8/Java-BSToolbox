package cz.bliksoft.javautils.streams.xml;
/**
 * https://stackoverflow.com/questions/17222902/remove-namespace-prefix-while-jaxb-marshalling
 */
import java.io.Writer;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;

public class ElementWriter extends DelegatingXMLStreamWriter {

  private static final NamespaceContext emptyNamespaceContext = new NamespaceContext() {

    @Override
    public String getNamespaceURI(String prefix) {
      return "";
    }

    @Override
    public String getPrefix(String namespaceURI) {
      return "";
    }

    @SuppressWarnings("rawtypes")
	@Override
    public Iterator getPrefixes(String namespaceURI) {
      return null;
    }

  };

  @Override
	public void writeStartDocument() throws XMLStreamException {
		// TODO Auto-generated method stub
//		super.writeStartDocument();
	}
  
  @Override
	public void writeNamespace(String prefix, String uri) throws XMLStreamException {
		// TODO Auto-generated method stub
//		super.writeNamespace(prefix, uri);	  
	}
  
  public static XMLStreamWriter filter(Writer writer) throws XMLStreamException {
    return new ElementWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(writer));
  }

  public ElementWriter(XMLStreamWriter writer) {
    super(writer);
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return emptyNamespaceContext;
  }

}