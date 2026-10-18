package com.suzhong.njl.web.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class StrictIntegerDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsIntegersAndNullAndRejectsFractions() throws Exception {
        assertThat(read("{\"sortOrder\":0}").sortOrder()).isZero();
        assertThat(read("{\"sortOrder\":null}").sortOrder()).isNull();
        assertThatThrownBy(() -> read("{\"sortOrder\":1.5}"))
                .isInstanceOf(JsonMappingException.class);
        assertThatThrownBy(() -> read("{\"sortOrder\":\"10\"}"))
                .isInstanceOf(JsonMappingException.class);
    }

    private BannerWriteRequest read(String json) throws Exception {
        return objectMapper.readValue(json, BannerWriteRequest.class);
    }
}
