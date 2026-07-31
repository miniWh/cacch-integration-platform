# REQ-OA-002 国内登记共享盘目录自动维护

---

## 1. 基本信息

| 项目 | 内容 |
|------|------|
| 需求编号 | REQ-OA-002 |
| 需求标题 | 国内登记报告「资料列表」驱动共享盘目录自动创建与清理 |
| 需求类型 | feature |
| 所属系统 | oa / platform |
| 优先级 | P1(高) |
| 状态 | drafting |
| 提出人 | — |
| 提出日期 | 2026-07-30 |
| 负责人 | — |
| 预计上线 | 待定 |
| **关联需求** | [REQ-OA-001 国内登记报告资料列表附件上传](./REQ-OA-001-国内登记报告资料列表附件上传.md) |
| **确认方案** | **OA 为期望态 + 共享盘目录探测 + 按需创建 L3 + 空目录安全删除** |

---

## 2. 需求背景

REQ-OA-001 已实现「共享盘 → OA」附件上传：业务在共享盘按规范目录存放资料，中台定时读取并上传至 OA 资料列表「附件」列。

当前仍存在以下问题：

1. OA 新建或变更国内登记资料列表后，共享盘上**未必已存在规范目录**，负责人需手工建文件夹，易漏建、命名不一致；
2. 资料项标记为「不需要」后，共享盘上**空目录仍残留**，干扰扫描与人工识别；
3. REQ-OA-001 当前**不读取「需要」字段**，全部资料行均参与附件扫描，与业务「不需要则不放资料」的规则不一致。

**目标**：

1. 从 OA 读取国内登记信息及资料列表（含「需要 / 不需要」标记）；
2. 按 **登记负责人 + IPDP 项目名称（项目编号）+ 资料项目名称** 与共享盘对齐；
3. 目录不存在时按规范**自动创建**（L1 → L2 → L3）；
4. 资料项为「不需要」时**不创建** L3；若 L3 已存在且**为空**则**删除**；**非空不删**；
5. 与 REQ-OA-001 附件同步**分任务调度**，目录治理优先于附件上传执行。

---

## 3. 确认方案概述

### 3.1 方案说明

```
OA 数据库              集成中台                    共享盘（SMB）
  │                    │                          │
  │  ① SQL 查 formmain_4070│                          │
  │     + formson_5464  │                          │
  │     （含「需要」）   │                          │
  │  ─────────────────→│                          │
  │                    │  ② 按 L3 路径分组聚合决策   │
  │                    │  ③ exists / isEmpty       │
  │                    │  ───────────────────────→│
  │                    │  ④ mkdir / 删空 L3        │
  │                    │  ←───────────────────────│
  │                    │  ⑤ 写 PG 治理记录         │
```

| 角色 | 职责 |
|------|------|
| **登记负责人（业务）** | 在 OA 维护资料列表及「需要 / 不需要」；在共享盘 L3 目录存放资料文件 |
| **致远 OA 数据库** | 提供 formmain_4070 / formson_5464 只读查询（含「需要」字段） |
| **集成中台** | 读 OA、计算期望路径、探测共享盘、创建缺失目录、删除空 L3、记治理状态 |
| **共享盘** | 目录实际载体；中台需 **读 + 建目录 + 删空目录** 权限 |
| **PG 中间表** | 记录目录治理动作（创建 / 删除 / 跳过），可审计、可查询 |

### 3.2 核心原则

| 原则 | 说明 |
|------|------|
| OA 为期望态 | 以 OA 资料列表为准，决定哪些 L3 应存在、哪些应清理 |
| 与附件同步互补 | REQ-OA-002：OA → 共享盘（目录）；REQ-OA-001：共享盘 → OA（文件） |
| 路径规范一致 | L1/L2/L3 命名与 REQ-OA-001 §4.2 完全一致 |
| 按需建 L3 | 仅「需要」的资料项创建 L3；「不需要」不建 |
| 安全删除 | 按 **L3 路径组**决策；组内全部为「不需要」且**空**才删 L3；含文件或非空则不删 |
| 路径组优先 | 同 sharePath 多行须聚合后再决策，**禁止逐行删目录**（见 §5.1） |
| 不迁移动产 | 不自动重命名、不移动已有非空目录；OA 改名导致路径变化另议 |
| 不回写 OA | 不修改 OA 任何字段（含「完成时间」） |
| 失败可重试 | 单条失败不阻断批次；记录 FAILED，下轮重试 |

### 3.3 与 REQ-OA-001 的关系

