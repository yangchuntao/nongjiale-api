# S5 冒烟 E01-2：未实现路径返回 404

## ① 概要设计

MVP-0 没有支付、收银台或钱包。`GET` / `POST` `/api/payments`、`/api/checkout`、`/api/pay`、`/api/wallet` 以及其它没有控制器的路径，应返回 HTTP 404，信封与现有错误一致：`{code, message, data}`。

先前这些请求落到 `GlobalExceptionHandler.handleUnexpected(Exception)`，HTTP 和 `code` 都是 500，文案是「服务器繁忙」。E01 门禁要求「没有支付实现」表现为 404，而不是服务器故障。

不新增支付控制器，不改已有接口的业务状态码。`CHECKED_IN` 的中文仍是「已入住」。

## ② 详细设计

Spring Boot 3.3 在找不到控制器、静态资源也不存在时抛出 `NoResourceFoundException`（`org.springframework.web.servlet.resource`）。关闭静态资源映射且 `spring.mvc.throw-exception-if-no-handler-found=true` 时，则抛出 `NoHandlerFoundException`。两者都是 `ServletException`，会被 `@ExceptionHandler(Exception.class)` 接住。

修复只加一个更具体的处理器，由 Spring 按异常类型优先匹配：

| 异常 | HTTP | code | message | data |
|------|------|------|---------|------|
| `NoResourceFoundException` | 404 | 404 | 接口不存在 | null |
| `NoHandlerFoundException` | 404 | 404 | 接口不存在 | null |

文案常量是 `GlobalExceptionHandler.ROUTE_NOT_FOUND_MESSAGE`，与「不支持的请求」「服务器繁忙」同一处维护。不按路径名单特判支付 URL。

已有映射保持不变：

- `ApiException` 仍用自身状态码（例如房源不存在 404「房源不存在」）
- 校验失败 400，方法不匹配 405「不支持的请求」，坏请求体 400
- 其它未分类异常仍是 500「服务器繁忙」，且不把异常信息写进信封
- `GET /health` 仍是 `{"status":"UP","service":"nongjiale-api"}`，不包业务信封
- `/api/admin/**`、`/api/merchant/**` 等鉴权规则不变。这些前缀下未登录仍是 401，不先变成 404

## ③ 改动点

| 对象 | 动作 |
|------|------|
| `GlobalExceptionHandler` | 为 `NoResourceFoundException`、`NoHandlerFoundException` 返回 404 信封 |
| `GlobalExceptionHandlerTest` | 两种异常都是 404；注解同时声明这两类；500 分支仍不泄露原因 |
| `UnknownRouteIntegrationTest` | 未知路径与支付类路径 404；`/health`、`GET /api/regions` 仍成功；`POST /api/regions` 仍是 405 |
| `README.md` | 说明未实现路径的 404 信封 |
| `docs/S5-E01-UNKNOWN-ROUTE.md` | 本说明 |

## ④ 影响点

- 没有控制器的 URL 从 500「服务器繁忙」改为 404「接口不存在」。这是 E01 期望行为。
- 已实现接口的成功、校验、鉴权、业务 404/409/410 不变。
- 不增加支付、下单或钱包实现。
- 404 不再打 `未处理异常` 错误日志。真正未分类的异常仍记 error。
- 行覆盖率门禁仍是 JaCoCo LINE ≥ 85%，由 `mvn test` 执行。本次 `mvn test`：83 个测试，0 失败；LINE 2393/2508（95.41%），`jacoco:check` 通过。
