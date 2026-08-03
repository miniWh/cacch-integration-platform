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
- CRM 客户入驻时需对客户企业及对接人进行实名核验

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
| 认证记录复用 | 同一企业统一社会信用代码 / 同一人身份证号只认证一次；后续系统查询时直接返回已有结果 |
| 异步回调解耦 | 认证为异步流程：发起认证后立即返回 `PENDING` 及认证 URL，法大大回调到达后更新为 `SUCCESS` / `FAILED` |
| 业务唯一键 | 企业按**统一社会信用代码（uscc）**去重，个人按**身份证号（idNumber）**去重；接口不返回 bizNo，业务系统以 uscc 或 idNumber 追踪认证状态 |
| 幂等查询 | 业务系统可反复调用查询接口，每次返回最新认证状态，不重复发起认证 |
| 回调匿名接收 | 回调接口为匿名接口，不需要验签，仅限法大大平台访问（通过网络层 ACL 控制） |

### 3.3 认证类型

| 认证类型 | 认证对象 | 法大大对应能力 | 典型场景 |
|----------|----------|---------------|----------|
| 企业实名认证 | 外部企业 | 调用 `/user/api/verify/company/url` 获取企业实名认证页面 URL，企业在页面完成对公打款/纸质审核/法定代表人授权等认证 | 供应商准入、客户入驻 |
| 个人实名认证 | 外部企业联系人 | 调用 `/user/api/verify/person/url` 获取个人实名认证页面 URL，个人在页面完成三要素/人脸等认证 | 联系人身份核验 |

---

## 4. 业务流程

### 4.1 核心流程：查询 → 判断 → 获取 URL → 页面认证 → 回调

```
业务系统                      集成平台                         法大大
   │                            │                               │
   │  ① 查询/认证请求             │                               │
   │  (企业名称+uscc 或           │                               │
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
   │  (按 uscc 或身份证号)          │                               │
   │ ──────────────────────────→ │                               │
   │   ← ─ ─ 返回 SUCCESS ─ ─ ─  │                               │
```

### 4.2 查询接口详细判断逻辑

业务系统调用查询接口时的处理逻辑：

```
输入：认证类型（ENTERPRISE/PERSON）+ 业务唯一键（uscc 或 idNumber）

STEP 1：查本地认证记录表
  ├─ 有记录 + 状态 = SUCCESS → 返回 { certified: true, 认证信息, certifiedAt }
  ├─ 有记录 + 状态 = PENDING → 返回 { certified: false, status: "PENDING", authUrl, message: "认证处理中" }
  ├─ 有记录 + 状态 = FAILED → 返回 { certified: false, status: "FAILED", failReason, canRetry: true }
  └─ 无记录                  → STEP 2

STEP 2：自动发起认证（如有 autoAuth 标记）
  ├─ 调用法大大企业/个人认证 URL 接口
  ├─ 获取法大大返回的认证页面 URL
  ├─ 入库认证记录（状态 PENDING）
  └─ 返回 { certified: false, needAuth: true, status: "PENDING", authUrl }
```

> **说明**：查询接口支持 `autoAuth` 参数。当 `autoAuth=true`（默认）时，未查到记录自动发起认证；当 `autoAuth=false` 时，仅返回是否需要认证，由业务系统决定是否发起。认证发起后，业务系统通过原有的 uscc（企业）或 idNumber（个人）再次查询即可获取最新状态，接口不返回 bizNo。

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
（无记录）
    │
    ↓ 发起认证（获取 URL）
 PENDING ──────────→ SUCCESS（法大大回调认证通过）
    │
    └──────────────→ FAILED（法大大回调认证失败）

状态说明：
  PENDING   — 已发起认证，等待法大大回调
  SUCCESS   — 实名认证通过
  FAILED    — 实名认证不通过