```mermaid
flowchart LR
    OA[(OA formmain_4070 / formson_5464)]
    SD[(共享盘 SMB)]
    PG[(PG 中台库)]

    subgraph REQ_OA_002 [REQ-OA-002 目录治理]
        P[ProvisionManager]
    end

    subgraph REQ_OA_001 [REQ-OA-001 附件上传]
        S[AttachmentSyncManager]
    end

    OA -->|读资料列表+需要标记| P
    P -->|mkdir / 删空 L3| SD
    P -->|治理记录| PG

    OA -->|读资料列表| S
    SD -->|读最终版本文件| S
    S -->|上传+CAP4| OA
    S -->|同步记录| PG
```

**联动建议**：

| 场景 | 建议 |
|------|------|
| 资料项「不需要」 | REQ-OA-001 **跳过**该 L3 扫描与上传（修订 REQ-OA-001 B2） |
| 任务调度顺序 | 目录治理 **先于** 附件同步（如治理 03:00，上传 04:00） |
| 手工在「不需要」目录放文件 | 治理任务不删；附件同步也不处理 |

---

## 4. 业务对象与目录规范

### 4.1 OA 字段映射

**主表 formmain_4070**（与 REQ-OA-001 一致）

| 业务含义 | OA 字段 | 示例值 | 用途 |
|----------|---------|--------|------|
| 登记负责人 | **field0223** | 杨燕玲 | 共享盘 L1 目录 |
| IPDP 名称 | **field0160** | 21% 环丙氟虫胺·螺虫乙酯可分散液剂 (6+15) | 共享盘 L2 名称段 |
| IPDP 项目编号 | **field0164** | IPDP-202605-107 | 共享盘 L2 括号内编号 |

**子表 formson_5464**

| 业务含义 | OA 字段 | 示例值 | 用途 |
|----------|---------|--------|------|
| 资料项目 | **field0214** | 农药登记变更申请表 | 共享盘 L3 目录 |
| **需要** | **待确认 field02xx** | 需要 / 不需要 | **本需求核心判定字段** |
| 子表行 ID | **id** | — | 日志追踪、治理记录关联 |

> **field0218（资料附件）**：属 REQ-OA-001 附件同步专用字段，**本需求不参与目录创建/删除判定**，SQL 与 DTO 均不读取。

> **子表关联（已确认）**：`formson_5464.formmain_id = formmain_4070.id`

> **匹配规则**：与 REQ-OA-001 一致，按 **登记负责人 + IPDP 名称 + IPDP 项目编号（field0164）+ 资料项目名称** 定位 L3。

### 4.2 共享盘目录结构

与 REQ-OA-001 §4.2 完全一致：

```
\\192.168.1.8\国内登记资料\                         ← 共享盘根路径
└── {登记负责人}/                               ← L1，field0223
    └── {IPDP名称（项目编号）}/                 ← L2，field0160 + field0164
        └── {资料项目名称}/                     ← L3，field0214
            ├── xxx_最终版本.pdf
            └── ...
```

**L2 命名规范**：

| 项 | 说明 |
|----|------|
| 格式 | `{field0160 IPDP名称，可含括号}（{field0164 项目编号}）` |
| 项目编号位置 | **最后一对**括号内为 field0164；名称段可含配方括号如 `(6+15)` |
| 括号 | 默认中文括号 `（）`；磁盘已有英文括号时 REQ-OA-001 扫描侧兼容 |
| 项目编号 | 以 OA `field0164` 原文为准，不限制格式 |

**完整路径示例**：

```
\\192.168.1.8\国内登记资料\李庆辉\21%环丙氟虫胺·螺虫乙酯可分散液剂 (6+15)（IPDP-202605-107）\农药登记申请表\
```

### 4.3 「需要 / 不需要」判定

> **字段物理名仍待 OA 确认**（见 §14.1 B-1、§14.3 O-1）；**空值默认规则已确认**（见 §14.1 B-2）。

| OA 存储值 | 语义 | 目录行为 |
|-----------|------|----------|
| 需要 | 需要该资料 | 缺则建 L3（及所需 L1/L2） |
| 不需要 | 不需要该资料 | 不建 L3；已存在且空则删 L3 |
| **空值 / NULL / 空白** | **视为「需要」**（**已确认**） | 与「需要」相同：缺则建 L3；**不执行空目录删除** |

**空值默认「需要」（已确认 B-2）**：

| 项 | 说明 |
|----|------|
| 适用范围 | OA 子表「需要」列为 `NULL`、空字符串或仅空白字符 |
| 归一化 | 读取后统一视为 **需要**（`normalizedRequired=true`），再参与 L3 路径组聚合（§5.1） |
| 建目录 | 与显式「需要」一致；路径组 groupRetain=true 时缺则建 L1/L2/L3 |
| 删目录 | **不**因空值触发 L3 删除；仅路径组 groupRetain=false 且目录为空时才删 |
| 设计理由 | 保守策略，避免历史数据或未填字段导致误删共享盘目录 |

