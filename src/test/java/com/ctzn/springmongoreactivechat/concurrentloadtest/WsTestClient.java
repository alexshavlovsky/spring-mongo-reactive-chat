package com.ctzn.springmongoreactivechat.concurrentloadtest;

import com.ctzn.springmongoreactivechat.mockclient.MockChatClient;
import com.ctzn.springmongoreactivechat.mockclient.MockChatClientImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

class WsTestClient {

    private final WebSocketClient webSocketClient;
    private final MockChatClientImpl chat;

    private WsTestClient(URI uri) {
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
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

    static WsTestClient newInstance(String uri) {
        return new WsTestClient(URI.create(uri));
    }

    void connectBlocking() throws InterruptedException {
        webSocketClient.connectBlocking();
    }

    void closeBlocking() throws InterruptedException {
        webSocketClient.closeBlocking();
    }

    MockChatClient getChat() {
        return chat;
    }
}
