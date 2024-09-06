package cz.bliksoft.javautils.freemarker.extensions;

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

public class Log implements TemplateMethodModelEx, TemplateDirectiveModel {
	Logger logger;
	Level logLevel = Level.INFO;

	public Log() {
		this(null, null);
	}

	public Log(Level level) {
		this(level, null);
	}

	public Log(String logName) {
		this(null, logName);
	}

	public Log(Level level, String loggerName) {
		if (loggerName == null)
			logger = Logger.getLogger(Log.class.getName());
		else
			logger = Logger.getLogger(loggerName);
		if (level == null)
			logLevel = Level.INFO;
		else
			logLevel = level;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List args) throws TemplateModelException {
		if (logger.isLoggable(logLevel)) {
			for (Object o : args) {
				if (o != null)
					logger.log(logLevel, o.toString());
				else
					logger.log(logLevel, "NULL");
			}
		}
		return "";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
			throws TemplateException, IOException {
		try (StringWriter w = new StringWriter()) {
			body.render(w);
			logger.log(logLevel, w.toString());
		}
	}
}
