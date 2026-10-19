package com.suzhong.njl;

import java.util.TimeZone;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 苏中农家乐后端启动类。
 *
 * <p>{@code @SpringBootApplication} 以 {@code com.suzhong.njl} 为根做组件扫描，
 * 覆盖 config、security、web、service、wechat、cache 等子包；Mapper 接口没有逐个加
 * {@code @Mapper}，由 {@code @MapperScan} 统一注册动态代理，XML 位置见
 * {@code mybatis-plus.mapper-locations}。
 */
@SpringBootApplication
@MapperScan("com.suzhong.njl.mapper")
public class NjlApplication {

    static {
        // 必须在 Spring 启动前设置：JDBC 时区、日志时间与 JSON 序列化都会读 JVM 默认时区
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    /** 进程入口，交给 Spring Boot 完成容器启动。 */
    public static void main(String[] args) {
        SpringApplication.run(NjlApplication.class, args);
    }
}
