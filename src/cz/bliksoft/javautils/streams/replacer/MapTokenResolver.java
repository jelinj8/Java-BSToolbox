package cz.bliksoft.javautils.streams.replacer;

import java.util.HashMap;
import java.util.Map;

/**
 * http://tutorials.jenkov.com/java-howto/replace-strings-in-streams-arrays-files.html
 * 
 * @author Jakob Jenkov
 *
 */

public class MapTokenResolver implements ITokenResolver {

	protected Map<String, String> tokenMap = new HashMap<String, String>();

	public MapTokenResolver(Map<String, String> tokenMap) {
		this.tokenMap = tokenMap;
	}

	public String resolveToken(String tokenName) {
		return this.tokenMap.get(tokenName);
	}

}
