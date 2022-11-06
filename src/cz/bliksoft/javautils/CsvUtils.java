package cz.bliksoft.javautils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class CsvUtils {

	public static <V> Map<String, Map<String, V>> loadCsv(BufferedReader source, String separator, boolean hasHeader,
			BiFunction<Integer, String, V> converter) throws IOException {
		String line = source.readLine();
		if (line == null)
			return null;
		String[] cols = line.split(separator, -1);

		ArrayList<String> colNames = new ArrayList<>(cols.length);
		;
		if (hasHeader) {
			for (String c : cols) {
				colNames.add(c);
			}
			line = source.readLine();
			if (line != null)
				cols = line.split(separator, -1);
		} else {
			for (int i = 0; i < cols.length; i++)
				colNames.add(StringUtils.numberAsString(i));
		}

		Map<String, Map<String, V>> result = new LinkedHashMap<>();
		int rownum = 0;
		while (line != null) {
			if (cols.length > 0) {
				Map<String, V> row = new LinkedHashMap<>();
				result.put(StringUtils.numberAsString(rownum++), row);

				int i = 0;
				for (String colname : colNames) {
					if (StringUtils.hasLength(cols[i]))
						row.put(colname, converter.apply(i, cols[i]));
					i++;
					if (i >= cols.length)
						break;
				}
			}
			line = source.readLine();
			if (line != null)
				cols = line.split(separator, -1);
		}

		return result;
	}

	public static Map<String, Map<String, String>> loadCsv(BufferedReader source, String separator, boolean hasHeader)
			throws IOException {
		return loadCsv(source, separator, hasHeader, (i, s) -> {
			return s;
		});
	}
}
