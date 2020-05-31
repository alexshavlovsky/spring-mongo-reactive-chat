package com.ctzn.springmongoreactivechat.reactiveloadtest;

class SupervisorBot extends ReactiveTestClient {

    SupervisorBot() {
        super("PUBLISH", 2000);
    }

    @Override
    boolean publishStrategy(ServerMessage msg) {
        return false;
    }
}
