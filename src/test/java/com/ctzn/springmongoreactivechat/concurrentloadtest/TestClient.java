package com.ctzn.springmongoreactivechat.concurrentloadtest;

import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClient;

public interface TestClient {
    void connect() throws InterruptedException;

    void close() throws InterruptedException;

    MockChatClient getChat();
}
