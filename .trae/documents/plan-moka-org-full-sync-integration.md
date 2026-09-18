# Moka 组织架构全量同步集成方案

## Context

以 Moka 开放平台「组织架构全量同步」接口（文档锚点 `#-59`）为例进行集成开发，并提供测试接口。

- **上游接口**：`PUT https://api.mokahr.com/api-platform/v2/departments`
- **鉴权**：HTTP Basic Auth，API Key 作为 username、password 为空（即 `Base64("apiKey:")`），比 IHR 的 OAuth2 简单——**无需 token 服务与 Redis 缓存**
- **同步语义**：以 `departmentCode` 为主键全量对比——系统没有→新增；两边都有→更新；系统有但本次未传→标记删除
- **成功响应**：`{"code": 0, "msg": "success", "data": {"result": {"new": 0, "update": 0, "delete": 0}}}`（文档返回字段表写 200 为成功，成功判定需兼容 0 与 200）
- **数据来源**：采用透传模式——测试接口接收调用方传入的部门列表，校验后转发 Moka（用户未另作选择，按推荐最小集成实施）
- **分层**：单聚合、无跨域编排、无 DB 写操作 → **Controller → Service → Client**，不引入 Manager，不需要 Flyway 脚本

完全复用 IHR 集成既有模式（Client 日志、Properties 构造器绑定、异常包装、Controller/Converter 风格）。

## 新增文件清单（业务域 `moka`）

### 1. common 模块

**`common/constant/moka/MokaConstants.java`**
- `LOG_BIZ = "Moka"`（日志业务标识）
- `DEPT_FULL_SYNC_PATH = "/api-platform/v2/departments"`
- `RESPONSE_CODE_SUCCESS_LEGACY = 200`（文档表格口径）与成功判定常量 `RESPONSE_CODE_SUCCESS = 0`（示例口径）
- final 类 + 私有构造器，禁止实例化

**`common/config/moka/MokaProperties.java`**
- `@Getter @ConfigurationProperties(prefix = "moka")`，构造器绑定扁平参数（参照 `IhrProperties`）
- `baseUrl`：默认 `https://api.mokahr.com`，自动去除结尾斜杠
- `apiKey`：经环境变量 `MOKA_API_KEY` 注入，Javadoc 标注禁止记入日志

### 2. integration 模块

**`integration/moka/client/dto/MokaDepartment.java`**
- 字段：`name` / `departmentCode` / `parentCode`（必填）、`type`（1 普通部门默认 / 2 门店）、`sequence`（0~10000 两位小数）、`localizedNames`（多语言，嵌套 `MokaLocalizedName`：`locale` / `propValue`）
- `@Data` + 完整中文 Javadoc

**`integration/moka/client/dto/MokaDeptSyncRequest.java`**
- `departments: List<MokaDepartment>`（必填）、`operatorEmail`（可选，操作人邮箱仅用于 Moka 侧日志）

**`integration/moka/client/dto/MokaDeptSyncResponse.java`**
- `code` / `msg` / `data.result{new, update, delete}`
- `isSuccess()`：`code == 0 || code == 200`
- 平铺便捷方法 `getNewCount()` / `getUpdateCount()` / `getDeleteCount()`（data 为 null 时返回 null）

**`integration/moka/client/MokaOrgClient.java`**（参照 `IhrOrgClient`）
- `@Slf4j @Component @RequiredArgsConstructor`，注入 `RestTemplate`（复用 `integration.config.RestTemplateConfig` 默认 bean，30s 超时）+ `MokaProperties`
- `syncDepartmentsFull(MokaDeptSyncRequest request)`：
  - URL：`properties.getBaseUrl() + MokaConstants.DEPT_FULL_SYNC_PATH`，`new URI()` 构造（非法直接抛 `RestClientException`）
  - Header：`Content-Type: application/json`；`Authorization: Basic Base64(apiKey + ":")`——**Authorization 头不参与日志输出**
  - 调用前后用 `ThirdPartyHttpLogSupport.logRequest / logResponse` 打印 INFO 完整入参出参
  - 响应体为空 → INFO 说明原因后抛 `RestClientException`；`RestClientException` 打 INFO 终止原因 + ERROR 堆栈后原样上抛
  - 成功时 INFO 打印 new/update/delete 数量

