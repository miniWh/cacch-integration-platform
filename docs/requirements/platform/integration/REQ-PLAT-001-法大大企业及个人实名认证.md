# REQ-PLAT-001 法大大企业及个人实名认证

---

## 1. 基本信息

| 项目 | 内容 |
|------|------|
| 需求编号 | REQ-PLAT-001 |
| 需求标题 | 集成法大大实名认证能力，为外部系统提供统一的企业及个人实名认证接口 |
| 需求类型 | integration |
| 所属系统 | platform |
| 优先级 | P1(高) |
| 状态 | draft |
| 提出人 | — |
| 提出日期 | 2026-08-03 |
| 负责人 | — |
| 预计上线 | 待定 |
| **确认方案** | **集成平台统一管理认证记录 + 对外查询/认证接口 + 法大大回调** |

---

## 2. 需求背景

### 2.1 业务背景

公司多个业务系统（致远 OA、勤策 CRM 等）在日常运营中涉及外部企业或外部企业联系人的实名认证需求。例如：

- OA 供应商准入时需验证供应商企业的工商注册信息（企业实名认证）
- 外部企业联系人登录系统时需要验证个人身份（个人实名认证）
- CRM 客户入驻时需对客户企业及对接人进行实名核验，且须明确所属内部法人主体（如南通泰禾化工股份有限公司、上海泰禾国际贸易有限公司等）

### 2.2 痛点

```
当前方案                                  存在问题
─────────────────────────────────────    ───────────────────────────────
各业务系统独立对接法大大、独立存储          ① 认证记录分散，无法跨系统复用
认证结果                                    ② 同一企业/个人可能被重复认证
                                          ③ 各系统认证口径不一致
                                          ④ 法大大接口变更需多系统逐一改造
                                          ⑤ 无法全局监控认证成功率与异常
```

### 2.3 目标

1. 在集成平台统一管理外部企业及个人的实名认证记录，作为全公司唯一的认证数据源
2. 对外提供标准化的查询与认证接口，各业务系统通过调用接口即可获取或发起实名认证
3. 集成法大大实名认证 API，支持**企业实名认证**与**个人实名认证**两种场景
4. 接收法大大认证回调，实时更新认证状态，无需业务系统关注异步回调逻辑
5. 后续新系统接入时，只需调用集成平台接口，无需重复对接法大大

> **核心原则**：集成平台是外部实名认证的唯一入口，各业务系统不再直接对接法大大。

---

## 3. 确认方案概述

### 3.1 整体架构

```
┌──────────┐    查询/认证请求     ┌────────────────┐    获取认证URL   ┌──────────┐
│  OA 系统  │ ──────────────────→ │                │ ──────────────→ │  法大大   │
└──────────┘                     │                │                 │  平台    │
                                 │   集成平台      │ ←────────────── │ (fadada) │
┌──────────┐    查询/认证请求     │ (认证中台)      │   认证结果回调   └──────────┘
│ CRM 系统  │ ──────────────────→ │                │
└──────────┘                     │  统一存储       │
                                 │  认证记录       │
┌──────────┐    查询/认证请求     │                │
│ 未来系统  │ ──────────────────→ │                │
└──────────┘                     └────────────────┘
```

| 角色 | 职责 |
|------|------|
| **业务系统（OA / CRM / …）** | 调用集成平台接口查询或发起认证；将认证 URL 透传给终端用户/企业；展示认证结果 |
| **集成平台** | 统一管理认证记录、调用法大大 API 获取认证 URL、接收回调、对外暴露查询/认证接口 |
| **法大大平台** | 提供认证页面，用户/企业在页面完成实名认证，结果通过回调通知 |
| **PG 数据库** | 存储认证记录（企业认证表 + 个人认证表），作为认证状态唯一权威来源 |

### 3.2 核心原则

| 原则 | 说明 |
|------|------|
| 集成平台为唯一入口 | 所有外部系统的实名认证需求统一走集成平台，禁止业务系统直连法大大 |
| 认证记录复用 | 按 **内部企业全称 + uscc**（企业）或 **内部企业全称 + idNumber + mobile**（个人）判定：同一组合仅一条 `SUCCESS`；`FAILED` 可有多条；个人换手机号视为未认证 |
| 内部企业归属 | 每条认证记录归属唯一内部法人企业；同一外部企业/个人对不同内部企业须**分别认证**（如张三对 A 公司、B 公司各认证一次） |
| 逐次留痕 | 每次发起认证新增一条记录；`sourceSystem` 写入该条记录，**仅作来源审计，无查询/去重/状态判定作用** |
| 异步回调解耦 | 认证为异步流程：发起认证后立即返回 `PENDING` 及认证 URL，法大大回调到达后更新为 `SUCCESS` / `FAILED` |
| 业务唯一键 | 企业：**internalCompanyName + uscc**；个人：**internalCompanyName + idNumber + mobile**（结合 `authType` 区分表）；与 `sourceSystem` 无关 |
| 来源系统记录 | `sourceSystem` 取值 **`CRM`** / **`OA`**；**仅在 STEP 2 新发起认证时**必填并写入记录，供审计溯源 |
| 幂等查询 | 业务系统调用查询接口时**必须传入 internalCompanyName**；若该组合已有 `SUCCESS` 或进行中 `PENDING`，不重复发起认证 |
| 回调匿名接收 | 回调接口为匿名接口，不需要验签，仅限法大大平台访问（通过网络层 ACL 控制） |

### 3.3 内部企业（法人主体）

认证记录按**内部法人企业**隔离。外部系统调用时必须传入 **`internalCompanyName`（内部企业全称）**。

| 内部企业全称（示例） | 说明 |
|---------------------|------|
| 南通泰禾化工股份有限公司 | 泰禾集团下属法人主体之一 |
| 上海泰禾国际贸易有限公司 | 泰禾集团下属法人主体之一 |
| … | 其他法人主体，见配置 `fdd.internal-companies` |

> **规则**：单条认证记录仅对应一家内部企业。同一外部供应商（uscc）或个人（idNumber+mobile）为不同内部企业办理业务时，须按「内部企业 + uscc」或「内部企业 + idNumber + mobile」组合**分别查询与认证**。同一身份证号更换手机号后，在同一内部企业下视为**未认证**，须重新实名。
>
> **说明（法大大内外企）**：法大大侧 `companyType`（内部企业/外部企业）与泰禾 `internalCompanyName` 无关；同一自然人可在法大大同时归属内部企业与外部企业，中台不以此限制个人认证业务键。