**单行归一化（供开发参考）**：

```
normalizedRequired(row) =
    isBlank(row.itemRequired) || equalsIgnoreCase(row.itemRequired, "需要")
```

**L3 路径组决策（必须先聚合，禁止逐行删目录）**：

```
// ★ 必须先归一化路径段，再用归一化后的值计算 sharePath（见 §5.1 步骤②）
normalizedSegments = normalizeEachSegment(row.ownerName, row.ipdpName, row.ipdpProjectNo, row.itemName)
groupKey = buildL3SharePath(normalizedSegments)   // → 完整 L3 UNC
group = rows.groupBy(groupKey)
retain = group.anyMatch(r -> normalizedRequired(r))

if (retain) {
    // 创建分支：L3 不存在 → mkdir（组内任一行需要即保留/创建）
} else {
    // 删除分支：组内全部为显式「不需要」且 L3 存在且空 → delete
}
```

---

## 5. 业务流程

### 5.1 总体流程

> **关键约束**：创建/删除决策以 **L3 完整路径（sharePath）** 为粒度，**禁止按子表行逐条删目录**。同一 L3 路径下多行（如不同资料分类共用 field0214）须先分组聚合，组内 **任一行「需要」（含空值默认）则保留 L3**（见 §12）。

```
定时 / 手动触发
    │
    ▼
① OA SQL：按主表 Redis 游标分批 JOIN 拉取资料行（含「需要」），见 §5.5
    │
    ▼
② 归一化 itemRequired（空值→需要）；路径段归一化：L1/L2/L3 经 ShareDrivePathNormalizer 归一化（见 §5.2）
    │   归一化后任一路径段为空 → 记 FAILED，跳过该行
    │   用归一化后的路径段计算每行 L3 sharePath
    │   ★ 必须先归一化再计算 sharePath，确保原始值不同但归一化后相同的行落入同一路径组
    │
    ▼
③ 按 sharePath 分组聚合 → groupRetain = 组内 anyMatch(需要)
    │
    ▼
④ 共享盘探测：exists / isEmpty（按 sharePath，每组一次）
    │
    ├─ groupRetain=true  + L3 不存在 → mkdir -p（L1→L2→L3）→ 记 CREATED
    ├─ groupRetain=true  + L3 已存在 → 记 SKIPPED_EXISTS
    ├─ groupRetain=false + L3 不存在 → 记 SKIPPED_NOT_REQUIRED
    └─ groupRetain=false + L3 已存在
           ├─ 空目录 → 删除 L3 → 记 DELETED
           └─ 非空   → 不删 → 记 SKIPPED_NOT_EMPTY + 企微提醒（见 §14.1 B-4）
    │
    ▼
⑤ 写 PG 治理记录（按子表行维度写入，append 模式，每轮生成 run_id）
    │   组内「需要」行 → action 取路径组决策（CREATED / SKIPPED_EXISTS / DELETED / …）
    │   组内「不需要」行且 groupRetain=true → action = SKIPPED_GROUP_RETAINED
    │       message 注明「由同组其他行触发」，group_retain = true
    │
    ▼
⑥ 输出本轮统计；FAILED / SMB 不可用 / SKIPPED_NOT_EMPTY 企微告警（可选）
```

### 5.2 创建规则

| 层级 | 创建条件 |
|------|----------|
| L1 | 该 L2 路径组对应项目下，**至少有一个 L3 路径组 groupRetain=true** |
| L2 | 同上（同一 formMainId / 同一 field0164 项目） |
| L3 | **该 sharePath 路径组 groupRetain=true** 且 L3 不存在 |

- 创建顺序：**L1 → L2 → L3**（逐级 `mkdir`，父级已存在则跳过）。
- 同一 L2 下多条「需要」资料项：共用一个 L2，分别建多个 L3。
- **路径组 groupRetain=false：永不创建 L3**（空值不归入「不需要」，见 §4.3）。

**目录名归一化与非法字符（复用 REQ-OA-001）**：

| 项 | 说明 |
|----|------|
| 工具类 | `ShareDrivePathNormalizer.normalize()` + `ShareDriveConstants.FORBIDDEN_DIR_CHARS` |
| 非法字符 | `\ / : * ? " < > \|` 及 `\u0000`（**不含 `%`**，IPDP 名称可含百分号） |
| 处理规则 | trim → 删除非法字符 → 合并连续空白 → 去掉首尾点号 |
| 校验失败 | 归一化后 L1/L2/L3 任一段为空 → 该行记 **FAILED**，打 INFO 含原始 OA 字段值，**不参与分组、不执行 mkdir** |
| 与 REQ-OA-001 一致 | 建目录与扫描匹配使用同一套归一化规则，避免「OA 有值但无法建目录」与「建了目录但扫不到」 |

