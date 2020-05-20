package com.ctzn.springmongoreactivechat.loadtest;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class IntegrationLoadTest {

    private List<TestClient> bots;

    private void addBot(String nickPrefix) {
        TestClient client = TestClient.newInstance("ws://localhost:8085/ws/", nickPrefix);
        bots.add(client);
        client.connect();
    }

    private void waitOnLatch(String text, Predicate<TestClient> predicate) throws InterruptedException {
        log.info(text);
        int i = 0;
        while (bots.stream().anyMatch(predicate)) {
            Thread.sleep(100);
            if (i++ == 50) Assert.fail(text + " timeout");
        }
    }

    private void spawnBots(int botsNum) throws InterruptedException {
        bots = new ArrayList<>();
        String nickPrefix = "Test_" + botsNum + "_bot_";
        log.info("Bots number: {}", botsNum);

        log.info("Instantiate bots");
        for (int i = 0; i < botsNum; i++) addBot(nickPrefix);

        waitOnLatch("Waiting for snapshot", bot -> bot.snapshotLatch.getCount() != 0);

        waitOnLatch("Waiting for snapshot update self", bot -> bot.snapshotUpdateSelfLatch.getCount() != 0);

        log.info("Send messages");
        String msg = UUID.randomUUID().toString();
        for (TestClient testClient : bots) {
            testClient.sendMsg(msg);
            Thread.sleep(50);
        }

        log.info("1s delay...");
        Thread.sleep(1000);

        log.info("Disconnect bots");
        for (TestClient bot : bots) if (bot.getReadyState() == ReadyState.OPEN) bot.close();
        waitOnLatch("Waiting for connection closing", bot -> bot.disconnectLatch.getCount() != 0);

        bots.stream().map(bot -> bot.getNick() + ": " + bot.messagesMap.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue().size())
                .collect(Collectors.joining(", ", "{", "}"))).forEach(log::info);

        log.info("Make assertions");
        Set<String> expectedSet = bots.stream().map(TestClient::getNick).collect(Collectors.toSet());
        bots.forEach(bot -> {
            Map<String, List<ServerMessageTestModel>> msgMap = bot.messagesMap;
            List<ServerMessageTestModel> msgList = msgMap.get("msg");
            Set<String> actualSet = msgList.stream().filter(m -> m.getUserNick().contains(nickPrefix)).map(ServerMessageTestModel::getUserNick).collect(Collectors.toSet());
            Assert.assertEquals(expectedSet, actualSet);
        });
    }

    @Test
    public void test_10_bots() throws InterruptedException {
        spawnBots(10);
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