### 3.4 认证类型

| 认证类型 | 认证对象 | 法大大对应能力 | 典型场景 |
|----------|----------|---------------|----------|
| 企业实名认证 | 外部企业 | 调用 `/user/api/verify/company/url` 获取企业实名认证页面 URL，企业在页面完成对公打款/纸质审核/法定代表人授权等认证 | 供应商准入、客户入驻 |
| 个人实名认证 | 外部企业联系人 | 调用 `/user/api/verify/person/url` 获取个人实名认证页面 URL，个人在页面完成三要素/人脸等认证 | 联系人身份核验 |

---

## 4. 业务流程

### 4.1 核心流程（对齐法大大认证/签署流程图）

```
开始
  │
  ├─ 查询个人实名认证信息（本地 + getAccount）
  │     ├─ 已认证 → 进入企业认证查询
  │     └─ 未认证 → 创建用户(createAccount) → 个人实名认证 URL → 异步回调
  │
  ├─ 查询企业实名认证信息（本地 + getCompany）
  │     ├─ 已认证 → 可进入合同签署（本期范围外）
  │     └─ 未认证 → 校验管理员已个人实名
  │                  → 创建企业并绑定管理员(createCompany)
  │                  → 企业实名认证 URL → 异步回调
  │
结束（签署相关：文件上传 / 创建推送签署 / 回调，本期不做）
```

> **顺序约束**：发起企业认证前，企业管理员必须在同一 `internalCompanyName` 下已有个人 `SUCCESS` 记录（业务键含管理员 `idNumber + mobile`）；请求体需同时传管理员 `personName` / `idNumber` / `mobile`。

### 4.2 查询 → 判断 → 获取 URL → 页面认证 → 回调（原流程细化）

```
业务系统                      集成平台                         法大大
   │                            │                               │
   │  ① 查询/认证请求             │                               │
   │  (内部企业全称 +             │                               │
   │   企业名称+uscc 或           │                               │
   │   姓名+身份证号)              │                               │
   │ ──────────────────────────→ │                               │
   │                            │                               │
   │                      ② 查本地 DB                          │
   │                       ┌───┴───┐                            │
   │                       │有记录？│                            │
   │                       └───┬───┘                            │
   │                     YES    │    NO                         │
   │                       │    │                               │
   │                ③ 返回已有    │  ④ 调法大大获取认证 URL ─────→│
   │                   认证结果    │                               │
   │   ← ─ ─ ─ ─ ─ ─ ─ ─ ─ ─    │                               │
   │                            │  ⑤ 入库 PENDING                │
   │                            │  ⑥ 返回 needAuth=true         │
   │   ← ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─│     + authUrl + status=PENDING│
   │                            │                               │
   │  ⑦ 引导用户访问 authUrl      │                               │
   │      （企业/个人完成认证）    │                               │
   │                            │                               │
   │                            │  ⑧ 认证结果回调 ←────────────── │
   │                            │     (异步，3 分钟内)             │
   │                            │                               │
   │                            │  ⑨ 更新认证状态                 │
   │                            │     SUCCESS / FAILED           │
   │                            │                               │
   │  ⑩ 业务系统再次查询          │                               │
   │  (内部企业+uscc/身份证号)      │                               │
   │ ──────────────────────────→ │                               │
   │   ← ─ ─ 返回 SUCCESS ─ ─ ─  │                               │
```

### 4.2 查询接口详细判断逻辑

业务系统调用查询接口时的处理逻辑：

```
输入：
  查询判定键：authType（ENTERPRISE/PERSON）+ internalCompanyName（内部企业全称，必填）
            + uscc（企业）或 idNumber+mobile（个人）
  审计字段（仅 STEP 2 发起新认证）：sourceSystem（CRM/OA）、sourceBizNo（可选）

STEP 1：按 internalCompanyName + uscc / idNumber+mobile 查本地认证记录表（可有多条 FAILED）
  ├─ 存在 SUCCESS 记录（该组合唯一）→ 返回 { certified: true, 认证信息, certifiedAt }
  ├─ 不存在 SUCCESS，但存在 PENDING 记录 → 取最新一条 PENDING
  │     返回 { certified: false, status: "PENDING", authUrl, message: "认证处理中" }
  ├─ 不存在 SUCCESS/PENDING，仅有 FAILED 记录 → 取最新一条 FAILED
  │     返回 { certified: false, status: "FAILED", failReason, canRetry: true }；若 autoAuth=true 则进入 STEP 2
  └─ 无任何记录 → 若 autoAuth=true 则 STEP 2，否则返回需认证提示

STEP 2：自动发起认证（autoAuth=true 且该 internalCompanyName+uscc 或 internalCompanyName+idNumber+mobile 组合无 SUCCESS、无进行中 PENDING）
  ├─ 校验 internalCompanyName 非空且在配置允许列表内（见 §9.2 `fdd.internal-companies`）
  ├─ 校验 sourceSystem ∈ { CRM, OA }（本步骤必填）
  ├─ 调用法大大企业/个人认证 URL 接口
  ├─ 获取法大大返回的认证页面 URL
  ├─ **新增**一条认证记录（状态 PENDING；写入 internalCompanyName、sourceSystem、sourceBizNo）
  └─ 返回 { certified: false, needAuth: true, status: "PENDING", authUrl }
```

> **说明**：查询与状态判定依赖 **internalCompanyName + uscc** 或 **internalCompanyName + idNumber + mobile**。同一 idNumber 换 mobile 视为新主体。同一 idNumber 对不同内部企业视为不同认证主体，须分别认证。`sourceSystem` 仅审计。同一组合仅一条 `SUCCESS`，`FAILED` 可有多条。

### 4.3 回调处理流程

```
法大大 → POST /api/v1/fdd/callback（匿名接口，无需验签）
  ├─ 解析回调数据
  │   ├─ transactionNo / companyId / accountId → 匹配本地认证记录
  │   ├─ authStatus → SUCCESS / FAILED
  │   └─ authDetail → 认证结果详情（JSONB 存原始报文）
  ├─ 更新认证记录
  │   ├─ auth_status = 回调结果
  │   ├─ auth_detail = 原始回调报文
  │   ├─ certified_at = NOW()（如 SUCCESS）
  │   └─ fail_reason = 错误描述（如 FAILED）
  └─ 返回 200
```

> **安全说明**：回调接口不验签，通过网络层 ACL 限制仅法大大服务器 IP 可访问，防止外部恶意请求。

### 4.4 状态流转

