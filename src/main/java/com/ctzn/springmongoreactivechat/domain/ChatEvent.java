package com.ctzn.springmongoreactivechat.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.event.Level;
import org.slf4j.event.SubstituteLoggingEvent;

import java.util.Date;

@Data
@AllArgsConstructor
public class ChatEvent {
    String type;
    String time;
    String message;
    String payload1;
    String payload2;
    String payload3;

    public static ChatEvent fromLoggingEvent(SubstituteLoggingEvent e) {
        String time = new Date(e.getTimeStamp()).toString();
        String message = e.getMessage();
        String level = e.getLevel().toString();
        return new ChatEvent("log", time, message, level, "", "");
    }

    public static ChatEvent fromMessage(Message message) {
        String name = message.getNickName();
        String dir = ">";
        String time = message.getTimestamp().toString();
        String msgText = message.getText();
        return new ChatEvent("msg", time, msgText, name, dir, "");
    }

    public static ChatEvent fromError(Throwable e) {
        SubstituteLoggingEvent event = new SubstituteLoggingEvent();
        event.setLevel(Level.ERROR);
        event.setTimeStamp(new Date().getTime());
        event.setMessage(e.getMessage());
        return fromLoggingEvent(event);
    }

    private static final ObjectMapper json = new ObjectMapper();

    public String asJson() {
        try {
            return json.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            try {
                return json.writeValueAsString(ChatEvent.fromError(e));
            } catch (JsonProcessingException ex) {
                return "{}";
            }
        }
    }
}
