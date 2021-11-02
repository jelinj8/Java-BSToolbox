package cz.bliksoft.javautils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.MessageFormat;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class StringUtils {

	private StringUtils() {

	}

	private static SecureRandom random = null;

	/**
	 * náhodná změť znaků
	 * 
	 * @return
	 */
	public static String randomString() {
		if (random == null)
			random = new SecureRandom();
		return new BigInteger(130, random).toString(32);
	}

	/**
	 * je řetězec prázdný?
	 * 
	 * @param value
	 * @return
	 */
	public static boolean isEmpty(String value) {
		return (value == null || value.length() == 0);
	}

	/**
	 * je řetězec neprázdný?
	 * 
	 * @param value
	 * @return
	 */
	public static boolean hasValue(String value) {
		return !isEmpty(value);
	}

	/**
	 * trim ošetřený na NULL vstup (vrátí zase null)
	 * 
	 * @param val
	 * @return
	 */
	public static String trim(String val) {
		if (val == null)
			return null;
		return val.trim();
	}

	public static boolean hasLength(String value) {
		if (isEmpty(value))
			return false;
		return value.length() > 0;
	}

	/**
	 * pospojuje řetězce do seznamu s oddělovačem
	 * 
	 * @param separator oddělovač, který bude vložen mezi spojované hodnoty
	 * @param args      hodnoty pro spojení
	 * @return spojený řetězec
	 */
	public static String concatenateList(String separator, Object... args) {
		StringBuilder builder = new StringBuilder();
		for (Object o : args) {
			if (o != null) {
				if ((builder.length() > 0) && (hasText(separator))) {
					builder.append(separator);
				}
				builder.append(o.toString());
			}
		}
		return builder.toString();
	}

	/**
	 * poskládá řetězec z X opakování jedné hodnoty, oddělené oddělovačem
	 * 
	 * @param separator oddělovač
	 * @param value     opakovaná hodnota
	 * @param count     počet opakování
	 * @return sestavený řetězec
	 */
	public static String repeatToString(String separator, Object value, int count) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < count; i++) {
			if ((i > 0) && (hasText(separator))) {
				builder.append(separator);
			}
			if (value != null) {
				builder.append(value.toString());
			}
		}
		return builder.toString();
	}

	/**
	 * sestaví řetězec ze vstupních hodnot, oddělených oddělovačem a doplněných o
	 * příponu (příklad: 'val1 = ?,val2 = ?,val3 = ?,val4 = ?,val5 = ?')
	 * 
	 * @param value     přípona k doplnění ke každé hodnotě
	 * @param separator oddělovač
	 * @param args      hodnoty
	 * @return sestavený řetězec
	 */
	public static String appendToEach(String value, String separator, Object... args) {
		StringBuilder builder = new StringBuilder();
		for (Object o : args) {
			if ((builder.length() > 0) && (hasText(separator))) {
				builder.append(separator);
			}
			if (o != null) {
				builder.append(o.toString());
			}
			if (hasText(value)) {
				builder.append(value);
			}
		}
		return builder.toString();
	}

	/**
	 * sestaví řetězec ze vstupních hodnot, oddělených oddělovačem a doplněných o
	 * příponu (příklad: 'val1 = ?,val2 = ?,val3 = ?,val4 = ?,val5 = ?')
	 * 
	 * @param value     přípona k doplnění ke každé hodnotě
	 * @param separator oddělovač
	 * @param args      hodnoty
	 * @return sestavený řetězec
	 */
	public static String appendToEach(String value, String separator, String... args) {
		StringBuilder builder = new StringBuilder();
		for (String o : args) {
			if ((builder.length() > 0) && (hasText(separator))) {
				builder.append(separator);
			}
			if (o != null) {
				builder.append(o);
			}
			if (hasText(value)) {
				builder.append(value);
			}
		}
		return builder.toString();
	}

	/**
	 * kontrola, zda je řetězec prázdný
	 * 
	 * @param txt hodnota ke kontrole
	 * @return true pokud je řetězec neprázdný (!=null && length>0)
	 */
	public static boolean hasText(String txt) {
		return ((txt != null) && (txt.length() > 0));
	}

	public static boolean hasText(Object txt) {
		if (txt == null)
			return false;
		if (txt instanceof String)
			return hasText((String) txt);
		return hasText(txt.toString());
	}

	public static boolean hasNonWhitespaceText(String txt) {
		if (!hasText(txt))
			return false;
		return (txt.trim().length() > 0);
	}

	public static boolean containsDigits(String txt) {
		if (txt == null)
			return false;
		for (char c : txt.toCharArray()) {
			if (Character.isDigit(c))
				return true;
		}
		return false;
	}

	public static boolean isAlpha(String txt) {
		if (txt == null)
			return false;
		for (char c : txt.toCharArray()) {
			if (!Character.isLetter(c) && !Character.isWhitespace(c))
				return true;
		}
		return false;
	}

	/**
	 * vrátí řetězec pokud obsahuje text, jinak default hodnotu
	 * 
	 * @param txt
	 * @param defaultResult
	 * @return txt nebo defaultResult
	 */
	public static String hasTextDefault(String txt, String defaultResult) {
		if ((txt != null) && (txt.length() > 0))
			return txt;
		else
			return defaultResult;

	}

	/**
	 * otestuje hodnotu řetězce, zda obsahuje text a vrátí jeden ze dvou parametrů.
	 * Pokud je návratový parametr řetězec a hodnota není prázdná, je tento řetězec
	 * použit jako MessageFormat
	 * 
	 * @param txt             hodnota, která má být testována na obsah textu
	 * @param hasTextResult   návratová hodnota nebo formátovací řetězec
	 * @param hasntTextResult návratová hodnota
	 * @return
	 */
	public static Object hasTextSelect(String txt, Object hasTextResult, Object hasntTextResult) {
		if ((txt != null) && (txt.length() > 0))
			if (hasTextResult instanceof String)
				return format((String) hasTextResult, txt);
			else
				return hasTextResult;
		else
			return hasntTextResult;
	}

	public static String format(String format, Object... args) {
		return MessageFormat.format(format, args);
	}

	public static String hash(char[] password) throws NoSuchAlgorithmException {
		MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
		byte[] passHash = sha256.digest((new String(password)).getBytes());
		StringBuilder sb = new StringBuilder();
		for (byte b : passHash) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}

	public static String innerXml(Node node) {
		DOMImplementationLS lsImpl = (DOMImplementationLS) node.getOwnerDocument().getImplementation().getFeature("LS",
				"3.0");
		LSSerializer lsSerializer = lsImpl.createLSSerializer();
		lsSerializer.getDomConfig().setParameter("xml-declaration", false);
		NodeList childNodes = node.getChildNodes();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < childNodes.getLength(); i++) {
			sb.append(lsSerializer.writeToString(childNodes.item(i)));
		}
		return sb.toString();
	}

	public static String outerXml(Node node) {
		DOMImplementationLS lsImpl = (DOMImplementationLS) node.getOwnerDocument().getImplementation().getFeature("LS",
				"3.0");
		LSSerializer lsSerializer = lsImpl.createLSSerializer();
		lsSerializer.getDomConfig().setParameter("xml-declaration", false);
		return lsSerializer.writeToString(node);
		// NodeList childNodes = node.getChildNodes();
		// StringBuilder sb = new StringBuilder();
		// for (int i = 0; i < childNodes.getLength(); i++) {
		// sb.append(lsSerializer.writeToString(childNodes.item(i)));
		// }
		// return sb.toString();
	}

	public static boolean isHtmlString(String text) {
		if (!hasText(text))
			return false;
		return text.toUpperCase().startsWith("<HTML>");
	}

	public static String escapeForHTML(String text) {
		if (text == null)
			return null;
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public static String preformatForHTML(String text) {
		if (text == null)
			return null;
		text = escapeForHTML(text);
		String[] paragraphs = text.split("\\n");
		text = appendToEach("<br/>\n", null, paragraphs);
		return text;
	}

	public static String initCap(String text) {
		if (hasText(text)) {
			String[] txts = text.split(" ");
			StringBuilder result = new StringBuilder();
			for (String s : txts) {
				result.append(s.substring(0, 1).toUpperCase());
				result.append(s.substring(1).toLowerCase());
				result.append(" ");
			}
			return result.toString().trim();
		} else if (text == null) {
			return null;
		} else {
			return "";
		}

	}

	public static String removeUnsafeChars(String text) {
		if (text == null)
			return null;
		return text.replace(' ', '_').replace('<', '_').replace('>', '_').replace('#', '_');
	}

	public static String getHTMLBodyContent(String HTML) {
		String doc = HTML.toUpperCase();
		int bodyIndex = doc.indexOf("<BODY>");
		if (bodyIndex > -1) {
			int endBodyIndex = doc.indexOf("</BODY>");
			if (endBodyIndex == -1) {
				return HTML.substring(bodyIndex + 6);
			} else {
				return HTML.substring(bodyIndex + 6, endBodyIndex);
			}
		} else {
			return HTML;
		}
	}

	public static boolean equalStrings(String value1, String value2) {
		if (value1 == null && value2 == null)
			return true;
		if (value1 != null)
			return value1.equals(value2);
		else
			return value2.equals(value1);
	}

	public static String ellipsis(String input, int length) {
		if (input == null)
			return null;
		if (input.length() <= length)
			return input;
		return input.substring(0, length - 2) + "\u2026";
	}
}