```
（无 SUCCESS 记录）
    │
    ↓ 每次发起认证新增一条记录（sourceSystem 写入该条，不参与判定）
 PENDING ──────────→ SUCCESS（法大大回调认证通过；同一 internalCompanyName+uscc/idNumber 仅一条 SUCCESS）
    │
    └──────────────→ FAILED（法大大回调认证失败；可有多条 FAILED 历史记录）

状态说明：
  PENDING   — 已发起认证，等待法大大回调
  SUCCESS   — 实名认证通过（internalCompanyName+uscc/idNumber 组合下唯一）
  FAILED    — 实名认证不通过（可多次重试，每次新增记录）
```

> **说明**：认证结果暂不设置有效期，SUCCESS 状态长期有效。如后续业务需要过期重认证，再行扩展。

---

## 5. 功能点

| 序号 | 功能点 | 描述 | 必选/可选 |
|------|--------|------|-----------|
| F1 | 统一查询接口 | 对外提供企业/个人实名认证状态查询，按 internalCompanyName+uscc 或 internalCompanyName+idNumber+mobile 返回结果 | 必选 |
| F2 | 自动发起认证 | 该组合无 SUCCESS 且无进行中 PENDING 时，autoAuth=true 自动调用法大大并新增记录 | 必选 |
| F3 | 企业实名认证 URL | 调法大大 `/user/api/verify/company/url` 获取企业认证页面 URL | 必选 |
| F4 | 个人实名认证 URL | 调法大大 `/user/api/verify/person/url` 获取个人认证页面 URL | 必选 |
| F5 | 认证回调接收 | 接收法大大异步认证结果回调（匿名接口），更新认证状态 | 必选 |
| F6 | 认证记录复用 | 同一 internalCompanyName+uscc 或 internalCompanyName+idNumber+mobile 仅一条 SUCCESS；个人换号视为未认证 | 必选 |
| F7 | 原始报文留存 | 法大大请求与回调原始报文 JSONB 完整保存，便于问题排查 | 必选 |
| F8 | 认证记录查询 API | 按认证类型、内部企业、状态、来源系统、时间范围分页查询（管理后台用） | 可选 |
| F9 | 认证失败告警 | 认证 FAILED 或回调超时（3 分钟）触发企微 Webhook 告警 | 可选 |
| F10 | API Key 鉴权 | 与法大大功能同期实现：对外接口校验 API Key，回调接口除外 | 必选 |
| F11 | 逐次认证留痕 | 每次发起认证新增一条记录；FAILED 可多条；SUCCESS 按企业 uscc 或个人 idNumber+mobile 与内部企业组合唯一 | 必选 |
| F12 | 内部企业归属 | 每条记录绑定内部企业全称；同一人/企业对不同内部企业须分别认证 | 必选 |

**明确不做**：

| 项 | 说明 |
|----|------|
| 法大大电子签章 | 本需求仅包含实名认证能力，不含合同签署/签章 |
| 法大大存证/公证 | 不在本期范围 |
| 业务侧通知 | 集成平台不主动推送给业务系统；业务系统通过查询接口获取最新状态 |
| 认证有效期 | 暂不设置认证结果有效期，SUCCESS 长期有效 |
| 个人四要素/人脸认证 | 个人认证仅使用三要素（姓名+身份证号+手机号），法大大页面完成剩余验证 |
| API 频率限制 | 法大大侧暂未限制调用频率，集成平台暂不做限流 |
| 身份证号/手机号加密 | 本期明文存储与接口传输，不做 AES/SM4 加密 |

---

## 6. 涉及系统与模块

| 系统 | 模块/接口 | 说明 |
|------|-----------|------|
| 法大大平台 | `/user/api/verify/company/url` | 企业实名认证 URL 获取 |
| 法大大平台 | `/user/api/verify/person/url` | 个人实名认证 URL 获取 |
| 法大大平台 | `/base/login/oauth2/accessToken` | OAuth2 Token 获取（加密模式） |
| 法大大平台 | 认证结果回调 | 异步通知认证结果 |
| 致远 OA | 供应商/客户管理模块 | 调用集成平台查询/认证接口 |
| 勤策 CRM | 客户管理模块 | 调用集成平台查询/认证接口 |
| 集成中台 integration | `FddClient`（HTTP 客户端） | 调用法大大 API |
| 集成中台 service | `FddEnterpriseAuthService`、`FddPersonAuthService` | 企业/个人认证业务逻辑 |
| 集成中台 manager | `FddAuthManager` | 编排查询、认证、回调更新 |
| 集成中台 dao（PG） | 认证记录 Mapper | 企业/个人认证记录 CRUD |
| 集成中台 web | `FddAuthController`、`FddCallbackController` | 对外查询/认证接口、法大大回调接收 |
| 集成中台 web | `ApiKeyAuthFilter`（或等价拦截器） | API Key 鉴权（与法大大同期实现） |
| 集成中台 common | `FddProperties`、`FddConstants`、`FddSourceSystem` | 配置绑定、常量、来源系统枚举；`internal-companies` 内部企业白名单 |
| 集成中台 dao | `ApiKeyDO`、`ApiKeyMapper` | API Key 持久化（复用 `t_integration_api_key`） |
| 集成中台 service | `IApiKeyService` / `ApiKeyServiceImpl` | API Key 校验逻辑 |

---

## 7. 接口草案

### 7.1 对外接口（业务系统调用）

#### 7.1.1 统一查询/认证接口

```
POST /api/v1/fdd/auth/query
```

