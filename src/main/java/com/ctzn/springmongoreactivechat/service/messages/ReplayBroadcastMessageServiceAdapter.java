package com.ctzn.springmongoreactivechat.service.messages;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

@Service
@Profile("replay-service")
public class ReplayBroadcastMessageServiceAdapter implements BroadcastMessageService {

    private final ReplayProcessor<Message> processor = ReplayProcessor.create(50);
    private final FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    public ReplayBroadcastMessageServiceAdapter() {
        sink.next(Message.newInfo("Service started"));
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
