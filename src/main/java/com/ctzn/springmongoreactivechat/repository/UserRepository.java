package com.ctzn.springmongoreactivechat.repository;

import com.ctzn.springmongoreactivechat.domain.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {
}