**请求参数**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| authType | String | 是 | 认证类型：ENTERPRISE（企业）/ PERSON（个人） |
| internalCompanyName | String | 是 | **内部企业全称**（如「南通泰禾化工股份有限公司」），查询判定键之一；须在配置 `fdd.internal-companies` 允许列表内 |
| enterpriseName | String | 条件 | **外部**企业名称（authType=ENTERPRISE 时必填） |
| uscc | String | 条件 | 统一社会信用代码（authType=ENTERPRISE 时必填，与 internalCompanyName 组成业务键） |
| personName | String | 条件 | 姓名（authType=PERSON 时必填） |
| idNumber | String | 条件 | 身份证号（authType=PERSON 时必填，与 internalCompanyName、mobile 组成业务键） |
| mobile | String | 条件 | 手机号（authType=PERSON 时必填，业务判定键之一；换号视为未认证） |
| autoAuth | Boolean | 否 | 是否自动发起认证，默认 true |
| sourceSystem | String | 条件 | 发起来源，**仅允许** `CRM` 或 `OA`；STEP 2 新发起认证时必填；审计字段 |
| sourceBizNo | String | 否 | 来源系统业务单号（审计溯源） |

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| certified | Boolean | 是否已认证通过 |
| needAuth | Boolean | 是否已发起认证（true 表示本次发起或已有 PENDING 记录） |
| status | String | 认证状态：PENDING / SUCCESS / FAILED |
| authType | String | 认证类型：ENTERPRISE / PERSON |
| internalCompanyName | String | 内部企业全称 |
| authUrl | String | 法大大认证页面 URL（status=PENDING 且本次发起或首次查询时返回） |
| enterpriseName | String | 外部企业名称（authType=ENTERPRISE 时返回） |
| uscc | String | 统一社会信用代码（authType=ENTERPRISE 时返回，业务追踪用） |
| personName | String | 姓名（authType=PERSON 时返回） |
| idNumber | String | 身份证号（authType=PERSON 时返回，明文） |
| mobile | String | 手机号（authType=PERSON 时返回，明文） |
| sourceSystem | String | 当前命中记录的发起来源（审计回显，CRM / OA）；**非查询条件** |
| failReason | String | 失败原因（status=FAILED 时返回） |
| certifiedAt | String | 认证通过时间（ISO 8601，status=SUCCESS 时返回） |
| message | String | 提示信息 |

> **说明**：查询与追踪使用 **internalCompanyName + uscc（企业）或 internalCompanyName + idNumber + mobile（个人）**。同一外部主体对不同内部企业须分别调用；个人换手机号须重新认证。

#### 7.1.2 认证状态查询接口

```
GET /api/v1/fdd/auth/status?authType=ENTERPRISE&internalCompanyName={name}&uscc={uscc}
GET /api/v1/fdd/auth/status?authType=PERSON&internalCompanyName={name}&idNumber={idNumber}&mobile={mobile}
```

按 **internalCompanyName + uscc / idNumber+mobile** 查询当前有效认证状态（优先 SUCCESS，其次最新 PENDING，再次最新 FAILED）。返回结构与 7.1.1 响应一致。

### 7.2 法大大回调接口（集成平台接收）

```
POST /api/v1/fdd/callback
```

> 该接口由法大大平台调用，为**匿名接口，不验签**。通过网络层 ACL 限制仅法大大服务器 IP 可访问。

**回调处理要点**：

| 项 | 说明 |
|----|------|
| 访问控制 | 匿名接口，不验签；网络层 ACL 限制来源 IP 为法大大服务器 |
| 幂等 | 同一 transactionNo 的多次回调以首次成功处理的结果为准，后续为幂等跳过 |
| 原始报文 | 回调完整 JSON 存入 `auth_detail` 字段（JSONB） |
| 日志 | INFO 级别打印回调入参（脱敏姓名/身份证号） |
| 超时 | 认证发起后 3 分钟内未收到回调，标记为超时并告警 |

### 7.3 管理接口（可选）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/fdd/auth/records` | 分页查询认证记录（按类型/内部企业/状态/来源系统/时间） |
| POST | `/api/v1/fdd/auth/retry` | 手动重试失败的认证（传入 authType + internalCompanyName + uscc/idNumber） |

---

## 8. 数据库设计

### 8.1 企业实名认证记录表

```
t_integration_fdd_enterprise_auth（法大大企业实名认证记录）

├─ id                      BIGINT PK              -- 雪花主键
├─ internal_company_name   VARCHAR(256) NOT NULL  -- 内部企业全称（业务判定键之一）
├─ transaction_no          VARCHAR(64)            -- 法大大侧认证流水号（回调匹配用）
├─ enterprise_name         VARCHAR(256) NOT NULL  -- 外部企业名称
├─ uscc                    VARCHAR(32) NOT NULL   -- 统一社会信用代码（业务判定键之一）
├─ auth_url                VARCHAR(1024)          -- 法大大认证页面 URL
├─ auth_status             VARCHAR(16) NOT NULL   -- PENDING / SUCCESS / FAILED
├─ request_detail          JSONB                  -- 发起认证时法大大请求/响应原始报文
├─ auth_detail             JSONB                  -- 法大大回调原始报文
├─ fail_reason             VARCHAR(512)           -- 失败原因
├─ source_system           VARCHAR(16) NOT NULL   -- 审计：本次发起来源（CRM/OA），不参与业务判定
├─ source_biz_no           VARCHAR(128)           -- 审计：来源系统业务单号
├─ certified_at            TIMESTAMP              -- 认证通过时间（SUCCESS 时写入）
├─ created_at              TIMESTAMP NOT NULL
├─ updated_at              TIMESTAMP NOT NULL
├─ is_deleted              INT DEFAULT 0

索引：
  UNIQUE (internal_company_name, uscc) WHERE is_deleted = 0 AND auth_status = 'SUCCESS'
  INDEX (transaction_no)
  INDEX (internal_company_name, uscc, auth_status, created_at DESC)
  INDEX (internal_company_name, created_at)
  INDEX (source_system, created_at)
  INDEX (auth_status, created_at)
  INDEX (enterprise_name)
```

> **记录策略**：
> - **查询与去重键**：`internal_company_name` + `uscc`（与 `source_system` 无关）。
> - 同一外部企业（uscc）对不同内部企业须分别认证、分别存储。
> - **`SUCCESS` 唯一**：同一 `(internal_company_name, uscc)` 仅一条 `SUCCESS`。
> - **`FAILED` / `PENDING` 可多条**；`transaction_no` 用于回调匹配。

### 8.2 个人实名认证记录表

