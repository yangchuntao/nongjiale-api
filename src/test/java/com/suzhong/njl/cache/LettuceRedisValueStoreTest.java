package com.suzhong.njl.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class LettuceRedisValueStoreTest {

    @Test
    void delegatesGetSetAndDeleteToStringRedisTemplate() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("njl:region:all")).thenReturn("[]");

        LettuceRedisValueStore store = new LettuceRedisValueStore(redis);

        assertThat(store.get("njl:region:all")).isEqualTo("[]");
        store.set("njl:region:all", "[]", Duration.ofMinutes(30));
        store.delete("njl:region:all");

        verify(values).set("njl:region:all", "[]", Duration.ofMinutes(30));
        verify(redis).delete("njl:region:all");
    }
}
