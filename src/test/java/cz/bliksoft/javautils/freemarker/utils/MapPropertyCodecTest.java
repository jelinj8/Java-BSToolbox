package cz.bliksoft.javautils.freemarker.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MapPropertyCodecTest {

	@Test
	void decodePreservesOrder() {
		Map<String, String> result = MapPropertyCodec.decode("small=small.ttf;large=large.ttf");
		assertEquals("[small, large]", result.keySet().toString());
		assertEquals("small.ttf", result.get("small"));
		assertEquals("large.ttf", result.get("large"));
	}

	@Test
	void decodeBlankOrNullReturnsEmptyMap() {
		assertTrue(MapPropertyCodec.decode(null).isEmpty());
		assertTrue(MapPropertyCodec.decode("  ").isEmpty());
	}

	@Test
	void encodeRoundTrips() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("small", "small.ttf");
		map.put("large", "large.ttf");
		String encoded = MapPropertyCodec.encode(map);
		assertEquals("small=small.ttf;large=large.ttf", encoded);
		assertEquals(map, MapPropertyCodec.decode(encoded));
	}

	@Test
	void encodeEmptyOrNullReturnsEmptyString() {
		assertEquals("", MapPropertyCodec.encode(null));
		assertEquals("", MapPropertyCodec.encode(new LinkedHashMap<String, String>()));
	}
}
