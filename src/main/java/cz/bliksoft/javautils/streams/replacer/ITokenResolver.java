package cz.bliksoft.javautils.streams.replacer;

/**
 * http://tutorials.jenkov.com/java-howto/replace-strings-in-streams-arrays-files.html
 * 
 * @author Jakob Jenkov
 *
 */

public interface ITokenResolver {

	public String resolveToken(String tokenName);
}
