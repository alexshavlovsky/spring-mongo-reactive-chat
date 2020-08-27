package com.ctzn.springmongoreactivechat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"replay-service", "file-system-attachments"})
class SpringMongoReactiveChatApplicationTests {

    @Test
    void contextLoads() {
    }

}