```
t_integration_fdd_person_auth（法大大个人实名认证记录）

├─ id                      BIGINT PK              -- 雪花主键
├─ internal_company_name   VARCHAR(256) NOT NULL  -- 内部企业全称（业务判定键之一）
├─ transaction_no          VARCHAR(64)            -- 法大大侧认证流水号（回调匹配用）
├─ person_name             VARCHAR(64) NOT NULL   -- 姓名
├─ id_number               VARCHAR(18) NOT NULL   -- 身份证号（业务判定键之一，明文存储）
├─ mobile                  VARCHAR(11) NOT NULL   -- 手机号（业务判定键之一，明文存储；换号视为未认证）
├─ fdd_account_id          VARCHAR(64)            -- 法大大本地用户 accountId
├─ auth_url                VARCHAR(1024)          -- 法大大认证页面 URL
├─ auth_status             VARCHAR(16) NOT NULL   -- PENDING / SUCCESS / FAILED
├─ request_detail          JSONB                  -- 发起认证时法大大请求/响应原始报文
├─ auth_detail             JSONB                  -- 法大大回调原始报文
├─ fail_reason             VARCHAR(512)           -- 失败原因
├─ source_system           VARCHAR(16) NOT NULL   -- 审计：本次发起来源（CRM/OA/SYNC），不参与业务判定
├─ source_biz_no           VARCHAR(128)           -- 审计：来源系统业务单号
├─ certified_at            TIMESTAMP              -- 认证通过时间（SUCCESS 时写入）
├─ created_at              TIMESTAMP NOT NULL
├─ updated_at              TIMESTAMP NOT NULL
├─ is_deleted              INT DEFAULT 0

索引：
  UNIQUE (internal_company_name, id_number, mobile) WHERE is_deleted = 0 AND auth_status = 'SUCCESS'
  INDEX (transaction_no)
  INDEX (internal_company_name, id_number, mobile, auth_status, created_at DESC)
  INDEX (internal_company_name, created_at)
  INDEX (source_system, created_at)
  INDEX (auth_status, created_at)
  INDEX (person_name)
```

> **数据说明**：查询键为 `internal_company_name` + `id_number` + `mobile`。换手机号视为未认证。同一自然人对不同内部企业须分别认证。`SUCCESS` 按组合唯一；`FAILED`/`PENDING` 可多条；`source_system` 仅审计。

---

## 9. 技术方案

### 9.1 环境配置

法大大提供测试与生产两套环境，通过 Spring Profile 切换：

| 环境 | base-url | 认证地址（OAuth2 Token） | appId | appSecret |
|------|----------|------------------------|-------|-----------|
| **生产** | `http://10.80.61.92` | `http://10.80.61.92/base/login/oauth2/accessToken` | `345011` | `${FDD_APP_SECRET}` |
| **测试** | `http://10.80.61.82` | `http://10.80.61.82/base/login/oauth2/accessToken` | `268282` | `${FDD_APP_SECRET}` |

> **安全说明**：`appSecret` 通过环境变量 `FDD_APP_SECRET` 注入，禁止硬编码于代码或配置文件。测试与生产环境使用不同的环境变量值。appId 为非敏感标识，可直接配置于 yml。

### 9.2 配置项

**application-prod.yml**：

```yaml
fdd:
  base-url: http://10.80.61.92
  auth-url: http://10.80.61.92/base/login/oauth2/accessToken
  app-id: 345011
  app-secret: ${FDD_APP_SECRET}
  # 实名认证 URL 获取接口
  enterprise-auth-url: http://10.80.61.92/user/api/verify/company/url
  person-auth-url: http://10.80.61.92/user/api/verify/person/url
  # HTTP 超时
  connect-timeout: 10s
  read-timeout: 30s
  # 回调配置（作为企业/个人认证接口 notifyUrl 参数传递，无需在法大大后台单独配置）
  # 具体地址开发阶段按部署环境补充
  callback-url: http://{集成平台地址}/api/v1/fdd/callback
  # 回调超时（分钟）：发起认证后超过此时间未收到回调，标记超时并告警
  callback-timeout-minutes: 3
  # Token 缓存（OAuth2 accessToken 有效期 30 分钟）
  token-cache-minutes: 25
  # 企业认证方案：0 标准方案（默认），1 对公打款，2 纸质审核，3 法定代表人授权认证
  enterprise-verified-way: 0
  # 个人认证方案：0 三要素标准方案（默认）
  person-verified-way: 0
  # 重试配置
  max-retry: 3
  # 内部法人企业全称白名单（调用方 internalCompanyName 须精确匹配）
  internal-companies:
    - 南通泰禾化工股份有限公司
    - 上海泰禾国际贸易有限公司
```

**application-test.yml**：

```yaml
fdd:
  base-url: http://10.80.61.82
  auth-url: http://10.80.61.82/base/login/oauth2/accessToken
  app-id: 268282
  app-secret: ${FDD_APP_SECRET}
  enterprise-auth-url: http://10.80.61.82/user/api/verify/company/url
  person-auth-url: http://10.80.61.82/user/api/verify/person/url
  connect-timeout: 10s
  read-timeout: 30s
  # 回调配置（作为企业/个人认证接口 notifyUrl 参数传递，无需在法大大后台单独配置）
  # 具体地址开发阶段按测试部署环境补充
  callback-url: http://{集成平台测试地址}/api/v1/fdd/callback
  callback-timeout-minutes: 3
  token-cache-minutes: 25
  enterprise-verified-way: 0
  person-verified-way: 0
  max-retry: 3
  internal-companies:
    - 南通泰禾化工股份有限公司
    - 上海泰禾国际贸易有限公司
```

### 9.3 代码模块预估

| 模块 | 类/文件 | 说明 |
|------|---------|------|
| common | `FddProperties` | 法大大配置绑定（构造器注入） |
| common | `FddConstants` | 常量：接口路径、`LOG_BIZ = "Fdd"` |
| common | `FddEnums` | `AuthType`（ENTERPRISE/PERSON）、`AuthStatus`（PENDING/SUCCESS/FAILED）、`FddSourceSystem`（CRM/OA） |
| dao | `ApiKeyDO`、`ApiKeyMapper` | API Key 实体与 Mapper（复用 `t_integration_api_key`） |
| dao | `FddEnterpriseAuthDO` | 企业认证记录实体 |
| dao | `FddPersonAuthDO` | 个人认证记录实体 |
| dao | `FddEnterpriseAuthMapper` | 企业认证记录 Mapper |
| dao | `FddPersonAuthMapper` | 个人认证记录 Mapper |
| integration | `FddClient` | 法大大 HTTP 客户端（OAuth2 Token 获取 + 认证 URL 获取） |
| integration | `FddClient.dto.*` | `FddEnterpriseAuthRequest/Response`、`FddPersonAuthRequest/Response`、`FddCallbackRequest` |
| integration | `FddTokenSupport` | OAuth2 Token 获取与缓存（加密模式：SHA256(timestamp+appKey)） |
| service | `IApiKeyService` / `ApiKeyServiceImpl` | API Key 校验（BCrypt 哈希比对） |
| service | `IFddEnterpriseAuthService` / `FddEnterpriseAuthServiceImpl` | 企业认证业务逻辑（查库、发起认证、逐次留痕） |
| service | `IFddPersonAuthService` / `FddPersonAuthServiceImpl` | 个人认证业务逻辑 |
| manager | `IFddAuthManager` / `FddAuthManagerImpl` | 编排：统一查询入口、路由企业/个人、回调更新 |
| web | `FddAuthController` | 对外查询/认证 REST 接口 |
| web | `FddCallbackController` | 法大大回调接收（匿名接口） |
| web | `ApiKeyAuthFilter`（或等价拦截器） | 对外接口 API Key 鉴权，`/api/v1/fdd/callback` 除外 |
| web | `FddAuthConverter`（MapStruct） | DO ↔ VO 转换 |
| web | `dto/fdd/request/FddAuthQueryRequest` | 查询请求 DTO（含 internalCompanyName、sourceSystem 校验） |
| web | `dto/fdd/vo/FddAuthQueryVO` | 查询响应 VO |

