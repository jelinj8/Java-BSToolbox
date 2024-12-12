package cz.bliksoft.javautils.freemarker.extensions.global;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import cz.bliksoft.javautils.streams.NoCloseOutputStream;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Writes first argument and/or macro body to commandline (system.out)
 */
public class CMDWrite implements TemplateMethodModelEx, TemplateDirectiveModel {
	Logger log = Logger.getLogger(CMDWrite.class.getName());

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
		String prompt = String.valueOf(arguments.get(0));
		System.out.println(prompt);
		return "";
	}

	@Override
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {

		try (NoCloseOutputStream ncos = new NoCloseOutputStream(System.out); PrintWriter w = new PrintWriter(ncos)) {

			if (!params.isEmpty()) {
				@SuppressWarnings("unchecked")
				Iterator<Entry<String, TemplateModel>> iterator = params.entrySet().iterator();
				Entry<String, TemplateModel> firstParam = iterator.next();
				if (firstParam != null) {
					w.write(String.valueOf(firstParam.getValue()));
				}
			}

			if (body != null) {
				body.render(w);
			}
		}
	}

}
