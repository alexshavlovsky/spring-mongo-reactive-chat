# SpringBoot WebFlux MongoDB WebSocket Reactive Chat

Pure Reactive WebFlux MongoDB WebSocket Chat. The chat is built on top of MongoDB's API: Capped collections, Tailable cursors, ReactiveMongoOperations template.

## Build and run instructions

```
1. Start MongoDB:
dcker run --name test-mongo -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=mongoadmin -e MONGO_INITDB_ROOT_PASSWORD=secret -d mongo

2. Set appropriate spring.data.mongodb.host in the application.properties

3. Build and run the Spring Boot application. Open the URL in a browser:
localhost:8080
```

## Technology Stack

Component                      | Technology
---                            | ---
Backend engine                 | Spring Boot WebFlux
Database                       | Reactive MongoDB
Protocol                       | Reactive WebSockets
Frontend engine                | Pure JS + WebSockets + Bootstrap