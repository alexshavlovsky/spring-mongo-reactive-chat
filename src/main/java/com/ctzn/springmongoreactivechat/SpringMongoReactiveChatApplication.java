package com.ctzn.springmongoreactivechat;

import com.ctzn.springmongoreactivechat.domain.Person;
import com.ctzn.springmongoreactivechat.repository.ReactivePersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringMongoReactiveChatApplication {

    @Autowired
    ReactivePersonRepository reactiveMongoRepository;

    @Bean
    CommandLineRunner startup() {
        return a -> reactiveMongoRepository.deleteAll().doOnSuccess(
                v -> reactiveMongoRepository.save(new Person("Luke", "Thompson")).doOnNext(System.out::println).subscribe()
        ).subscribe();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringMongoReactiveChatApplication.class, args);
    }

}
