# CRM 员工帐号查询接口文档

> 版本：v1.0  
> 适用模块：`cacch-integration-platform` CRM（勤策）业务域  
> 服务前缀：`/api/v1/crm/employees`

---

## 〇、环境信息

| 环境 | 地址 | 说明 |
|------|------|------|
| 测试环境 | `http://10.80.68.10:8081` | 联调测试使用 |
| 生产环境 | `http://10.80.68.11:8081` | 正式运行环境 |

完整接口 URL = `{环境地址}` + `{服务前缀}` + `{接口路径}`，例如：

```
POST http://10.80.68.10:8081/api/v1/crm/employees/query-by-id
```

---

## 〇·二、鉴权说明

本服务对外接口**不做独立鉴权**，所有请求的身份认证由**前置网关统一处理**。

- 本服务内部无 API Key 校验拦截器（与客如云订单接口一致）；
- 调用方在请求到达本服务前，须通过前置网关的认证；
- 认证通过后，网关将请求透传至本服务，本服务代理调用勤策 OpenAPI 并返回结构化结果；
- 若调用方未通过网关认证，请求将在网关层被拒绝，无法到达本服务。

**对接方需关注**：

1. 调用时需携带网关要求的认证凭据（具体认证方式与 Header 名称以网关约定为准，常见方式如 `Authorization: Bearer <token>` 或 `X-API-Key: <api-key>`）；
2. 测试环境与生产环境的认证凭据相互独立，不可混用；
3. 认证凭据由系统管理员统一申请与分配，请勿在代码或文档中硬编码。
4. **禁止直连勤策**：勤策 `openId` / `appKey` 仅配置在集成中台，其他项目只调本接口。

---

## 一、统一返回体

所有接口统一返回 `Result<T>` 包装结构：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 业务码，`0` 表示成功，非 `0` 表示失败 |
| `message` | string | 业务消息 |
| `data` | object / null | 员工信息；勤策成功但查无员工时为 `null` |

**调用方判定规则**：`code == 0` 才认为中台处理成功；再判断 `data` 是否为 `null`（`null` 表示无匹配员工）。

---

## 二、员工对象字段表（`data`）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 勤策员工唯一标识 |
| `empId` | string | 第三方员工 ID（勤策 `emp_id`），可空 |
| `empCode` | string | 员工登录帐号（勤策 `emp_code`），对接 OA 等常用 |
| `empName` | string | 员工姓名 |
| `mobile` | string | 手机号，勤策未返回时为 `null` |
| `status` | string | 在职状态，勤策未返回时为 `null` |

---

## 三、接口列表

### 3.1 按勤策员工 ID 查询（推荐）

| 项 | 内容 |
|---|---|
| URL | `POST /api/v1/crm/employees/query-by-id` |
| Content-Type | `application/json` |
| 功能 | 根据勤策员工 `id` 查询帐号信息（如订单上的 `owner.id`） |

#### 请求体

```json
{
  "employeeId": "8858965636174056137"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `employeeId` | string | 是 | 勤策员工唯一标识 |

#### 响应示例（有数据）

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "id": "8858965636174056137",
    "empId": null,
    "empCode": "zhangsan",
    "empName": "张三",
    "mobile": null,
    "status": null
  }
}
```

#### 响应示例（无员工）

```json
{
  "code": 0,
  "message": "操作成功",
  "data": null
}
```

---

### 3.2 组合条件查询

| 项 | 内容 |
|---|---|
| URL | `POST /api/v1/crm/employees/query` |
| Content-Type | `application/json` |
| 功能 | 按 `id` / `empId` / `empCode` 之一或组合查询；与勤策一致时优先 `id` |

#### 请求体

```json
{
  "id": null,
  "empId": null,
  "empCode": "zhangsan"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string | 否* | 勤策员工 ID |
| `empId` | string | 否* | 第三方员工 ID |
| `empCode` | string | 否* | 员工登录帐号 |

\* `id`、`empId`、`empCode` **至少一项非空**。

#### 响应

结构同 3.1；多条命中时取勤策返回的**首条**员工。

---

## 四、错误码

| code | message（示例） | 触发场景 |
|------|-----------------|----------|
| `0` | 操作成功 | 成功（含 `data=null`） |
| `40002` | 参数缺失 | `employeeId` 为空，或组合查询三项皆空 |
| `50001` | 集成错误 | 勤策 `return_code != 0`、HTTP 失败、超时等 |
| `99901` | 系统错误 | 未预期异常 |

勤策失败时 `message` 中会包含简要原因，**不会**返回勤策密钥或完整三方报文。

---

## 五、可选请求头

| Header | 必填 | 说明 |
|--------|------|------|
| `X-Trace-Id` | 否 | 调用方传入便于与中台日志关联 |
| 网关鉴权 Header | 是（经网关） | 以网关约定为准 |

---

## 六、调用示例

> 以下以**测试环境** `http://10.80.68.10:8081` 为例，生产请替换为 `http://10.80.68.11:8081`。  
> 鉴权 Header 以网关约定为准，示例中用 `{网关鉴权Header}` 占位。

### 6.1 按 ID 查询（cURL）

```bash
curl -X POST "http://10.80.68.10:8081/api/v1/crm/employees/query-by-id" \
  -H "Content-Type: application/json" \
  -H "{网关鉴权Header}: {网关鉴权凭据}" \
  -H "X-Trace-Id: my-project-trace-001" \
  -d '{"employeeId":"8858965636174056137"}'
```

### 6.2 按 empCode 查询（cURL）

```bash
curl -X POST "http://10.80.68.10:8081/api/v1/crm/employees/query" \
  -H "Content-Type: application/json" \
  -H "{网关鉴权Header}: {网关鉴权凭据}" \
  -d '{"empCode":"zhangsan"}'
```

---

## 七、对接注意事项

1. **主键来源**：CRM 订单等业务数据中的 `owner.id`、`creator_id.id` 即 `employeeId`。
2. **下游帐号**：对接致远 OA 等系统时，通常使用返回的 `empCode`。
3. **只读接口**：可重复调用；注意网关与勤策侧限流。
4. **查无数据**：`code=0` 且 `data=null`，勿与系统错误混淆。
5. **研发联调**：中台内部仍可使用 `POST /api/v1/crm/open-api/employees/*` 查看勤策原始信封，**不对业务方作为主契约**。
6. **环境地址**：测试 `http://10.80.68.10:8081`，生产 `http://10.80.68.11:8081`。

---

## 八、对内联调接口（附录）

| URL | 说明 |
|-----|------|
| `POST /api/v1/crm/open-api/employees/query-by-id` | 返回勤策原始 `CrmOpenApiResponse` |
| `POST /api/v1/crm/open-api/employees/query` | 透传勤策员工查询 Body |

仅供集成中台研发排障，其他项目请使用本文第三节产品化接口。