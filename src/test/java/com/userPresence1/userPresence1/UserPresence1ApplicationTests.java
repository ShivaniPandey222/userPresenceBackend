package com.userPresence1.userPresence1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserPresence1ApplicationTests {

	private static final Logger log = LoggerFactory.getLogger(UserPresence1ApplicationTests.class);
	@Test
	void contextLoads() {
		log.info("shivani testing - This message should be clearly visible!");
        log.debug("This is a debug message from shivani's test."); // Will only show if logging level is DEBUG
        log.warn("This is a warning from shivani's test.");
	}

}
