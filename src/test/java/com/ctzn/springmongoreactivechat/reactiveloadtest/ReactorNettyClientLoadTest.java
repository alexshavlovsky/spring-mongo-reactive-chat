package com.ctzn.springmongoreactivechat.reactiveloadtest;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"test", "replay-service", "file-system-attachments"})
@RunWith(SpringRunner.class)
public class ReactorNettyClientLoadTest {

    @Test
    @Ignore
    public void reactor_load_test() throws InterruptedException {
        int ITER_NUM = 10;
        int BOTS_NUM = 10;
        for (int z = 0; z < ITER_NUM; z++) {
            System.out.println("Iteration: " + z);
            System.out.println("-------------");
            SupervisorBot supervisorBot = new SupervisorBot();
            System.out.println("Connect a supervisor");
            List<SpawnBot> bots = new ArrayList<>();
            System.out.println("Spawn bots");
            for (int i = 0; i < BOTS_NUM; i++) bots.add(new SpawnBot(supervisorBot));
            Thread.sleep(4000);
            System.out.println("Assert");
            Set<String> expectedNicksSet = bots.stream().map(ReactiveTestClient::getNick).collect(Collectors.toSet());
            expectedNicksSet.add(supervisorBot.getNick());
            bots.forEach(bot -> {
                Collection<ChatClient> snapshot = bot.getSnapshot();
                Map<String, List<ServerMessage>> msgMap = bot.getMessagesMap();
                Assert.assertNotNull("Every bot has received an initial snapshot", msgMap.get("snapshot"));
                Assert.assertEquals("Every bot has received an initial snapshot", 1, msgMap.get("snapshot").size());
                Assert.assertEquals("All bots have been visible", BOTS_NUM + 1, snapshot.size());
                Set<String> actualNicksSet = snapshot.stream().map(ChatClient::getNick).collect(Collectors.toSet());
                Assert.assertEquals("All bots have been visible", expectedNicksSet, actualNicksSet);

                System.out.println(snapshot.size() + ": " + bot.getNick() + ": " + msgMap.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue().size())
                        .collect(Collectors.joining(", ", "{", "}")));
            });
            System.out.println("Disconnect bots");
            bots.forEach(ReactiveTestClient::close);
            System.out.println("Disconnect the supervisor");
            supervisorBot.close();
            Thread.sleep(1000);
            System.out.println("Done\n");
        }
    }
}