### 9.4 依赖关系

```
web ──→ manager ──→ service ──→ dao ──→ common
  │         │
  │         └──→ integration (FddClient)
  │
  ├──→ ApiKeyAuthFilter ──→ service (ApiKey 校验)
  └──→ FddCallbackController（匿名，不走 API Key）
```

### 9.5 安全设计要点

| 项 | 方案 |
|----|------|
| 接口鉴权 | **与法大大功能同期实现** API Key 鉴权：复用表 `t_integration_api_key`，新增 Entity/Mapper/Service 及 `ApiKeyAuthFilter`；对外 `/api/v1/fdd/auth/**` 需携带 API Key，回调 `/api/v1/fdd/callback` 除外 |
| 回调访问控制 | 回调接口为匿名接口，不验签；回调地址通过企业/个人认证接口请求参数 `notifyUrl` 传递（无需在法大大后台预配置）；通过网络层 ACL（Nginx/防火墙）限制仅法大大服务器 IP 可访问 |
| 个人敏感数据 | 身份证号、手机号**明文存储**，接口**明文传输**（本期不做 AES/SM4 加密） |
| 日志脱敏 | 日志打印时仍须脱敏姓名（仅保留姓）、身份证号（仅保留前 6 + 后 4）、手机号（仅保留前 3 + 后 4） |
| 来源系统 | `sourceSystem` 仅在新发起认证时必填（CRM/OA）；不参与查询、去重或状态判定 |
| 内部企业 | `internalCompanyName` **必填**；与 uscc/idNumber 组成业务键；须在 `fdd.internal-companies` 白名单内 |
| 传输安全 | 法大大为内网地址（HTTP）；对外接口视部署环境决定是否启用 HTTPS |

### 9.6 法大大 Client 设计要点

```
FddClient（@Component，构造器注入 RestTemplate + ObjectMapper + FddProperties + FddTokenSupport）
├─ getEnterpriseAuthUrl(request) → FddEnterpriseAuthResponse
│   ├─ 获取 OAuth2 accessToken（FddTokenSupport，带缓存）
│   ├─ 构造请求体（JSON）
│   ├─ POST 调用 /user/api/verify/company/url
│   ├─ logRequest("企业认证URL", request)   ← ThirdPartyHttpLogSupport
│   ├─ logResponse("企业认证URL", response) ← ThirdPartyHttpLogSupport
│   └─ 返回 FddEnterpriseAuthResponse（含 data.url / transactionNo）
│
└─ getPersonAuthUrl(request) → FddPersonAuthResponse
    ├─ 同上
    └─ 返回 FddPersonAuthResponse（含 data.url / transactionNo）

FddTokenSupport（@Component）
├─ getAccessToken() → String
│   ├─ 缓存未过期 → 直接返回
│   └─ 缓存过期/无缓存 → POST auth-url 获取新 token，缓存后返回
│      请求体：{ appId, sign, timestamp }
│      timestamp = yyyyMMddHHmmss（非 Unix 毫秒）
│      sign = SHA256(timestamp + appSecret).toUpperCase()
│      成功码 code=0；后续业务请求头 Authorization: bearer {accessToken}
│

FddCallbackController（web 模块，构造器注入 FddAuthManager）
└─ POST /api/v1/fdd/callback
    ├─ 解析回调 JSON
    ├─ 委托 FddAuthManager.handleCallback()
    └─ 返回 "success"（法大大约定格式）
```

> **Token 管理**：法大大使用 OAuth2 加密模式鉴权（接口文档「获取鉴权 Token（加密模式）」），需先通过 `auth-url` 获取 accessToken（Token 有效期 30 分钟），再携带 token 调用认证 URL 获取接口。Token 带本地缓存，避免每次调用都重新获取。认证 URL 本身无有效期限制。

---

## 10. 字段映射（法大大接口 → 集成平台）

> **说明**：`internalCompanyName` 为集成平台业务字段，**不传法大大**；法大大侧仍使用 uscc / idNumber 作为 tpOrgId / tpAccountId。

### 10.1 企业认证请求 `/user/api/verify/company/url`

**集成平台 → 法大大**：

| 法大大参数 | 集成平台字段 | 说明 |
|------------|-------------|------|
| companyId | — | 法大大本地企业唯一标识，本期不传，由法大大在页面认证时创建/关联 |
| tpOrgId | uscc | 企业在第三方业务系统的唯一标识，本场景使用统一社会信用代码 |
| verifiedChannel | 固定 0 | 标准实名认证渠道 |
| verifiedWay | `fdd.enterprise-verified-way` | 企业认证方案，默认 0（标准方案） |
| isRepeatVerified | 自动计算 | 首次认证传 1，失败重试传 2 |
| companyInfoDTO.companyName | enterpriseName | 企业名称 |
| companyInfoDTO.creditCode | uscc | 统一社会信用代码 |
| companyInfoDTO.creditCodePath | — | 统一社会信用代码电子版 uuid，本期不传 |
| bankCardDTO | — | 对公账号信息，本期不传 |
| accountId | — | 法大大本地用户唯一标识，本期不传 |
| applicationType | 固定 0 | 企业管理员身份：0 全部（默认） |
| notifyUrl | `fdd.callback-url` | 异步回调地址，必传 |
| tpAccountId | — | 用户在第三方业务系统唯一标识，本期不传 |
| returnUrl | — | 同步跳转地址，可选，业务系统可配置 |
| isSendSms | 固定 0 | 是否发送实名认证短信，默认 0 否 |
| pageModify | 固定 2 | 用户是否能修改扫码后的认证页面信息，默认 2 不允许 |

### 10.2 个人认证请求 `/user/api/verify/person/url`

**集成平台 → 法大大**：

