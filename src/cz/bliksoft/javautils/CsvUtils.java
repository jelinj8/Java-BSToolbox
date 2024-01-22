package cz.bliksoft.javautils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class CsvUtils {

	public static <V> Map<String, Map<String, V>> loadCsvMap(BufferedReader source, String separator, boolean hasHeader,
			List<String> columnNames, String keyName, BiFunction<Integer, String, V> converter) throws IOException {
		String line = source.readLine();
		if (line == null)
			return null;

		List<String> colNames = null;

		String[] cols = line.split(separator, -1);

		if (columnNames != null) {
			colNames = columnNames;
		} else {
			if (hasHeader) {
				// use first line as headers
				colNames = new ArrayList<>(cols.length);
				for (String c : cols) {
					colNames.add(c);
				}
			} else {
				// no header line and no column names, use just numbers
				colNames = new ArrayList<>(cols.length);
				for (int i = 0; i < cols.length; i++)
					colNames.add(StringUtils.numberAsString(i));
			}
		}

		if (hasHeader) {
			// go to line after header
			line = source.readLine();
			if (line != null)
				cols = line.split(separator, -1);
		}

		Map<String, Map<String, V>> result = new LinkedHashMap<>();
		int rownum = 0;
		while (line != null) {
			if (cols.length > 0) {
				Map<String, V> row = new LinkedHashMap<>();

				int i = 0;
				for (String colname : colNames) {
					if (StringUtils.hasLength(cols[i]))
						row.put(colname, converter.apply(i, cols[i]));
					i++;
					if (i >= cols.length)
						break;
				}

				if (keyName != null)
					result.put(row.get(keyName).toString(), row);
				else
					result.put(StringUtils.numberAsString(rownum++), row);
			}
			line = source.readLine();
			if (line != null)
				cols = line.split(separator, -1);
		}

		return result;
	}

	public static <V> List<Map<String, V>> loadCsvList(BufferedReader source, String separator, boolean hasHeader,
			List<String> columnNames, BiFunction<Integer, String, V> converter) throws IOException {
		String line = source.readLine();
		if (line == null)
			return null;
		List<String> colNames = null;

		String[] cols = line.split(separator, -1);

		if (columnNames != null) {
			colNames = columnNames;
		} else {
			if (hasHeader) {
				// use first line as headers
				colNames = new ArrayList<>(cols.length);
				for (String c : cols) {
					colNames.add(c);
				}
			} else {
				// no header line and no column names, use just numbers
				colNames = new ArrayList<>(cols.length);
				for (int i = 0; i < cols.length; i++)
					colNames.add(StringUtils.numberAsString(i));
			}
		}

		if (hasHeader) {
			// go to line after header
			line = source.readLine();
			if (line != null)
				cols = line.split(separator, -1);
		}

		LinkedList<Map<String, V>> result = new LinkedList<>();
		while (line != null) {
			if (cols.length > 0) {
				Map<String, V> row = new LinkedHashMap<>();
				result.add(row);

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

	public static Map<String, Map<String, String>> loadCsvMap(BufferedReader source, String separator,
			boolean hasHeader, String keyName) throws IOException {
		return loadCsvMap(source, separator, hasHeader, null, keyName, (i, s) -> {
			return s;
		});
	}

	public static Map<String, Map<String, String>> loadCsvMap(BufferedReader source, String separator,
			boolean hasHeader, List<String> colNames, String keyName) throws IOException {
		return loadCsvMap(source, separator, hasHeader, colNames, keyName, (i, s) -> {
			return s;
		});
	}

	public static List<Map<String, String>> loadCsvList(BufferedReader source, String separator, boolean hasHeader)
			throws IOException {
		return loadCsvList(source, separator, hasHeader, null, (i, s) -> {
			return s;
		});
	}

	public static List<Map<String, String>> loadCsvList(BufferedReader source, String separator, boolean hasHeader,
			List<String> colNames) throws IOException {
		return loadCsvList(source, separator, hasHeader, colNames, (i, s) -> {
			return s;
		});
	}
}
