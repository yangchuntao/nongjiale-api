package com.suzhong.njl.web.admin.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * 只接受 JSON 整数。带小数点的数字交给现有「请求体格式不正确」，不截成整数。
 */
public class StrictIntegerDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
            throw context.weirdNumberException(parser.getDecimalValue(), Integer.class, "须为整数");
        }
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            return (Integer) context.handleUnexpectedToken(Integer.class, parser);
        }
        return parser.getIntValue();
    }

    @Override
    public Integer getNullValue(DeserializationContext context) {
        return null;
    }
}
