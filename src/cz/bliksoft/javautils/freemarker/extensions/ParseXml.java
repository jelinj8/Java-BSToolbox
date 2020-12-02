package cz.bliksoft.javautils.freemarker.extensions;

import java.util.List;

import org.w3c.dom.Document;

import cz.bliksoft.javautils.xml.XmlUtils;
import freemarker.ext.dom.NodeModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class ParseXml implements TemplateMethodModelEx {

	@Override
	public Object exec(List arg0) throws TemplateModelException {
		Document doc;
		try {
			doc = XmlUtils.convertStringToDocument(arg0.get(0).toString());
			return freemarker.ext.dom.NodeModel.wrap(doc.getDocumentElement());
		} catch (Exception e) {
			throw new TemplateModelException("Failed to parse XML", e);
		}
	}

}
