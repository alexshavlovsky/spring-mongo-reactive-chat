package com.ctzn.springmongoreactivechat.configuration;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class TestMessagesSeederTest {

    @Test
    void getTestMessages() {
        StepVerifier.create(MessageSeeder.getInitSequence(10)).expectNextCount(11).verifyComplete();
    }
}
