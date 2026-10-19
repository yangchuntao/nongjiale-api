package com.suzhong.njl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * 在线接口文档（OpenAPI 3）配置。
 *
 * <p>仅在 {@code springdoc.api-docs.enabled=true} 时对外提供：
 * <ul>
 *   <li>{@code GET /v3/api-docs} — OpenAPI JSON，供前端代码生成或 Apifox/Postman 导入；</li>
 *   <li>{@code GET /swagger-ui.html} — 可视化调试页。</li>
 * </ul>
 *
 * <p>默认关闭，本地 {@code local} profile 打开；生产（{@code mysql} profile）除非显式设置
 * {@code OPENAPI_ENABLED=true}，否则不注册文档端点，避免暴露内部接口清单。
 *
 * <p>鉴权模型：全局声明 {@code bearerAuth}（HTTP Bearer，非 JWT），需要登录的控制器在类上
 * 标注 {@code @SecurityRequirement(name = "bearerAuth")}；公开接口不加，Swagger 上不显示锁标。
 */
@Configuration
public class OpenApiConfig {

    /** 安全方案名，需与控制器 {@code @SecurityRequirement(name = ...)} 保持一致。 */
    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * 文档元信息与鉴权方案。
     *
     * <p>描述里只写跨接口通用的约定（信封、错误码、令牌），单接口细节放在各控制器的
     * {@code @Operation} 上，避免两处重复维护。
     *
     * @return OpenAPI 根对象
     */
    @Bean
    public OpenAPI nongjialeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("苏中种子 · 农家乐 API")
                        .version("MVP-2")
                        .description("""
                                苏中农家乐垂直平台后端，覆盖门户（游客）、商家、运营三端。

                                **统一响应信封**：除 `/health` 外均为 `{code, message, data}`，`code = 0` 表示成功。

                                **鉴权**：`Authorization: Bearer <token>`。令牌为 32 字节随机串，存进程内存，
                                默认 12 小时过期，服务重启即失效；它不是 JWT，不能自行解析。

                                **常见错误码**：400 参数或状态非法；401 未登录或令牌失效；403 角色不符或账号停用；
                                404 资源不存在；409 状态冲突或乐观锁版本不一致；410 门店已下架或目的地未开通；
                                503 微信登录未配置。

                                **分页**：`page` 从 1 开始，`pageSize` 默认 20、上限 100。
                                """)
                        .license(new License().name("内部项目，未开源")))
                .addServersItem(new Server().url("http://localhost:8080").description("本地"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("opaque")
                                .description("登录接口返回的 token 原文，填入时无需再加 Bearer 前缀")));
    }
}
