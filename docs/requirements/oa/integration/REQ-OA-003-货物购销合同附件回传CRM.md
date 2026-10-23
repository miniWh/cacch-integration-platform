# REQ-OA-003 OA货物购销合同附件回传CRM合同

---

## 1. 基本信息

| 项目 | 内容 |
|------|------|
| 需求编号 | REQ-OA-003 |
| 需求标题 | 提供执行接口，将 OA 货物购销合同附件地址回传至 CRM 合同（由 ESB 定时调用） |
| 需求类型 | integration |
| 所属系统 | oa / crm |
| 优先级 | P1(高) |
| 状态 | draft |
| 提出人 | 张仁杰 |
| 提出日期 | 2026-10-23 |
| 负责人 | 周洪福 |
| 预计上线 | 待定 |
| **关联需求** | 无 |

---

## 2. 需求背景

内部购销合同在致远 OA 中以 CAP4 表单 `formmain_5296` 维护，合同审批完成后 OA 自动生成带电子印章图片的合同附件（附件字段 `field0360`），同时表单上登记 NC 销售订单号（`field0273`）。CRM 侧合同由 CRM 订单关联生成，但 OA 侧生成的合同附件**不会自动同步回 CRM**，当前依赖人工下载后再上传到 CRM 合同，存在重复劳动、遗漏滞后、无法追溯等问题。

**上下游链路**：

```
CRM（勤策）
  │ ① 推送自有销售订单
  ▼
OA 销售订单表单 formmain_2817（field0329 = NC销售订单号，field0493 = 销售订单来源）
  │ ② 自动生成
  ▼
OA 货物购销合同 formmain_5296（field0273 = 同一 NC销售订单号）
  │ ③ 业务员审批
  ▼
OA 自动生成带电子印章图片的合同附件（field0360）
  │ ④ 本需求回传
  ▼
CRM 合同（修改销售合同接口 contractEdit）
```

**关键前置**：查询 SQL 已通过 `formmain_2817.field0493 = 'crm订单'` 完成「CRM 订单来源」判断，**不再反查 CRM 订单做存在性校验**，业务链路直接调用 CRM 修改销售合同接口。

**执行方式**：本需求不实现 `@Scheduled` 定时任务，调度统一由 ESB 负责；中台暴露 REST 执行接口，并把 SQL 查询结果落入**中间记录表**，每次执行只处理中间表中未执行的数据。

> 附件由 OA 在审批完成后**自动生成并已带电子印章图片**，中台只做搬运，不参与盖章与文件加工，本期也不做文件转存（Q2）。

---

## 3. 需求描述

### 3.1 功能点

| 序号 | 功能点 | 描述 | 必选/可选 |
|------|--------|------|-----------|
| F1 | 执行接口（ESB 定时调用） | 中台提供 REST 执行接口，**不做本地定时任务**，由 ESB 按既定频率调用；接口同步返回本次执行统计 | 必选 |
| F2 | 增量扫描 OA 购销合同（NOT IN 中间表） | 先查中间表已入库的 `oa_form_main_id`，再用 `NOT IN` 只拉取**尚未入中间表**的符合条件的合同：`FINISHEDFLAG=1`、`field0273` 非空、`field0360` 非空、且 `formmain_2817.field0329 = field0273` 与 `field0493 = 'crm订单'` 同时满足；NOT IN 列表分片（每片 ≤ 1000，规避 Oracle ORA-01795） | 必选 |
| F3 | 中间记录表落库 | 将 SQL 查询出的订单/合同数据（主表ID、流程发起时间、流程状态、NC销售订单号、销售订单编号、合同附件）写入**中间记录表**；已存在记录不覆盖同步状态 | 必选 |
| F4 | 取未执行数据 | 每次执行只处理中间记录表中 `PENDING` / `RETRY` 的数据，**不设失败次数上限**（`retry_count` 仅用于观测） | 必选 |
| F5 | 获取 OA 合同附件 | 用 `field0360` 关联 `ctp_attachment.sub_reference` 取附件 ID、文件名、文件标识，拼装 OA 附件访问地址（一个合同只有一个附件） | 必选 |
| F6 | 回传 CRM 合同 | 调用 CRM **修改销售合同**接口（`contractEdit`），将附件地址写入合同自定义字段 | 必选 |
| F7 | 幂等与去重 | 同一 OA 合同只回传一次；已成功记录不再调用 CRM | 必选 |
| F8 | 并发处理 | 使用线程池并发处理多笔订单（订单间无关联）；数据库**乐观锁**按记录抢占，防止高频调用时重复执行 | 必选 |
| F9 | 失败记录、无上限重试与告警 | 失败即回写失败原因并**立即推送企微 Webhook 告警**；记录保持 `RETRY` 可重试状态，每次执行自动重试直至成功，**不设失败次数上限、不转终态** | 必选 |
| F10 | 同步状态查询 | 提供 REST 接口分页查询中间记录表同步状态（含状态、跳过/失败原因、重试次数、CRM 合同标识） | 必选 |
| F11 | 单条重置重试 | 支持按记录 ID 或 `nc_order_no` 将 SKIPPED 等非 `SUCCESS` 记录重置为待处理，便于人工补偿 | 可选 |

### 3.2 交互流程

