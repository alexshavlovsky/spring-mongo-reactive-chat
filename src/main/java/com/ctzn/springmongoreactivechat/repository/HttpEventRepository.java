package com.ctzn.springmongoreactivechat.repository;

import com.ctzn.springmongoreactivechat.domain.HttpEvent;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HttpEventRepository extends ReactiveMongoRepository<HttpEvent, String> {
}
