package com.ctzn.springmongoreactivechat.configuration;

import com.ctzn.springmongoreactivechat.domain.Message;
import com.ctzn.springmongoreactivechat.domain.User;
import com.ctzn.springmongoreactivechat.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;

@Configuration
public class DbSeeder {

    private UserRepository userRepository;
    private ReactiveMongoTemplate template;

    public DbSeeder(UserRepository userRepository, ReactiveMongoTemplate template) {
        this.userRepository = userRepository;
        this.template = template;
    }

    @Bean
    CommandLineRunner startup() {
        return a -> {
            CollectionOptions options = CollectionOptions.empty().capped().size(1024 * 1024).maxDocuments(100);
            template.collectionExists(Message.class)
                    .flatMap(e -> e ? template.dropCollection(Message.class) : Mono.empty())
                    .switchIfEmpty(template.createCollection(Message.class, options).then()).subscribe();
            userRepository.deleteAll().doOnSuccess(
                    v -> userRepository.save(new User("TestUser")).doOnNext(System.out::println).subscribe()
            ).subscribe();
        };
    }
}
