# 项目长期记忆（nongjiale-api）

## 版本控制与远端

- 远端 `origin` = `https://github.com/yangchuntao/nongjiale-api.git`，主分支 `main`，走 HTTPS + Git Credential Manager 认证，无需配置 SSH 密钥。
- 网络注意：`github.com:443` 经本机代理（127.0.0.1 动态端口）访问可能偶发 `CONNECT tunnel failed, response 502`；此时 `api.github.com`、`codeload.github.com` 仍正常，属代理侧对 github.com 隧道的临时故障，等代理恢复后直接 `git push` 即可，不必改 SSH。

## 提交约定

- 提交信息用中文，采用 `feat:` / `docs:` / `fix:` 前缀；代码与 `.workbuddy/memory` 变更可同一提交。
- 提交前至少跑 `mvn -DskipTests compile`，改动涉及行为时跑 `mvn test`（含 JaCoCo 行覆盖率门禁）。

## 接口文档

- 已接入 springdoc-openapi 2.6.0：`/v3/api-docs`、`/swagger-ui.html`，由 `springdoc.*.enabled` 控制，默认 `${OPENAPI_ENABLED:false}`，仅 `local` profile 打开。
- 泛型 record `ApiResponse<T>` / `PageResult<T>` 不得加 `@Schema(name=...)`，否则 schema 复用会让 `data` 指向错误类型。
