FROM openjdk:8-jdk-alpine
ENV SPRING_PROFILES_ACTIVE mongo-service, mongo-grid-attachments, mongo-video-transcoder
EXPOSE 8080
COPY target/chat-service.jar chat-service.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","chat-service.jar"]

# docker build -t chat-service .
# docker -d run -p 8080:8080 --name chat_service chat-service
