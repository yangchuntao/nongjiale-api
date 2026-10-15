# 苏中种子 · 农家乐垂直平台后端空仓

Suzhong Seed — empty backend for a Chinese rural farmstay (农家乐) marketplace.

这是正式版从零开始的后端空仓：Java 17 / Spring Boot 3，只保留可运行骨架。业务域（预订、商户、支付等）尚未引入，MVP-0 后续再拆。

This is a from-scratch production backend skeleton on Java 17 and Spring Boot 3. It is runnable and nothing more. Business domains such as booking, merchant, and payment are not included; later MVP-0 work will split them out.

## 技术栈 / Stack

- Java 17（可用 JDK 17 及以上编译）
- Spring Boot 3.5.x
- Maven

## 本地运行 / Run locally

需要 JDK 17+ 和 Maven 3.6.3+。

JDK 17+ and Maven 3.6.3+ are required.

```bash
mvn test
mvn spring-boot:run
```

启动后访问 `GET http://localhost:8080/health`，返回：

```json
{"status":"UP","service":"suzhong-seed"}
```

## 包结构 / Layout

```
com.suzhong.seed       启动类 / application entry
com.suzhong.seed.web   HTTP 入口，当前仅健康检查 / health check only
```
