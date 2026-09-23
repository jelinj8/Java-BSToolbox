package cz.bliksoft.javautils.freemarker.utils;

/**
 * Declares the expected Java type of a printer/config "advanced property"
 * (or template-variable override), and whether it should be automatically
 * propagated as a typed default template variable at generation time.
 *
 * <p>
 * {@code type} drives both the coercion performed by {@link TemplateValueCoercion}
 * and which value-editor widget a UI presents for the property; it is one of
 * {@code Integer.class}, {@code Double.class}, {@code Boolean.class},
 * {@code Map.class} (an ordered {@code String->String} map, see
 * {@link MapPropertyCodec}), or {@code String.class} (the default, untyped
 * case).
 */
public final class AdvancedPropertyDefinition {

	private final Class<?> type;
	private final boolean templatePropagate;

	public AdvancedPropertyDefinition(Class<?> type, boolean templatePropagate) {
		this.type = type;
		this.templatePropagate = templatePropagate;
	}

	public static AdvancedPropertyDefinition of(Class<?> type) {
		return new AdvancedPropertyDefinition(type, false);
	}

	public static AdvancedPropertyDefinition propagated(Class<?> type) {
		return new AdvancedPropertyDefinition(type, true);
	}

	public Class<?> type() {
		return type;
	}

	public boolean templatePropagate() {
		return templatePropagate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AdvancedPropertyDefinition))
			return false;
		AdvancedPropertyDefinition other = (AdvancedPropertyDefinition) o;
		return templatePropagate == other.templatePropagate && java.util.Objects.equals(type, other.type);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(type, templatePropagate);
	}

	@Override
	public String toString() {
		return "AdvancedPropertyDefinition[type=" + type + ", templatePropagate=" + templatePropagate + "]";
	}
}
