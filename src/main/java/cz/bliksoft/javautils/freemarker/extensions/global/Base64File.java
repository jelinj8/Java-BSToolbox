package cz.bliksoft.javautils.freemarker.extensions.global;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cz.bliksoft.javautils.Base64Utils;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Base64File implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arg0) throws TemplateModelException {
		File fToEncode = new File(String.valueOf(arg0.get(0)));

		if (!fToEncode.exists())
			throw new TemplateModelException("File to encode " + fToEncode.getAbsolutePath() + " was not found!");
		try {
			return Base64Utils.fileToBase64(fToEncode);
		} catch (IOException e) {
			throw new TemplateModelException("Failed to encode file " + fToEncode.getName(), e);
		}
	}

}
