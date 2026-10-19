package com.suzhong.njl.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用时钟配置。
 *
 * <p>全站统一使用 {@code Asia/Shanghai}，避免「数据库、JVM、JSON 序列化各用各的时区」。
 * 通过注入 {@code Clock} 而不是直接调用 {@code LocalDateTime.now()}，
 * 让审计填充、令牌过期等逻辑在测试中可以替换为固定时钟。
 */
@Configuration
public class ClockConfig {

    /** 业务统一时区，供时钟 Bean 与其他需要显式指定时区的代码复用。 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /** 产出系统时钟；JVM 默认时区已由启动类 static 块兜底，这里再显式指定一次。 */
    @Bean
    public Clock applicationClock() {
        return Clock.system(ZONE);
    }
}
