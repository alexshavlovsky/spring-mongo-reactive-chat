package com.ctzn.springmongoreactivechat.reactiveloadtest;

class SupervisorBot extends ReactiveTestClient {

    SupervisorBot() {
        super("PUBLISH", 1000);
    }

    @Override
    boolean publishStrategy(ServerMessage msg) {
        return false;
    }
}
