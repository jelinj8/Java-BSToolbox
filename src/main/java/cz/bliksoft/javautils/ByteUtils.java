package cz.bliksoft.javautils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ByteUtils {
	static Logger log = Logger.getLogger(ByteUtils.class.getName());

	/**
	 * Returns the index within this byte-array of the first occurrence of the
	 * specified(search bytes) byte array.<br>
	 * Starting the search at the specified index, and end at the specified index.
	 *
	 * borrowed from
	 * https://github.com/riversun/finbin/blob/master/src/main/java/org/riversun/finbin/BinarySearcher.java
	 *
	 * @param srcBytes    search in
	 * @param searchBytes searched data
	 * @param startIndex  start index (zero based)
	 * @param endIndex    end index
	 * @return zero based index of found data or -1
	 */
	public static int indexOf(byte[] srcBytes, byte[] searchBytes, int startIndex, int endIndex) {
		if (searchBytes.length == 0 || (endIndex - startIndex + 1) < searchBytes.length) {
			return -1;
		}
		int maxScanStartPosIdx = srcBytes.length - searchBytes.length;
		final int loopEndIdx;
		if (endIndex < maxScanStartPosIdx) {
			loopEndIdx = endIndex;
		} else {
			loopEndIdx = maxScanStartPosIdx;
		}
		int lastScanIdx = -1;
		label: // goto label
		for (int i = startIndex; i <= loopEndIdx; i++) {
			for (int j = 0; j < searchBytes.length; j++) {
				if (srcBytes[i + j] != searchBytes[j]) {
					continue label;
				}
				lastScanIdx = i + j;
			}
			if (endIndex < lastScanIdx || lastScanIdx - i + 1 < searchBytes.length) {
				// it becomes more than the last index
				// or less than the number of search bytes
				return -1;
			}
			return i;
		}
		return -1;
	}

	/**
	 * Search bytes in byte array returns indexes within this byte-array of all
	 * occurrences of the specified(search bytes) byte array in the specified range
	 *
	 * borrowed from
	 * https://github.com/riversun/finbin/blob/master/src/main/java/org/riversun/finbin/BinarySearcher.java
	 *
	 * @param srcBytes         search in
	 * @param searchBytes      searched data
	 * @param searchStartIndex zero based start index
	 * @param searchEndIndex   end index
	 * @return result index list
	 */
	public static List<Integer> searchBytes(byte[] srcBytes, byte[] searchBytes, int searchStartIndex,
			int searchEndIndex) {
		final int destSize = searchBytes.length;
		final List<Integer> positionIndexList = new ArrayList<Integer>();
		int cursor = searchStartIndex;
		while (cursor < searchEndIndex + 1) {
			int index = ByteUtils.indexOf(srcBytes, searchBytes, cursor, searchEndIndex);
			if (index >= 0) {
				positionIndexList.add(index);
				cursor = index + destSize;
			} else {
				cursor++;
			}
		}
		return positionIndexList;
	}

	/**
	 * read input stream as binary data
	 *
	 * @param requestStream source input stream to be read
	 * @return array of bytes
	 */
	public static byte[] getInputAsBinary(InputStream requestStream) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			byte[] buf = new byte[100000];
			int bytesRead = 0;
			while ((bytesRead = requestStream.read(buf)) != -1) {
				bos.write(buf, 0, bytesRead);
			}
			requestStream.close();
			bos.close();
		} catch (IOException e) {
			log.log(Level.SEVERE, "error while decoding input stream", e);
		}
		return bos.toByteArray();
	}
}
