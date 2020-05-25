package com.ctzn.springmongoreactivechat.reactiveloadtest;

class SpawnBot extends ReactiveTestClient {
    private final SupervisorBot supervisorBot;

    SpawnBot(SupervisorBot supervisorBot) {
        super(null, 0);
        this.supervisorBot = supervisorBot;
    }

    @Override
    boolean publishStrategy(ServerMessage msg) {
        return supervisorBot.getClientId().equals(msg.getClientId()) && "msg".equals(msg.getType()) && "PUBLISH".equals(msg.getPayload());
    }
}
