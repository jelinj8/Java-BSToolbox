package cz.bliksoft.javautils.strings;

/**
 * Implemented by objects that can produce a locale-aware string representation.
 */
public interface ITranslatable {
	/** Returns the localised string representation of this object. */
	public String localizedToString();
}