```
ESB 定时调用执行接口 POST /api/v1/oa/contract-attachments/execute
    ├─ 获取 Redis 执行锁失败（已有执行中的任务）→ 直接返回「执行中」，本次跳过
    ├─ 回收超时 PROCESSING 记录（last_sync_at 超阈值 → RETRY）
    └─ 获取成功 → 继续
    ↓
阶段一：增量扫描 OA 购销合同（显式传入 NOT IN 排除列表）
    ├─ 查询中间表已入库的 oa_form_main_id
    ├─ 按 scan-chunk-size（≤1000）分片，逐片查询 OA：
    │      formmain_5296 + EXISTS formmain_2817（来源 = crm订单）
    │      AND fm.ID NOT IN ( ? 已入库 ID 分片 )
    │  （中间表为空时不下发 NOT IN 条件）
    └─ 按 oa_form_main_id 幂等 UPSERT 到中间记录表
           ├─ 新记录 → PENDING
           └─ 已存在 → 仅刷新 OA 快照字段，不覆盖 sync_status / retry_count
    ↓
阶段二：捞取未执行数据并并发处理
    ├─ 查询中间表 sync_status IN (PENDING, RETRY)，限制 batch-size（无失败次数上限）
    ├─ 逐条乐观锁抢占：UPDATE ... SET sync_status='PROCESSING', version=version+1
    │                     WHERE id=? AND version=? AND sync_status IN ('PENDING','RETRY')
    ├─ 抢占成功 → 提交线程池并发执行（订单间无依赖）
    │      ├─ ① 用附件引用（field0360）查 OA 附件
    │      │      ├─ 无附件 / 文件标识为空 → SKIPPED（OA附件缺失）
    │      │      └─ 命中 → 得到 fileId / 文件名 / 附件地址
    │      ├─ ② 调用 CRM 修改销售合同接口（data_id = CRM合同ID）
    │      │      ├─ return_code = 0 → SUCCESS
    │      │      └─ return_code ≠ 0 → RETRY + 回写失败原因 + 企微告警
    │      └─ 单条异常不阻断其他任务（同样 RETRY + 告警）
    └─ 等待本批任务完成（含整体超时保护），汇总统计
    ↓
阶段三：返回本次执行统计
    ↓
释放 Redis 执行锁
```

### 3.3 已确认决策与待确认项

**已确认**

| # | 事项 | 结论 |
|---|------|------|
| D1 | CRM 订单校验 | 查询 SQL 已用 `field0493 = 'crm订单'` 判定来源，**不再反查 CRM 订单**，直接调用 CRM 修改销售合同接口 |
| D2 | CRM 合同修改接口 | `POST /api/ig/v1/contractEdit/{openid}/{timestamp}/{digest}/{msg_id}`，参数与响应见 6.3 |
| D3 | OA 附件 | 本期只要求「能通过接口获取到 OA 附件」，**不做文件转存**，CRM 侧仅写入附件地址；转存方案后续确认 |
| D4 | 扫描策略 | 先查中间表已入库 `oa_form_main_id`，用 **`NOT IN` 分片（≤1000）**只查询未入库的合同，避免每次全量扫描 OA；执行阶段只取中间表未执行数据 |
| D5 | 附件数量 | 一个合同只有一个附件 |
| D6 | `FINISHEDFLAG` 类型 | 按数值取值，`= 1` |
| D7 | `field0493` | 销售订单来源字段，条件由「非空」改为 `= 'crm订单'` |
| D8 | 并发策略 | 线程池并发处理多笔订单 + 数据库乐观锁，保证高频调用下不重复执行 |
| D9 | 提出人 / 负责人 | 张仁杰 / 周洪福 |
| D10 | 失败处理 | **不设失败次数上限**：失败即回写原因并告警，记录保持 `RETRY`，后续执行持续重试 |
| D11 | ESB 调度频率 | **每 10 分钟执行一次**（Q11 已确认） |

**暂定项（不阻塞编码，按配置占位，联调确认后调整）**

| # | 暂定项 | 当前方案 | 影响 |
|---|--------|---------|------|
| Q9 | **CRM 合同 `data_id` 的来源**：`contractEdit` 必填 `data_id`，当前 SQL 未返回该值 | 暂按配置项 `crm.contract.data-id-field` 指定 OA 表单字段，未配置时以中间表 `crm_contract_id` 人工维护值兜底；同时预留扩展字段 | 影响 F6 实际可用，但不阻塞编码 |
| Q10 | CRM 合同**附件自定义字段的字段 key** | 暂以配置项 `crm.contract.attachment-field-key` 占位，联调时按 fieldList 结果填入 | 影响附件地址写入位置 |
| Q12 | `fields` / `slave_fields` 传输格式 | 按官方示例传 JSON 对象；`slave_fields` 无明细时传 `{}` | 影响请求体与签名原文 |

---

## 4. 业务流程

### 4.1 数据流

