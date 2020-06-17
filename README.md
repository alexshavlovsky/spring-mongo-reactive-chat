# SpringBoot WebFlux MongoDB WebSocket Reactive Chat

Pure Reactive WebFlux MongoDB WebSocket Chat. The chat is built on top of MongoDB's API: Capped collections, Tailable cursors, ReactiveMongoOperations template, GridFS template.

A prebuilt limited-functionality Bootstrap client is included and served by SpringBoot at `http://localhost:8080`

The full-featured Angular PrimeNG frontend client which supports nick names and file attachments can be found here:
<br>
[Angular 9 PrimeNg chat client](https://github.com/alexshavlovsky/primeng-chat-client.git).

A small Java stress testing client for this chat:
<br>
[Stress testing client for WebSocket chat](https://github.com/alexshavlovsky/ws-chat-test-java-client).

## Screenshots

<p align="center">
  <img src="screenshots/1_client_1.png?raw=true" width="360"/>
  <img src="screenshots/2_client_2.png?raw=true" width="360"/>  
</p>

## Build and run instructions

### MongoDB setup
```
1. Start MongoDB:
dcker run --name test-mongo -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=mongoadmin -e MONGO_INITDB_ROOT_PASSWORD=secret -d mongo

2. Set appropriate spring.data.mongodb.host in the application.properties

3. Build and run the Spring Boot application. Open the URL in a browser:
localhost:8080
```
### No-Mongo setup (in-memory chat, file-system attachments storage)
```
1. In the application.properties set:
spring.profiles.active=replay-service, file-system-attachments
 
2. Build and run the Spring Boot application. Open the URL in a browser:
localhost:8080
```
## Technology Stack

Component                      | Technology
---                            | ---
Backend engine                 | Spring Boot WebFlux
Database                       | Reactive MongoDB
Protocol                       | Reactive WebSockets
Server side thumbnails         | [Thumbnailator - a thumbnail generation library for Java](https://github.com/coobird/thumbnailator)
Caching                        | Spring cache 
Frontend engine                | Pure JS + WebSockets + Bootstrap
or (see description)           | Angular 9 + PrimeNG

## Chat protocol summary
```
                       /-----------Messages-----------\
                      /                                \
                     /                          /---Client message--\
                    /                          /                     \
                   /                      Authentication:       Public messages:
                  /                     - hello    (U)        - msg       (M) (text)
        /----Server Message------\      - updateMe (U)        - richMsg   (M) (text with file attachments)
       /          |               \                           - setTyping (M) (repeated every 2 seconds
      /           |                \                                           while the user is typing)
  info (M)    snapshot (U)      snapshotUpdate (M)
  (text)   (list of clients)   (mutation of clients list)
                                - addUser     (new user has been connected)
                                - updateUser  (user has changed his nick and/or uid)
                                - removeUser  (user has been disconnected)

   (M) - multicast messages (these messages are forwarded by the server to each connected client):
           - server-clients: info, snapshotUpdate
           - client-clients: msg, richMsg, setTyping
   (U) - unicast messages:
           - hello     client-server greeting with user data (uid and nick)
                       must be send by a client within first 5 seconds of ws session
           - snapshot  server-client greeting with the list of clients in the chat
           - updateMe  client-server with new user data (uid and nick)
```
