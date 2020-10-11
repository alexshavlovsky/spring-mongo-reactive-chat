package com.ctzn.springmongoreactivechat.service.messages;

import com.ctzn.springmongoreactivechat.configuration.MessageSeeder;
import com.ctzn.springmongoreactivechat.domain.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;

import javax.annotation.PostConstruct;

@Service
@Profile("replay-service")
public class ReplayBroadcastMessageServiceAdapter implements BroadcastMessageService {

    @Value("${test_messages_count}")
    int testMessagesCount;

    private final ReplayProcessor<Message> processor = ReplayProcessor.create(50);
    private final FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    @PostConstruct
    public void saveInitSequence() {
        MessageSeeder.getInitSequence(testMessagesCount).map(sink::next).blockLast();
    }

    @Override
    public Flux<Message> getTopic() {
        return processor;
    }

    @Override
    public Mono<Message> saveMessage(Message message) {
        sink.next(message);
        return Mono.just(message);
    }
}