```
          ESB                    致远 OA（Oracle，只读）        集成中台（PG + Redis + 线程池）        勤策 CRM
           │                              │                              │                             │
           │ ⓪ 按频率调用执行接口          │                              │                             │
           │ ────────────────────────────→│                              │                             │
           │                              │ ① NOT IN 增量扫描 5296        │                             │
           │                              │    + 2817（来源=crm订单）     │                             │
           │                              │ ←──────────────────────────── │                             │
           │                              │                              │ ② 中间表幂等 UPSERT          │
           │                              │                              │    新记录 PENDING            │
           │                              │                              │ ③ 乐观锁抢占 + 线程池并发    │
           │                              │ ④ ctp_attachment 取附件       │                             │
           │                              │ ←──────────────────────────── │                             │
           │                              │                              │ ⑤ contractEdit 回传附件地址   │
           │                              │                              │ ───────────────────────────→ │
           │                              │                              │ ←──── return_code=0 / 1 ──── │
           │                              │                              │ ⑥ 回写 SUCCESS/SKIPPED/RETRY │
           │ ←──────── 本次统计结果 ───────│                              │                             │
```

### 4.2 状态机

| 状态 | 含义 | 后续动作 |
|------|------|----------|
| PENDING | 已扫描登记，待处理 | 当前或下次执行时被抢占处理 |
| PROCESSING | 已被线程池任务抢占，正在处理 | 超过 `processing-timeout-minutes` 未更新则回收为 RETRY |
| SUCCESS | CRM 回传成功 | 终态，不再处理 |
| SKIPPED | 业务性跳过（OA 附件缺失等） | 终态；人工确认后可重置重试 |
| RETRY | 执行失败（含重试失败），已记录原因并告警 | **无次数上限**，每次执行持续重试直至 SUCCESS |

> 不设 `FAILED` 终态：失败不会被"放弃"，而是持续重试 + 每失败一次告警一次，避免数据无限期滞留。

### 4.3 幂等与并发

- 中间记录表唯一键：`oa_form_main_id`（一个合同只有一个附件），UPSERT 时不覆盖 `sync_status` / `retry_count`；
  状态值为 `SUCCESS` 的记录永久跳过，杜绝重复回传；
- 记录级**乐观锁抢占**：`version` 字段配合 `WHERE id=? AND version=? AND status IN ('PENDING','RETRY')`，抢占成功才提交线程池；
- 进程级使用 **Redis 执行锁**（`integration:oa:contract-attachment:lock`，TTL = `lock-ttl-seconds`），ESB 重复/并发调用时未拿到锁直接返回「执行中」，避免重复扫描与重复回传；
- `PROCESSING` 僵尸记录回收：每次执行开始时，将 `sync_status='PROCESSING'` 且 `last_sync_at` 早于 `processing-timeout-minutes` 的记录置为 `RETRY`；
- 状态回写使用 `REQUIRES_NEW` 独立事务，单条失败不影响其他任务。

---

## 5. 涉及系统与模块

| 系统 | 模块/接口 | 说明 |
|------|-----------|------|
| 致远 OA | Oracle 只读库 `formmain_2817` | CRM 推送的销售订单表单；`field0329` 为 NC销售订单号、`field0493` 为销售订单来源（`crm订单`） |
| 致远 OA | Oracle 只读库 `formmain_5296` | 提供已完成购销合同、NC销售订单号、附件引用 |
| 致远 OA | Oracle 只读库 `ctp_attachment` | 按 `sub_reference = field0360` 取附件 fileId / 文件名 |
| 致远 OA | REST `/seeyon/rest/attachment/file/{fileId}` | 附件获取接口，用于拼装附件访问地址（本期不转存） |
| 勤策 CRM | 修改销售合同 `POST /api/ig/v1/contractEdit/{openid}/{timestamp}/{digest}/{msg_id}` | 回传合同附件地址（见 6.3） |
| ESB | 定时调用中台执行接口 | 调度方，按频率调用 `POST /api/v1/oa/contract-attachments/execute`，替代本地定时任务 |
| 集成中台 | PG `t_integration_oa_contract_attachment_sync`（中间记录表） | 存储 SQL 查询出的订单/合同数据、同步状态、乐观锁版本、重试、审计 |
| 集成中台 | Redis 执行锁 `integration:oa:contract-attachment:lock` | ESB 并发调用的互斥保护 |
| 集成中台 | 线程池 `oaContractAttachmentExecutor` | 多笔订单并发处理 |
| 集成中台 | 企微 Webhook | 达最大重试次数告警 |

### 5.1 现有能力复用

| 能力 | 现有实现 |
|------|----------|
| OA 只读数据源（强制只读） | `OaDataSourceConfiguration` / `ReadOnlyOaJdbcTemplate` |
| NC 单号查 OA 附件 | `OaAttachmentDbClient.findFirstAttachmentByNcNo` |
| CRM OpenAPI 签名与调用 | `CrmClient` / `CrmDigestSupport` / `RestTemplateHttpClient` |
| 中间状态表范式 | `OaRegAttachmentSyncDO` |
| 线程池配置范式 | `KeruyunAsyncConfig`（`ThreadPoolTaskExecutor`） |
| 批量结果对象 | `common.dto.batch.BatchProcessResult` |

---

## 6. 字段映射

### 6.1 OA → 中台中间记录表

