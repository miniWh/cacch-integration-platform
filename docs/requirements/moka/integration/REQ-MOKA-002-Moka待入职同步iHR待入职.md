# REQ-MOKA-002 — Moka 待入职同步 iHR 待入职设计方案

> 版本：v2.0 · 日期：2026-10-12 · 作者：hongfu_zhou@cacch.com · 状态：待实施

---

## 一、需求概述

从 Moka ATS 开放平台查询处于「待入职」阶段的候选人信息，按两步走策略同步至 iHR 开放平台「新增待入职-简略」接口（`POST /openapi/thirdparty/api/staff/v1/entryForms/create`），形成 **Moka → 本地快照 → iHR** 的解耦链路。

### 1.1 触发参数（固定值）

| 参数 | 值 | 说明 |
|------|----|------|
| `pipelineIds` | `[163155, 163156]` | 待入职流程 ID |
| `stageIds` | `[226137]` | 待入职阶段 ID |

### 1.2 同步策略

| 项目 | 说明 |
|------|------|
| 拉取方式 | 全量分页拉取（每次触发都查全量） |
| 分页处理 | `list_by_condition`：每次 limit=20，循环 `data.next` 翻页 |
| 中间库写入 | 幂等 upsert：按 `application_id` 做 `ON CONFLICT DO UPDATE` |
| **数据补全** | 快照入库后，**再调两个 Moka 接口补充字段**（解决 `list_by_condition` 不含部门编码和入职日期的问题） |

### 1.3 两步走拆分

| 步骤 | 接口 | 内部子阶段 | 说明 |
|------|------|-----------|------|
| 第一步 | `POST /api/v1/moka/entry-candidates/sync` | **1a** 快照入库<br>**1b** 职位详情补全<br>**1c** Offer 扩展补全 | 拉取 → 补全 → 本地快照 + 状态表 |
| 第二步 | `POST /api/v1/moka/entry-candidates/push-ihr` | — | 本地表 → iHR 待入职 |

**拆分理由**：业务解耦，便于后续接入其他 HR/审批系统时只需扩展第二步的同步目标列。

### 1.4 关键字段来源（补全逻辑说明）

| 字段 | 用途 | 原始来源 | 补全接口 |
|------|------|---------|---------|
| `moka_department_code` | 第二步匹配 iHR 部门 | `list_by_condition` **不返回** | **职位详情接口** `jobs/getJobs`（按 jobId 查，取 response.departmentCode） |
| `check_in_date` | 第二步作为 iHR `expectEntryDate` | `list_by_condition` **不返回** | **Offer 扩展接口** `v3/getOfferExtendInfos`（按 applicationId 查，取 checkInDate） |
| `offer_department_code` | 备选部门编码（offer 上的入职部门） | `list_by_condition` **不返回** | **Offer 扩展接口**（取 offer.departmentCode） |

**补全策略**：
- 职位接口：按 **jobId 去重**后批量查询（多条 application 可能共用一个 jobId），避免重复调用
- Offer 扩展接口：按 **applicationId** 批量查询（项目常量已定义该接口支持 `applicationIds` 数组）
- 两个接口均为回写 UPDATE 操作（首次快照入库时字段为 null，补全阶段 SET）

---

## 二、整体架构

