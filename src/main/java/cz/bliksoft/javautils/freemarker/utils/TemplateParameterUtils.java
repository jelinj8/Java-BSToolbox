package cz.bliksoft.javautils.freemarker.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.bliksoft.javautils.xmlfilesystem.FileSystem;

/**
 * Parses the {@code {var|type|name|title[|default[|parameters]]}} parameter
 * declarations found in FreeMarker template comments. This format is generic
 * (not tied to ZPL templates) and is used by web/desktop UIs to build parameter
 * forms, and by {@link cz.bliksoft.javautils.freemarker.FreemarkerGenerator} to
 * fill in default values for variables not supplied by the caller.
 * <p>
 * {@code COMMENT} and {@code HINT} texts can be translated: with a default part
 * ({@code {var|comment|-|key|text}}) the title position holds an XML-filesystem
 * translation key and the default is the fallback text (see
 * {@link #resolveText(String, String)}); without it
 * ({@code {var|comment|-|text}}) the title is the text itself. A {@code HINT}
 * adds no form row - {@link #parseFormParameters(String)} attaches its text to
 * the preceding parameter.
 * <p>
 * Any parameter's title can be translated too: {@code :key[:fallback]}, e.g.
 * {@code {var|int|printQuantity|:labels/param/printQuantity:počet výtisků|1|1:100}}
 * ({@link #resolveTitle(String, Locale)}).
 */
public class TemplateParameterUtils {

	// {var|type|name|title[|default[|parameters]]}
	public static final String PROPERTY_DEFINITION_PATTERN = "^\\s*\\{var\\|(?<type>[^\\|]+)\\|(?<name>[^\\|\\s]+)\\|(?<title>[^\\|\\}]+)(?:\\|(?<default>[^\\|\\}]*)(?:\\|(?<parameters>[^\\}]*?))?)?\\}\\s*$";
	public static final String PATT_NAME = "name";
	public static final String PATT_TITLE = "title";
	public static final String PATT_DEFAULT = "default";
	public static final String PATT_PARAMS = "parameters";
	public static final String PATT_TYPE = "type";
	public static final Pattern regexPattern = Pattern.compile(PROPERTY_DEFINITION_PATTERN, Pattern.MULTILINE);

	public static final String TYPE_COMMENT = "COMMENT";
	public static final String TYPE_HINT = "HINT";
	/** Title/key placeholder meaning "no translation key". */
	public static final String NO_KEY = "-";
	/**
	 * Prefix of a translated title {@code :key[:fallback]}, also the separator of
	 * its fallback text (see {@link #resolveTitle(String, Locale)}).
	 */
	public static final String TITLE_KEY_PREFIX = ":";

	private static final Logger log = Logger.getLogger(TemplateParameterUtils.class.getName());

	private TemplateParameterUtils() {
	}

	/** A single {@code {var|...}} parameter declaration parsed from a template. */
	public static class TemplateParameter {
		private final String type;
		private final String name;
		private final String title;
		private final String defaultValue;
		private final String parameters;
		private final String hint;

		public TemplateParameter(String type, String name, String title, String defaultValue, String parameters) {
			this(type, name, title, defaultValue, parameters, null);
		}

		public TemplateParameter(String type, String name, String title, String defaultValue, String parameters,
				String hint) {
			this.type = type;
			this.name = name;
			this.title = title;
			this.defaultValue = defaultValue;
			this.parameters = parameters;
			this.hint = hint;
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

		/**
		 * Help text for the parameter (from the {@code HINT} lines following it), or
		 * {@code null}.
		 */
		public String getHint() {
			return hint;
		}
	}

	/**
	 * Resolves a possibly translated text: the XML-filesystem translation of
	 * {@code key}, or {@code fallback} when the key is {@code null}, blank,
	 * {@value #NO_KEY} or has no translation.
	 */
	public static String resolveText(String key, String fallback) {
		return resolveText(key, fallback, null);
	}

	/**
	 * {@link #resolveText(String, String)} in the given language
	 * ({@link FileSystem#getTranslation(String, Locale)}, e.g. a web request's
	 * locale); {@code null} = the language the translations were loaded in.
	 */
	public static String resolveText(String key, String fallback, Locale locale) {
		if (key == null || key.trim().isEmpty() || NO_KEY.equals(key.trim()))
			return fallback;
		String translated = FileSystem.getTranslation(key.trim(), locale);
		return translated != null ? translated : fallback;
	}