| 源字段 | 目标字段 | 类型 | 转换规则 | 备注 |
|--------|----------|------|----------|------|
| `formmain_5296.ID` | `oa_form_main_id` | BIGINT | 直传 | 幂等唯一键 |
| `formmain_5296.START_DATE` | `oa_start_date` | TIMESTAMP | 直传 | 流程发起时间，原 SQL 排序列 |
| `formmain_5296.FINISHEDFLAG` | `flow_status` | VARCHAR(16) | `1 → 已完成`、`0 → 未完成`、其他 → `未知`（对齐原 SQL CASE） | 流程状态快照 |
| `formmain_5296.FIELD0273` | `nc_order_no` | VARCHAR(64) | TRIM | NC 销售订单号 |
| `formmain_5296.FIELD0104` | `oa_contract_no` | VARCHAR(64) | TRIM | 销售订单编号，便于排查 |
| `formmain_2817.FIELD0329` | 不落库（关联条件） | VARCHAR(2000) | 与 `field0273` 关联 | 确认合同来源于 CRM 推送的销售订单 |
| `formmain_2817.FIELD0493` | 不落库（过滤条件） | — | `= 'crm订单'` | 销售订单来源 |
| `formmain_5296.FIELD0360` | `oa_attachment_ref` | VARCHAR(64) | TRIM | 对应 `ctp_attachment.sub_reference` |
| `ctp_attachment.ID` | `oa_attachment_id` | VARCHAR(64) | 转文本 | 附件唯一标识 |
| `ctp_attachment.FILE_URL` | `attachment_file_id` | VARCHAR(255) | TRIM | OA 附件文件标识（fileId） |
| `ctp_attachment.FILENAME` | `attachment_file_name` | VARCHAR(255) | 直传 | |
| 拼装结果 | `attachment_url` | VARCHAR(1000) | `{oa.base-url}/seeyon/rest/attachment/file/{fileId}` | 回传 CRM 的地址 |

### 6.2 中间记录表 → CRM 合同

| 中台字段 | CRM 字段 | 说明 |
|----------|----------|------|
| `crm_contract_id` | `data_id`、`fields.id` | 合同数据 ID，来源见 Q9 |
| `attachment_url` | `fields.{附件自定义字段key}` | 附件地址（字段 key 见 Q10） |
| `attachment_file_name` | 可选，待确认 | 是否需要写入附件名称字段 |
| `sync_message` | — | 记录 CRM `return_code` / `return_msg` |

### 6.3 CRM 修改销售合同接口（contractEdit）

- **请求**：`POST https://openapi.qince.com/api/ig/v1/contractEdit/{openid}/{timestamp}/{digest}/{msg_id}`（域名与签名复用现有 `CrmClient`）
- **约束**：合同头仅支持修改**自定义字段**和部分预置字段（`contract_date`、`contract_title`、`valid_start_date`、`valid_end_date`、`customer` 等）；合同行（`slave_fields.ig_contract_detail`）暂不支持修改预置字段

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `data_id` | VARCHAR | 是 | 合同数据 ID |
| `fields` | VARCHAR | 是 | `{"字段key": 值}`，非特殊字段按附录六格式 |
| `slave_fields` | VARCHAR | 是 | `{"对象key": [{"字段key": 值}]}`；无明细修改时传 `{}` |
| `is_trigger` | boolean | 否 | 是否触发工作流，默认 `false` |
| `trigger_employee` | VARCHAR(32) | 否 | `is_trigger=true` 时必填 |
| `check_rule` | boolean | 否 | 是否校验验证规则与查重规则，默认 `false` |

- **响应**：`{"msg_id":"X000001","return_code":0,"return_msg":""}`，`return_code=0` 成功、`1` 失败
- **本需求请求体**：

```json
{
  "data_id": "{crmContractId}",
  "fields": {
    "id": "{crmContractId}",
    "{attachmentFieldKey}": "{oaAttachmentUrl}"
  },
  "slave_fields": {},
  "is_trigger": false,
  "check_rule": false
}
```

---

## 7. 技术方案

### 7.1 架构影响

按四层架构新增/改造，依赖方向保持不变（web → manager → service → dao/integration → common）。

| 模块 | 设计 |
|------|------|
| **common** | 新增 `constant.oa.OaContractAttachmentConstants`（业务标识、执行锁 Key、默认批量/扫描分片/超时、附件地址模式）、`constant.crm.CrmContractConstants`（`contractEdit` 路径、`data_id`/`fields`/`slave_fields` 字段名）、`config.oa.OaContractAttachmentSyncProperties`（批量、扫描分片、锁 TTL、超时回收、线程池参数、告警开关）、`config.crm.CrmContractProperties`（附件自定义字段 key）、`enums.oa.OaContractAttachmentSyncStatusEnum`（含 PROCESSING）、`dto.oa.OaContractAttachmentSyncResult`（继承 `BatchProcessResult`） |
| **dao** | 新增 `entity.oa.OaContractAttachmentSyncDO`（含 `version` 乐观锁字段）、`mapper.oa.OaContractAttachmentSyncMapper`（UPSERT、CAS 抢占、超时回收、状态回写） |
| **integration** | 新增 `oa.client.OaContractDbClient`（按 NOT IN 排除列表分批扫描合同 + 按附件引用查附件）、`oa.client.dto.OaContractRow`；`crm.client.CrmClient` 增加 `contractEdit()`，新增 `crm.client.dto.CrmContractEditRequest/Response` |
| **service** | 新增 `service.oa.api.IOaContractAttachmentSyncService` 及实现（中间表 UPSERT、按状态取待处理、CAS 抢占、状态回写、分页查询、重置重试）、`IOaContractAttachmentSyncLockService`（Redis 执行锁） |
| **manager** | 新增 `manager.oa.api.IOaContractAttachmentSyncManager` 及实现（加锁 → 扫描 UPSERT → 回收僵尸 → 抢占 → 线程池并发 → CRM 回传 → 告警 → 统计） |
| **async** | **不新增**。本需求不做本地定时任务，调度由 ESB 负责；线程池 Bean 在 web 配置层注册 |
| **web** | 新增 `controller.oa.OaContractAttachmentSyncController`（执行/查询/重置重试）、`config.oa.OaContractAttachmentAsyncConfig`（`oaContractAttachmentExecutor`）、`dto/convert` |