```
┌──────────────────────────────────────────────────────────────────────────┐
│  第一步：Moka → 本地快照 + 补全                                            │
│                                                                           │
│  POST /api/v1/moka/entry-candidates/sync                                  │
│       │                                                                   │
│       ▼                                                                   │
│  MokaEntryCandidateSyncManager                                            │
│       │                                                                   │
│       ├─ 【1a 快照入库】                                                  │
│       │   MokaCandidatesService.listEntryCandidates()                     │
│       │     └─ MokaCandidatesClient.listApplications()                    │
│       │        （固定 pipelineIds=[163155,163156], stageIds=[8]）          │
│       │   → 幂等 upsert 主表 + 3 子表 + 状态表（department_code=null,      │
│       │     check_in_date=null）                                          │
│       │                                                                   │
│       ├─ 【1b 职位详情补全】                                               │
│       │   收集主表中去重的 jobId 列表                                      │
│       │   → MokaJobClient.getJobDetailsByJobIds()                          │
│       │   → UPDATE 主表 SET moka_department_code = ?                       │
│       │                                                                   │
│       └─ 【1c Offer 扩展补全】                                             │
│           收集主表中所有 applicationId 列表                                │
│           → MokaOfferClient.getOfferExtendInfos()                          │
│           → UPDATE 主表 SET check_in_date = ?,                             │
│                         offer_department_code = ?                         │
│                                                                           │
│  最终表结构：                                                               │
│  t_integration_moka_entry_application  （主表，含补全后的 department_code  │
│                                           和 check_in_date）               │
│  t_integration_moka_entry_candidate_base  （子表 1）                       │
│  t_integration_moka_entry_graduate_school （子表 2）                      │
│  t_integration_moka_entry_citizen         （子表 3）                       │
│  t_integration_moka_entry_sync             （状态表）                       │
└──────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────┐
│  第二步：本地表 → iHR 待入职                                                │
│                                                                           │
│  POST /api/v1/moka/entry-candidates/push-ihr                              │
│       │                                                                   │
│       ▼                                                                   │
│  MokaEntryCandidatePushManager                                            │
│       │                                                                   │
│       ├─ 主表 JOIN 子表 JOIN 状态表                                         │
│       │  WHERE ihr_sync_status = 'PENDING'                                │
│       │                                                                   │
│       ├─ 查 t_integration_ihr_department                                   │
│       │  （Moka department_code → iHR ihr_dept_id 匹配）                   │
│       │                                                                   │
│       ├─ IhrStaffEntryFormService.createEntryForm()                       │
│       │   （字段映射 + Token 复用 + 401 刷新重试）                        │
│       │                                                                   │
│       └─ 更新 t_integration_moka_entry_sync.ihr_sync_status               │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 三、数据库变更

### 3.1 表拆分设计

Moka `list_by_condition` 响应 `ApplicationItem` 含 5 层嵌套结构，且字段确定，按嵌套层级拆为 4 张业务表 + 1 张状态表：

| 表 | 层级 | 说明 | 与主表关系 |
|----|------|------|-----------|
| `t_integration_moka_entry_application` | ApplicationItem 顶层 | 申请级别字段 + **补全字段**（department_code、check_in_date） | — 主表 |
| `t_integration_moka_entry_candidate_base` | CandidateBaseInfo + ContactInfo | 候选人基础信息 + 联系方式（1:1 合入） | `application_id` FK |
| `t_integration_moka_entry_graduate_school` | GraduateSchool | 毕业院校信息 | `application_id` FK |
| `t_integration_moka_entry_citizen` | Citizen | 证件信息 | `application_id` FK |
| `t_integration_moka_entry_sync` | — | iHR 同步状态跟踪 | `application_id` FK |

### 3.2 补全字段说明

主表新增以下补全字段（Step 1a 入库时为 null，Step 1b/1c 回写）：

| 列名 | 类型 | 补全来源 | 说明 |
|------|------|---------|------|
| `moka_department_code` | VARCHAR(64) | 职位详情接口 `jobs/getJobs` | **部门编码**（code 字段），第二步匹配 iHR 部门的关键字段 |
| `offer_department_code` | VARCHAR(64) | Offer 扩展接口 `v3/getOfferExtendInfos` | Offer 上的入职部门编码（备选；offer 的部门信息通常比职位的更接近实际入职部门） |
| `offer_department_name` | VARCHAR(128) | Offer 扩展接口 | Offer 入职部门名称 |
| `check_in_date` | TIMESTAMP | Offer 扩展接口 | **入职日期**（checkinDate），iHR 必填的 `expectEntryDate` 来源 |

### 3.3 Flyway V26 脚本

文件路径：`cacch-integration-web/src/main/resources/db/migration/V26__moka_entry_candidate.sql`

```sql
-- =============================================
-- V26: Moka 待入职候选人快照表（5 张表）+ iHR 同步状态表
--   第一步：Moka → 本地快照 + 补全
--     1a. list_by_condition → 主表 + 3 子表（department_code/check_in_date 为 null）
--     1b. jobs/getJobs     → 主表 moka_department_code 回写
--     1c. v3/getOfferExtendInfos → 主表 check_in_date/offer_department_code 回写
--   第二步：本地快照 → iHR entryForms/create
--
--   表拆分：
--     主表   t_integration_moka_entry_application        applicationId 级别 + 补全字段
--     子表1  t_integration_moka_entry_candidate_base    CandidateBaseInfo + ContactInfo
--     子表2  t_integration_moka_entry_graduate_school   GraduateSchool
--     子表3  t_integration_moka_entry_citizen           Citizen
--     状态表 t_integration_moka_entry_sync              iHR 同步状态
-- =============================================


