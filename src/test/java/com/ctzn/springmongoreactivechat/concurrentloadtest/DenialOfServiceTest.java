package com.ctzn.springmongoreactivechat.concurrentloadtest;

import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.ChatClient;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClient;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"test", "replay-service", "file-system-attachments"})
class DenialOfServiceTest {

    private void log(Object o) {
        System.out.println(o.toString());
    }

    private TestClient newBot(TestClientFactory botFactory) throws InterruptedException {
        TestClient client = botFactory.newClient();
        client.connect();
        return client;
    }

    @Test
    void test_1000_connections_dumb_dos() throws InterruptedException {
        TestClientFactory attackers = new TestClientFactory("http://localhost:8085/api/ws/", "reactor", false);
        TestClientFactory validUsers = new TestClientFactory("http://localhost:8085/api/ws/", "reactor", true);

        sleep(1000);

        int attackersNum = 1000;

        log("Add some observers");
        List<TestClient> observers = new ArrayList<>();
        for (int i = 0; i < 10; i++) observers.add(newBot(validUsers));

        log("First wave");
        for (int i = 0; i < attackersNum; i++) {
            if (i % 100 == 0) log(i);
            newBot(attackers);
            Thread.sleep(2);
        }
        TestClient reference = newBot(attackers);
        while (!reference.disconnected()) {
            log("Wait for timeout and cleanup...");
            sleep(1000);
        }
        sleep(1000);

        log("Test wave");
        Random random = new Random();
        for (int i = 0; i < attackersNum; i++) {
            if (i % 100 == 0) log(i);
            TestClient bot = newBot(attackers);
            if (random.nextInt(10) != 0) bot.getChat().sendSetTyping();
            if (random.nextInt(50) == 0) observers.add(newBot(validUsers));
            sleep(2);
        }
        reference = newBot(attackers);
        while (!reference.disconnected()) {
            log("Wait for timeout and cleanup...");
            sleep(1000);
        }
        log("Add some observers");
        for (int i = 0; i < 10; i++) observers.add(newBot(validUsers));
        sleep(1000);

        log("Make assertions");
        Supplier<Stream<User>> users = () -> observers.stream().map(TestClient::getChat).map(MockChatClient::getUser);
        Set<String> userNicksList = users.get().map(User::getNick).collect(Collectors.toSet());
        Set<String> userIdsList = users.get().map(User::getId).collect(Collectors.toSet());
        observers.forEach(bot -> {
            MockChatClient chat = bot.getChat();
            Set<String> actualUserNicksList = chat.getChatClients().stream().map(ChatClient::getNick).collect(Collectors.toSet());
            Set<String> actualUserIdsList = chat.getChatClients().stream().map(ChatClient::getClientId).collect(Collectors.toSet());
            Assertions.assertEquals(observers.size(), chat.getChatClients().size(), "The number of visible bots corresponds to the number of observers");
            Assertions.assertEquals(userNicksList, actualUserNicksList, "All nicks are visible");
            Assertions.assertEquals(userIdsList, actualUserIdsList, "All ids are visible");
        });

        log("Disconnect observers");
        for (TestClient bot : observers) bot.close();
        sleep(observers.size());

        log("Done");
    }
}
