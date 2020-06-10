package com.ctzn.springmongoreactivechat.concurrentloadtest;

import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClient;
import com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient.MockChatClientImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

class WsTestClient implements TestClient {

    private final WebSocketClient webSocketClient;
    private final MockChatClientImpl chat;

    private WsTestClient(URI uri, boolean autoGreeting) {
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                if (autoGreeting) chat.sendHello();
            }

            @Override
            public void onMessage(String message) {
                chat.handleJson(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (remote)
                    System.out.println("WS client was disconnected by the server with code: " + code + " " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("WS client error occurred...\n" + ex);
            }
        };

        chat = new MockChatClientImpl(webSocketClient::send);
    }

    static WsTestClient newInstance(String uri, boolean autoGreeting) {
        return new WsTestClient(URI.create(uri), autoGreeting);
    }

    @Override
    public void connect() throws InterruptedException {
        webSocketClient.connectBlocking();
    }

    @Override
    public void close() throws InterruptedException {
        webSocketClient.closeBlocking();
    }

    @Override
    public MockChatClient getChat() {
        return chat;
    }

    @Override
    public boolean disconnected() {
        return webSocketClient.isClosed();
    }
}
