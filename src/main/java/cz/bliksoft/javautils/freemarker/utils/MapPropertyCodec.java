package cz.bliksoft.javautils.freemarker.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encodes/decodes an ordered {@code String->String} map as
 * {@code name1=value1;name2=value2;...}, the format used by
 * {@code Map}-typed advanced properties (e.g. a printer's font-name-to-file
 * mapping, where the first entry is the implicit default). Order is
 * preserved on decode via a {@link LinkedHashMap}.
 */
public final class MapPropertyCodec {

	private MapPropertyCodec() {
	}

	public static Map<String, String> decode(String raw) {
		Map<String, String> result = new LinkedHashMap<>();
		if (raw == null || raw.trim().isEmpty())
			return result;
		for (String part : raw.split(";")) {
			int eq = part.indexOf('=');
			if (eq > 0)
				result.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
		}
		return result;
	}

	public static String encode(Map<String, String> map) {
		if (map == null || map.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> e : map.entrySet()) {
			if (sb.length() > 0)
				sb.append(';');
			sb.append(e.getKey()).append('=').append(e.getValue() != null ? e.getValue() : "");
		}
		return sb.toString();
	}
}
