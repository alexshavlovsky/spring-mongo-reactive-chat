package com.ctzn.springmongoreactivechat.repository;

import com.ctzn.springmongoreactivechat.domain.Message;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends ReactiveMongoRepository<Message, String> {
}
