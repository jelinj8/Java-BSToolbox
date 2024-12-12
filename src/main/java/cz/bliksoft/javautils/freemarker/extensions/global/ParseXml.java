package cz.bliksoft.javautils.freemarker.extensions.global;

import java.util.List;

import cz.bliksoft.javautils.xml.XmlUtils;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class ParseXml implements TemplateMethodModelEx {

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arg0) throws TemplateModelException {
		try {
			return freemarker.ext.dom.NodeModel.wrap(XmlUtils.convertStringToNode(arg0.get(0).toString()));
		} catch (Exception e) {
			throw new TemplateModelException("Failed to parse XML", e);
		}
	}

}