-- ---------- 主表：Moka 申请级别字段 + 补全字段 ----------
CREATE TABLE IF NOT EXISTS t_integration_moka_entry_application (
    -- 内部主键（MyBatis-Plus 雪花生成；application_id 为业务唯一）
    id                          BIGINT       NOT NULL,
    -- ========== Moka ApplicationItem 顶层字段 ==========
    application_id              INTEGER      NOT NULL,   -- 申请 ID（Moka 业务主键，UNIQUE）
    candidate_id                INTEGER,                  -- 候选人 ID
    hire_mode                   INTEGER,                  -- 招聘模式：1=社招 2=校招
    applied_at                  TIMESTAMP,                -- 申请时间（毫秒时间戳 → TIMESTAMP）
    moved_at                    TIMESTAMP,                -- 移动阶段时间（毫秒时间戳 → TIMESTAMP）
    moka_created_at             TIMESTAMP,                -- Moka 侧创建时间（毫秒时间戳）
    moka_updated_at             TIMESTAMP,                -- Moka 侧更新时间（毫秒时间戳）
    -- 职位信息（Job）—— 含原始部门名
    moka_job_id                 VARCHAR(64),              -- Moka 职位 ID（补全阶段用于查职位详情）
    moka_job_title              VARCHAR(128),             -- Moka 职位名称
    moka_job_department         VARCHAR(128),             -- Moka 职位原始部门名（list_by_condition 返回）
    -- 流程信息（Pipeline）
    moka_pipeline_id            INTEGER,                  -- Moka 流程 ID
    moka_pipeline_name          VARCHAR(128),             -- Moka 流程名称
    -- 阶段信息（Stage）
    moka_stage_id               INTEGER,                  -- Moka 阶段 ID
    moka_stage_name             VARCHAR(128),             -- Moka 阶段名称
    moka_stage_type             INTEGER,                  -- Moka 阶段类型：102=待入职
    -- 申请所有者（OwnerInfo）
    moka_owner_id               INTEGER,                  -- 申请所有者 ID
    moka_owner_name             VARCHAR(64),              -- 申请所有者姓名
    -- ========== 补全字段（Step 1b/1c 回写） ==========
    -- Step 1b：职位详情接口补全
    moka_department_code        VARCHAR(64),              -- 职位所属部门编码（code）—— 第二步匹配 iHR 部门的关键字段
    -- Step 1c：Offer 扩展接口补全
    check_in_date               TIMESTAMP,                -- 入职日期（checkinDate）—— iHR expectEntryDate 来源
    offer_department_code       VARCHAR(64),              -- Offer 入职部门编码（备选，通常比职位部门更准确）
    offer_department_name       VARCHAR(128),             -- Offer 入职部门名称
    offer_approval_status       VARCHAR(32),              -- Offer 审批状态：UNINITIATED/INPROGRESS/APPROVED/REJECTED/RECALLED
    -- ========== 归档原因（ArchiveReasons）—— 待入职阶段通常为 null ==========
    archive_reason_id           INTEGER,
    archive_reason_type         INTEGER,
    archive_reason_name         VARCHAR(128),
    archived_at                 TIMESTAMP,
    -- ========== 审计/辅助字段 ==========
    sync_batch                  VARCHAR(64),              -- 同步批次号（便于追溯）
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                  SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_entry_application PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_entry_application_appid UNIQUE (application_id)
);

