# 第三方系统接口对接规范

适用模块：`cacch-integration-integration`（第三方客户端封装）。覆盖勤策 CRM、企业微信、iHR、客如云等外部系统接口对接。

---

## 一、适用边界（强制）

- **新对接接口**、新增业务域 Client **一律遵守本规范**。
- **存量实现**（如尚未按本规范组织的 `CrmClient` 等）**保持现状、不强制改造**；后续迭代时逐步迁移，不在业务需求中顺带重构。
- 本规范与 `project_rules.md` 第八节（命名）、第十一节（日志）、第十六节（Javadoc）以及 `java-third-party-http-io-logging.md`、`java-info-logging-on-exit.md` 配合使用，冲突时以 `project_rules.md` 为准。

---

## 二、Client 结构与抽象基类

- **一个业务域/子域一个聚合 Client**：`{业务域}{子域}Client`（如 `CrmProductClient`），承载该域全部接口方法，**禁止按单接口建 Client**（如 `CrmQueryProductClient`）。
- 同一业务域的各 Client 共享抽象基类 `Abstract{业务域}Client`（如 `AbstractCrmClient`），基类统一承载：
  - 签名算法（timestamp、msgId、digest）
  - 请求体序列化（与签名使用**同一份** JSON 字符串）
  - URI 构造（路径占位符展开）
  - 统一响应解析（含 `response_data` 二次解析）
  - 空响应判断、异常兜底并抛 `RestClientException`
- 基类字段使用 `protected final` + `@RequiredArgsConstructor`；子类声明显式构造器调用 `super(...)` 并标注 `@Component`。
- 子类**只**声明接口方法与必要的数据转换，**禁止**复制签名、URI 拼接、响应解析、异常处理代码。

```java
// 正确 —— 基类承载公共逻辑
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCrmClient {
    protected static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    protected final RestTemplateHttpClient httpClient;
    protected final CrmProperties crmProperties;

    protected CrmOpenApiResponse post(String baseUrl, String pathTemplate, Object body,
                                      String biz, String action) { ... }

    private CrmOpenApiResponse parseResponse(String responseBody) { ... }
}

// 正确 —— 子类只声明接口
@Component
public class CrmProductClient extends AbstractCrmClient {
    public CrmProductClient(RestTemplateHttpClient httpClient, CrmProperties crmProperties) {
        super(httpClient, crmProperties);
    }

    public CrmProductQueryResponse queryProducts(CrmProductQueryRequest request) { ... }
}
```

```java
// 错误 —— Client 内重复实现签名/解析（禁止）
public class CrmXxxClient {
    public XxxResponse query(...) {
        String digest = md5(body + "|" + appKey + "|" + ts);   // 重复签名
        JsonNode data = objectMapper.readTree(raw);            // 重复解析
    }
}
```

---

## 三、统一响应基类（强制）

- 统一信封字段 `return_code` / `return_msg` / `msg_id` / `response_data` 与 `isSuccess()` **只在** `{业务域}BaseResponse<T>`（`integration.{biz}.client.dto` 包）声明一次。
- 接口响应 DTO 继承基类并指定泛型，**不再重复声明**信封字段：

```java
// 基类
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class CrmBaseResponse<T> {
    @JsonProperty("return_code")
    private String returnCode;
    @JsonProperty("return_msg")
    private String returnMsg;
    @JsonProperty("msg_id")
    private String msgId;
    @JsonProperty("response_data")
    private T responseData;

    public boolean isSuccess() {
        return CrmConstants.RETURN_CODE_SUCCESS.equals(returnCode);
    }
}

// 子类 —— 只声明泛型
public class CrmProductQueryResponse extends CrmBaseResponse<List<CrmProductInfo>> {
}
```

- 成功判断统一使用业务域常量（如 `CrmConstants.RETURN_CODE_SUCCESS`），**禁止**各接口自行写 `"0"` 字面量比较。
- 基类必须保留 `@JsonProperty` 三方原始命名，禁止依赖驼峰自动映射。

---

## 四、请求 DTO 规范

- 请求 DTO 使用 `@Data` + `@JsonInclude(JsonInclude.Include.NON_NULL)`：未赋值字段不进入请求体，也不会进入签名串。
- 三方字段用 `@JsonProperty` 显式映射；金额、系数、税率等数值统一用 `BigDecimal`。
- **入站别名（强制）**：`@JsonProperty` 只声明出站蛇形名，Spring 反序列化默认忽略未知属性，会导致本系统测试端点按驼峰传参时字段被**静默丢弃**。因此每个下划线字段必须同时声明驼峰别名 `@JsonAlias`，例如：
  ```java
  @JsonProperty("prd_code")
  @JsonAlias("prdCode")
  private String prdCode;
  ```
  确保 `{"prdCode":"xxx"}` 与 `{"prd_code":"xxx"}` 均可入站，出站仍为 `prd_code`。
- 枚举型字段使用 `String` + 常量类取值，**不绑定 Java 枚举**，避免三方新增枚举值导致反序列化失败。
- 请求字段全部可选时，Service 需对 `null` 请求做兜底，Client 不得假设非空。

```java
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmProductQueryRequest {
    @JsonProperty("page_number")
    private Integer pageNumber;
    @JsonProperty("classification")
    private String classification;
}
```

---

## 五、常量三分类（强制）

新增接口必须同步使用/新建以下三类常量，放在 `common.constant.{biz}`：

