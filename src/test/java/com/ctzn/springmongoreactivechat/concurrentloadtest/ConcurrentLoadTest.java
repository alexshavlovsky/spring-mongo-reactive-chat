package com.ctzn.springmongoreactivechat.concurrentloadtest;

import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.ChatClient;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClient;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.ServerMessage;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.User;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"test", "replay-service", "file-system-attachments"})
@RunWith(SpringRunner.class)
public class ConcurrentLoadTest {

    private void log(String s) {
        System.out.println(s);
    }

    private void sleep() throws InterruptedException {
        Thread.sleep(500);
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
        for (int i = 0; i < botsNum; i++) bots.add(newBot(botFactory));
        TestClient chatObserver = newBot(botFactory);
        sleep();

        log("Send messages");
        List<String> messagesList = new ArrayList<>();
        for (TestClient bot : bots) {
            String msg = UUID.randomUUID().toString();
            bot.getChat().sendMsg(msg);
            messagesList.add(msg);
            Thread.sleep(10);
        }
        sleep();

        log("Make assertions");
        Supplier<Stream<User>> users = () -> Stream.concat(Stream.of(chatObserver), bots.stream()).map(TestClient::getChat).map(MockChatClient::getUser);
        Set<String> userNicksList = users.get().map(User::getNick).collect(Collectors.toSet());
        Set<String> userIdsList = users.get().map(User::getId).collect(Collectors.toSet());
        bots.forEach(bot -> {
            MockChatClient chat = bot.getChat();
            Map<String, List<ServerMessage>> msgMap = chat.getMessageMap();
            log(chat.getUser().getNick() + ": " + msgMap.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue().size()).collect(Collectors.joining(", ", "{", "}")));
            Assert.assertEquals("A server greeting was received", 1, msgMap.get("snapshot").size());
            List<String> actualMsgList = msgMap.get("msg").stream().map(ServerMessage::getPayload).collect(Collectors.toList());
            Assert.assertTrue("All messages were received", actualMsgList.containsAll(messagesList));
            Set<String> actualUserNicksList = chat.getChatClients().stream().map(ChatClient::getNick).collect(Collectors.toSet());
            Set<String> actualUserIdsList = chat.getChatClients().stream().map(ChatClient::getClientId).collect(Collectors.toSet());
            Assert.assertEquals("All nicks were visible", userNicksList, actualUserNicksList);
            Assert.assertEquals("All ids were visible", userIdsList, actualUserIdsList);
        });

        log("Disconnect bots");
        for (TestClient bot : bots) bot.close();
        sleep();

        Set<String> actualClients = chatObserver.getChat().getChatClients().stream().map(ChatClient::getNick).collect(Collectors.toSet());
        Assert.assertEquals("Exactly one client is visible", actualClients.size(), 1);
        Assert.assertEquals("Observer is visible", actualClients.toArray()[0], chatObserver.getChat().getUser().getNick());

        log("Disconnect the observer");
        chatObserver.close();
        sleep();

        log("Done");
    }

    private final String TEST_URI = "ws://localhost:8085/ws/";
    private final TestClientFactory wsBotFactory = new TestClientFactory(TEST_URI, "ws", true);
    private final TestClientFactory reactorBotFactory = new TestClientFactory(TEST_URI, "reactor", true);

    @Test
    public void test_25_bots() throws InterruptedException {
        spawnBots(25, reactorBotFactory);
        spawnBots(25, wsBotFactory);
    }

    @Test
    public void test_50_bots() throws InterruptedException {
        spawnBots(50, wsBotFactory);
        spawnBots(50, reactorBotFactory);
    }

    @Test
    public void test_100_bots() throws InterruptedException {
        spawnBots(100, reactorBotFactory);
        spawnBots(100, wsBotFactory);
    }
}