### 7.2 数据库变更

新增 Flyway 脚本 `V31__oa_contract_attachment_sync.sql`（当前最新为 V30，禁止修改历史脚本）。表定位为**中间记录表**：保存 SQL 查询出的订单/合同数据快照 + 同步状态 + 乐观锁版本：

```sql
CREATE TABLE IF NOT EXISTS t_integration_oa_contract_attachment_sync (
    id                   BIGINT       NOT NULL,
    oa_form_main_id      BIGINT       NOT NULL,
    oa_start_date        TIMESTAMP,
    flow_status          VARCHAR(16),
    nc_order_no          VARCHAR(64),
    oa_contract_no       VARCHAR(64),
    oa_attachment_ref    VARCHAR(64),
    oa_attachment_id     VARCHAR(64),
    attachment_file_id   VARCHAR(255),
    attachment_file_name VARCHAR(255),
    attachment_url       VARCHAR(1000),
    crm_contract_id      VARCHAR(64),
    sync_status          VARCHAR(16)  NOT NULL,
    skip_reason          VARCHAR(255),
    sync_message         VARCHAR(1000),
    retry_count          INT          NOT NULL DEFAULT 0,   -- 仅观测，不设上限
    version              INT          NOT NULL DEFAULT 0,
    last_sync_at         TIMESTAMP,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted           SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT pk_t_integration_oa_contract_attachment_sync PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_oa_contract_attachment_sync
    ON t_integration_oa_contract_attachment_sync (oa_form_main_id) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_oa_contract_attachment_sync_status
    ON t_integration_oa_contract_attachment_sync (sync_status, updated_at) WHERE is_deleted = 0;
CREATE INDEX IF NOT EXISTS idx_oa_contract_attachment_sync_nc
    ON t_integration_oa_contract_attachment_sync (nc_order_no) WHERE is_deleted = 0;
```

> 主键 `BIGINT` 由 MyBatis-Plus 雪花算法生成，不加 `SERIAL`；`sync_status` 取值见 `OaContractAttachmentSyncStatusEnum`；`version` 为乐观锁字段。

### 7.3 配置变更

`application-test.yml` / `application-prod.yml` 增加：

```yaml
oa:
  contract-attachment-sync:
    enabled: true                       # 紧急开关；false 时执行接口直接返回「已停用」
    batch-size: 200                     # 单次执行最多处理条数（ESB 10 分钟一次，需在该周期内完成）
    scan-chunk-size: 500                # NOT IN 分片大小，必须 ≤ 1000（Oracle ORA-01795）
    lock-ttl-seconds: 600               # 执行锁 TTL，需大于单次最长执行时间且不小于调度周期
    processing-timeout-minutes: 10      # PROCESSING 僵尸记录回收阈值
    alert-include-reason: true          # 失败告警是否携带失败原因
    attachment-url-mode: oa-rest        # 本期固定 oa-rest（不做文件转存）
    executor:
      core-pool-size: 4
      max-pool-size: 8
      queue-capacity: 200
      keep-alive-seconds: 60

crm:
  contract:
    data-id-field: ""                   # Q9 暂定：OA 表单承载 CRM 合同 ID 的字段，空则用中间表人工维护值
    attachment-field-key: ""            # Q10 暂定：CRM 合同附件自定义字段 key
```

> 无 cron 配置：调度频率与超时由 ESB 侧管控，已确认 **每 10 分钟调用一次**（D11）。

### 7.4 涉及代码

