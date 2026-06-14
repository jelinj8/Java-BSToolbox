package cz.bliksoft.javautils.freemarker.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the {@code {var|type|name|title[|default[|parameters]]}} parameter
 * declarations found in FreeMarker template comments. This format is generic
 * (not tied to ZPL templates) and is used by web/desktop UIs to build parameter
 * forms, and by {@link cz.bliksoft.javautils.freemarker.FreemarkerGenerator} to
 * fill in default values for variables not supplied by the caller.
 */
public class TemplateParameterUtils {

	// {var|type|name|title[|default[|parameters]]}
	public static final String PROPERTY_DEFINITION_PATTERN = "^\\s*\\{var\\|(?<type>[^\\|]+)\\|(?<name>[^\\|\\s]+)\\|(?<title>[^\\|\\}]+)(?:\\|(?<default>[^\\|\\}]*)(?:\\|(?<parameters>[^\\}]+?))?)?\\}\\s*$";
	public static final String PATT_NAME = "name";
	public static final String PATT_TITLE = "title";
	public static final String PATT_DEFAULT = "default";
	public static final String PATT_PARAMS = "parameters";
	public static final String PATT_TYPE = "type";
	public static final Pattern regexPattern = Pattern.compile(PROPERTY_DEFINITION_PATTERN, Pattern.MULTILINE);

	private TemplateParameterUtils() {
	}

	/** A single {@code {var|...}} parameter declaration parsed from a template. */
	public static class TemplateParameter {
		private final String type;
		private final String name;
		private final String title;
		private final String defaultValue;
		private final String parameters;

		public TemplateParameter(String type, String name, String title, String defaultValue, String parameters) {
			this.type = type;
			this.name = name;
			this.title = title;
			this.defaultValue = defaultValue;
			this.parameters = parameters;
		}

		public String getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public String getTitle() {
			return title;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public String getParameters() {
			return parameters;
		}
	}

	/** Parses all {@code {var|...}} declarations from the given template source. */
	public static List<TemplateParameter> parseParameters(String templateSource) {
		List<TemplateParameter> result = new ArrayList<>();
		Matcher matcher = regexPattern.matcher(templateSource);
		while (matcher.find()) {
			result.add(new TemplateParameter(matcher.group(PATT_TYPE), matcher.group(PATT_NAME),
					matcher.group(PATT_TITLE), matcher.group(PATT_DEFAULT), matcher.group(PATT_PARAMS)));
		}
		return result;
	}

	/**
	 * Extracts the typed default values declared for the template's parameters,
	 * keyed by parameter name. {@code INFO}, {@code COMMENT} and {@code CSVFILE}
	 * parameters are skipped, as they have no usable scalar default.
	 */
	public static Map<String, Object> extractDefaultVariables(String templateSource) {
		Map<String, Object> result = new LinkedHashMap<>();
		for (TemplateParameter param : parseParameters(templateSource)) {
			String type = param.getType() != null ? param.getType().toUpperCase() : "";
			String name = param.getName();
			String defaultValue = param.getDefaultValue();

			if ("INFO".equals(type) || "COMMENT".equals(type) || "CSVFILE".equals(type))
				continue;

			switch (type) {
			case "INT":
				int intValue = 0;
				try {
					if (defaultValue != null && !defaultValue.isEmpty())
						intValue = Integer.parseInt(defaultValue);
				} catch (NumberFormatException ignored) {
				}
				result.put(name, intValue);
				break;
			case "BOOLEAN":
				result.put(name, Boolean.parseBoolean(defaultValue));
				break;
			default:
				result.put(name, defaultValue != null ? defaultValue : "");
				break;
			}
		}
		return result;
	}
}