```

> **说明**：认证结果暂不设置有效期，SUCCESS 状态长期有效。如后续业务需要过期重认证，再行扩展。

---

## 5. 功能点

| 序号 | 功能点 | 描述 | 必选/可选 |
|------|--------|------|-----------|
| F1 | 统一查询接口 | 对外提供企业/个人实名认证状态查询，按 uscc/idNumber 返回认证结果 | 必选 |
| F2 | 自动发起认证 | 查询无记录时自动调用法大大 API 获取认证 URL；支持 autoAuth 参数控制 | 必选 |
| F3 | 企业实名认证 URL | 调法大大 `/user/api/verify/company/url` 获取企业认证页面 URL | 必选 |
| F4 | 个人实名认证 URL | 调法大大 `/user/api/verify/person/url` 获取个人认证页面 URL | 必选 |
| F5 | 认证回调接收 | 接收法大大异步认证结果回调（匿名接口），更新认证状态 | 必选 |
| F6 | 认证记录复用 | 同一企业/个人已认证通过时，后续系统查询直接返回，不重复认证 | 必选 |
| F7 | 原始报文留存 | 法大大请求与回调原始报文 JSONB 完整保存，便于问题排查 | 必选 |
| F8 | 认证记录查询 API | 按认证类型、状态、时间范围分页查询认证记录（管理后台用） | 可选 |
| F9 | 认证失败告警 | 认证 FAILED 或回调超时（3 分钟）触发企微 Webhook 告警 | 可选 |

**明确不做**：

| 项 | 说明 |
|----|------|
| 法大大电子签章 | 本需求仅包含实名认证能力，不含合同签署/签章 |
| 法大大存证/公证 | 不在本期范围 |
| 业务侧通知 | 集成平台不主动推送给业务系统；业务系统通过查询接口获取最新状态 |
| 认证有效期 | 暂不设置认证结果有效期，SUCCESS 长期有效 |
| 个人四要素/人脸认证 | 个人认证仅使用三要素（姓名+身份证号+手机号），法大大页面完成剩余验证 |
| API 频率限制 | 法大大侧暂未限制调用频率，集成平台暂不做限流 |

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
| 集成中台 integration | `FddClient`（HTTP 客户端）、`FddCallbackController` | 调用法大大 API、接收回调 |
| 集成中台 service | `FddEnterpriseAuthService`、`FddPersonAuthService` | 企业/个人认证业务逻辑 |
| 集成中台 manager | `FddAuthManager` | 编排查询、认证、回调更新 |
| 集成中台 dao（PG） | 认证记录 Mapper | 企业/个人认证记录 CRUD |
| 集成中台 web | `FddAuthController` | 对外查询/认证接口 |
| 集成中台 common | `FddProperties`、`FddConstants` | 配置绑定、常量定义 |

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
| enterpriseName | String | 条件 | 企业名称（authType=ENTERPRISE 时必填） |
| uscc | String | 条件 | 统一社会信用代码（authType=ENTERPRISE 时必填，业务唯一键） |
| personName | String | 条件 | 姓名（authType=PERSON 时必填） |
| idNumber | String | 条件 | 身份证号（authType=PERSON 时必填，业务唯一键） |
| mobile | String | 条件 | 手机号（authType=PERSON 时必填，三要素认证） |
| autoAuth | Boolean | 否 | 是否自动发起认证，默认 true |
| sourceSystem | String | 是 | 调用来源：OA / CRM / …（用于审计追踪） |
| sourceBizNo | String | 否 | 来源系统业务单号（便于溯源） |

**响应参数**：

| 字段 | 类型 | 说明 |
|------|------|------|
| certified | Boolean | 是否已认证通过 |
| needAuth | Boolean | 是否已发起认证（true 表示本次发起或已有 PENDING 记录） |
| status | String | 认证状态：PENDING / SUCCESS / FAILED |
| authType | String | 认证类型：ENTERPRISE / PERSON |
| authUrl | String | 法大大认证页面 URL（status=PENDING 且本次发起或首次查询时返回） |
| enterpriseName | String | 企业名称（authType=ENTERPRISE 时返回） |
| uscc | String | 统一社会信用代码（authType=ENTERPRISE 时返回，业务追踪用） |
| personName | String | 姓名（authType=PERSON 时返回） |
| idNumber | String | 身份证号（authType=PERSON 时返回，业务追踪用，脱敏） |
| failReason | String | 失败原因（status=FAILED 时返回） |
| certifiedAt | String | 认证通过时间（ISO 8601，status=SUCCESS 时返回） |
| message | String | 提示信息 |

> **说明**：接口不返回 bizNo。业务系统通过请求时传入的 uscc（企业）或 idNumber（个人）作为追踪标识，后续查询时使用相同的唯一键即可获取最新认证状态。

#### 7.1.2 认证状态查询接口

```
GET /api/v1/fdd/auth/status?authType=ENTERPRISE&uscc={uscc}
GET /api/v1/fdd/auth/status?authType=PERSON&idNumber={idNumber}
```

按业务唯一键查询单条认证记录的最新状态。返回结构与 7.1.1 响应一致。

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
| GET | `/api/v1/fdd/auth/records` | 分页查询认证记录（按类型/状态/时间） |
| POST | `/api/v1/fdd/auth/retry` | 手动重试失败的认证（传入 authType + uscc/idNumber） |

---

## 8. 数据库设计

### 8.1 企业实名认证记录表

```
t_integration_fdd_enterprise_auth（法大大企业实名认证记录）

