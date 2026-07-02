package cz.bliksoft.javautils.xml.xpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class NowAndFormatDateTest {

	@Test
	void nowReturnsEpochMillisAsLong() throws Exception {
		long before = System.currentTimeMillis();
		Object result = new NowFunction().evaluate(Collections.emptyList());
		long after = System.currentTimeMillis();

		assertInstanceOf(Long.class, result);
		long value = (Long) result;
		assertTrue(value >= before && value <= after, "now() should be a current epoch-millis value");
	}

	@Test
	void formatDateAcceptsLongEpochMillisDirectly() throws Exception {
		long millis = 1_700_000_000_000L;
		String pattern = "yyyy-MM-dd HH:mm:ss";
		String expected = DateTimeFormatter.ofPattern(pattern)
				.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));

		Object result = new FormatDateFunction().evaluate(Arrays.asList(Long.valueOf(millis), pattern));

		assertEquals(expected, result);
	}

	@Test
	void formatDateFormatsNowResult() throws Exception {
		Object now = new NowFunction().evaluate(Collections.emptyList());
		Object formatted = new FormatDateFunction().evaluate(Arrays.asList(now, "yyyy"));

		assertEquals(String.valueOf(Year.now().getValue()), formatted);
	}
}
