package com.ctzn.springmongoreactivechat.repository;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Tailable;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MessageRepository extends ReactiveMongoRepository<Message, String> {

    @Tailable
    @Query("{}")
    Flux<Message> findAllMessages();

}