| 模块 | 新增/修改文件 | 说明 |
|------|--------------|------|
| common | `constant/oa/OaContractAttachmentConstants.java` | 新增：业务常量 |
| common | `constant/crm/CrmContractConstants.java` | 新增：`contractEdit` 接口常量 |
| common | `config/oa/OaContractAttachmentSyncProperties.java` | 新增：执行参数与线程池配置 |
| common | `config/crm/CrmContractProperties.java` | 新增：CRM 合同附件字段 key |
| common | `enums/oa/OaContractAttachmentSyncStatusEnum.java` | 新增：状态枚举 |
| common | `dto/oa/OaContractAttachmentSyncResult.java` | 新增：批量结果 |
| dao | `entity/oa/OaContractAttachmentSyncDO.java` | 新增：中间记录表实体（含 version） |
| dao | `mapper/oa/OaContractAttachmentSyncMapper.java` | 新增：UPSERT / CAS / 查询 |
| integration | `oa/client/OaContractDbClient.java` | 新增：NOT IN 分批扫描合同 + 按附件引用查附件 |
| integration | `oa/client/dto/OaContractRow.java` | 新增：合同行 DTO |
| integration | `crm/client/CrmClient.java` | 修改：新增 `contractEdit` |
| integration | `crm/client/dto/CrmContractEditRequest.java` | 新增：合同修改请求 |
| integration | `crm/client/dto/CrmContractEditResponse.java` | 新增：合同修改响应 |
| service | `oa/api/IOaContractAttachmentSyncService.java` + `impl` | 新增：中间表维护、抢占、状态回写、查询 |
| service | `oa/api/IOaContractAttachmentSyncLockService.java` + `impl` | 新增：Redis 分布式执行锁 |
| manager | `oa/api/IOaContractAttachmentSyncManager.java` + `impl` | 新增：执行编排与并发处理 |
| web | `controller/oa/OaContractAttachmentSyncController.java` | 新增：执行 / 状态查询 / 重置重试 |
| web | `config/oa/OaContractAttachmentAsyncConfig.java` | 新增：`oaContractAttachmentExecutor` 线程池 |
| web | `dto/oa/request/OaContractAttachmentSyncExecuteRequest.java`、`OaContractAttachmentSyncQueryRequest.java`、`dto/oa/vo/OaContractAttachmentSyncRecordVO.java`、`OaContractAttachmentSyncExecuteVO.java` | 新增 |
| web | `convert/oa/OaContractAttachmentSyncConverter.java` | 新增：MapStruct 转换 |
| web | `resources/db/migration/V31__oa_contract_attachment_sync.sql` | 新增：建表 |
| web | `resources/application-test.yml` / `application-prod.yml` | 修改：新增配置 |

### 7.5 关键实现要点

#### 7.5.1 OA 合同增量扫描 SQL（NOT IN 中间表）

先查中间表已入库的 `oa_form_main_id`，再以 `NOT IN` 分片查询未入库的合同，避免每轮全量扫描 OA（OA 全表数据量随年份增长，NOT IN 可把扫描量降到"新增部分"）。

```sql
SELECT fm.ID           AS oa_form_main_id,
       fm.START_DATE   AS oa_start_date,
       fm.FINISHEDFLAG AS finished_flag,
       fm.FIELD0273    AS nc_order_no,
       fm.FIELD0104    AS oa_contract_no,
       fm.FIELD0360    AS oa_attachment_ref
FROM formmain_5296 fm
WHERE fm.FINISHEDFLAG = 1
  AND fm.FIELD0273 IS NOT NULL
  AND fm.FIELD0360 IS NOT NULL
  AND fm.ID NOT IN (?, ?, ... )            -- 中间表已入库 ID 分片，每片 ≤ 1000
  AND EXISTS (
      SELECT 1
      FROM formmain_2817 f2817
      WHERE f2817.field0329 = fm.FIELD0273
        AND f2817.field0493 = 'crm订单'
  )
ORDER BY fm.ID
```

实现要点：

- **分片**：`scan-chunk-size` 默认 500，**必须 ≤ 1000**，规避 Oracle `ORA-01795`（IN 列表最多 1000 个表达式）；
- **列表为空**：中间表无数据时不下发 `NOT IN` 条件，走首次全量；
- **禁止 NULL**：`NOT IN` 子查询/列表含 `NULL` 会导致整体返回空集，排除列表只取中间表非空 `oa_form_main_id`，并在 Java 侧过滤 null；
- 分页通过 `OaDbDialectSupport.appendPagination` 适配 Oracle；
- `FINISHEDFLAG` 按数值比较；`finished_flag` 按原 SQL `CASE` 规则转 `flow_status`；
- `field0493 = 'crm订单'` 完成订单来源判断，**不再调用 CRM 订单查询**；
- 原 SQL 的 `LEFT JOIN formson_5297` 未取任何 `fs` 列，且 `formmain_2817` 仅作来源判断，直接 JOIN 会因一对多产生行放大，故使用 `EXISTS`；
- 空串视为无效值，Java 侧 `StringUtils.hasText` 二次过滤；
- `ReadOnlyOaJdbcTemplate.assertSelectOnly` 保证只读。

#### 7.5.2 中间记录表 UPSERT

按 `oa_form_main_id` 幂等写入，已存在记录**只刷新 OA 快照字段**，不重置 `sync_status` / `retry_count`：

```sql
INSERT INTO t_integration_oa_contract_attachment_sync (...)
VALUES (...)
ON CONFLICT (oa_form_main_id) DO UPDATE
SET oa_start_date = EXCLUDED.oa_start_date,
    flow_status   = EXCLUDED.flow_status,
    nc_order_no   = EXCLUDED.nc_order_no,
    oa_contract_no = EXCLUDED.oa_contract_no,
    oa_attachment_ref = EXCLUDED.oa_attachment_ref,
    oa_attachment_id  = EXCLUDED.oa_attachment_id,
    attachment_file_id = EXCLUDED.attachment_file_id,
    attachment_file_name = EXCLUDED.attachment_file_name,
    attachment_url = EXCLUDED.attachment_url,
    updated_at = CURRENT_TIMESTAMP
WHERE t_integration_oa_contract_attachment_sync.is_deleted = 0;
```