├─ id                      BIGINT PK              -- 雪花主键
├─ transaction_no          VARCHAR(64)            -- 法大大侧认证流水号（回调匹配用）
├─ enterprise_name         VARCHAR(256) NOT NULL  -- 企业名称
├─ uscc                    VARCHAR(32) NOT NULL   -- 统一社会信用代码（业务唯一键）
├─ auth_url                VARCHAR(1024)          -- 法大大认证页面 URL
├─ auth_status             VARCHAR(16) NOT NULL   -- PENDING / SUCCESS / FAILED
├─ auth_detail             JSONB                  -- 法大大回调原始报文
├─ fail_reason             VARCHAR(512)           -- 失败原因
├─ source_system           VARCHAR(32)            -- 首次调用来源系统
├─ source_biz_no           VARCHAR(128)           -- 来源系统业务单号
├─ certified_at            TIMESTAMP              -- 认证通过时间
├─ retry_count             INT DEFAULT 0          -- 重试次数
├─ created_at              TIMESTAMP NOT NULL
├─ updated_at              TIMESTAMP NOT NULL
├─ is_deleted              INT DEFAULT 0

索引：
  UNIQUE (uscc) WHERE is_deleted = 0 AND auth_status = 'SUCCESS'
  INDEX (transaction_no)
  INDEX (auth_status, created_at)
  INDEX (enterprise_name)
```

> **说明**：`uscc` 唯一索引仅约束 `SUCCESS` 状态的记录，PENDING/FAILED 允许重新发起认证产生新记录。`transaction_no` 用于回调匹配，非业务唯一键。

### 8.2 个人实名认证记录表

```
t_integration_fdd_person_auth（法大大个人实名认证记录）

├─ id                      BIGINT PK              -- 雪花主键
├─ transaction_no          VARCHAR(64)            -- 法大大侧认证流水号（回调匹配用）
├─ person_name             VARCHAR(64) NOT NULL   -- 姓名
├─ id_number               VARCHAR(128) NOT NULL  -- 身份证号（业务唯一键，加密存储 ⚠️）
├─ mobile                  VARCHAR(64)            -- 手机号（三要素，加密存储）
├─ auth_url                VARCHAR(1024)          -- 法大大认证页面 URL
├─ auth_status             VARCHAR(16) NOT NULL   -- PENDING / SUCCESS / FAILED
├─ auth_detail             JSONB                  -- 法大大回调原始报文
├─ fail_reason             VARCHAR(512)           -- 失败原因
├─ source_system           VARCHAR(32)            -- 首次调用来源系统
├─ source_biz_no           VARCHAR(128)           -- 来源系统业务单号
├─ certified_at            TIMESTAMP              -- 认证通过时间
├─ retry_count             INT DEFAULT 0          -- 重试次数
├─ created_at              TIMESTAMP NOT NULL
├─ updated_at              TIMESTAMP NOT NULL
├─ is_deleted              INT DEFAULT 0

