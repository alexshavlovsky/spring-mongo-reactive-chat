package com.ctzn.springmongoreactivechat.concurrentloadtest;

import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.ChatClient;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClient;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.ServerMessage;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"test", "replay-service", "file-system-attachments"})
class ConcurrentLoadTest {

    private void log(String s) {
        System.out.println(s);
    }

    private TestClient newBot(TestClientFactory botFactory) throws InterruptedException {
        TestClient client = botFactory.newClient();
        client.connect();
        return client;
    }

    private void spawnBots(int botsNum, TestClientFactory botFactory) throws InterruptedException {
        List<TestClient> bots = new ArrayList<>();

        log("Bots number: " + botsNum);

        log("Connect bots");
        TestClient chatObserver = newBot(botFactory);
        for (int i = 0; i < botsNum; i++) bots.add(newBot(botFactory));

        Set<String> botsIds = bots.stream().map(TestClient::getChat).map(MockChatClient::getUser).map(User::getId).collect(Collectors.toSet());

        while (true) {
            long seen = bots.stream().filter(bot -> bot.getChat().getChatClients().stream().filter(chatClient -> botsIds.contains(chatClient.getClientId())).count() > 0).count();
            long actual = bots.stream().filter(bot -> bot.getChat().getChatClients().stream().filter(chatClient -> botsIds.contains(chatClient.getClientId())).count() == botsNum).count();
            int expected = botsNum;
            if (actual == expected) break;
            log("Wait for clients: " + seen + "/" + actual + "/" + expected);
            sleep(500);
        }

        log("Send setTyping");
        for (TestClient bot : bots) bot.getChat().sendSetTyping();

        while (true) {
            long actual = bots.stream().filter(bot ->
                    bot.getChat().getServerMessages().stream()
                            .filter(serverMessage -> "setTyping".equals(serverMessage.getType()) && botsIds.contains(serverMessage.getClient().getClientId())).count() == botsNum
            ).count();
            int expected = botsNum;
            if (actual == expected) break;
            log("Wait for setTyping: " + actual + "/" + expected);
            sleep(500);
        }

        log("Send messages");
        List<String> messagesList = new ArrayList<>();
        for (TestClient bot : bots) {
            String msg = UUID.randomUUID().toString();
            bot.getChat().sendMsg(msg);
            messagesList.add(msg);
        }

        while (true) {
            long actual = bots.stream().filter(bot ->
                    bot.getChat().getServerMessages().stream()
                            .filter(serverMessage -> "msg".equals(serverMessage.getType()) && botsIds.contains(serverMessage.getClient().getClientId())).count() == botsNum
            ).count();
            int expected = botsNum;
            if (actual == expected) break;
            log("Wait for messages: " + actual + "/" + expected);
            sleep(500);
        }

        log("Make assertions");
        Supplier<Stream<User>> users = () -> Stream.concat(Stream.of(chatObserver), bots.stream()).map(TestClient::getChat).map(MockChatClient::getUser);
        Set<String> userNicksList = users.get().map(User::getNick).collect(Collectors.toSet());
        Set<String> userIdsList = users.get().map(User::getId).collect(Collectors.toSet());
        bots.forEach(bot -> {
            MockChatClient chat = bot.getChat();
            Map<String, List<ServerMessage>> msgMap = chat.getServerMessages().stream()
                    .collect(HashMap::new, (m, v) -> m.merge(v.getType(), Stream.of(v).collect(Collectors.toList()),
                            (a, n) -> {
                                a.addAll(n);
                                return a;
                            }), Map::putAll);
            log(chat.getUser().getNick() + ": " + msgMap.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue().size()).collect(Collectors.joining(", ", "{", "}")));
            Assertions.assertEquals(1, msgMap.get("snapshot").size(), "A server greeting was received");

            List<String> actualSetTypingList = msgMap.get("setTyping").stream().map(ServerMessage::getClient).map(ChatClient::getNick).collect(Collectors.toList());
            List<String> expectedSetTypingList = bots.stream().map(b -> b.getChat().getUser().getNick()).collect(Collectors.toList());
            Assertions.assertTrue(actualSetTypingList.containsAll(expectedSetTypingList), "All setTyping messages were received");

            List<String> actualMsgList = msgMap.get("msg").stream().map(ServerMessage::getPayload).collect(Collectors.toList());
            Assertions.assertTrue(actualMsgList.containsAll(messagesList), "All messages were received");

            Set<String> actualUserNicksList = chat.getChatClients().stream().map(ChatClient::getNick).collect(Collectors.toSet());
            Set<String> actualUserIdsList = chat.getChatClients().stream().map(ChatClient::getClientId).collect(Collectors.toSet());
            Assertions.assertEquals(userNicksList, actualUserNicksList, "All nicks were visible");
            Assertions.assertEquals(userIdsList, actualUserIdsList, "All ids were visible");
        });

        log("Disconnect bots");
        for (TestClient bot : bots) bot.close();

        while (true) {
            long actual = chatObserver.getChat().getChatClients().stream().filter(chatClient -> botsIds.contains(chatClient.getClientId())).count();
            int expected = 0;
            if (actual == expected) break;
            log("Wait for all the clients are disconnected: " + actual + "/" + expected);
            sleep(100);
        }

        Set<String> actualClients = chatObserver.getChat().getChatClients().stream().map(ChatClient::getNick).collect(Collectors.toSet());
        Assertions.assertEquals(actualClients.size(), 1, "Exactly one client is visible");
        Assertions.assertEquals(actualClients.toArray()[0], chatObserver.getChat().getUser().getNick(), "Observer is visible");

        log("Disconnect the observer");
        chatObserver.close();

        while (!chatObserver.disconnected()) {
            log("Wait for chat observer is disconnected...");
            sleep(100);
        }

        log("Done");
    }

    private final String TEST_URI = "http://localhost:8085/api/ws/";
    private final TestClientFactory wsBotFactory = new TestClientFactory(TEST_URI, "ws", true);
    private final TestClientFactory reactorBotFactory = new TestClientFactory(TEST_URI, "reactor", true);

    @Test
    void test_25_bots() throws InterruptedException {
        spawnBots(25, reactorBotFactory);
        spawnBots(25, wsBotFactory);
    }

    @Test
    void test_50_bots() throws InterruptedException {
        spawnBots(50, reactorBotFactory);
        spawnBots(50, wsBotFactory);
    }
}
