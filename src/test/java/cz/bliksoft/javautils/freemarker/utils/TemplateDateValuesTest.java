package cz.bliksoft.javautils.freemarker.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;

class TemplateDateValuesTest {

	private static final ZoneId ZONE = ZoneId.of("Europe/Prague");
	private static final Clock CLOCK = Clock.fixed(LocalDateTime.of(2026, 10, 2, 14, 30, 15).atZone(ZONE).toInstant(),
			ZONE);

	private static LocalDateTime dt(String expression) {
		return TemplateDateValues.evaluate(expression, CLOCK);
	}

	@Test
	void isoValues() {
		assertEquals(LocalDateTime.of(2026, 3, 5, 0, 0), dt("2026-03-05"));
		assertEquals(LocalDateTime.of(2026, 3, 5, 8, 15), dt("2026-03-05T08:15"));
		assertEquals(LocalDateTime.of(2026, 3, 5, 8, 15, 30), dt("2026-03-05 8:15:30"));
		assertEquals(LocalDate.of(2026, 3, 5), TemplateDateValues.parseDate("2026-03-05T23:00", CLOCK));
	}

	@Test
	void keywordsAndOffsets() {
		assertEquals(LocalDateTime.of(2026, 10, 2, 0, 0), dt("today"));
		assertEquals(LocalDateTime.of(2026, 10, 2, 14, 30, 15), dt("NOW"));
		assertEquals(LocalDateTime.of(2026, 10, 9, 0, 0), dt("today+7"));
		assertEquals(LocalDateTime.of(2026, 10, 9, 0, 0), dt("today + 7d"));
		assertEquals(LocalDateTime.of(2026, 10, 16, 0, 0), dt("today+2w"));
		assertEquals(LocalDateTime.of(2026, 9, 2, 0, 0), dt("today-1M"));
		assertEquals(LocalDateTime.of(2027, 10, 2, 0, 0), dt("today+1y"));
		assertEquals(LocalDateTime.of(2026, 10, 2, 13, 30, 15), dt("now-1h"));
		assertEquals(LocalDateTime.of(2026, 10, 2, 15, 0, 15), dt("now+30m"));
		assertEquals(LocalDateTime.of(2026, 10, 2, 14, 30, 45), dt("now+30s"));
		assertEquals(LocalDateTime.of(2026, 10, 3, 12, 30, 15), dt("now+1d-2h"));
		assertEquals(LocalDateTime.of(2026, 10, 3, 8, 0), dt("today 08:00+1d"));
		assertEquals(LocalDateTime.of(2026, 2, 28, 0, 0), dt("2026-03-31-1M"));
		// a DATE takes the date part
		assertEquals(LocalDate.of(2026, 10, 3), TemplateDateValues.parseDate("now+12h", CLOCK));
	}

	@Test
	void invalidExpressions() {
		for (String bad : new String[] { "tomorrow", "today+", "2026-13-01", "today+1x", "now 08:00", "25:00" })
			assertThrows(IllegalArgumentException.class, () -> dt(bad), bad);
		assertNull(dt(" "));
		assertNull(dt(null));
		assertNull(TemplateDateValues.tryParseDate("tomorrow"));
		assertNull(TemplateDateValues.tryParseDateTime("2026-02-30"));
	}

	@Test
	void formatting() {
		assertEquals("2026-10-02", TemplateDateValues.formatDate(LocalDate.of(2026, 10, 2)));
		assertEquals("2026-10-02T08:05", TemplateDateValues.formatDateTime(LocalDateTime.of(2026, 10, 2, 8, 5)));
		assertEquals("2026-10-02T08:05:09", TemplateDateValues.formatDateTime(LocalDateTime.of(2026, 10, 2, 8, 5, 9)));
		assertNull(TemplateDateValues.formatDate(null));
		assertNull(TemplateDateValues.formatDateTime(null));
	}

	@Test
	void conversions() {
		LocalDateTime t = LocalDateTime.of(2026, 10, 2, 8, 5);
		Instant instant = t.atZone(ZoneId.systemDefault()).toInstant();
		assertEquals(t, TemplateDateValues.toLocalDateTime(Date.from(instant)));
		assertEquals(t, TemplateDateValues.toLocalDateTime(instant));
		assertEquals(t, TemplateDateValues.toLocalDateTime(java.sql.Timestamp.valueOf(t)));
		assertEquals(t, TemplateDateValues.toLocalDateTime(instant.toEpochMilli()));
		assertEquals(t.toLocalDate(), TemplateDateValues.toLocalDate(java.sql.Date.valueOf(t.toLocalDate())));
		assertEquals(t.toLocalDate(), TemplateDateValues.toLocalDate(t));
		assertEquals(t.toLocalDate().atStartOfDay(), TemplateDateValues.toLocalDateTime(t.toLocalDate()));
		assertEquals(LocalDate.of(2026, 1, 2), TemplateDateValues.toLocalDate("2026-01-02"));
		assertNull(TemplateDateValues.toLocalDate("x"));
		assertNull(TemplateDateValues.toLocalDate(null));
	}

	@Test
	void ranges() {
		assertArrayEquals(new LocalDateTime[] { dt("today"), dt("today+90") },
				TemplateDateValues.parseRange("today..today+90", CLOCK));
		assertArrayEquals(new LocalDateTime[] { null, dt("today") }, TemplateDateValues.parseRange("..today", CLOCK));
		assertArrayEquals(new LocalDateTime[] { dt("2026-01-01"), null },
				TemplateDateValues.parseRange("2026-01-01..", CLOCK));
		assertNull(TemplateDateValues.parseRange("10:20", CLOCK));
		assertNull(TemplateDateValues.parseRange(null, CLOCK));
	}
}
