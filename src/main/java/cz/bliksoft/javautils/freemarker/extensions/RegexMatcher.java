package cz.bliksoft.javautils.freemarker.extensions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class RegexMatcher implements TemplateMethodModelEx {

	Pattern regexPattern;
	private Map<String, Integer> namedGroups = null;

	public RegexMatcher(String regex) {
		regexPattern = Pattern.compile(regex);
	}
	
	public void addGroup(String name, int index) {
		if(namedGroups == null)
			namedGroups = new HashMap<>();
		namedGroups.put(name, index);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		List<Map<String, Object>> result = new ArrayList<>();
		if (arguments.size() == 1) {

			try {
				Object arg = arguments.get(0);
				if (arg == null)
					return "";
				String text;
				text = arg.toString();

				Matcher matcher = regexPattern.matcher(text);

				while (matcher.find()) {

					Map<String, Object> m = new HashMap<>();
					m.put("match", matcher.group());

					List<String> groups = new ArrayList<>();
					for (int i = 0; i < matcher.groupCount(); i++) {
						groups.add(matcher.group(i));
					}
					m.put("groups", groups);

					try {
						namedGroups = getNamedGroups(regexPattern);
					} catch (Exception e) {
					}

					if (namedGroups != null) {
						Map<String, String> namedMatches = new HashMap<>();
						for (Entry<String, Integer> kvp : namedGroups.entrySet()) {
							namedMatches.put(kvp.getKey(), matcher.group(kvp.getValue()));
						}
						m.put("namedGroups", namedMatches);
					}
				}

			} catch (Exception e) {
				throw new TemplateModelException("Failed to process regex", e);
			}
		}
		return result;

	}

	@SuppressWarnings("unchecked")
	private static Map<String, Integer> getNamedGroups(Pattern regex) throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		Method namedGroupsMethod = Pattern.class.getDeclaredMethod("namedGroups");
		namedGroupsMethod.setAccessible(true);

		Map<String, Integer> namedGroups = null;
		namedGroups = (Map<String, Integer>) namedGroupsMethod.invoke(regex);

		if (namedGroups == null) {
			throw new InternalError();
		}

		return Collections.unmodifiableMap(namedGroups);
	}

}
