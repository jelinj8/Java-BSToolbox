package cz.bliksoft.javautils.freemarker.extensions.global;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import cz.bliksoft.javautils.CryptUtils;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Base64File implements TemplateMethodModelEx {

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arg0) throws TemplateModelException {
		File fToEncode = new File(String.valueOf(arg0.get(0)));

		if (!fToEncode.exists())
			throw new TemplateModelException("File to encode " + fToEncode.getAbsolutePath() + " was not found!");
		byte[] bytes = new byte[(int) fToEncode.length()];
		try (FileInputStream fis = new FileInputStream(fToEncode)) {
			fis.read(bytes);
		} catch (FileNotFoundException e) {
			throw new TemplateModelException("File to encode " + fToEncode.getAbsolutePath() + " was not found!", e);
		} catch (IOException e) {
			throw new TemplateModelException("Failed to read " + fToEncode.getAbsolutePath(), e);
		}

		return CryptUtils.base64Encode(bytes);
	}

}