### 5.3 删除规则（仅 L3，按路径组决策）

| 路径组条件 | 动作 |
|------------|------|
| groupRetain=true | **不删除**（组内任一行需要即保留，即使其他行显式「不需要」） |
| groupRetain=false + L3 **不存在** | 无操作 |
| groupRetain=false + L3 **存在且为空** | **删除 L3 目录** |
| groupRetain=false + L3 **存在且非空** | **不删除**；记 SKIPPED_NOT_EMPTY；**建议企微提醒登记负责人**（见 §14.1 B-4） |

**「空目录」定义（建议）**：

- 目录下无任何文件、无任何子目录（列举结果为空）；
- **默认**：含 `desktop.ini`、`Thumbs.db` 等系统文件视为**非空**（更安全）；
- 是否忽略系统文件名单：**待确认 B-3**。

**安全约束**：

- **禁止**递归删除 L2 / L1；
- **禁止**删除含 `_最终版本` 或任意普通文件的目录；
- 删除前**二次列举**确认为空（降低并发误删风险）。

### 5.4 拉取范围

与 REQ-OA-001 一致：

| 模式 | 说明 |
|------|------|
| 全量定时 | 主表 Redis 游标分批（`formBatchSize`，默认 50）；每批拉取对应主表下**全部**子表行 |
| 单表触发 | 指定 formMainId |
| 测试过滤 | 登记负责人白名单（配置项） |

### 5.5 OA 查询 SQL（草案）

> **须复用现有 `OaRegReportDbClient` 主表分批模式**，禁止对 JOIN 结果直接 `LIMIT`（避免单项目子表行占满批次导致其他项目被截断）。

**阶段一：主表游标取批次 ID**（Redis 存 `lastFormMainId`，与 REQ-OA-001 附件同步共用游标服务基础设施，但使用**独立 cursor key**：`integration:oa:reg-report:provision:cursor`；REQ-OA-001 附件同步为 `integration:oa:reg-report:sync:cursor`，避免两任务互相跳批）

```sql
-- listFormMainIdsAfterCursor(afterFormMainIdExclusive, formBatchSize, ownerFilter)
SELECT m.id
FROM formmain_4070 m
-- 可选 JOIN org_member 过滤 field0223 登记负责人
WHERE m.id > :afterFormMainIdExclusive
ORDER BY m.id
LIMIT :formBatchSize;
```

**阶段二：按主表 ID 列表拉全量子表行**（本批次**不截断**子表行数）

```sql
-- queryItemRows(formMainIds, applySubRowLimit=false)
SELECT
    m.id              AS form_main_id,
    m.field0223       AS owner_name,       -- 若存成员 ID，JOIN org_member 取姓名
    m.field0160       AS ipdp_name,
    m.field0164       AS ipdp_project_no,
    s.id              AS item_row_id,
    s.field0214       AS item_name,
    s.field02xx       AS item_required      -- ★ 待 OA 确认物理字段名
FROM formmain_4070 m
INNER JOIN formson_5464 s ON s.formmain_id = m.id
WHERE m.id IN (:formMainIds)
ORDER BY m.id, s.id;
```

**调用约定**（对齐已实现代码）：

| 场景 | 方法 | 说明 |
|------|------|------|
| 全量定时 | `listFormMainIdsAfterCursor` + `queryItemRows(IN, limit=false)` | `formBatchSize` 默认 50 |
| 指定 formMainId | `queryItemRows(singleId, subRowBatchSize, limit=true)` | 单表触发时可限子表行 |
| 游标耗尽 | 重置 Redis 游标为 0，从头新一轮 | 与 REQ-OA-001 一致 |

**DTO / Mapper 扩展（必选）**：

| 类 | 变更 |
|----|------|
| `OaRegReportItemRow` | 新增 `itemRequired` 字段（String，OA 原始值） |
| `ItemRowMapper` | 映射 `item_required` 列 |
| `buildItemRowSelectClause` | SELECT 增加 `s.field02xx AS item_required` |
| `OaRegReportProperties` | 新增 `fieldItemRequired` 配置项（见 §10） |

---

## 6. 功能点

