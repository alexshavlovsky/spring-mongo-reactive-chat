package com.ctzn.springmongoreactivechat.configuration;

import com.ctzn.springmongoreactivechat.domain.Message;
import com.ctzn.springmongoreactivechat.domain.dto.ChatClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MessageSeeder {
    private static Flux<Message> getTestMessages(int num) {
        int clientId = new Random().nextInt(10000);
        ChatClient testClient = new ChatClient("session" + clientId, "client" + clientId, "Test client " + clientId);
        Instant now = Instant.now();
        Stream<Message> messageStream = IntStream.range(0, num).mapToObj(i ->
                new Message(testClient, "msg", "Message #" + i, Date.from(now.minus(num - 1 - i, ChronoUnit.HOURS)))
        );
        return Flux.fromStream(messageStream);
    }

    public static Flux<Message> getInitSequence(int num) {
        return MessageSeeder.getTestMessages(num).concatWith(Mono.just(Message.newInfo("Service started")));
    }
}