COMMENT ON TABLE  t_integration_moka_entry_application IS 'Moka待入职候选人-申请主表（含补全的部门编码/入职日期）';
COMMENT ON COLUMN t_integration_moka_entry_application.application_id      IS 'Moka 申请 ID（业务唯一键）';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_job_id        IS 'Moka 职位 ID（用于 Step 1b 查职位详情补全部门编码）';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_job_department IS 'Moka 职位原始部门名（list_by_condition 返回）';
COMMENT ON COLUMN t_integration_moka_entry_application.moka_department_code IS '【Step 1b 补全】职位所属部门编码（code），第二步匹配 iHR 部门的关键字段';
COMMENT ON COLUMN t_integration_moka_entry_application.check_in_date       IS '【Step 1c 补全】入职日期（checkinDate），iHR expectEntryDate 来源';
COMMENT ON COLUMN t_integration_moka_entry_application.offer_department_code IS '【Step 1c 补全】Offer 入职部门编码（备选，通常比职位部门更准确）';
COMMENT ON COLUMN t_integration_moka_entry_application.offer_approval_status IS '【Step 1c 补全】Offer 审批状态';

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_entry_app_cand
    ON t_integration_moka_entry_application(candidate_id) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_entry_app_job
    ON t_integration_moka_entry_application(moka_job_id) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_entry_app_dept_code
    ON t_integration_moka_entry_application(moka_department_code) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_entry_app_sync_batch
    ON t_integration_moka_entry_application(sync_batch) WHERE is_deleted = 0;


-- ---------- 子表 1：候选人基础信息 + 联系方式 ----------
CREATE TABLE IF NOT EXISTS t_integration_moka_entry_candidate_base (
    id                          BIGINT       NOT NULL,
    application_id              INTEGER      NOT NULL,   -- 关联主表 application_id（UNIQUE，1:1）
    moka_name                   VARCHAR(64),              -- 候选人姓名
    moka_gender                 INTEGER,                  -- 性别：1=男 2=女
    moka_age                    INTEGER,                  -- 年龄
    moka_birth_date             TIMESTAMP,                -- 出生日期
    moka_nationality            VARCHAR(64),              -- 国籍
    moka_experience             INTEGER,                  -- 工作经验
    moka_candidate_created_at   TIMESTAMP,                -- 候选人创建时间
    moka_candidate_archived     BOOLEAN,                  -- 是否入库
    moka_candidate_archived_at  TIMESTAMP,                -- 候选人归档时间
    moka_is_blacklist           BOOLEAN,                  -- 是否黑名单
    -- ContactInfo（1:1 合入）
    moka_contact_phone          VARCHAR(32),              -- 手机号（敏感，日志脱敏）
    moka_contact_email          VARCHAR(128),             -- 邮箱
    moka_country_calling_code   VARCHAR(16),              -- 国家呼叫代码
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                  SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_entry_candidate_base PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_entry_candidate_base_appid UNIQUE (application_id)
);

COMMENT ON TABLE  t_integration_moka_entry_candidate_base IS 'Moka待入职候选人-基础信息（含联系方式）';
COMMENT ON COLUMN t_integration_moka_entry_candidate_base.application_id      IS '关联主表 application_id（1:1）';
COMMENT ON COLUMN t_integration_moka_entry_candidate_base.moka_contact_phone IS '手机号（敏感，日志脱敏）';


-- ---------- 子表 2：毕业院校 ----------
CREATE TABLE IF NOT EXISTS t_integration_moka_entry_graduate_school (
    id                          BIGINT       NOT NULL,
    application_id              INTEGER      NOT NULL,   -- 关联主表 application_id（UNIQUE，1:1）
    last_speciality             VARCHAR(128),             -- 毕业专业
    last_school                 VARCHAR(128),             -- 毕业学校
    graduate_date               INTEGER,                  -- 毕业时间（年份）
    academic_degree             INTEGER,                  -- 学历：1=其他 2=初中及以下 3=高中 4=中专 5=大专 6=本科 7=硕士 8=MBA 9=博士
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                  SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_entry_graduate_school PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_entry_graduate_school_appid UNIQUE (application_id)
);

