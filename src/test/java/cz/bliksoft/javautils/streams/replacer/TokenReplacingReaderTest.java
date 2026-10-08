package cz.bliksoft.javautils.streams.replacer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Token replacement, including malformed input at end of stream. */
class TokenReplacingReaderTest {

	private static String replace(String input) {
		Map<String, String> tokens = new HashMap<>();
		tokens.put("a", "A");
		tokens.put("empty", "");
		return assertTimeoutPreemptively(Duration.ofSeconds(5),
				() -> new TokenReplacingReader(input, new MapTokenResolver(tokens)).readAsString());
	}

	@Test
	void replacesKnownTokens() {
		assertEquals("xAy", replace("x${a}y"));
		assertEquals("xy", replace("x${empty}y"));
		assertEquals("x${unknown}y", replace("x${unknown}y"));
		assertEquals("$5 and $", replace("$5 and $"));
	}

	@Test
	void unterminatedTokenIsPassedThrough() {
		assertEquals("x${a", replace("x${a"));
		assertEquals("x${", replace("x${"));
		assertEquals("A${b", replace("${a}${b"));
	}

	@Test
	void dollarAtEndOfInput() {
		assertEquals("x$", replace("x$"));
	}
}