| 常量类 | 命名 | 承载内容 |
|--------|------|----------|
| Path 类 | `{业务域}{模块}PathConstants` | 该接口 URL 路径模板（含 `{openid}/{timestamp}/{digest}/{msgId}` 占位符） |
| 业务参数类 | `{业务域}{模块}Constants` | 枚举取值、默认分页等业务参数 |
| 日志域类 | `{业务域}{模块}LogConstants` | `LOG_BIZ` 常量与 `buildAction()` |

```java
public final class CrmProductPathConstants {
    private CrmProductPathConstants() {}
    public static final String QUERY_PRODUCT_PATH =
            "/api/product/v1/queryProduct/{openid}/{timestamp}/{digest}/{msgId}";
}

public final class CrmProductConstants {
    private CrmProductConstants() {}
    public static final int DEFAULT_PAGE_NUMBER = 1;
    public static final int DEFAULT_PAGE_SIZE = 1000;
}

public final class CrmProductLogConstants {
    private CrmProductLogConstants() {}
    public static final String LOG_BIZ = "CrmProduct";
    public static String buildAction(String action) { return action; }
}
```

- **禁止**在 Client / Service 中硬编码路径、日志前缀、默认分页值。

---

## 六、DTO 命名

| 类型 | 命名模式 | 示例 |
|------|----------|------|
| 请求 DTO | `{业务域}{接口语义}Request` | `CrmProductQueryRequest` |
| 响应 DTO | `{业务域}{接口语义}Response` extends `{业务域}BaseResponse<T>` | `CrmProductQueryResponse` |
| data 对象 DTO | `{业务域}{接口语义}Info` | `CrmProductInfo` |
| 嵌套结构 | `{业务域}{结构}Info` | `CrmProductUnitInfo` / `CrmProductExtInfo` |

- `response_data` 顶层为数组时直接用 `List<XxxInfo>` 作为泛型，**不额外包一层** `PageInfo`。
- `@JsonIgnoreProperties(ignoreUnknown = true)` 必须加在业务数据 DTO 上，保证三方新增字段不报错。

---

## 七、响应数据解析

- 基类 `post(...)` 返回原始信封，`response_data` 为 `JsonNode`；若为 JSON 字符串则在基类 `parseResponse` 中**二次解析**。
- 各 Client **仅在** `isSuccess()` 且数据非空时转换业务 DTO（`OBJECT_MAPPER.convertValue`）；失败响应不解析业务数据，避免异常掩盖真实错误码。
- 转换结果为空时返回空集合（`List.of()`），禁止返回 `null` 集合。

---

## 八、Service 层规范

- Service 只做：参数兜底 → 调用 Client → 错误码转换，**不处理** HTTP 细节、签名、序列化。
- 空请求新建默认对象，禁止 NPE；分页参数为空或 `< 1` 时取常量默认值。
- 非成功返回码抛 `BizException(ResultCode.INTEGRATION_ERROR, "xxx失败, return_code=..., return_msg=...")`。
- `RestClientException` 统一转 `BizException(ResultCode.INTEGRATION_ERROR, "xxx调用失败: ...", e)`。
- `catch (BizException e)` 先打 ERROR 再重抛；提前终止须打 INFO（见 `java-info-logging-on-exit.md`）。
- 业务层禁止打印完整请求体/响应体。

---

## 九、测试端点规范

- 类名 `Test{业务域}Controller`，路由 `/api/v1/test/{domain}/...`，与正式端点 **URL + 类名双隔离**，联调完成后可整体删除。
- 只做 `Result.success(...)` 转发，异常交由 `GlobalExceptionHandler` 处理，Controller 不 catch 业务异常。
- 请求体使用 `@RequestBody(required = false)`，支持空条件联调；**不打印**请求体，仅打印「收到 xxx 测试请求」。

---

## 十、日志规范

- 三方出入参日志统一由 `RestTemplateHttpClient.doPostOrPut(...)` 脱敏打印，Client / Service **禁止重复输出**完整报文。
- 日志前缀固定中文方括号 `【业务标识】`，`biz` 取自 `{业务域}{模块}LogConstants.LOG_BIZ`。
- 凭证（appKey、secret、token）禁止出现在任何日志中。

---

## 十一、配置规范

- 复用业务域已有 `{biz}.base-url` 与凭证配置（如 `open-id` / `app-key`），**不按单接口新增独立地址配置**。
- 凭证通过环境变量注入（如 `${CRM_APP_KEY}`），禁止硬编码；新增配置项须同步更新 `application.yml`、`application-test.yml`、`application-prod.yml` 及配置属性类。

---

## 十二、新增接口自查清单

- [ ] 常量三件套（Path / 业务参数 / 日志域）已建或已复用
- [ ] Client 继承 `Abstract{业务域}Client`，未复制签名、URI、解析、异常逻辑
- [ ] 响应 DTO 继承 `{业务域}BaseResponse<T>`，无重复信封字段
- [ ] 请求 DTO 使用 `@JsonInclude(NON_NULL)`，枚举字段用 `String`
- [ ] 业务数据 DTO 加 `@JsonIgnoreProperties(ignoreUnknown = true)`
- [ ] Service 完成空请求兜底、默认分页、`BizException` 转换与终止 INFO 日志
- [ ] 测试端点类名/路径符合 `Test{业务域}Controller` + `/api/v1/test/{domain}/...`
- [ ] 公开类与方法 Javadoc 完整，`@author hongfu_zhou@cacch.com`
- [ ] `mvn -DskipTests compile` 通过
