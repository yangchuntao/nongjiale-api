package com.suzhong.njl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

/**
 * MyBatis-Plus 插件配置。
 *
 * <p>注册两个内置拦截器，负责乐观锁与物理分页。驼峰映射、枚举处理器、主键策略等
 * 在 {@code application.yml} 的 {@code mybatis-plus.*} 中声明，不在这里重复。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 产出 MyBatis-Plus 拦截器链。
     *
     * <p>顺序为「乐观锁 → 分页」：分页插件放在最后，对最终 SQL 做 count 与 LIMIT 改写；
     * 乐观锁插件给带 {@code @Version} 的更新追加版本条件，影响行数为 0 时由 Service 判为 409。
     *
     * <p>分页参数：{@code maxLimit=100} 与 {@code PageQuery} 的上限保持一致，防止外部传超大
     * pageSize 拖垮数据库；{@code overflow=false} 表示页码越界返回空页，而不是悄悄回到第一页，
     * 便于前端发现分页参数异常。
     *
     * @return 已装配两个内部拦截器的 MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(100L);
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
