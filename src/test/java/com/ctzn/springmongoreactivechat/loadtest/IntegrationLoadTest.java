package com.ctzn.springmongoreactivechat.loadtest;

import org.java_websocket.enums.ReadyState;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"test", "replay-service", "file-system-attachments"})
@RunWith(SpringRunner.class)
public class IntegrationLoadTest {

    private List<TestClient> bots;

    private void log(String s) {
        System.out.println(s);
    }

    private void sleep() throws InterruptedException {
        Thread.sleep(500);
    }

    private void addBot(String nickPrefix, boolean putOnlySnapshot) {
        TestClient client = TestClient.newInstance("ws://localhost:8085/ws/", nickPrefix, putOnlySnapshot);
        bots.add(client);
        client.connect();
    }

    private void waitOnLatch(String text, Predicate<TestClient> predicate) throws InterruptedException {
        log(text);
        int i = 0;
        while (bots.stream().anyMatch(predicate)) {
            Thread.sleep(100);
            if (i++ == 50) Assert.fail(text + " timeout");
        }
    }

    private void spawnBots(int botsNum) throws InterruptedException {
        bots = new ArrayList<>();
        String uid = new Random().nextInt(1000000000) + "";
        String nickPrefix = "Test_" + uid + "_bot_";
        log("Bots number: " + botsNum);

        log("Instantiate bots");
        for (int i = 0; i < botsNum; i++) {
            addBot(nickPrefix, true);
            Thread.sleep(20);
        }
        addBot("OBSERVER" + uid, false);

        waitOnLatch("Waiting for snapshot", bot -> bot.snapshotLatch.getCount() != 0);

        waitOnLatch("Waiting for snapshot update self", bot -> bot.snapshotUpdateSelfLatch.getCount() != 0);

        TestClient chatObserver = bots.remove(bots.size() - 1);

        log("Send messages");
        String msg = UUID.randomUUID().toString();
        for (TestClient testClient : bots) {
            testClient.sendMsg(msg);
            Thread.sleep(20);
        }

        sleep();

        log("Disconnect bots");
        for (TestClient bot : bots) if (bot.getReadyState() == ReadyState.OPEN) bot.close();
        waitOnLatch("Waiting for connection closing", bot -> bot.disconnectLatch.getCount() != 0);

        sleep();

        log("Make assertions");
        Set<String> expectedSet = bots.stream().map(TestClient::getNick).collect(Collectors.toSet());
        bots.forEach(bot -> {
            if (bot.disconnectLatch.getCount() != 0) Assert.fail("Concurrency issue");
            Map<String, List<ServerMessageTestModel>> msgMap = bot.getMessageMap();
            log(bot.getNick() + ": " + msgMap.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue().size())
                    .collect(Collectors.joining(", ", "{", "}")));
            List<ServerMessageTestModel> msgList = msgMap.get("msg");
            Set<String> actualMsgSet = msgList.stream().map(ServerMessageTestModel::getUserNick).filter(n -> n.contains(nickPrefix)).collect(Collectors.toSet());
            Assert.assertEquals("All messages was received", expectedSet, actualMsgSet);
            Set<String> actualPutOnlyUserSet = bot.getChatSnapshot().stream().map(ChatClientTestModel::getNick).filter(n -> n != null && n.startsWith(nickPrefix)).collect(Collectors.toSet());
            Assert.assertEquals("All bots was visible", expectedSet, actualPutOnlyUserSet);
        });
        Set<ChatClientTestModel> observerSnapshot = chatObserver.getChatSnapshot();
        Set<String> actualAfterDisconnectUserSet = observerSnapshot.stream().map(ChatClientTestModel::getNick).filter(n -> n != null && n.startsWith(nickPrefix)).collect(Collectors.toSet());
        Assert.assertEquals("All bots was removed", 0, actualAfterDisconnectUserSet.size());
        Assert.assertEquals("Observer is here", 1, observerSnapshot.stream().map(ChatClientTestModel::getNick).filter(n -> n != null && n.equals(chatObserver.getNick())).count());

        log("Disconnect the chat observer");
        chatObserver.closeBlocking();

        sleep();

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
