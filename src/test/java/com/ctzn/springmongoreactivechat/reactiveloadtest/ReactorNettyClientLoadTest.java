package com.ctzn.springmongoreactivechat.reactiveloadtest;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.DirectProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"test", "replay-service", "file-system-attachments"})
@RunWith(SpringRunner.class)
public class ReactorNettyClientLoadTest {

    @Test
    @Ignore
    public void reactor_load_test() throws InterruptedException {
        for (int z = 0; z < 2; z++) {
            DirectProcessor<String> commands = DirectProcessor.create();
            List<ReactiveTestClient> bots = new ArrayList<>();
            System.out.println("Prepare");
            for (int i = 0; i < 100; i++) bots.add(new ReactiveTestClient(commands));
            System.out.println("Wait");
            Thread.sleep(1000);
            commands.onNext("PUBLISH");
            System.out.println("Wait");
            Thread.sleep(1000);
            System.out.println("Assert");
            ReactiveTestClient bot = bots.get(0);
            Map<String, List<ServerMessage>> msgMap = bot.getMessagesMap();
            System.out.println(bot.getNick() + ": " + msgMap.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue().size())
                    .collect(Collectors.joining(", ", "{", "}")));

            Collection<ChatClient> snapshot = bot.getSnapshot();
            System.out.println(snapshot.size());
            System.out.println("Close");
            bots.forEach(ReactiveTestClient::close);
        }
    }
}
