package com.ctzn.springmongoreactivechat.service.messages;

import com.ctzn.springmongoreactivechat.configuration.MessageSeeder;
import com.ctzn.springmongoreactivechat.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.annotation.PostConstruct;

@Service
@Profile("replay-service")
public class ReplayBroadcastMessageServiceAdapter implements BroadcastMessageService {

    private Logger LOG = LoggerFactory.getLogger(ReplayBroadcastMessageServiceAdapter.class);

    @Value("${test_messages_count}")
    int testMessagesCount;

    private final Sinks.Many<Message> replaySink = Sinks.many().replay().limit(64);
    private final Flux<Message> replayFlux = replaySink.asFlux();

    @PostConstruct
    public void saveInitSequence() {
        MessageSeeder.getInitSequence(testMessagesCount).map(replaySink::tryEmitNext).blockLast();
    }

    @Override
    public Flux<Message> getTopic() {
        return replayFlux;
    }

    @Override
    synchronized public Mono<Message> saveMessage(Message message) {
        Sinks.EmitResult result = replaySink.tryEmitNext(message);
        if (result != Sinks.EmitResult.OK) LOG.error("Emmit result: {}", result);
        return Mono.just(message);
    }
}