> PostgreSQL 12 禁 `MERGE`，统一使用 `ON CONFLICT DO UPDATE`。

#### 7.5.3 附件查询与地址

- 复用现有口径：`sub_reference = TRIM(field0360)`，一个合同只有一个附件，查询结果唯一；
- 附件地址按 `{oa.base-url}/seeyon/rest/attachment/file/{fileId}` 拼装；
- 本期**不下载、不转存**，仅保证可通过 OA 接口获取附件；后续如 CRM 无法访问 OA，再切换为「下载 + CRM 附件上传」模式。

#### 7.5.4 CRM 修改销售合同

```java
public CrmContractEditResponse contractEdit(CrmContractEditRequest request) {
    return post(crmProperties.getBaseUrl(), CrmContractConstants.CONTRACT_EDIT_PATH, request, "修改销售合同");
}
```

- `data_id` 必填，来源见 Q9；
- `fields` 写入合同自定义字段：`id` + 附件字段 key；
- `slave_fields` 必填但无明细修改，传空对象 `{}`；
- `is_trigger=false`、`check_rule=false`（默认）；
- 判定成功：`return_code == 0`；失败原因记录 `return_code` + `return_msg`；
- 类上使用 `@JsonPropertyOrder` 固定字段顺序，保证签名原文一致。

#### 7.5.5 执行接口与并发控制

| 方法 | 路径 | 调用方 | 说明 |
|------|------|--------|------|
| POST | `/api/v1/oa/contract-attachments/execute` | ESB 定时调用（每 10 分钟） | NOT IN 增量扫描 + 处理未执行数据，同步返回统计 |
| GET | `/api/v1/oa/contract-attachments/records` | 人工/运维 | 分页查询中间表同步状态 |
| POST | `/api/v1/oa/contract-attachments/records/{id}/retry` | 人工 | 将非 `SUCCESS` 记录重置为 PENDING |

执行接口请求（可选参数）：

```json
{ "fullScan": false, "ncOrderNo": null }
```

- `fullScan` 默认 `false`：按 NOT IN 增量扫描；置 `true` 时忽略排除列表做一次性全量初始化（人工使用）；
- `ncOrderNo` 非空：仅处理该 NC 销售订单号对应记录，用于快速补偿。

并发控制分三层：

1. **进程级**：Redis 执行锁 `setIfAbsent(key, token, ttl)`，未拿到锁的 ESB 请求立即返回「执行中」；finally 中用 Lua 比对 token 后释放；
2. **记录级**：乐观锁抢占（`version` + 状态条件），保证同一记录不会被两个任务重复处理；
3. **任务级**：`oaContractAttachmentExecutor` 线程池（有界队列 + `CallerRunsPolicy`），订单间无依赖，可并行；等待本批完成并设置整体超时。

```java
// 记录抢占
int claimed = mapper.claim(id, version);
if (claimed == 0) { /* 已被其他任务处理，跳过 */ }
executor.execute(() -> processOne(record));
```

- 单条异常逐条捕获并回写状态，不阻断批次；
- 接口始终返回 `Result.success`，失败明细在统计对象内体现；
- `PROCESSING` 超时记录在每次执行开始时回收为 `RETRY`，避免线程异常导致永久卡死。

#### 7.5.6 同步状态查询

- 查询中间记录表，支持按 `sync_status`、`nc_order_no`、`oa_start_date` 区间过滤，默认按 `updated_at` 倒序；
- 返回 OA 侧数据（主表ID、NC销售订单号、销售订单编号、流程状态、附件名、附件地址）、CRM 合同 ID、同步状态、跳过/失败原因、重试次数、最后同步时间；
- DO → VO 通过 MapStruct `OaContractAttachmentSyncConverter` 转换，禁止手工 setter。

#### 7.5.7 日志与告警

- 业务标识：`【OaContractCrm】`；
- 每个分支（抢占、跳过、失败、重试、成功）均打 INFO，带 `oaFormMainId`、`ncOrderNo`、`attachmentId`、`crmContractId`、原因；
- CRM Client 入参/出参按三方日志规范 INFO 脱敏打印；
- **每次失败即告警**（无次数上限），企微 Webhook 告警内容含 `oaFormMainId`、`ncOrderNo`、`crmContractId`、失败原因、当前 `retry_count`；
- 告警去重：`dedupType=record`、`dedupId={oaFormMainId}`，避免同一记录每 10 分钟重复轰炸，但每条记录至少告警一次。

---

## 8. 验收标准