| 序号 | 功能点 | 描述 | 必选/可选 |
|------|--------|------|-----------|
| F1 | SQL 查 OA 资料列表 | JOIN 主表+子表，含「需要」字段 | 必选 |
| F2 | 期望路径计算与 L3 分组 | 拼 L1/L2/L3 sharePath，按路径组聚合 groupRetain | 必选 |
| F3 | 共享盘目录探测 | exists、isEmpty、list | 必选 |
| F4 | 目录创建 | 按需 mkdir -p（L1→L2→L3） | 必选 |
| F5 | 空 L3 删除 | 「不需要」且空目录时删除 L3 | 必选 |
| F6 | 治理记录与幂等 | PG 记录 CREATED/DELETED/SKIPPED/FAILED | 必选 |
| F7 | 定时任务 | cron 调度，与附件任务错开 | 必选 |
| F8 | 手动触发 | 按 formMainId 或全量触发 | 可选 |
| F9 | 治理记录查询 API | 分页查询治理动作与原因 | 可选 |
| F10 | 异常与非空目录告警 | FAILED、SMB 不可用、SKIPPED_NOT_EMPTY 企微通知 | 可选（SKIPPED_NOT_EMPTY 建议默认开启，见 B-4） |

**明确不做**：

| 项 | 说明 |
|----|------|
| L1/L2 自动删除 | 即使其下所有 L3 已删，也不自动删 L2/L1 |
| 目录重命名 / 迁移 | OA 字段变更导致路径变化时不自动搬迁旧目录 |
| 非空目录强制删除 | 「不需要」但含文件时仅跳过并记日志 |
| OA 字段回写 | 不修改 OA 任何列 |
| 附件上传 | 属 REQ-OA-001，本需求不包含 |

---

## 7. 中间表设计

**表名**：`t_integration_oa_reg_share_dir_provision`

```
t_integration_oa_reg_share_dir_provision（共享盘目录治理记录，append 模式：每轮执行新增记录，保留全部历史）
├─ id                      BIGINT PK
├─ run_id                  VARCHAR   -- 执行轮次标识（如时间戳+UUID），同轮所有记录共享
├─ form_main_id            BIGINT    -- formmain_4070.id
├─ owner_name              VARCHAR   -- field0223 登记负责人
├─ ipdp_name               VARCHAR   -- field0160 IPDP名称
├─ ipdp_project_no         VARCHAR   -- field0164 项目编号
├─ item_name               VARCHAR   -- field0214 资料项目名称
├─ item_row_id             VARCHAR   -- formson_5464.id
├─ item_required           VARCHAR   -- 需要/不需要（快照）
├─ share_path              VARCHAR   -- L3 完整路径（归一化后）
├─ group_retain            BOOLEAN   -- 所属路径组 groupRetain 决策（true=保留/创建）
├─ action                  VARCHAR   -- CREATED/DELETED/SKIPPED_EXISTS/SKIPPED_NOT_EMPTY/SKIPPED_NOT_REQUIRED/SKIPPED_GROUP_RETAINED/FAILED
├─ action_message          VARCHAR   -- 跳过或失败原因
├─ provisioned_at          TIMESTAMP -- 本次治理时间
├─ created_at / updated_at / is_deleted
```

**索引建议**：

```sql
-- 按业务键查最新治理记录（append 模式，取最新 provisioned_at）
INDEX idx_provision_biz_key (owner_name, ipdp_name, ipdp_project_no, item_name, provisioned_at DESC)
WHERE is_deleted = 0;

-- 按主表、动作查询
INDEX idx_provision_form_action (form_main_id, action);

-- 按执行轮次查询
INDEX idx_provision_run (run_id);
```

---

## 8. 涉及系统与模块

| 系统 | 模块/接口 | 说明 |
|------|-----------|------|
| 共享盘 | SMB | 需 **读 + 建目录 + 删空目录** |
| **致远 OA 数据库** | formmain_4070 / formson_5464 | **只读 SELECT**（含「需要」字段） |
| 集成中台 integration | `IShareDriveClient`（扩展 mkdir/isEmpty/deleteEmpty） | 共享盘读写操作，见 §11 |
| 集成中台 integration | `OaRegReportDbClient`（扩展查询「需要」） | OA 库只读；扩展 SELECT + DTO |
| 集成中台 integration | `OaRegReportItemRow` / `ItemRowMapper` | 新增 `itemRequired` 字段映射 |
| 集成中台 common | `OaRegReportProperties` | 新增 `fieldItemRequired` 配置绑定 |
| 集成中台 manager | `IOaRegShareDirectoryProvisionManager` | 编排 |
| 集成中台 service | `IOaRegShareDirectoryProvisionService` | 治理记录 |
| 集成中台 async | `OaRegShareDirectoryProvisionTask` | 定时任务 |
| 集成中台 web | `OaRegShareDirectoryProvisionController` | 手动触发 / 查询 |
| 集成中台 dao（PG） | 治理记录 Mapper | Flyway 迁移 |
| 企微 Webhook | 告警 | 可选 |

