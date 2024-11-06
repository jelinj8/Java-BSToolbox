package cz.bliksoft.javautils.freemarker.extensions.global;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class LogMessage implements TemplateMethodModelEx, TemplateDirectiveModel {
	Logger log = Logger.getLogger(LogMessage.class.getName());

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List args) throws TemplateModelException {
		if (args.size() < 1)
			return "";

		String msg = String.valueOf(args.get(0));
		log.info(msg);
		return "";
	}

	@Override
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		String message = null;

		Object parObject = params.get("message");
		if (parObject != null)
			message = String.valueOf(parObject);

		Level level = Level.INFO;

		parObject = params.get("level");
		if (parObject != null) {
			level = Level.parse(String.valueOf(parObject));
		}

		if (body != null) {
			try (StringWriter sw = new StringWriter()) {
				body.render(sw);
				if (message != null) {
					log.log(level, message + "\n" + sw.toString());
				} else
					log.log(level, sw.toString());
			}
		} else if (message != null) {
			log.log(level, message);
		}
	}
}