| 法大大参数 | 集成平台字段 | 说明 |
|------------|-------------|------|
| accountId | — | 法大大本地用户唯一标识，本期不传 |
| tpAccountId | idNumber | 用户在第三方业务系统的唯一标识，本场景使用身份证号 |
| verifiedChannel | 固定 0 | 标准实名认证渠道 |
| verifiedWay | `fdd.person-verified-way` | 验证方案，默认 0（三要素标准方案） |
| verifiedType | 自动计算 | 1 首次认证，2 重新认证 |
| name | personName | 用户姓名 |
| certType | 固定 "0" | 证件类型：0 身份证 |
| idCard | idNumber | 证件号码 |
| idCardPositiveFile | — | 证件照国徽面 uuid，本期不传 |
| idCardNegativeFile | — | 证件照人像面 uuid，本期不传 |
| personalBankCard | — | 银行卡号，本期不传 |
| notifyUrl | `fdd.callback-url` | 异步回调地址，必传 |
| returnUrl | — | 同步跳转地址，可选 |
| isSendSms | 固定 0 | 默认 0 否 |
| resultType | — | 刷脸结果页面显示，本期不传 |
| otherCertType | 固定 0 | 是否支持其他证件类型，默认 0 身份证 |
| miniProgram | 固定 0 | 0 非小程序（默认） |

> **说明**：个人认证手机号（mobile）为集成平台对外三要素字段，用于业务记录及失败通知；法大大接口中未直接接收手机号，由用户在法大大认证页面补充完成验证。

### 10.3 法大大响应 → 集成平台

| 法大大响应字段 | 集成平台字段 | 说明 |
|----------------|-------------|------|
| code | — | 返回码，100000 表示成功 |
| message | — | 返回描述 |
| data.url | authUrl | 认证页面 URL，返回给业务系统 |
| data.transactionNo | transaction_no | 认证流水号，用于回调匹配 |
| timestamp | — | 响应时间 |

### 10.4 法大大回调 → 集成平台

#### 10.4.1 企业实名认证回调 `notifyType = ENTERPRISE_IDENTIFY`

| 法大大回调字段 | 集成平台字段 | 说明 |
|----------------|-------------|------|
| notifyType | — | ENTERPRISE_IDENTIFY |
| companyId | fdd_company_id | 法大大本地企业唯一标识 |
| tpOrgId | uscc | 统一社会信用代码 |
| status | auth_status | 1未认证,2认证中,3已认证,4认证失败,5认证失效,6待授权,7授权失败 |
| transactionNo | transaction_no | 认证流水号 |

#### 10.4.2 个人实名认证回调 `notifyType = PERSONAL_IDENTIFY`

| 法大大回调字段 | 集成平台字段 | 说明 |
|----------------|-------------|------|
| notifyType | — | PERSONAL_IDENTIFY |
| accountId | fdd_account_id | 法大大本地用户唯一标识 |
| tpAccountId | id_number | 身份证号 |
| status | auth_status | 0未认证,1已认证,2认证失败,3认证中,4待授权,5认证失效,6授权失败 |
| transactionNo | transaction_no | 认证流水号 |

> **状态映射**：法大大回调 status 映射为集成平台 `AuthStatus`：
> - 已认证（企业 3 / 个人 1）→ `SUCCESS`
> - 认证失败（企业 4 / 个人 2）→ `FAILED`
> - 其他中间状态（认证中 / 待授权 / 授权失败 / 认证失效等）→ 保持 `PENDING`，**业务系统无需感知中间态**，仅感知 PENDING → SUCCESS/FAILED 的最终状态变化

---

## 11. 验收标准

| 序号 | 验收点 | 验收方式 | 通过标准 |
|------|--------|----------|----------|
| 1 | 统一查询接口 | 手动 | 传入 internalCompanyName+企业名称+uscc，无记录时返回 needAuth=true + status=PENDING + authUrl |
| 2 | 自动发起认证 | 手动 | 该组合无记录且 autoAuth=true 时，法大大侧返回认证 URL |
| 3 | 企业认证成功 | 手动 | 回调成功后，按 internalCompanyName+uscc 查询返回 certified=true |
| 4 | 个人认证成功 | 手动 | 回调成功后，按 internalCompanyName+idNumber+mobile 查询返回 certified=true |
| 5 | 认证记录复用 | 自动 | 同一业务键已 SUCCESS 时，再次查询直接返回，法大大无新请求 |
| 6 | 内部企业隔离 | 手动 | 同一 idNumber+mobile 对 A、B 两家内部企业分别认证，各自独立 SUCCESS 记录 |
| 7 | 逐次留痕 | 手动 | 同一组合多次失败产生多条 FAILED 记录 |
| 8 | SUCCESS 唯一 | 自动 | 同一 internalCompanyName+uscc 或 internalCompanyName+idNumber+mobile 的 SUCCESS 最多一条 |
| 9 | 内部企业校验 | 自动 | internalCompanyName 不在白名单返回参数错误 |
| 10 | 来源系统 | 手动 | 新发起认证时 sourceSystem 非 CRM/OA 报参数错误 |
| 11 | 个人换号未认证 | 手动 | 同一 internalCompanyName+idNumber 更换 mobile 后查询返回未认证/可重新发起 |
| 11 | 回调匿名接收 | 自动 | 回调接口无需签名；非法大大 IP 被网络层拒绝 |
| 12 | 原始报文留存 | 手动 | request_detail、auth_detail 含完整 JSON |
| 13 | 认证失败处理 | 手动 | FAILED 后查询返回 failReason；autoAuth=true 可再次发起 |
| 14 | 接口鉴权 | 自动 | 无 API Key 返回 401 |
| 15 | 日志脱敏 | 手动 | 日志中姓名、身份证号、手机号脱敏 |
| 16 | 回调超时告警 | 手动 | 3 分钟未回调标记超时并告警 |
| 17 | OAuth2 Token | 自动 | Token 获取成功并缓存 |
| 18 | 认证 URL 可访问 | 手动 | authUrl 可在浏览器打开法大大认证页 |

---

## 12. 待确认信息清单

> **使用说明**：请逐项确认，将「确认结果」列补充完整。标记 ✅ 的为已确认项。

### 12.1 法大大平台