**模块分层（遵守项目规范）**：

| 模块 | 类（建议） | 职责 |
|------|------------|------|
| integration | `ShareDriveDirectorySupport` | L2 路径格式化（复用 REQ-OA-001） |
| integration | `ShareDrivePathNormalizer` | L1/L2/L3 目录段归一化、非法字符剔除（复用 REQ-OA-001） |
| integration | `IShareDriveClient` / `ShareDriveClientImpl` | 扩展 exists / mkdirs / isEmpty / deleteEmptyDirectory |
| integration | `OaRegReportDbClient` | 拉取资料行 + item_required；复用主表游标分批 |
| integration | `OaRegReportItemRow` | 资料行 DTO，含 `itemRequired` |
| integration | `OaRegReportDbClient.ItemRowMapper` | 映射 `item_required` 列 |
| common | `OaRegReportProperties` | `fieldItemRequired` 等 OA 字段配置 |
| manager | `OaRegShareDirectoryProvisionManagerImpl` | **L3 路径分组聚合**、创建/删除决策、统计 |
| service | `OaRegShareDirectoryProvisionServiceImpl` | 治理记录 CRUD |
| async | `OaRegShareDirectoryProvisionTask` | `@Scheduled` |
| web | `OaRegShareDirectoryProvisionController` | REST 触发与查询 |

**SMB 写操作扩展（`IShareDriveClient` 新增方法）**：

| 方法 | 说明 | 实现要点 |
|------|------|----------|
| `boolean existsDirectory(String path)` | 判断 UNC 目录是否存在 | SMB `SmbFile.exists()` + `isDirectory()` |
| `void mkdirs(String path)` | 递归创建 L1→L2→L3 | 逐级创建；父级已存在则跳过；无写权限抛 BizException |
| `boolean isEmptyDirectory(String path)` | 目录是否为空 | 列举文件/子目录；按 `ignore-system-files` 配置决定是否忽略系统文件 |
| `void deleteEmptyDirectory(String path)` | 删除空 L3 | **调用前须二次 isEmpty**；仅删单层目录，禁止递归删 L2/L1 |

> 读写可共用同一 SMB 连接池；若 REQ-OA-001 与 REQ-OA-002 使用不同 AD 账号，通过 `share-dir-provision.smb-*` 独立配置（见 §10）。

---

## 9. 接口草案

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/oa/reg-reports/share-directory/provision/trigger` | 手动触发；可选 `formMainId` |
| GET | `/api/v1/oa/reg-reports/share-directory/provision/records` | 分页查询治理记录（按 owner/action/time） |

---

## 10. 配置项（草案）

```yaml
oa:
  reg-report:
    field-item-required: field02xx   # ★ 待 OA 确认「需要」字段物理名
  share-dir-provision:
    enabled: false                   # 默认关闭，test 验证后开启
    cron: "0 0 3 * * ?"              # 建议早于附件同步任务
    form-batch-size: 50              # 主表（项目）每批数量，对齐 OaRegReportDbClient
    sub-row-batch-size: 500          # 仅指定 formMainId 触发时限制子表行数
    owner-allowlist: []              # 测试白名单，空=全量
    alert-not-empty-skipped: true    # SKIPPED_NOT_EMPTY 是否企微提醒（见 B-4，建议默认 true）
    ignore-system-files: []          # 空目录判定时忽略的文件名（默认空=不忽略，更安全；见 §5.3 B-3）
    # 确认 B-3 后可取消注释添加：
    # - desktop.ini
    # - Thumbs.db
    # 可选：与 REQ-OA-001 只读账号分离时使用独立 SMB 写账号
    # smb-username: provision-svc
    # smb-password: ${OA_SHARE_PROVISION_PASSWORD}