索引：
  UNIQUE (id_number) WHERE is_deleted = 0 AND auth_status = 'SUCCESS'
  INDEX (transaction_no)
  INDEX (auth_status, created_at)
  INDEX (person_name)
```

> ⚠️ **数据安全提醒**：身份证号、手机号等个人敏感信息须加密存储（AES-256 或应用层加密），日志打印须脱敏，遵守《个人信息保护法》要求。`id_number`、`mobile` 字段存储密文，因此 VARCHAR 长度按密文长度设定。

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
  callback-url: http://{集成平台测试地址}/api/v1/fdd/callback
  callback-timeout-minutes: 3
  token-cache-minutes: 25
  enterprise-verified-way: 0
  person-verified-way: 0
  max-retry: 3
```

### 9.3 代码模块预估

| 模块 | 类/文件 | 说明 |
|------|---------|------|
| common | `FddProperties` | 法大大配置绑定（构造器注入） |
| common | `FddConstants` | 常量：接口路径、`LOG_BIZ = "Fdd"` |
| common | `FddEnums` | `AuthType`（ENTERPRISE/PERSON）、`AuthStatus`（PENDING/SUCCESS/FAILED） |
| dao | `FddEnterpriseAuthDO` | 企业认证记录实体 |
| dao | `FddPersonAuthDO` | 个人认证记录实体 |
| dao | `FddEnterpriseAuthMapper` | 企业认证记录 Mapper |
| dao | `FddPersonAuthMapper` | 个人认证记录 Mapper |
| integration | `FddClient` | 法大大 HTTP 客户端（OAuth2 Token 获取 + 认证 URL 获取） |
| integration | `FddClient.dto.*` | `FddEnterpriseAuthRequest/Response`、`FddPersonAuthRequest/Response`、`FddCallbackRequest` |
| integration | `FddTokenSupport` | OAuth2 Token 获取与缓存（加密模式：SHA256(timestamp+appKey)） |
| integration | `FddCallbackController` | 法大大回调接收（匿名接口，直接处理 + 状态更新） |
| service | `IFddEnterpriseAuthService` / `FddEnterpriseAuthServiceImpl` | 企业认证业务逻辑（查库、发起认证） |
| service | `IFddPersonAuthService` / `FddPersonAuthServiceImpl` | 个人认证业务逻辑 |
| manager | `IFddAuthManager` / `FddAuthManagerImpl` | 编排：统一查询入口、路由企业/个人、回调处理 |
| web | `FddAuthController` | 对外查询/认证 REST 接口 |
| web | `FddAuthConverter`（MapStruct） | DO ↔ VO 转换 |
| web | `dto/fdd/request/FddAuthQueryRequest` | 查询请求 DTO |
| web | `dto/fdd/vo/FddAuthQueryVO` | 查询响应 VO |

### 9.4 依赖关系

```
web ──→ manager ──→ service ──→ dao ──→ common
  │                    │
  │                    └──→ integration (FddClient)
  │
  └──→ integration (FddCallbackController)
```

### 9.5 安全设计要点

| 项 | 方案 |
|----|------|
| 接口鉴权 | 对外查询/认证接口使用 API Key 鉴权（复用现有 `security` 模块），各业务系统分配独立 Key |
| 回调访问控制 | 回调接口为匿名接口，不验签；回调地址通过企业/个人认证接口请求参数 `notifyUrl` 传递（无需在法大大后台预配置）；通过网络层 ACL（Nginx/防火墙）限制仅法大大服务器 IP 可访问 |
| 敏感数据加密 | 身份证号、手机号使用 AES-256 加密存储 |
| 日志脱敏 | 日志打印时自动脱敏姓名（仅保留姓）、身份证号（仅保留前 6 + 后 4）、手机号（仅保留前 3 + 后 4） |
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
│      sign = SHA256(timestamp + appSecret).toUpperCase()
│

FddCallbackController（@RestController，构造器注入 FddAuthManager）
└─ POST /api/v1/fdd/callback
    ├─ 解析回调 JSON
    ├─ 委托 FddAuthManager.handleCallback()
    └─ 返回 "success"（法大大约定格式）
