package cz.bliksoft.javautils.freemarker.extensions;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class Log implements TemplateMethodModelEx {
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

}
