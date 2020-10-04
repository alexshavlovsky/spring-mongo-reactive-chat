package com.ctzn.springmongoreactivechat.configuration;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class TestMessagesSeederTest {

    @Test
    void getTestMessages() {
        StepVerifier.create(MessageSeeder.getTestMessages(10)).expectNextCount(10).verifyComplete();
    }
}
