package com.ctzn.springmongoreactivechat.concurrentloadtest.mockclient;

import com.fasterxml.jackson.databind.ObjectMapper;

class Mapper {
    private static final ObjectMapper mapper = new ObjectMapper();

    String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            return "{}";
        }
    }

    <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }
}