| 序号 | 验收点 | 验收方式 | 通过标准 |
|------|--------|----------|----------|
| 1 | 执行接口 | 手动（模拟 ESB） | `POST /execute` 正常返回统计；无本地定时任务 |
| 2 | NOT IN 增量扫描 | 手动（连续两次调用） | 第二次仅查询未入库合同，OA 返回行数明显减少；中间表无重复行 |
| 3 | NOT IN 分片 | 自动 | 排除列表按 ≤1000 分片，不触发 ORA-01795；列表为空时不拼 NOT IN |
| 4 | 来源过滤 | 手动 | 仅 `field0493='crm订单'` 的销售订单对应合同被扫描；其他来源不进入中间表 |
| 5 | 未执行数据筛选 | 手动 | 仅 `PENDING`/`RETRY` 被处理；`SUCCESS` 不重复调用 CRM |
| 6 | 中间记录表落库 | 手动 | 主表ID、流程发起时间、流程状态、NC销售订单号、销售订单编号、合同附件均落库 |
| 7 | 附件缺失跳过 | 手动 | 状态 SKIPPED，原因「OA附件缺失」，其余记录正常处理 |
| 8 | 回传成功 | 手动 | `return_code=0`，状态 SUCCESS，CRM 合同可见附件地址 |
| 9 | 幂等 | 自动 | 重复执行不产生第二次 `contractEdit` 调用 |
| 10 | 乐观锁并发 | 手动（并发触发） | 同一记录不会被两个任务处理；`version` 正确递增 |
| 11 | PROCESSING 回收 | 手动 | 人为置为 PROCESSING 并超时后，下次执行自动回收为 RETRY |
| 12 | 并发执行保护 | 手动 | 上次执行未结束时再次调用 `POST /execute` 立即返回「执行中」 |
| 13 | 失败无上限重试 | 手动 | CRM 返回非 0 时状态 RETRY + 记录原因 + 立即告警；下次执行继续重试，`retry_count` 持续累加，无终态 |
| 14 | 同步状态查询 | 手动 | `GET /records` 可按状态/NC单号分页查询，返回原因与重试次数 |
| 15 | 构建与迁移 | 自动 | `mvn clean package` 通过；test 环境 Flyway V31 成功执行 |

---

## 9. 风险与依赖

| 风险/依赖 | 影响 | 应对措施 |
|-----------|------|----------|
| CRM 合同 `data_id` 来源暂定（Q9） | F6 可能取不到合同 ID | 配置项 `crm.contract.data-id-field` 占位 + 中间表人工维护兜底；联调前补齐 |
| CRM 合同附件字段 key 暂定（Q10） | 附件地址写入位置待定 | 配置项占位，按 fieldList 结果填入 |
| `fields` / `slave_fields` 格式暂定（Q12） | 请求体或签名不一致 | 联调比对官方示例与 `CrmClient` 现有签名逻辑 |
| 中间表 ID 规模增长导致 NOT IN 列表变长 | 分片次数与查询耗时上升 | `scan-chunk-size` 分片；建议按 `oa_start_date` 限定扫描窗口；接口性能监控 |
| 永不失败的记录持续重试 | 每 10 分钟重复调用 CRM / 重复告警 | 告警按记录去重；人工可通过查询接口发现长期 RETRY 记录并重置或修正数据 |
| 调度依赖 ESB 配置 | 未配置或频率错误则链路不触发 | 已确认每 10 分钟一次；上线前确认地址、超时与鉴权；提供 `enabled` 开关与执行日志 |
| ESB 接口超时小于单次执行耗时 | ESB 报错重试，中台仍在执行 | `batch-size` + 线程池控制单次耗时；Redis 锁防并发；ESB 超时需 ≥ 单次最长执行时间 |
| 线程池并发过高打到 CRM 限流 | 大量 RETRY | 控制 `max-pool-size`、有界队列；按 CRM 限流阈值调参 |
| 线程异常导致记录停留 PROCESSING | 记录不再被处理 | 超时回收机制 + 人工重置重试接口 |
| CRM 回传触发 CRM 侧业务规则（如合同状态限制） | 部分合同长期 RETRY | 记录 `return_code` / `return_msg`；通过查询接口识别长期失败记录，人工修正后重置 |
| `formmain_2817` / `formmain_5296` 一对多 | 行放大 | 关联改为 `EXISTS`，只做存在性判断 |
| OA 只读数据源网络/权限 | 扫描失败 | 数据源未配置时执行接口短路并打 INFO；连接异常告警 |
| 附件地址含 OA 内部域名 | CRM 侧不可解析 | 本期仅回传地址；若确认不可访问，后续切换「下载 + CRM 附件上传」（D3） |

---

## 10. 变更记录

| 日期 | 修改人 | 修改内容 |
|------|--------|----------|
| 2026-10-23 | — | 创建文档，明确功能点、字段映射、技术方案与待确认项 |
| 2026-10-23 | — | 按最新 SQL 补充 `formmain_2817` 关联与 `field0493` 过滤条件；补充 CRM→OA→合同→附件的上下游链路背景 |
| 2026-10-23 | — | 调整为「中台提供执行接口 + ESB 定时调用」，移除本地定时任务；新增中间记录表落库与同步状态查询接口；补全提出人、负责人与关联需求 |
| 2026-10-23 | — | 按业务确认更新：取消 CRM 订单反查改为直接调用 `contractEdit`；改为每轮全量扫描 + 中间表未执行数据处理；补充线程池并发与乐观锁；明确单合同单附件、来源 `crm订单`、附件不转存；补全 CRM 接口契约 |
| 2026-10-23 | — | 扫描改为「中间表 NOT IN 分片」增量；取消失败次数上限（失败即记录原因并告警、持续重试，移除 FAILED 终态）；确认 ESB 每 10 分钟调用；Q9/Q10/Q12 转暂定按配置占位 |
