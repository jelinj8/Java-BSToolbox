package cz.bliksoft.javautils.freemarker.extensions;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.bliksoft.javautils.xml.XmlUtils;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class PrettyPrintXml implements TemplateMethodModelEx {

	Logger log = Logger.getLogger(PrettyPrintXml.class.getName());

	private String result(String input) {
		try {
			return XmlUtils.prettyPrintXml(input);
		} catch (Exception e) {
			log.log(Level.INFO, "Failed to format XML", e);
			return input;
		}
	}

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arg0) throws TemplateModelException {
		return result(arg0.get(0).toString());
	}

}