### 3. service 模块

**`service/moka/api/IMokaOrgService.java`** + **`service/moka/api/impl/MokaOrgServiceImpl.java`**
- 接口方法 `MokaDeptSyncResponse syncDepartmentsFull(MokaDeptSyncRequest request)`，完整中文 Javadoc
- 实现：`@Slf4j @Service @RequiredArgsConstructor`，调用 Client；`RestClientException` 包装为 `BizException(ResultCode.INTEGRATION_ERROR, "Moka 组织架构同步失败: ...")`（Moka 为 Basic Auth 固定凭证，无 IHR 式 token 刷新重试逻辑）

### 4. web 模块

**`dto/moka/request/MokaOrgSyncRequest.java`**（对外请求体）
- `departments: List<@Valid MokaDepartmentItem>`（`@NotNull`、`@NotEmpty`）、`operatorEmail`（可选，`@Email`）
- `MokaDepartmentItem`：`@NotBlank name` / `@NotBlank departmentCode` / `@NotBlank parentCode`、`type`、`sequence`、`localizedNames`

**`convert/moka/MokaOrgConverter.java`**（MapStruct）
- `@Mapper(componentModel = "spring")`：web request → integration `MokaDeptSyncRequest`（含嵌套列表元素映射）

**`dto/moka/vo/MokaDeptSyncResultVO.java`**
- `newCount` / `updateCount` / `deleteCount`（经 Converter 由响应 `result` 映射，或在 Controller 组装——优先走 MapStruct）

**`controller/moka/MokaOrgController.java`**（测试接口）
- `@Slf4j @Validated @RestController @RequestMapping("/api/v1/moka/departments")`
- `PUT /api/v1/moka/departments/full-sync`：`@Valid @RequestBody MokaOrgSyncRequest` → Converter 转 integration 请求 → `IMokaOrgService.syncDepartmentsFull` → `Result.success(MokaDeptSyncResultVO)`
- 上游返回 `isSuccess()==false` 时抛 `BizException(ResultCode.INTEGRATION_ERROR)` 携带 code/msg
- Controller 不 catch 业务异常，交由 `GlobalExceptionHandler`

**`config/moka/MokaConfiguration.java`**
- `@Configuration @EnableConfigurationProperties(MokaProperties.class)`

### 5. 配置文件（3 个 yml 各加 `moka` 节点）

`cacch-integration-web/src/main/resources/application.yml`、`application-test.yml`、`application-prod.yml`：

```yaml
# Moka 开放平台（组织架构全量同步等）；api-key 经环境变量 MOKA_API_KEY 注入，禁止记入日志
moka:
  base-url: ${MOKA_BASE_URL:https://api.mokahr.com}
  api-key: ${MOKA_API_KEY:}          # application-prod.yml 中改为 ${MOKA_API_KEY}（缺失即启动失败）
```

## 规范要点（实现时遵守）

- 所有类 `@author hongfu_zhou@cacch.com`，公开类与方法完整中文 Javadoc（第十六节）
- 构造器注入（`@RequiredArgsConstructor` + `private final`），禁止字段注入
- 日志前缀中文方括号【Moka】，逻辑提前终止/异常路径必须打 INFO，Client 出入参走 `ThirdPartyHttpLogSupport` 脱敏
- 禁止在业务日志打印 apiKey；`ResultCode` 沿用现有 50001/50002/50003，无需新增错误码
- 不改任何已执行 Flyway 脚本；本次无表结构变更

## 验证步骤

1. 编译：`mvn -q compile`（父工程聚合编译，确认 MapStruct 生成与依赖方向正确）
2. 启动 test profile：`MOKA_API_KEY=<测试key>` 下启动，确认 `MokaProperties` 绑定成功
3. 无真实 key 的降级验证：不配置 MOKA_API_KEY 调用测试接口 → 应返回 50001 集成错误（证明链路通畅、异常正确包装）
4. 有真实 key 时：`PUT /api/v1/moka/departments/full-sync` 传 1~2 个测试部门（一级部门 parentCode="0"）→ 期望 `{"code":0,...}` 返回 new/update/delete 计数；再传空列表验证删除标记语义（谨慎，仅测试租户）