```

---

## 11. 权限与安全

| 项 | 要求 |
|----|------|
| 共享盘账号 | 需 **读 + 建目录 + 删空目录**；建议专用 AD 读写账号，与 REQ-OA-001 只读账号分离（S-3） |
| 共享盘 UNC | 沿用 `\\192.168.1.8\国内登记资料`；中台进程须能挂载/访问该共享 |
| SMB 写权限最小化 | 仅授权目标根目录下 **创建子目录、列举、删除空文件夹**；禁止共享级管理员 |
| OA 库 | **只读 SELECT**（与 REQ-OA-001 相同账号即可）；本需求**不写** field0218 |
| 审计 | 所有 CREATED / DELETED 打 INFO；须含 sharePath、itemRowId、groupRetain |
| 并发 | 同一 L3 sharePath 加 Redis 分布式锁（键如 `integration:oa:share-provision:lock:{sharePathHash}`），或任务内按路径串行 |
| 误删防护 | 按路径组 groupRetain 决策；仅删 L3；删除前二次 isEmpty；非空一律不删 |
| mkdir 失败 | 无写权限 / 磁盘满 / 路径非法 → 记 FAILED，打 ERROR 含 SMB 异常，可选企微告警 |
| delete 失败 | 二次列举变为非空 → 中止删除，记 SKIPPED_NOT_EMPTY；SMB 异常记 FAILED |

---

## 12. 异常与边界

| 场景 | 处理 |
|------|------|
| 「需要」字段为空（NULL/空串/空白） | 视为「需要」，走创建分支；不删 L3 | 见 §4.3 B-2 |
| field0164 为空 | 跳过该项目 L2/L3 治理，记 FAILED，告警 |
| field0223 未解析为姓名 | 跳过，与 REQ-OA-001 一致 |
| L2 磁盘目录与 OA 不一致（旧格式） | 不自动迁移；仅按 OA 规则**新建**期望路径 |
| 同 field0214 多行（不同分类） | **必须先按 sharePath 分组**；L3 目录共用；**组内任一行「需要」则 groupRetain=true，禁止删 L3** | 见 §5.1 |
| 路径段含 Windows 非法字符 | `ShareDrivePathNormalizer` 归一化；归一化后为空 → FAILED | 见 §5.2 |
| 「不需要但 L3 非空」 | 不删；记 SKIPPED_NOT_EMPTY；**建议企微提醒登记负责人**手动清理 | 见 §14.1 B-4 |
| 删除时目录被并发写入文件 | 二次列举为非空 → 中止删除 |
| SMB 不可用 / 无写权限 | 本轮终止或单条 FAILED，企微告警 |
| 全部资料项均为「不需要」 | 不建 L1/L2；仅处理已有 L3 的空目录清理 |

---

## 13. 验收标准

| 序号 | 场景 | 预期 |
|------|------|------|
| 1 | OA 新建项目，资料项为「需要」，共享盘无对应路径 | 自动生成 L1/L2/L3，路径与 §4.2 一致 |
| 2 | 资料项改为「不需要」，L3 为空 | 下次治理任务删除该 L3 |
| 3 | 资料项为「不需要」，L3 内有文件 | 目录保留，记 SKIPPED_NOT_EMPTY，日志可查 |
| 4 | 资料项为「不需要」，L3 不存在 | 不创建 L3 |
| 4a | 「需要」字段为空，共享盘无 L3 | 视为「需要」，自动创建 L3 |
| 5 | REQ-OA-001 附件同步 | 「不需要」资料项不再扫描上传（需 REQ-OA-001 配套修订） |
| 6 | 治理记录 | POST 触发后可 GET 查询 CREATED/DELETED 等动作 |
| 7 | 幂等 | 重复执行「需要且已存在」的 L3 不重复 mkdir |
| 8 | 同 field0214 多行 | 一行「需要」、一行「不需要」共用 L3：保留目录不删；仅一行「不需要」（无其他行共用该 L3）且 L3 为空：正常触发删除 |
| 9 | 路径含非法字符 | 归一化后无法生成有效目录名 → FAILED，不 mkdir |

---

## 14. 待确认信息清单

> **使用说明**：请逐项向对应责任方咨询，将「确认结果」列补充完整。★ 为阻塞开发项。

### 14.1 业务规则（咨询：国内登记部 / 业务负责人）

| 编号 | 确认项 | 咨询对象 | 确认结果 | 备注 |
|------|--------|----------|----------|------|
| B-1 ★ | 「需要 / 不需要」子表字段物理名与枚举值 | 登记部 / OA 管理员 | | 阻塞 SQL |
| B-2 | ~~空值默认「需要」还是「不需要」~~ | 登记部 | **已确认：空值默认「需要」** | NULL/空串/空白均视为需要；见 §4.3 |
| B-3 | 空目录是否忽略 desktop.ini 等系统文件 | 登记部 / IT | | 默认不忽略 |
| B-4 | 「不需要但 L3 非空」是否需企微提醒负责人手动清理 | 登记部 | | **建议默认开启**（`alert-not-empty-skipped: true`）；提醒含 owner、sharePath、itemName |
| B-5 | 目录治理与附件同步的 cron 顺序 | 登记部 / 运维 | | 建议治理先于上传 |
| B-6 | REQ-OA-001 是否改为仅同步「需要」资料项 | 登记部 | | 建议同步修订 |

### 14.2 共享盘（咨询：IT 运维）

| 编号 | 确认项 | 咨询对象 | 确认结果 | 备注 |
|------|--------|----------|----------|------|
| S-1 ★ | 中台共享盘账号是否具备建目录、删空目录权限 | IT 运维 | | REQ-OA-001 当前为只读 |
| S-2 | 是否沿用 REQ-OA-001 同一 UNC 根路径 | IT 运维 | **`\\192.168.1.8\国内登记资料`** | 已确认 |
| S-3 | 专用读写账号或升级现有账号 | IT 运维 | | |

### 14.3 致远 OA（咨询：OA 管理员）

| 编号 | 确认项 | 咨询对象 | 确认结果 | 备注 |
|------|--------|----------|----------|------|
| O-1 ★ | 「需要」字段物理名（formson_5464） | OA 管理员 | | 如 field02xx |
| O-2 ★ | 「需要 / 不需要」存储值（中文/编码/数字） | OA 管理员 | | |
| O-3 | 同步 SQL 的 WHERE 条件（状态/时间） | OA 管理员 | | 与 REQ-OA-001 O12 对齐 |

### 14.4 集成中台（内部确认）

| 编号 | 确认项 | 确认结果 | 备注 |
|------|--------|----------|------|
| P-1 ★ | 定时任务 cron | | 默认 `0 0 3 * * ?` |
| P-2 ★ | form-batch-size / sub-row-batch-size | | 默认 50 / 500；见 §10 |
| P-3 | 是否提供手动触发 API | | 建议提供 |
| P-4 | enabled 默认 false，test 验证后开启 | | |
| P-5 | Flyway 版本号 | | 待开发时递增 |

### 14.5 OA 字段映射表（待 OA 管理员填写）

| 业务含义 | 表 / 字段 | 示例值 | 已确认 |
|----------|-----------|--------|--------|
| 登记信息主表 | formmain_4070 | — | ☑ |
| 资料列表子表 | formson_5464 | — | ☑ |
| 登记负责人 | formmain_4070.**field0223** | 杨燕玲 | ☑ |
| IPDP 名称 | formmain_4070.**field0160** | 21% 环丙氟虫胺… | ☑ |
| IPDP 项目编号 | formmain_4070.**field0164** | IPDP-202605-107 | ☑ |
| 资料项目 | formson_5464.**field0214** | 农药登记变更申请表 | ☑ |
| **需要** | formson_5464.**field02xx** | 需要 / 不需要 | ☐ |
| 子表行 ID | formson_5464.**id** | — | ☑ |

---

## 15. 风险与依赖

| 风险/依赖 | 影响 | 应对措施 |
|-----------|------|----------|
| O-1 / B-1「需要」字段未确认 | 无法开发 | 优先向 OA 管理员确认 |
| 共享盘仅有只读权限 | 无法 mkdir/删目录 | S-1 升级账号权限 |
| 误删非空目录 | 业务资料丢失 | 路径组 groupRetain + 严格 isEmpty + 二次确认；仅删 L3 |
| 同 field0214 多行（原始值差异） | 归一化前分组导致同路径拆组误删 | §5.1 步骤②先归一化再计算 sharePath 分组 |
| REQ-OA-001 仍扫描「不需要」项 | 行为不一致 | B-6 同步修订 REQ-OA-001 |
| OA 字段变更导致路径变化 | 旧目录残留、新目录新建 | 本期不自动迁移；人工或后续需求 |
| 与附件任务并发 | 创建目录同时读文件 | cron 错开；目录任务优先 |

---

## 16. 变更记录

| 日期 | 修改人 | 修改内容 |
|------|--------|----------|
| 2026-07-30 | — | 创建文档，输出目录自动创建与空 L3 清理方案 |
| 2026-07-31 | — | 确认 B-2：「需要」字段空值默认视为「需要」 |
| 2026-07-31 | — | 审查修订：§5.1 改为 L3 路径分组聚合决策（修复同 field0214 多行误删）；§5.5 对齐主表游标分批 SQL；补充 DTO/Mapper/SMB 写操作/非法字符校验/B-4 企微提醒 |
| 2026-07-31 | — | 二次审查修订：§5.1 步骤②归一化提前到 sharePath 计算前（修复分组 key 不一致导致误删变体）；§5.5 明确独立 cursor key 避免跳批；§5.3/§10 对齐 ignore-system-files 默认值（默认空=不忽略）；§7 新增 run_id/group_retain 字段、action 增加 SKIPPED_GROUP_RETAINED、改 append 模式 + 索引调整；§13 验收标准 8 措辞明确化 |
