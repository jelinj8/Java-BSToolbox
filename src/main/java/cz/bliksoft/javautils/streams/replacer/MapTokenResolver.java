package cz.bliksoft.javautils.streams.replacer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * http://tutorials.jenkov.com/java-howto/replace-strings-in-streams-arrays-files.html
 * 
 * @author Jakob Jenkov
 *
 */

public class MapTokenResolver implements ITokenResolver {

	public static String escapePropertiesValue(String value) {
		if (value == null)
			return null;
		return value.replace("\\", "\\\\");
	}

	protected Map<String, String> tokenMap = new HashMap<String, String>();

	private Function<String, String> transformer = null;

	public MapTokenResolver(Map<String, String> tokenMap) {
		this(tokenMap, null);
	}

	public MapTokenResolver(Map<String, String> tokenMap, Function<String, String> transformer) {
		this.tokenMap = tokenMap;
		this.transformer = transformer;
	}

	public String resolveToken(String tokenName) {
		String result = this.tokenMap.get(tokenName);
		if (transformer == null)
			return result;
		else
			return transformer.apply(result);
	}

}
