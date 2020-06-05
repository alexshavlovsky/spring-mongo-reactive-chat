package com.ctzn.springmongoreactivechat.concurrentloadtest;

class TestClientFactory {
    private final String uri;
    private final String type;
    private final boolean autoGreeting;

    TestClientFactory(String uri, String type, boolean autoGreeting) {
        this.uri = uri;
        this.type = type;
        this.autoGreeting = autoGreeting;
    }

    TestClient newClient() {
        switch (type) {
            case "reactor":
                return ReactorTestClient.newInstance(uri, autoGreeting);
            case "ws":
                return WsTestClient.newInstance(uri, autoGreeting);
        }
        throw new UnsupportedOperationException();
    }
}