COMMENT ON TABLE  t_integration_moka_entry_graduate_school IS 'Moka待入职候选人-毕业院校';


-- ---------- 子表 3：证件信息 ----------
CREATE TABLE IF NOT EXISTS t_integration_moka_entry_citizen (
    id                          BIGINT       NOT NULL,
    application_id              INTEGER      NOT NULL,   -- 关联主表 application_id（UNIQUE，1:1）
    citizen_id                  VARCHAR(64),              -- 证件号码（敏感，日志脱敏）
    certificate_type            INTEGER,                  -- 证件类型：1=身份证 2=香港身份证 3=澳门身份证 4=台湾身份证 5=护照 6=其他证件
    id_card_sign_organization  VARCHAR(128),             -- 签发机关
    id_card_validity            VARCHAR(128),             -- 证件有效期（原始 JSON 数组存 VARCHAR）
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted                  SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_entry_citizen PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_entry_citizen_appid UNIQUE (application_id)
);

COMMENT ON TABLE  t_integration_moka_entry_citizen IS 'Moka待入职候选人-证件信息';
COMMENT ON COLUMN t_integration_moka_entry_citizen.citizen_id IS '证件号码（敏感，日志脱敏）';


-- ---------- 同步状态表 ----------
CREATE TABLE IF NOT EXISTS t_integration_moka_entry_sync (
    id                      BIGINT       NOT NULL,
    application_id          INTEGER      NOT NULL,   -- 关联主表 application_id（UNIQUE）
    ihr_sync_status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    ihr_entry_id            VARCHAR(64),
    ihr_sync_message        VARCHAR(500),
    ihr_sync_time           TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted              SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_entry_sync PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_entry_sync_appid UNIQUE (application_id)
);

COMMENT ON TABLE  t_integration_moka_entry_sync IS 'Moka待入职同步状态表（iHR+未来多系统扩展）';

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_entry_sync_status
    ON t_integration_moka_entry_sync(ihr_sync_status) WHERE is_deleted = 0;
