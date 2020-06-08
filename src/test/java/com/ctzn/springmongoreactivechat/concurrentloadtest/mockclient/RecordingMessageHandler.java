package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

interface RecordingMessageHandler extends ChatRecorder {
    void handleJsonMessage(String json);
}
