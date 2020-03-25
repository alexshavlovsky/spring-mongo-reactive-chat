package com.ctzn.springmongoreactivechat.configuration;

import com.ctzn.springmongoreactivechat.domain.User;
import com.ctzn.springmongoreactivechat.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbSeeder {

    private UserRepository userRepository;

    public DbSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    CommandLineRunner startup() {
        return a -> userRepository.deleteAll().doOnSuccess(
                v -> userRepository.save(new User("TestUser")).doOnNext(System.out::println).subscribe()
        ).subscribe();
    }
}