	/**
	 * Parses the declarations for building a form: {@code :key[:fallback]} titles
	 * are translated, {@code COMMENT} titles are resolved to their display text
	 * (translation or fallback, the default part is cleared) and {@code HINT} lines
	 * are removed, their resolved texts attached
	 * (joined by newlines) to the preceding parameter's
	 * {@link TemplateParameter#getHint() hint}. A hint with no preceding parameter
	 * is dropped.
	 */
	public static List<TemplateParameter> parseFormParameters(String templateSource) {
		return parseFormParameters(templateSource, null);
	}

	/**
	 * {@link #parseFormParameters(String)} with the texts in the given language
	 * ({@link #resolveText(String, String, Locale)}); {@code null} = the language
	 * the translations were loaded in.
	 */
	public static List<TemplateParameter> parseFormParameters(String templateSource, Locale locale) {
		List<TemplateParameter> result = new ArrayList<>();
		for (TemplateParameter p : parseParameters(templateSource)) {
			String type = p.getType() != null ? p.getType().toUpperCase() : "";
			if (TYPE_HINT.equals(type)) {
				String text = text(p, locale);
				if (result.isEmpty()) {
					log.warning("Template hint with no preceding parameter ignored: " + text);
					continue;
				}
				TemplateParameter prev = result.get(result.size() - 1);
				String hint = prev.getHint() == null ? text : prev.getHint() + "\n" + text;
				result.set(result.size() - 1, new TemplateParameter(prev.getType(), prev.getName(), prev.getTitle(),
						prev.getDefaultValue(), prev.getParameters(), hint));
			} else if (TYPE_COMMENT.equals(type)) {
				result.add(new TemplateParameter(p.getType(), p.getName(), text(p, locale), null, p.getParameters()));
			} else {
				result.add(new TemplateParameter(p.getType(), p.getName(), resolveTitle(p.getTitle(), locale),
						p.getDefaultValue(), p.getParameters()));
			}
		}
		return result;
	}

	/**
	 * COMMENT/HINT display text: with a default part the title is a translation
	 * key, otherwise the text (or a {@code :key[:fallback]} title).
	 */
	private static String text(TemplateParameter p, Locale locale) {
		return p.getDefaultValue() == null ? resolveTitle(p.getTitle(), locale)
				: resolveText(p.getTitle(), p.getDefaultValue(), locale);
	}

	/**
	 * A parameter title: {@code :key[:fallback]} is the XML-filesystem translation
	 * of {@code key}, else {@code fallback} (the key itself when there is none);
	 * any other title is shown as it is.
	 */
	public static String resolveTitle(String title, Locale locale) {
		if (title == null || !title.startsWith(TITLE_KEY_PREFIX))
			return title;
		String rest = title.substring(TITLE_KEY_PREFIX.length());
		int sep = rest.indexOf(TITLE_KEY_PREFIX);
		String key = sep < 0 ? rest : rest.substring(0, sep);
		return resolveText(key, sep < 0 ? key : rest.substring(sep + 1), locale);
	}

	/** Parses all {@code {var|...}} declarations from the given template source. */
	public static List<TemplateParameter> parseParameters(String templateSource) {
		List<TemplateParameter> result = new ArrayList<>();
		Matcher matcher = regexPattern.matcher(templateSource);
		while (matcher.find()) {
			String parameters = matcher.group(PATT_PARAMS);
			if (parameters != null && parameters.isEmpty())
				parameters = null;
			result.add(new TemplateParameter(matcher.group(PATT_TYPE), matcher.group(PATT_NAME),
					matcher.group(PATT_TITLE), matcher.group(PATT_DEFAULT), parameters));
		}
		return result;
	}

	/**
	 * Extracts the typed default values declared for the template's parameters,
	 * keyed by parameter name. {@code INFO}, {@code COMMENT}, {@code HINT} and
	 * {@code CSVFILE} parameters are skipped, as they have no usable scalar default.
	 */
	public static Map<String, Object> extractDefaultVariables(String templateSource) {
		Map<String, Object> result = new LinkedHashMap<>();
		for (TemplateParameter param : parseParameters(templateSource)) {
			String type = param.getType() != null ? param.getType().toUpperCase() : "";
			String name = param.getName();
			String defaultValue = param.getDefaultValue();

			if ("INFO".equals(type) || TYPE_COMMENT.equals(type) || TYPE_HINT.equals(type) || "CSVFILE".equals(type))
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
			case "DECIMAL":
				Double decimalValue = TemplateValueCoercion.parseDecimal(defaultValue);
				result.put(name, decimalValue != null ? decimalValue : 0.0);
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
