# SpringBoot WebFlux MongoDB WebSocket Reactive Chat

Pure Reactive WebFlux MongoDB WebSocket Chat. The chat is built on top of MongoDB's API: Capped collections, Tailable cursors, ReactiveMongoOperations template, GridFS template.

A prebuilt limited-functionality Bootstrap client is included and served by SpringBoot at `http://localhost:8080`

The full-featured Angular PrimeNG frontend client which supports nick names, file attachments and video streaming can be found here:
<br>
[Angular 9 PrimeNg chat client](https://github.com/alexshavlovsky/primeng-chat-client.git).

A small Java stress testing client for this chat:
<br>
[Stress testing client for WebSocket chat](https://github.com/alexshavlovsky/ws-chat-test-java-client).

Build script for docker-compose (MongoDB profile, Angular client, nginx):
<br>
[MongoChat build scripts](https://github.com/alexshavlovsky/mongo-chat-ci-template.git). 

## Screenshots

### JS Bootstrap
<p align="center">
  <img src="screenshots/1_client_1.png?raw=true" width="360"/>
  <img src="screenshots/2_client_2.png?raw=true" width="360"/>  
</p>

### Angular PrimeNG
<p align="center">
  <img src="screenshots/3_angular_login.png?raw=true" width="720"/>  
</p>
<p align="center">
  <img src="screenshots/4_angular_chat.png?raw=true" width="720"/>  
</p>

## Build and run instructions

### Docker-compose (MongoDB profile, Angular client, nginx)
Prerequisites: git, JDK8, docker, docker-compose
```
1. git clone https://github.com/alexshavlovsky/mongo-chat-ci-template.git
2. cd mongo-chat-ci-template
3. Linux:   sh build.sh
   Windows: build.cmd
```

### MongoDB setup
```
1. Start MongoDB:
docker run --name test-mongo -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=mongoadmin -e MONGO_INITDB_ROOT_PASSWORD=secret -d mongo

2. Set appropriate spring.data.mongodb.host in the application.properties

3. In the application.properties set active profiles:
spring.profiles.active=mongo-service, mongo-grid-attachments

4. Build and run the Spring Boot application. Open the URL in a browser:
localhost:8080

5. The Angular client is recommended
```
### No-Mongo setup (in-memory chat, file-system attachments storage)
```
1. In the application.properties set active profiles:
spring.profiles.active=replay-service, file-system-attachments
 
2. Build and run the Spring Boot application. Open the URL in a browser:
localhost:8080

3. The Angular client is recommended
```
### Video transcoder and video streaming setup (MongoDB and Angular client are mandatory)
WARNING: the transcoding process of a single file may take several minutes depending on the file size
```
1. Start MongoDB:
docker run --name test-mongo -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=mongoadmin -e MONGO_INITDB_ROOT_PASSWORD=secret -d mongo

2. Set appropriate spring.data.mongodb.host in the application.properties

3. In the application.properties set active profiles:
spring.profiles.active=mongo-service, mongo-grid-attachments, mongo-video-transcoder

4. Build and run the Spring Boot application.

5. Use the Angular client: https://github.com/alexshavlovsky/primeng-chat-client.git 
```
## Technology Stack

Component                 | Technology
---                       | ---
Backend engine            | Spring Boot WebFlux
Database                  | Reactive MongoDB
Protocol                  | Reactive WebSockets
Server side thumbnails    | [Thumbnailator - a thumbnail generation library for Java](https://github.com/coobird/thumbnailator)
PDF documents thumbnails  | [PDF renderer - Java library for rendering PDF documents](https://github.com/katjas/PDFrenderer)
Video files thumbnails    | [The JAVE (Java Audio Video Encoder) library is Java wrapper on the ffmpeg project](https://github.com/a-schild/jave2)
HTML video transcoder     | Background service using JAVE ffmpeg wrapper (x264 and WEBM codecs are supported)
Video streaming           | Endpoint that supports partial requests
Caching                   | Caffeine in-memory cache
Frontend engine           | Pure JS + WebSockets + Bootstrap (limited-functionality)
or (see description)      | Angular 10 + PrimeNG
Prod packaging            | Docker Engine Container, Alpine Linux, OpenJDK, SpringBoot JAR

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
## Video transcoder summary
```
        array of sources
     +------------------------------------------------------+
     |                                                      |
     v                                                      |
+----------+  message  +-----------------------+    +--------------------+
| Frontend |---------->| Video file attachmens |--->| Compound Web Video |
+----------+           +-----------------------+    +--------------------+
     ^                     |                           |             ^
     |                     |                           v             |
     |                     |     +-------------------------+     +---------------------+
     |                     |     | Transcoding jobs queue: |<--->|  Transcoder facade  |
     |                     |     | 1 - MP4_480             |     +---------------------+
     |                     |     | 2 - WEBM_480            |             ^      ^
     |                     |     | 3 - MP4_720             |             |      |
     |                     |     | 4 - WEBM_720            |             |      v
     |                     |     | ...                     |             |  +-----------------+
     |                     |     +-------------------------+             |  | Ffmpeg executor |
     |                     |                                             |  +-----------------+
     |                     |    +--------------------+                   |
     |                     +--->| Attachment service |<------------------+
     |  original file           +--------------------+
     |  and transcoded sources           |
     +-----------------------------------+
```
