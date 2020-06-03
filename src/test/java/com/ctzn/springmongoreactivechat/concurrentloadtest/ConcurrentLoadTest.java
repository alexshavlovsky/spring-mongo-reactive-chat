package com.ctzn.springmongoreactivechat.concurrentloadtest;

import com.ctzn.springmongoreactivechat.mockclient.ChatClient;
import com.ctzn.springmongoreactivechat.mockclient.MockChatClient;
import com.ctzn.springmongoreactivechat.mockclient.ServerMessage;
import com.ctzn.springmongoreactivechat.mockclient.User;
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

    private WsTestClient newBot() throws InterruptedException {
        WsTestClient client = WsTestClient.newInstance("ws://localhost:8085/ws/");
        client.connectBlocking();
        client.getChat().sendHello();
        return client;
    }

    private void spawnBots(int botsNum) throws InterruptedException {
        List<WsTestClient> bots = new ArrayList<>();

        log("Bots number: " + botsNum);
        log("Instantiate bots");
        for (int i = 0; i < botsNum; i++) bots.add(newBot());
        WsTestClient chatObserver = newBot();
        sleep();

        log("Send messages");
        List<String> messagesList = new ArrayList<>();
        for (WsTestClient bot : bots) {
            String msg = UUID.randomUUID().toString();
            bot.getChat().sendMsg(msg);
            messagesList.add(msg);
            Thread.sleep(10);
        }
        sleep();

        log("Make assertions");
        Supplier<Stream<User>> users = () -> Stream.concat(Stream.of(chatObserver), bots.stream()).map(WsTestClient::getChat).map(MockChatClient::getUser);
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
        for (WsTestClient bot : bots) bot.closeBlocking();
        sleep();

        Set<String> actualClients = chatObserver.getChat().getChatClients().stream().map(ChatClient::getNick).collect(Collectors.toSet());
        Assert.assertEquals("Exactly one client is visible", actualClients.size(), 1);
        Assert.assertEquals("Observer is visible", actualClients.toArray()[0], chatObserver.getChat().getUser().getNick());

        log("Disconnect the observer");
        chatObserver.closeBlocking();

        log("Done");
    }

    @Test
    public void test_25_bots() throws InterruptedException {
        spawnBots(25);
    }

    @Test
    public void test_50_bots() throws InterruptedException {
        spawnBots(50);
    }

    @Test
    public void test_100_bots() throws InterruptedException {
        spawnBots(100);
    }
}