| 编号 | 确认项 | 确认结果 | 备注 |
|------|--------|----------|------|
| F1 ★ | 企业实名认证 API 地址与请求/响应字段 | ✅ `/user/api/verify/company/url` | 页面级认证，响应含 `data.url` |
| F2 ★ | 个人实名认证 API 地址与请求/响应字段 | ✅ `/user/api/verify/person/url` | 三要素标准方案（verifiedWay=0） |
| F3 ★ | 认证结果回调方式与数据结构 | ✅ `ENTERPRISE_IDENTIFY` / `PERSONAL_IDENTIFY` | 回调含 `status` + `transactionNo` |
| F4 ★ | OAuth2 accessToken 获取方式与有效期 | ✅ 加密模式 `/base/login/oauth2/accessToken`；Token 30 分钟 | 生产/测试各一套地址 |
| F5 ★ | App ID / App Secret | ✅ 已确认（见 §9.1） | appSecret 通过环境变量注入 |
| F6 | API 调用频率限制（QPS） | ✅ 暂不限制 | 文档未提供限制，集成平台暂不限流 |
| F7 ★ | 认证结果回调超时时间 | ✅ 3 分钟 | 超过 3 分钟未回调标记超时并告警 |
| F8 ★ | 是否支持测试环境 | ✅ 支持测试环境，不支持沙箱 | 测试环境地址：`http://10.80.61.82` |
| F9 | 认证 URL 有效期 | ✅ 无有效期 | 法大大认证 URL 无过期时间，可长期访问 |
| F10 | 企业/个人认证中间状态（认证中/待授权/授权失败）是否需要业务系统感知 | ✅ 无需感知 | 中间态保持 PENDING，业务系统仅感知 PENDING → SUCCESS/FAILED 最终状态 |

### 12.2 集成平台

| 编号 | 确认项 | 确认结果                    | 备注 |
|------|--------|-------------------------|------|
| P1 ★ | `app-secret` 管理方案 | ✅ 环境变量 `FDD_APP_SECRET` | 测试/生产分别配置 |
| P2 ★ | 身份证号加密方案（AES-256 / SM4） | ✅ 无需加密                  | 明文存储与接口传输；日志仍脱敏 |
| P3 ★ | 认证结果有效期 | ✅ 暂不设置有效期               | SUCCESS 长期有效，后续按需扩展 |
| P4 | 是否需要定时扫描 PENDING 超时记录并告警 | ✅ 需要                    | 超过 3 分钟未回调标记超时 |
| P5 | 是否需要认证次数统计看板 | ✅ 暂不设置                  | |
| P6 ★ | callback URL 是否需要配置到法大大后台 | ✅ 无需单独配置                | 回调地址通过 notifyUrl 参数传递 |
| P7 ★ | callback URL 具体地址 | ✅ 开发阶段补充                | 占位符 `{集成平台地址}`，联调前按部署环境写入 yml |
| P8 ★ | 集成平台地址是否可被法大大服务器访问 | ✅ 内网地址，法大大与集成平台在同一内网    | 法大大为内网部署 |
| P9 | 回调接口网络层 ACL 配置（限制来源 IP） |      ✅ 暂不设置                   | 需运维配合配置 Nginx/防火墙规则 |
| P10 | 认证 URL 是否需要业务系统缓存 | ✅ 无需缓存                  | 业务系统直接透传给终端用户，不存储 |
| P11 ★ | API Key 鉴权实现 | ✅ 与法大大同期实现              | 复用 `t_integration_api_key`，新增 Filter/Service |
| P12 ★ | 来源系统枚举 | ✅ CRM、OA                | 固定文本；**仅新发起认证时必填**；审计字段，不参与查询去重 |
| P13 ★ | 认证记录留痕策略 | ✅ 逐次新增                  | 查询键 internalCompanyName+uscc/idNumber；SUCCESS 唯一；FAILED 可多条 |
| P14 ★ | 内部企业归属 | ✅ 每条记录绑定一家内部企业          | 全称白名单配置；同一人/企业对多内部企业分别认证 |

---

## 13. 风险与依赖

| 风险/依赖 | 影响 | 应对措施 |
|-----------|------|----------|
| 法大大页面认证 URL 无法打开 | 用户无法完成认证 | 集成平台记录完整请求/响应报文；联系法大大排查 |
| 回调接口匿名无验签 | 存在伪造回调风险 | 网络层 ACL 限制仅法大大 IP 可访问回调接口 |
| 个人敏感信息明文 | 内网环境存储与传输风险 | 内网部署 + 日志脱敏；后续按需评估加密 |
| 并发认证 | 同一组合重复发起认证 | `(internal_company_name, uscc/id_number)` SUCCESS 部分唯一索引 + PENDING 幂等 |
| 回调网络波动 | 回调丢失或延迟 | 3 分钟超时告警 + 法大大侧重试机制 |
| OAuth2 Token 过期 | 认证 API 调用失败 | Token 带本地缓存，25 分钟刷新一次 |
| 法大大回调状态语义差异 | 企业/个人状态码不一致 | 统一映射为 PENDING/SUCCESS/FAILED |

---

## 14. 变更记录

| 日期 | 修改人 | 修改内容 |
|------|--------|----------|
| 2026-08-03 | — | 创建文档，初版需求梳理 |
| 2026-08-03 | — | 根据反馈优化：回调改为匿名接口不验签；个人认证仅三要素；接口不返回 bizNo 改用 uscc/idNumber 追踪；填充法大大环境地址与凭证；回调超时 3 分钟；支持测试环境不支持沙箱；暂不设认证有效期；删除业务系统确认项 |
| 2026-08-03 | — | 根据法大大接口文档更新：企业/个人认证改为页面级 URL 模式；补齐 `/user/api/verify/company/url`、`/user/api/verify/person/url` 请求/响应字段；更新回调结构 `ENTERPRISE_IDENTIFY` / `PERSONAL_IDENTIFY`；明确 API 调用频率暂不限制；调整数据库字段及 Client 设计 |
| 2026-08-03 | — | 确认剩余待确认项：认证 URL 无有效期；Token 有效期 30 分钟（接口文档加密模式）；认证中间态业务系统无需感知；回调 URL 通过 notifyUrl 参数传递无需法大大后台配置；认证 URL 无需业务系统缓存 |
| 2026-08-03 | — | 明确：身份证号/手机号明文存储与传输；callback-url 开发阶段补充；API Key 与法大大同期实现；认证记录逐次留痕；SUCCESS 唯一、FAILED 可多条 |
| 2026-08-03 | — | 澄清：sourceSystem 仅审计；业务键为 internalCompanyName+uscc/idNumber；同一人/企业对多内部企业分别认证 |
| 2026-08-05 | — | 个人业务键升级为 internalCompanyName+idNumber+mobile；换号视为未认证；明确法大大 companyType 与泰禾内部企业无关 |