```

> **Token 管理**：法大大使用 OAuth2 加密模式鉴权（接口文档「获取鉴权 Token（加密模式）」），需先通过 `auth-url` 获取 accessToken（Token 有效期 30 分钟），再携带 token 调用认证 URL 获取接口。Token 带本地缓存，避免每次调用都重新获取。认证 URL 本身无有效期限制。

---

## 10. 字段映射（法大大接口 → 集成平台）

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
| 1 | 统一查询接口 | 手动 | 传入企业名称+uscc，无记录时返回 needAuth=true + status=PENDING + authUrl |
| 2 | 自动发起认证 | 手动 | 查询无记录且 autoAuth=true 时，法大大侧返回认证 URL |
| 3 | 企业认证成功 | 手动 | 法大大回调 status=3（已认证）后，按 uscc 查询返回 certified=true |
| 4 | 个人认证成功 | 手动 | 法大大回调 status=1（已认证）后，按 idNumber 查询返回 certified=true |
| 5 | 认证记录复用 | 自动 | 同一 uscc 已 SUCCESS 时，另一个系统查询直接返回 certified=true，法大大侧无新请求 |
| 6 | 回调匿名接收 | 自动 | 回调接口无需签名即可正常处理；非法大大 IP 被网络层拒绝 |
| 7 | 原始报文留存 | 手动 | auth_detail 字段包含完整的回调 JSON |
| 8 | 认证失败处理 | 手动 | 法大大回调 FAILED 后，查询返回 failReason，支持 retry |
| 9 | 接口鉴权 | 自动 | 无 API Key 的请求返回 401 |
| 10 | 日志脱敏 | 手动 | 日志中姓名、身份证号、手机号均脱敏展示 |
| 11 | 回调超时告警 | 手动 | 认证发起 3 分钟后未收到回调，标记超时并触发告警 |
| 12 | OAuth2 Token | 自动 | Token 获取成功并缓存，过期前自动刷新 |
| 13 | 认证 URL 可访问 | 手动 | 返回的 authUrl 可在浏览器打开并进入法大大认证页面 |

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

| 编号 | 确认项 | 确认结果 | 备注 |
|------|--------|----------|------|
| P1 ★ | `app-secret` 管理方案 | ✅ 环境变量 `FDD_APP_SECRET` | 测试/生产分别配置 |
| P2 ★ | 身份证号加密方案（AES-256 / SM4） | | 密钥托管位置待确认 |
| P3 ★ | 认证结果有效期 | ✅ 暂不设置有效期 | SUCCESS 长期有效，后续按需扩展 |
| P4 | 是否需要定时扫描 PENDING 超时记录并告警 | ✅ 需要 | 超过 3 分钟未回调标记超时 |
| P5 | 是否需要认证次数统计看板 | | |
| P6 ★ | callback URL 是否需要配置到法大大后台 | ✅ 无需单独配置 | 回调地址通过企业/个人认证接口请求参数 `notifyUrl` 传递，无需在法大大后台预配置 |
| P7 ★ | 集成平台地址是否可被法大大服务器访问 | ✅ 内网地址，法大大与集成平台在同一内网 | 法大大为内网部署 |
| P8 | 回调接口网络层 ACL 配置（限制来源 IP） | | 需运维配合配置 Nginx/防火墙规则 |
| P9 | 认证 URL 是否需要业务系统缓存 | ✅ 无需缓存 | 业务系统直接透传给终端用户，不存储 |

---

## 13. 风险与依赖

| 风险/依赖 | 影响 | 应对措施 |
|-----------|------|----------|
| 法大大页面认证 URL 无法打开 | 用户无法完成认证 | 集成平台记录完整请求/响应报文；联系法大大排查 |
| 回调接口匿名无验签 | 存在伪造回调风险 | 网络层 ACL 限制仅法大大 IP 可访问回调接口 |
| 身份证号存储合规 | 法律风险 | AES-256 加密存储 + 日志脱敏 + 合规评审 |
| 并发认证 | 同一企业/个人重复发起认证 | DB 唯一索引 + 幂等判断 |
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
