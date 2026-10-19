package com.suzhong.njl.web.admin.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * 严格整数反序列化器，当前只用在 {@link BannerWriteRequest#sortOrder()} 上。
 *
 * <p>解决的问题：Jackson 默认会把 {@code 100.0} 截断成 {@code 100}、把字符串 {@code "100"}
 * 宽松转换成整数，运营端排序值因此可能被静默改写。本类只接受 JSON 整数 token：
 * <ul>
 *   <li>{@code VALUE_NUMBER_FLOAT}（任意带小数点的写法）抛 {@code weirdNumberException}，不截断；</li>
 *   <li>字符串、布尔、对象等非整数 token 走 {@code handleUnexpectedToken} 抛映射异常；</li>
 *   <li>JSON null 由 {@link #getNullValue} 返回 null，交给业务层按「未传」处理（新建默认 100，更新报错）。</li>
 * </ul>
 *
 * <p>注册方式：在字段上标注 {@code @JsonDeserialize(using = StrictIntegerDeserializer.class)}，
 * 用于 {@code POST /api/admin/banners} 与 {@code PUT /api/admin/banners/{id}} 的 {@code sortOrder}；
 * 不注册为全局反序列化器，避免影响其他接口。抛出的异常被 Spring 包装为
 * {@code HttpMessageNotReadableException}，由全局异常处理器统一返回 400「请求体格式不正确」。
 * 注意超范围整数仍由 {@code BannerRules} 的 0 到 1000000 校验兜底，提示「请输入整数排序值」。
 */
public class StrictIntegerDeserializer extends JsonDeserializer<Integer> {

    /**
     * 严格解析整数：小数抛「须为整数」，字符串/布尔/对象等非整数 token 交给 Jackson 报类型错误，
     * JSON 数字整数正常返回。
     */
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
