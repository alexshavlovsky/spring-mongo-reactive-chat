package com.ctzn.springmongoreactivechat;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@ActiveProfiles({"replay-service", "file-system-attachments"})
@RunWith(SpringRunner.class)
class SpringMongoReactiveChatApplicationTests {

	@Test
	void contextLoads() {
	}

}
