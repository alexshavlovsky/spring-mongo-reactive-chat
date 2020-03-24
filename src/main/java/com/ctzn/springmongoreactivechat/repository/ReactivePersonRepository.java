package com.ctzn.springmongoreactivechat.repository;

import com.ctzn.springmongoreactivechat.domain.Person;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReactivePersonRepository extends ReactiveMongoRepository<Person, String> {
//    Flux<Person> findByFirstName(String firstName);
//
//    Flux<Person> findByFirstNameOrderByLastName(String firstName, Pageable pageable);
//
//    Mono<Person> findByFirstNameAndLastName(String firstName, String lastName);
//
//    Mono<Person> findFirstByLastName(String lastName);
}
