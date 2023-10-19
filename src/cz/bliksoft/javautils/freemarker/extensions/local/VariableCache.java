package cz.bliksoft.javautils.freemarker.extensions.local;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * used as dynamic variable/list/hash map arg1: actions - add (to list), remove
 * (from list by value or map by key), put (to map by key), set (variable),
 * clear (full variable/list/map), get (variable/list/map) arg2: variable name
 * arg3: key or value arg4: value e.g.: -set var1 value -put map1 key value -add
 * list1 value -remove list1 value -remove map1 key -clear variable -get
 * variable -clear (all)
 */
public class VariableCache implements TemplateMethodModelEx {
	Logger log = Logger.getLogger(VariableCache.class.getName());

	Map<String, Object> valueCache = null;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object exec(List args) throws TemplateModelException {
		if (valueCache == null)
			valueCache = new HashMap<>();

		if (args.size() == 0)
			throw new TemplateModelException("Missing at least variable cache command");

		String command = String.valueOf(args.get(0));
		String arg1 = args.size() > 1 ? String.valueOf(args.get(1)) : null;

		Object currentValue = null;

		switch (command) {
		case "set":
			if (log.isLoggable(Level.FINER))
				log.fine(MessageFormat.format("{0}[{1}] = {2}", command, arg1, args.get(2)));
			valueCache.put(arg1, args.get(2));
			return args.get(2);
		case "add":
			currentValue = valueCache.get(arg1);
			if (log.isLoggable(Level.FINER))
				log.fine(MessageFormat.format("{0}[{1}] from {2} to {3}", command, arg1, currentValue, args.get(2)));
			if (currentValue == null) {
				currentValue = new ArrayList<Object>();
				valueCache.put(arg1, currentValue);
			}
			if (currentValue instanceof List) {
				((List<Object>) currentValue).add(args.get(2));
			}
			return args.get(2);
		case "put":
			currentValue = valueCache.get(arg1);
			if (currentValue == null) {
				currentValue = new HashMap<Object, Object>();
				valueCache.put(arg1, currentValue);
			}
			if (currentValue instanceof Map) {
				((Map<Object, Object>) currentValue).put(args.get(2), args.get(3));
			}
			if (log.isLoggable(Level.FINER))
				log.fine(MessageFormat.format("{0}[{1}] from {2} to {3}", command, arg1, currentValue, args.get(2)));

			return args.get(2);
		case "remove":
			currentValue = valueCache.get(arg1);
			if (currentValue != null) {
				if (currentValue instanceof Map) {
					((Map) currentValue).remove(args.get(2));
				} else if (currentValue instanceof List) {
					((List) currentValue).remove(args.get(2));
				} else {
					valueCache.remove(arg1);
				}
			}
			if (log.isLoggable(Level.FINER))
				log.fine(MessageFormat.format("{0}[{1}] = {2}", command, arg1, currentValue));
			return currentValue;
		case "get":
			currentValue = valueCache.get(arg1);
			if (log.isLoggable(Level.FINER))
				log.fine(MessageFormat.format("{0}[{1}] = {2}", command, arg1, currentValue));
			return currentValue;
		case "clear":
			currentValue = valueCache.get(arg1);
			if (currentValue != null) {
				if (currentValue instanceof Map) {
					((Map) currentValue).clear();
				} else if (currentValue instanceof List) {
					((List) currentValue).clear();
				} else {
					valueCache.remove(arg1);
				}
			}
			if (log.isLoggable(Level.FINER))
				log.fine(MessageFormat.format("{0}", command));
			return "";
		case "all":
			if (log.isLoggable(Level.FINER))
				log.fine(MessageFormat.format("{0}", command));
			return valueCache;
		default:
			throw new TemplateModelException("Unknown variable cache command '" + command + "'");
		}

	}
}
