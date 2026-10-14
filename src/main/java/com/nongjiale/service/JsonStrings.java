package com.nongjiale.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JsonStrings {
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};
    private final ObjectMapper mapper;

    public JsonStrings(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String write(List<String> values) {
        try {
            return mapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<String> read(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return mapper.readValue(raw, STRINGS);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
