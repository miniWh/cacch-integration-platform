---
description: 三方系统 Client 调用必须 INFO 打印每个接口的入参与出参
globs: **/integration/**/client/**/*.java
alwaysApply: false
---

# 三方接口入参 / 出参日志规范

编写或修改 `integration` 模块下的三方 **Client** 时：每次调用外部 HTTP/SDK 接口，必须在调用前后用 **INFO** 打印该接口的**完整入参与出参**（敏感字段脱敏）。

## 强制要求

| 项 | 要求 |
|----|------|
| 位置 | 仅在 `integration.{biz}.client`（或等价 Client 封装）内打印 |
| 工具 | 统一使用 `ThirdPartyHttpLogSupport.logRequest` / `logResponse` |
| 级别 | `INFO` |
| 入参 | 调用前打印：脱敏后的 `url` + 实际发出的请求体/查询参数 |
| 出参 | 收到响应后打印：实际收到的响应体（二进制可只打 `byteLength`） |
| 脱敏 | Token、secret、password、appKey 等经 `ThirdPartyHttpLogSupport` 脱敏；禁止明文 |

## 正确示例

```java
private <T> T post(String url, Object request, Class<T> responseType, String action) {
    ThirdPartyHttpLogSupport.logRequest(BIZ, action, url, request);
    try {
        T response = restTemplate.postForObject(url, request, responseType);
        ThirdPartyHttpLogSupport.logResponse(BIZ, action, response);
        // ...
        return response;
    } catch (RestClientException e) {
        log.info("【{}】{}终止, reason={}", BIZ, action, e.getMessage());
        log.error("【{}】{} HTTP 调用失败", BIZ, action, e);
        throw e;
    }
}
```

日志形态：

```
【Crm】查询订单入参, url=https://..., 入参={...}
【Crm】查询订单出参, 出参={...}
```

## 错误示例（禁止）

```java
// 静默调用，无入参/出参
T response = restTemplate.postForObject(url, request, responseType);

// 仅打业务摘要，不打三方报文
log.info("【Crm】查询订单成功");

// 在 Controller/Service 再打一遍完整三方报文（职责错误；业务层仍禁止完整请求体）
```

## 边界说明

- **必须打**：每个对外接口一次调用对应一对入参/出参日志
- **业务层**：Controller / Service / Manager **仍禁止**打印完整业务请求体；本规则仅约束三方 Client
- **大文件/二进制**：出参可记录 `statusCode` + `byteLength`，勿把整文件写入日志
- 细则与工具类见 `ThirdPartyHttpLogSupport`；总则见 `.trae/rules/project_rules.md` 第十一节