```

---

## 四、代码改动清单

### 4.1 模块总览

```
common  ─── 常量（已有 JOB_LIST_PATH / OFFER_EXTEND_INFO_PATH）+ 枚举
dao     ─── 5 张表 DO + Mapper + Flyway V26
service ─── ① 分页拉取 + 幂等 upsert  ② 职位详情批量补全  ③ Offer 扩展批量补全
manager ─── 第一步三阶段编排（1a→1b→1c）+ 第二步 iHR 推送编排
integration ── 新增 2 个 Client（MokaJobClient / MokaOfferClient）+ 复用现有
web     ─── Controller + Converter
```

### 4.2 按模块拆解

#### common 模块

| 文件 | 改动 |
|------|------|
| `constant/moka/MokaConstants` | **已有** `JOB_LIST_PATH` 和 `OFFER_EXTEND_INFO_PATH`，新增：`PIPELINE_ID_ENTRY_1=163155`、`PIPELINE_ID_ENTRY_2=163156`、`STAGE_ID_ENTRY=8`、`ENTRY_SYNC_BATCH_PREFIX="MOKA_ENTRY_"`、`STAGE_PENDING_CHECKIN="pending_checkin"` |
| 新增 `constant/moka/MokaEntrySyncStatusEnum` | 枚举：`PENDING / SUCCESS / FAILED / SKIPPED` |

#### dao 模块

| 文件 | 对应表 | 说明 |
|------|--------|------|
| `entity/moka/MokaEntryApplicationDO` | 主表 | ApplicationItem 顶层 + 补全字段 |
| `entity/moka/MokaEntryCandidateBaseDO` | 子表 1 | CandidateBaseInfo + ContactInfo |
| `entity/moka/MokaEntryGraduateSchoolDO` | 子表 2 | GraduateSchool |
| `entity/moka/MokaEntryCitizenDO` | 子表 3 | Citizen |
| `entity/moka/MokaEntrySyncDO` | 状态表 | iHR 同步状态 |
| `mapper/moka/MokaEntryApplicationMapper` | 主表 | upsert + 自定义 `updateDepartmentCodeById`、`updateOfferFieldsById` 回写方法 |
| `mapper/moka/MokaEntryCandidateBaseMapper` | 子表 1 | upsert |
| `mapper/moka/MokaEntryGraduateSchoolMapper` | 子表 2 | upsert |
| `mapper/moka/MokaEntryCitizenMapper` | 子表 3 | upsert |
| `mapper/moka/MokaEntrySyncMapper` | 状态表 | upsert + `selectPending()` |

#### integration 模块（**新增 2 个 Client**）

| 文件 | 说明 |
|------|------|
| **新增** `moka/client/MokaJobClient` | 调 `GET /api-platform/v1/jobs/getJobs`，按 jobId 批量查职位详情，取 `departmentCode` |
| **新增** `moka/client/dto/MokaJobResponse` | 职位查询响应 DTO |
| **新增** `moka/client/dto/MokaJobRequest` | 职位查询请求 DTO |
| **新增** `moka/client/MokaOfferClient` | 调 `POST /api-platform/v3/getOfferExtendInfos`，按 applicationId 批量查 offer 扩展，取 `checkInDate`、`departmentCode` |
| **新增** `moka/client/dto/MokaOfferExtendResponse` | Offer 扩展响应 DTO |
| **新增** `moka/client/dto/MokaOfferExtendRequest` | Offer 扩展请求 DTO |
| **复用** `MokaCandidatesClient` | `listApplications()`，Step 1a 复用 |

#### service 模块

| 文件 | 职责 |
|------|------|
| 新增 `moka/api/IMokaEntryCandidateService` + `impl` | ① 构造固定参数分页拉取 Step 1a；② 4 表幂等 upsert（含状态表）；③ 回写 department_code（Step 1b）；④ 回写 check_in_date + offer_department_code（Step 1c）；⑤ 主表 JOIN 子表 JOIN 状态表查待推送记录 |
| **已有** `ihr/api/IIhrStaffEntryFormService` | `createEntryForm()` 复用，无改动 |

#### manager 模块

| 文件 | 职责 |
|------|------|
| 新增 `moka/api/IMokaEntryCandidateSyncManager` + `impl` | **第一步三阶段编排**：<br>**1a** 分页拉取 + 4 表 upsert（department_code/check_in_date 为 null）→ 返回 jobId 去重集合<br>**1b** 按 jobId 批量查职位详情 → UPDATE 主表 moka_department_code<br>**1c** 按 applicationId 批量查 Offer 扩展 → UPDATE 主表 check_in_date/offer_department_code<br>→ 返回批次统计 |
| 新增 `moka/api/IMokaEntryCandidatePushManager` + `impl` | **第二步**：主表 JOIN 子表 JOIN 状态表 → 查 iHR 部门表匹配 → 字段映射 → 调 iHR createEntryForm → 更新同步状态 |

#### web 模块

| 文件 | 路由 | 说明 |
|------|------|------|
| 新增 `controller/moka/MokaEntryCandidateController` | `POST /api/v1/moka/entry-candidates/sync` | 第一步（含 1a+1b+1c 三阶段），无入参 |
| 同上 | `POST /api/v1/moka/entry-candidates/push-ihr` | 第二步，无入参 |
| 新增 `convert/moka/MokaEntryCandidateConverter` | — | 转换 1：`ApplicationItem` → 4 个 DO；转换 2：主表+子表+iHR 部门匹配 → `IhrStaffEntryFormCreateRequest` |

---

## 五、关键字段映射

### 5.1 第一步 Step 1a：Moka list_by_condition → 本地快照

主表 `t_integration_moka_entry_application`：

| 列名 | Moka 响应来源 | 转换说明 |
|------|-------------|---------|
| `application_id` | `ApplicationItem.applicationId` | INTEGER，UNIQUE |
| `candidate_id` | `candidateBaseInfo.candidateId` | INTEGER |
| `hire_mode` | `ApplicationItem.hireMode` | 1=社招 2=校招 |
| `moka_job_id` | `job.jobId` | String UUID（**Step 1b 补全的关键**） |
| `moka_job_title` | `job.jobTitle` | 直接映射 |
| `moka_job_department` | `job.department` 或 stage 相关 | 原始部门名（**list_by_condition 可能不返回，需实际验证**） |
| `moka_department_code` | — | **Step 1a 为 null，Step 1b 回写** |
| `check_in_date` | — | **Step 1a 为 null，Step 1c 回写** |
| `sync_batch` | Manager 生成 | 格式 `MOKA_ENTRY_yyyyMMddHHmmss` |

子表映射略（v1.1 文档已详述）。

### 5.2 第一步 Step 1b：职位详情接口补全

接口：`GET /api-platform/v1/jobs/getJobs`（常量已有 `JOB_LIST_PATH`）

| 主表列 | 补全来源 | 转换说明 |
|--------|---------|---------|
| `moka_department_code` | 职位详情响应的 `departmentCode` | 按 `moka_job_id` 去重后批量查，UPDATE 主表 |

**去重逻辑**：主表 `GROUP BY moka_job_id` 收集去重 jobId → 批量调职位接口 → Map<jobId, departmentCode> → UPDATE 主表 SET moka_department_code = map[jobId] WHERE moka_job_id = jobId

### 5.3 第一步 Step 1c：Offer 扩展接口补全

接口：`POST /api-platform/v3/getOfferExtendInfos`（常量已有 `OFFER_EXTEND_INFO_PATH`）

| 主表列 | 补全来源 | 转换说明 |
|--------|---------|---------|
| `check_in_date` | Offer 扩展响应的 `checkinDate` | ISO8601 格式（`2018-09-20T13:00:07.000Z`）→ LocalDate / TIMESTAMP |
| `offer_department_code` | Offer 扩展响应的 `offer.departmentCode` | 数字类型 → VARCHAR |
| `offer_department_name` | Offer 扩展响应的 `offer.departmentName` | 直接映射 |
| `offer_approval_status` | Offer 扩展响应的 `offer.approvalStatus` | UNINITIATED/INPROGRESS/APPROVED/REJECTED/RECALLED |

**批量逻辑**：收集主表所有 `application_id` → 按接口 `applicationIds` 参数批量传入 → UPDATE 主表。

### 5.4 第二步：本地快照 → iHR 待入职

| iHR 字段 | 必填 | 本地表来源 | 转换说明 |
|---------|------|----------|---------|
| `staffName` | ✅ | `candidate_base.moka_name` | 直接映射 |
| `mobile` | ✅ | `candidate_base.moka_contact_phone` | 敏感，脱敏日志 |
| `expectEntryDate` | ✅ | `application.check_in_date` | Step 1c 补全的 TIMESTAMP → `yyyy-MM-dd` |
| `departmentId` | ✅ | `application.moka_department_code` **JOIN** `t_integration_ihr_department` | 用 `department_code` 匹配 iHR 部门编码，取 `ihr_dept_id` |
| `sex` | ❌ | `candidate_base.moka_gender` → `MALE`/`FEMALE` | Integer→String |
| `idCardType` | ❌ | `citizen.certificate_type` → iHR 枚举 | Integer→iHR 枚举 |
| `idCardNo` | ❌ | `citizen.citizen_id` | 敏感，脱敏日志 |
| `email` | ❌ | `candidate_base.moka_contact_email` | 直接映射 |
| `positionId` | ❌ | JOIN `t_integration_ihr_position` 按 `moka_job_title` 匹配 | 匹配不到不传（iHR 可空） |
| `remark` | ❌ | 主表拼接 | `pipeline:xxx, stage:xxx` |

---

## 六、同步状态流转

```
第一步（快照 + 补全）
  ├─ 1a INSERT → 主表 department_code=null, check_in_date=null
  │             → 状态表 ihr_sync_status='PENDING'
  ├─ 1b UPDATE → 主表 moka_department_code 回写
  └─ 1c UPDATE → 主表 check_in_date / offer_department_code 回写

