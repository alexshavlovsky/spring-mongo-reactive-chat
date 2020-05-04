package com.ctzn.springmongoreactivechat.configuration;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;

@Configuration
@Profile("mongo-service")
//@EnableReactiveMongoRepositories("com.ctzn.springmongoreactivechat.repository")
class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.host}")
    String host;

    @Value("${spring.data.mongodb.port}")
    String port;

    @Value("${spring.data.mongodb.username}")
    String username;

    @Value("${spring.data.mongodb.password}")
    String password;

    @Value("${spring.data.mongodb.database}")
    String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public MongoClient reactiveMongoClient() {
        String uri = String.format("mongodb://%s:%s@%s:%s/?retryWrites=false", username, password, host, port);
        return MongoClients.create(uri);
    }

    @Bean
    public ReactiveGridFsTemplate reactiveGridFsTemplate() throws Exception {
        return new ReactiveGridFsTemplate(reactiveMongoDbFactory(), mappingMongoConverter(), "attachments");
    }
}
