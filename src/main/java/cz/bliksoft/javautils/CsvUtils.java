package cz.bliksoft.javautils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CsvUtils {

	/**
	 * csv separated by ';', values optionally enclosed in '"'
	 */
	public static final String SEPARATOR_SEMICOLON_WITH_QUOTED_STRINGS = ";(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

	/**
	 * csv separated by ',', values optionally enclosed in '"'
	 */
	public static final String SEPARATOR_COMMA_WITH_QUOTED_STRINGS = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

	/**
	 * csv separated by '|', values optionally enclosed in '"'
	 */
	public static final String SEPARATOR_PIPE_WITH_QUOTED_STRINGS = "\\|(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

	/**
	 * load CSV with map key
	 *
	 * @param source         reader
	 * @param separator      separator regex (Regexp.quote)
	 * @param hasHeader      true to use first line (or skip if colNames provided)
	 * @param columnNames    col names (overrides header)
	 * @param keyName        used column for map key, if not present, first column
	 *                       will be used
	 * @param keyConverter   converter for key value, defaults to String
	 * @param valueConverter converter to be used, input is column index and string
	 *                       value, output has to be castable to V type, defaults to
	 *                       original String value
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, Map<String, V>> loadCsvMap(BufferedReader source, String separator, boolean hasHeader,
			List<String> columnNames, String keyName, Function<String, K> keyConverter,
			BiFunction<Integer, String, V> valueConverter) throws IOException {
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

		Integer keyPosition = (keyName == null ? 0 : colNames.indexOf(keyName));

		if (hasHeader) {
			// go to line after header
			line = source.readLine();
			if (line != null)
				cols = line.split(separator, -1);
		}

		Map<K, Map<String, V>> result = new LinkedHashMap<>();

		while (line != null) {
			if (cols.length > 0) {
				Map<String, V> row = new LinkedHashMap<>();

				int i = 0;
				for (String colname : colNames) {
					if (StringUtils.hasLength(cols[i])) {
						V val = (valueConverter != null ? valueConverter.apply(i, cols[i]) : (V) cols[i]);
						if (val != null)
							row.put(colname, val);
					}
					i++;
					if (i >= cols.length)
						break;
				}

				if (keyConverter == null) {
					result.put((K) cols[keyPosition], row);
				} else {
					result.put((K) keyConverter.apply(cols[keyPosition]), row);
				}
			}
			line = source.readLine();
			if (line != null)
				cols = line.split(separator, -1);
		}

		return result;
	}

	/**
	 * loads CSV as key/value map
	 *
	 * @param <K>            type of value
	 * @param <V>            type of key
	 * @param source         reader
	 * @param separator      column separator
	 * @param hasHeader      is first line a header line?
	 * @param columnNames    list of columnNames (overrides header, if none
	 *                       specified, columns will be numbered
	 * @param keyName        key column (first if not specified)
	 * @param valueName      value column (key+1 if not specified)
	 * @param keyConverter   optional key type converter
	 * @param valueConverter optional value type converter
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> loadMappingFromCsv(BufferedReader source, String separator, boolean hasHeader,
			List<String> columnNames, String keyName, String valueName, Function<String, K> keyConverter,
			Function<String, V> valueConverter) throws IOException {
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

		Integer keyPosition = (keyName == null ? 0 : colNames.indexOf(keyName));
		Integer valuePosition = (valueName == null ? keyPosition + 1 : colNames.indexOf(valueName));

		if (hasHeader) {
			// go to line after header
			line = source.readLine();
			if (line != null)
				cols = line.split(separator, -1);
		}

		Map<K, V> result = new LinkedHashMap<>();

		while (line != null) {
			if (cols.length > 0) {
//				Map<String, V> row = new LinkedHashMap<>();

				K key = (keyConverter != null ? keyConverter.apply(cols[keyPosition]) : (K) cols[keyPosition]);
				V val = (valueConverter != null ? valueConverter.apply(cols[valuePosition]) : (V) cols[valuePosition]);
				result.put(key, val);
			}
			line = source.readLine();
			if (line != null)
				cols = line.split(separator, -1);
		}

		return (Map<K, V>) result;
	}

	/**
	 * load CSV as list of Maps
	 *
	 * @param source      reader
	 * @param separator   separator regex (Regexp.quote)
	 * @param hasHeader   true to use first line (or skip if colNames provided)
	 * @param columnNames col names (overrides header)
	 * @param converter   converter to be used, input is column index and string
	 *                    value, output should fit generic type
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
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
						if (converter != null)
							row.put(colname, converter.apply(i, cols[i]));
						else
							row.put(colname, (V) cols[i]);
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

	/**
	 * load CSV with map key
	 *
	 * @param source    reader
	 * @param separator separator regex (Regexp.quote)
	 * @param hasHeader true to use first line (or skip if colNames provided)
	 * @param keyName   used column for map key
	 * @return
	 * @throws IOException
	 */
	public static Map<String, Map<String, String>> loadCsvMap(BufferedReader source, String separator,
			boolean hasHeader, String keyName) throws IOException {
		return CsvUtils.<String, String>loadCsvMap(source, separator, hasHeader, null, keyName, null, null);
	}

	/**
	 * load CSV with map key
	 *
	 * @param source    reader
	 * @param separator separator regex (Regexp.quote)
	 * @param hasHeader true to use first line (or skip if colNames provided)
	 * @param colNames  col names (overrides header)
	 * @param keyName   used column for map key
	 * @return
	 * @throws IOException
	 */
	public static Map<String, Map<String, String>> loadCsvMap(BufferedReader source, String separator,
			boolean hasHeader, List<String> colNames, String keyName) throws IOException {
		return CsvUtils.<String, String>loadCsvMap(source, separator, hasHeader, colNames, keyName, null, null);
	}

	/**
	 * load CSV as list of Maps
	 *
	 * @param source    reader
	 * @param separator separator regex (Regexp.quote)
	 * @param hasHeader true to use first line (or skip if colNames provided)
	 * @return
	 * @throws IOException
	 */
	public static List<Map<String, String>> loadCsvList(BufferedReader source, String separator, boolean hasHeader)
			throws IOException {
		return loadCsvList(source, separator, hasHeader, null, (i, s) -> {
			return s;
		});
	}

	/**
	 * load CSV as list of Maps
	 *
	 * @param source    reader
	 * @param separator separator regex (Regexp.quote)
	 * @param hasHeader true to use first line (or skip if colNames provided)
	 * @param colNames  col names (overrides header)
	 * @return
	 * @throws IOException
	 */
	public static List<Map<String, String>> loadCsvList(BufferedReader source, String separator, boolean hasHeader,
			List<String> colNames) throws IOException {
		return loadCsvList(source, separator, hasHeader, colNames, (i, s) -> {
			return s;
		});
	}

	/**
	 * split a line using given separator into a map, use list as keys or use
	 * numbers as keys if none provided
	 *
	 * @param line
	 * @param separator
	 * @param columnNames
	 * @return
	 */
	public static HashMap<String, String> splitCsv(String line, String separator, List<String> columnNames) {
		if (line == null)
			return null;

		String[] cols = line.split(separator, -1);
		HashMap<String, String> rowResult = null;
		if (columnNames != null) {
			rowResult = new HashMap<>(columnNames.size());
			int index = 0;
			for (String vKey : columnNames) {
				if (index >= cols.length)
					break;
				rowResult.put(vKey, cols[index]);
				index++;
			}
		} else {
			rowResult = new HashMap<>(cols.length);
			for (Integer i = 0; i < cols.length; i++) {
				rowResult.put(StringUtils.numberAsString(i), cols[i]);
			}
		}
		return rowResult;
	}

	public static class LazyCsvMap extends HashMap<String, String> {
		private static final long serialVersionUID = -1445190954876268208L;

		String separator;
		List<String> columnNames;

		public LazyCsvMap(String separator, List<String> columnNames) {
			super();
			this.separator = separator;
			this.columnNames = columnNames;
		}

		public Map<String, String> getMap(String key) {
			return splitCsv(get(key), separator, columnNames);
		}
	}

	public static LazyCsvMap loadLazyCsvMap(BufferedReader source, String separator, boolean hasHeader,
			List<String> columnNames, String keyName) throws IOException {
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

		LazyCsvMap result = new LazyCsvMap(separator, colNames);
		int rownum = 0;

		Integer keyIndex = ((keyName == null) ? null : colNames.indexOf(keyName));

		while (line != null) {
			if (cols.length > 0) {
				if (keyName != null)
					result.put(cols[keyIndex], line);
				else
					result.put(StringUtils.numberAsString(rownum++), line);
			}
			line = source.readLine();
			if (line != null)
				cols = line.split(separator, -1);
		}
		return result;
	}

}
