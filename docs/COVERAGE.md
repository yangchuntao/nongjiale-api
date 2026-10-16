# 行覆盖率门禁

合并和本地验证都走 `mvn test`。JaCoCo 在测试结束后检查整包行覆盖率，低于 85% 构建失败。`mvn verify` 会先执行 `test`，所以同样过不了这道门。

本次测量（`mvn test`，JaCoCo 0.8.12）：

- 指标：**LINE / COVEREDRATIO（行覆盖率）**
- 结果：**95.41%**（2391 / 2506 行）
- 门禁：BUNDLE 最低 **0.85**。低于 85% 时 `jacoco:check` 失败，`mvn test` 失败
- 测试：**74** 个（Failures 0，Errors 0）

JaCoCo 检查日志把比例四舍五入到两位小数（本次显示 `0.95`）。以 XML 报告里的 covered / (covered + missed) 为准。

## ① 概要设计

在现有 Maven 测试生命周期上加一道行覆盖率门禁，不改业务行为、接口、枚举或状态文案。

- 统计范围是 `target/classes` 里的全部业务与脚手架类，按行计算覆盖比例。
- 不使用指令覆盖率（INSTRUCTION）或分支覆盖率（BRANCH）作为门禁。指令覆盖率只作报告参考。
- 阈值放在 `jacoco.line.minimum`（0.85），规则挂在 `jacoco:check`。
- `prepare-agent` 在测试前挂上 agent，`surefire` 跑完后同一 `test` 阶段先出 HTML/XML 报告，再执行 check。
- 覆盖率不够时补 JUnit 5 + Mockito 单测，优先打未覆盖的服务、缓存和异常分支，不新增集成测试进程。

`CHECKED_IN` 的中文保持「已入住」。Redis 仍只做缓存。Bearer 仍在进程内存。

## ② 详细设计

插件：`org.jacoco:jacoco-maven-plugin:0.8.12`。

| 执行 | 阶段 | 作用 |
|------|------|------|
| `prepare-agent` | 默认 initialize | 设置 `argLine`，数据写到 `target/jacoco.exec` |
| `report` | test | 生成 HTML 与 XML |
| `check` | test | BUNDLE 的 LINE COVEREDRATIO ≥ `${jacoco.line.minimum}` |

`maven-surefire-plugin` 写在 JaCoCo 前面，避免 check 抢在测试之前跑。check 的 `haltOnFailure` 使用插件默认值 true。

规则：

```xml
<counter>LINE</counter>
<value>COVEREDRATIO</value>
<minimum>0.85</minimum>
```

没有排除包。Mapper 接口没有可执行行，不单独进分母。

报告路径：

- HTML：`target/site/jacoco/index.html`（浏览器直接打开）
- XML：`target/site/jacoco/jacoco.xml`（CI 或脚本读取根节点 `counter type="LINE"`）

查看：

```bash
mvn test
# 通过后打开 target/site/jacoco/index.html
```

把阈值临时调到实测值以上可以确认门禁会失败，例如 `-Djacoco.line.minimum=0.99`。仓库默认仍是 0.85。

补测集中在这些缺口，全部是单测：

- `AdminArticleService` / `AdminArticleController`：列表、发布打点、地区不存在、删除
- `AdminMerchantService`：空列表、启停与令牌作废、重置密码
- `MerchantAccess`：未登录、停用、主体不一致
- `CacheAside` / `LettuceRedisValueStore`：命中、解析失败、Redis 故障降级、失效
- `RegionLevel` / `RegionLevelTypeHandler`：层级码与空值
- `PhoneMask`、`GlobalExceptionHandler`、`Tag` / `StoreTagRel`
- `NjlApplication.main` 用 Mockito 静态替身确认委托给 `SpringApplication.run`，不真正启动服务

## ③ 改动点

- `pom.xml`：JaCoCo 版本、`jacoco.line.minimum`、surefire 顺序、report 与 check
- `README.md`：本地 `mvn test` 门禁和报告路径
- `docs/COVERAGE.md`：本说明
- `src/test/java/**`：上表中的 JUnit 5 / Mockito 单测

业务源码、枚举、接口契约、Flyway、安全过滤器和缓存策略都没有改。

## ④ 影响点

- 本地和合并前的 `mvn test` / `mvn verify`：行覆盖率低于 85% 会失败。当前 95.41%，高于门禁。
- 测试变慢可忽略。新增的是单测；原有 `@SpringBootTest` 数量不变。
- 运行时行为不变：登录、意向状态机、商家权限、Redis 降级、异常信封都保持原样。
- 报告目录在 `target/`，不入库。
- 不改变 MySQL / H2 profile、端口、CORS 或种子数据。