第二步（推送 iHR）
  ├─ PENDING ── iHR 成功 ──▶ SUCCESS（回填 ihr_entry_id）
  ├─ PENDING ── iHR 业务码非 0 ──▶ FAILED
  ├─ PENDING ── department_code 匹配不到 ──▶ SKIPPED
  ├─ PENDING ── check_in_date 为 null ──▶ SKIPPED（Step 1c 未补全成功）
  └─ FAILED ── 重新触发第二步 ──▶ Manager 只查 PENDING，FAILED 需手动重置
```

---

## 七、事务控制

| 层级 | 事务声明 | 说明 |
|------|---------|------|
| `MokaEntryCandidateSyncManagerImpl` | 分批 upsert + 回写；1a/1b/1c 三阶段各自独立事务 | 每阶段完成后提交，避免长事务跨阶段 |
| `MokaEntryCandidatePushManagerImpl` | 状态更新 `REQUIRES_NEW`；iHR HTTP 调用在事务外 | DB 写操作加事务，HTTP 禁止加事务 |
| `Controller` | **禁止** `@Transactional` | — |

---

## 八、风险与处理策略

| 风险 | 处理策略 |
|------|---------|
| **性能**：每条 application 额外 2 次 HTTP 调用（职位 + offer） | 职位接口按 **jobId 去重**（多条共享同一 job 只查一次）；Offer 扩展接口按 applicationIds **批量传入**（减少请求次数） |
| Moka QPS 限制 | 批量调用间加 sleep（如每 50 条调用间隔 100ms） |
| `moka_job_id` 可能为 null | Step 1a 入库时 moka_job_id=null 的记录，Step 1b 自动跳过，department_code 保持 null，第二步标 SKIPPED |
| `check_in_date` 可能为 null（offer 未发送） | Step 1c 回写失败时，第二步 SKIPPED 并记 INFO 日志 |
| `moka_department_code` vs `offer_department_code` 哪个更准 | Offer 扩展接口的 `offer.departmentCode` 是入职部门，通常比职位接口更准确；第二步优先用 `offer_department_code`，null 时回退到 `moka_department_code` |
| 4 表 upsert 需保持 1:1 一致性 | 同一 application 的主表 + 3 子表 + 状态表 upsert 放在同一批次事务内 |

---

## 九、待确认事项（**已全部解决** ✅）

| # | 问题 | 解决方案 |
|---|------|---------|
| 1 | ~~expectEntryDate 来源~~ | ✅ **Offer 扩展接口 `checkinDate`**（Step 1c 补全） |
| 2 | ~~Moka 部门名/部门编码来源~~ | ✅ **职位详情接口 `departmentCode`**（Step 1b 补全）+ Offer 扩展 `offer.departmentCode`（备选） |

**两个补全接口的 API 路径已在项目 `MokaConstants` 中预定义**：

```java
// constant/moka/MokaConstants
public static final String JOB_LIST_PATH = "/api-platform/v1/jobs/getJobs";           // Step 1b 用
public static final String OFFER_EXTEND_INFO_PATH = "/api-platform/v3/getOfferExtendInfos"; // Step 1c 用
```

---

## 十、验证方式

| 阶段 | 验证项 |
|------|--------|
| 编译 | `mvn clean compile`，无 MapStruct 缺失 |
| Flyway | test 环境启动，V26 脚本正常执行，5 张表结构正确 |
| 第一步 1a | 快照表 + 状态表有数据；department_code=null、check_in_date=null；幂等 |
| 第一步 1b | 主表 moka_department_code 已回写；jobId 去重生效（同一 job 不重复查接口） |
| 第一步 1c | 主表 check_in_date + offer_department_code 已回写 |
| 第二步 | 状态 PENDING → SUCCESS/FAILED/SKIPPED；iHR 侧能看到新增待入职；department_code 匹配正确 |
| 全链路 | 重复触发两步接口，验证幂等 + FAILED 重试 |
