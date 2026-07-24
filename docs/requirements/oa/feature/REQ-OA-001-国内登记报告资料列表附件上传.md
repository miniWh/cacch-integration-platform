# REQ-OA-001 国内登记报告资料列表附件上传

---

## 1. 基本信息

| 项目 | 内容 |
|------|------|
| 需求编号 | REQ-OA-001 |
| 需求标题 | 国内登记报告「资料列表」共享盘驱动附件同步至 OA |
| 需求类型 | feature |
| 所属系统 | oa / platform |
| 优先级 | P1(高) |
| 状态 | reviewing |
| 提出人 | — |
| 提出日期 | 2026-07-23 |
| 负责人 | — |
| 预计上线 | 待定 |
| **确认方案** | **共享盘目录驱动 + OA 资料列表比对 + 中间表幂等同步** |

---

## 2. 需求背景

国内登记报告（致远 OA 表单，示例单号 `REG-202607-0128`）详情页包含「资料列表」子表，按资料分类/资料项目列出登记所需材料。当前「附件」列为空，业务人员需在 OA 与共享盘之间手工对照上传，易漏传、错传、重复上传。

**目标**：

1. 业务在**共享盘**按规范目录存放登记资料（支持多版本，以最新版为准）
2. 集成中台**定时/手动**读取 OA 国内登记报告及资料列表，与共享盘目录比对
3. 将最新版文件**自动上传**至 OA 对应资料项目行的「附件」列
4. **中间表**记录已同步版本，相同版本不重复上传

---

## 3. 确认方案概述

### 3.1 方案说明

```
共享盘              集成中台                    OA 数据库              OA REST（仅上传）
  │                    │                          │                      │
  │                    │  ① SQL 查 formmain_4070  │                      │
  │                    │     + formson_5464  ────→│                      │
  │                    │  ② 拼路径、读最新版文件   │                      │
  │  读文件 ◄───────────│                          │                      │
  │                    │  ③ 中间表幂等判断         │                      │
  │                    │  ④ 上传文件 ─────────────────────────────────→│
  │                    │     ← fileurl（文件 ID）   │                      │
  │                    │  ⑤ UPDATE field0218 ────→│                      │
  │                    │  ⑥ 写中间表 SUCCESS       │                      │
```

| 角色 | 职责 |
|------|------|
| **登记负责人（业务）** | 在共享盘按规范建目录、上传/更新资料文件 |
| **共享盘** | 存放登记资料原件，支持多版本并存 |
| **集成中台** | 读共享盘、**SQL 查 OA 库**、幂等判断、**REST 上传附件**、**SQL 回写 field0218**、记同步状态 |
| **致远 OA 数据库** | 存储 formmain_4070 / formson_5464；读列表、写附件 ID |
| **致远 OA REST** | **仅**文件上传（响应 **`fileurl`** = 文件 ID） |
| **PG 中间表** | 记录「表单 + 资料项目 + 版本」是否已成功同步，防重复上传 |

### 3.2 核心原则

| 原则 | 说明 |
|------|------|
| 共享盘为源 | 不以 OA 页面上传为主路径；文件权威来源是共享盘 |
| 按行精确匹配 | 每个资料项目行对应共享盘一个目录 |
| 最新版本优先 | 同目录多版本时，仅同步版本号最大者 |
| 按名称唯一匹配 | **路径拼接、业务关联、幂等键均使用名称**，不使用 IPDP 编号、产品编号、资料项目编码 |
| **查表读 + 查表写** | **读取资料列表、回写 field0218 均直连 OA 库**；**仅附件文件上传走 REST 接口** |
| 幂等不重复 | 中间表已记录 SUCCESS 的 `(登记负责人, IPDP名称, 资料项目名称, 版本)` 不再上传 |
| 仅写附件 | **只更新**资料列表子表「附件」列；不读不写「需要」、不回写「完成时间」 |
| 失败可重试 | OA 上传失败记 RETRY，定时任务下轮重试，超限告警 |

---

## 4. 业务对象与目录规范

### 4.1 OA 字段映射（已确认）

**主表 formmain_4070**

| 业务含义 | OA 字段 | 示例值 | 用途 |
|----------|---------|--------|------|
| 登记负责人 | **field0223** | 杨燕玲 | 共享盘 L1 目录；幂等键 |
| IPDP 名称 | **field0160** | 10% 环丙氟虫胺可分散液剂 | 共享盘 L2 目录；幂等键 |

**子表 formson_5464**

| 业务含义 | OA 字段 | 示例值 | 用途 |
|----------|---------|--------|------|
| 资料项目 | **field0214** | 农药登记变更申请表 | 共享盘 L3 目录；幂等键；匹配子表行 |
| 资料附件 | **field0218** | 文件 ID（见下） | **唯一写入目标**；存 OA **文件 ID** |
| 需要 | （不参与） | 需要 / 不需要 | 不读不写 |
| 完成时间 | （不参与） | — | 不读不写 |

> **field0218 取值（已确认）**：写入 OA 文件上传 REST 接口响应体中的 **`fileurl` 字段**（文件 ID），非 attachment_id 等其他字段。

> **子表关联（已确认）**：`formson_5464.formmain_id = formmain_4070.id`

> **匹配规则（已确认）**：共享盘路径、中间表幂等、业务唯一判断**均按名称**，不使用 IPDP 项目编号、产品编号、资料项目编码。

### 4.2 共享盘目录结构

```
\\192.168.1.8\国内登记资料\                         ← 共享盘根路径（已确认）
└── {登记负责人}/                               ← L1，field0223，如：杨燕玲
    └── {IPDP名称}/                             ← L2，field0160，如：10% 环丙氟虫胺可分散液剂
        └── {资料项目名称}/                     ← L3，field0214，如：农药登记变更申请表
            ├── 农药登记变更申请表_v1.pdf
            ├── 农药登记变更申请表_v2.pdf
            └── 农药登记变更申请表_v3.pdf         ← 同步此文件（最新版）
```

**路径拼接公式**：

```
\\192.168.1.8\国内登记资料\{field0223 登记负责人}\{field0160 IPDP名称}\{field0214 资料项目名称}\
```

> 原方案中的「IPDP 项目编号」「产品编号」层级**已取消**；统一改用 **IPDP 名称**（field0160）作为 L2，目录为**三级**结构。

### 4.6 已确认基础设施与 OA 表结构

| 类别 | 配置项 | 确认值 | 说明 |
|------|--------|--------|------|
| 共享盘 | 协议 | SMB | 基于 UNC 路径推断 |
| 共享盘 | 根路径 | `\\192.168.1.8\国内登记资料` | |
| 共享盘 | 访问账号 | `liuyanmiao` | 密码见 `application-{profile}.yml`，**禁止写入本文档/Git** |
| OA | 登记信息主表 | `formmain_4070` | field0223 负责人、field0160 IPDP名称 |
| OA | 资料列表子表 | `formson_5464` | field0214 资料项目、field0218 资料附件 |

> **安全说明**：共享盘密码由运维配置在 `share-drive.password`，不得硬编码于代码或需求文档。

### 4.3 版本规则（待业务确认后固化）

| 项 | 默认建议 | 确认结果（填写） |
|----|----------|------------------|
| 版本命名 | 文件名后缀 `_vN`，如 `xxx_v3.pdf` | |
| 取最新版 | 解析 N 取 max；同 N 取修改时间最新 | |
| 无版本号文件 | 目录内唯一有效文件视为 v1；多个则取修改时间最新 | |
| 允许扩展名 | pdf, doc, docx, xls, xlsx, png, jpg, jpeg, zip | |

### 4.4 幂等规则

**业务唯一键（已确认，按名称）**：

```
(登记负责人, IPDP名称, 资料项目名称, file_version)
```

| 条件 | 行为 |
|------|------|
| 中间表已有上述键且 `sync_status=SUCCESS`，checksum 未变 | **SKIP**，不重复上传 |
| 出现更大版本号（如 v3→v4） | **上传新版本**，更新中间表 |
| 同版本号但 checksum 变化 | **重新上传**（视为内容修订，待确认） |
| OA 上传失败 | 记 `RETRY`，下轮重试 |

### 4.5 资料项目行识别与附件归属机制

> **核心问题**：上传的附件如何知道属于哪一项资料项目？

附件归属通过 **「逐行遍历 + 双向键匹配 + OA 子表行定位」** 实现，分两步：

#### 第一步：共享盘文件 → 匹配到哪一行资料项目（读文件）

中台从 OA 拉取某张登记报告的**资料列表子表全部行**，**逐行**处理（不区分「需要/不需要」）：

```
FOR EACH 子表行 row IN formson_5464:
    ownerName = formmain_4070.field0223    ← 登记负责人
    ipdpName  = formmain_4070.field0160    ← IPDP 名称
    itemName  = row.field0214              ← 资料项目
    itemRowId = row.id                     ← OA 子表行 ID（写附件定位用）

    sharePath = {root}/{ownerName}/{ipdpName}/{itemName}/
    file      = pickLatestVersion(sharePath)

    → 该 file 在业务语义上属于「当前这一行 row」
```

| 匹配键 | OA 字段 | 作用 |
|--------|---------|------|
| 登记负责人 | field0223 | 共享盘 L1；幂等键 |
| IPDP 名称 | field0160 | 共享盘 L2；幂等键 |
| 资料项目名称 | field0214 | 共享盘 L3；幂等键；匹配子表行 |
| 子表行 ID | row.id | 写 OA 时定位 formson_5464 具体行 |

**示例**（资料列表第 1 行）：

| 字段 | 值 |
|------|-----|
| field0223 登记负责人 | 杨燕玲 |
| field0160 IPDP名称 | 10% 环丙氟虫胺可分散液剂 |
| field0214 资料项目 | 农药登记变更申请表 |
| 拼出共享盘路径 | `\\192.168.1.8\国内登记资料\杨燕玲\10% 环丙氟虫胺可分散液剂\农药登记变更申请表\` |
| 读取文件 | `农药登记变更申请表_v3.pdf` |

#### 第二步：附件写入 OA → REST 上传 + SQL 回写 field0218（已确认）

**混合对接（已确认）**：读取与回写**不走 REST**，仅文件二进制上传走 REST。

```
① REST 上传文件到 OA 附件库（唯一 HTTP 调用）
   POST /attachment/upload  (multipart)
   → 解析响应，取 **fileurl**（文件 ID）

② SQL 更新子表附件字段（直连 OA 库）
   UPDATE formson_5464
   SET field0218 = :file_id
   WHERE id = :sub_row_id
```

| 操作 | 方式 | 说明 |
|------|------|------|
| 查主表 + 资料列表 | **OA 库 SQL** | JOIN formmain_4070 ↔ formson_5464 |
| 上传文件 | **REST API** | 响应 **fileurl**（文件 ID） |
| 写 field0218 | **OA 库 SQL** | UPDATE 为 fileurl 值 |

**因此**：field0218 存 **fileurl（文件 ID）**；行定位靠 formson_5464.id；路径靠 field0223/field0160/field0214。

#### 整体数据流（一图看懂）

```
formson_5464 第 N 行
  ├─ field0214  ──→  拼共享盘 L3 路径  ──→  读到 v3.pdf
  ├─ id         ──→  SQL UPDATE 定位    ──→  写入 field0218
  └─ REST 上传  ──→  fileurl（文件 ID）

中间表幂等键（按名称）：
  (field0223, field0160, field0214, file_version)
```

#### 边界情况

| 情况 | 处理 |
|------|------|
| 两个资料分类下 field0214 相同 | 以 `row.id` 区分 OA 行；共享盘 L3 目录共用，附件写入各自子表行 |
| 名称含 `%`、空格等特殊字符 | **已确认（B13）**：共享盘文件夹名与 field0160 **完全一致** |
| 共享盘有目录但 OA 无对应行 | 不上传；可记日志（目录孤儿） |
| OA 有行但共享盘无目录 | 记 `SKIPPED / MISSING_DIR` |

---

### 4.7 OA 库查表对接（已确认）

> **结论：可行。** 读取资料列表、回写 field0218 均通过直连 OA 库；**仅文件上传**走 REST；field0218 写入响应 **fileurl**。

#### 4.7.1 为何可行

| 点 | 说明 |
|----|------|
| 表结构已知 | formmain_4070、formson_5464 及 field 映射已确认 |
| 读操作 | JOIN 主表+子表即可拿到 field0223/field0160/field0214/id |
| 写操作 | 仅需 UPDATE formson_5464.field0218，范围可控 |
| 上传操作 | 文件二进制必须走 REST，由 OA 标准流程入库附件表 |

#### 4.7.2 查询 SQL（草案）

```sql
-- 拉取待同步的资料列表行（过滤条件见 O12 待确认）
SELECT
    m.id              AS form_main_id,
    m.field0223       AS owner_name,
    m.field0160       AS ipdp_name,
    s.id              AS sub_row_id,
    s.field0214       AS item_name,
    s.field0218       AS current_file_id
FROM formmain_4070 m
INNER JOIN formson_5464 s ON s.formmain_id = m.id
WHERE 1 = 1
  -- AND m.state = ?           -- 表单状态过滤（待确认）
  -- AND m.modify_date >= ?    -- 增量同步（可选）
ORDER BY m.id, s.id;
```

> 子表关联键（已确认）：`formson_5464.formmain_id = formmain_4070.id`

#### 4.7.3 回写 SQL（草案）

```sql
-- 将 REST 上传响应 fileurl 写入 field0218（覆盖模式，见 B6 待确认）
UPDATE formson_5464
SET field0218 = :file_id
WHERE id = :sub_row_id;
```

#### 4.7.4 对接分工

| 步骤 | 通道 | 模块 |
|------|------|------|
| 查登记报告 + 资料行 | OA 库 SELECT | `integration.oa` → `OaRegReportDbClient` |
| 读共享盘文件 | SMB 挂载 | `ShareDriveClient` |
| 上传附件 | OA REST | `OaClient.uploadAttachment()` |
| 回写 field0218 | OA 库 UPDATE | `OaRegReportDbClient` |
| 幂等记录 | 集成 PG 库 | `OaRegAttachmentSyncService` |

#### 4.7.5 注意事项

| 项 | 说明 |
|----|------|
| 双数据源 | 集成 PG（中间表）与 OA 库（SQL Server/Oracle 等）**分离配置**，OA 库**不走 Flyway** |
| field0218 格式 | **已确认**：存文件 ID，取上传接口响应 **fileurl** |
| 覆盖 vs 追加 | B6 未确认前，SQL 直接 SET 为覆盖；追加需拼接原值 |
| 事务边界 | REST 上传与 SQL UPDATE **不在同一事务**；上传成功、UPDATE 失败记 RETRY |
| 权限最小化 | OA 库账号仅授权 SELECT formmain_4070/formson_5464 + UPDATE formson_5464.field0218 |
| OA 升级 | 直连库表耦合 OA 版本，升级时需回归验证 |

---

## 5. 具体操作方案

### 5.1 业务侧：共享盘存放资料（登记负责人）

**前置条件**：OA 国内登记报告已创建，field0223 登记负责人、field0160 IPDP名称 已填写正确。

| 步骤 | 操作 | 说明 |
|------|------|------|
| 1 | 打开共享盘根目录 | `\\192.168.1.8\国内登记资料` |
| 2 | 创建/进入三级目录 | `{登记负责人}/{IPDP名称}/{资料项目名称}/` |
| 3 | 上传资料文件 | 文件名带版本号，如 `农药登记变更申请表_v1.pdf` |
| 4 | 更新资料 | 在同目录新增更高版本，如 `_v2`、`_v3`；**不要删除旧版**（便于留痕） |
| 5 | 等待自动同步 | 中台定时任务扫描（默认每 10 分钟，见配置） |
| 6 | 在 OA 核对 | 打开登记报告 → 资料列表 Tab → 对应行「附件」列应出现文件 |

**目录命名注意**：

- 三级目录名须与 OA **field0223 / field0160 / field0214** 取值一致（名称完全匹配）
- IPDP 名称可能含 `%`、空格等，共享盘文件夹名须与 OA 显示一致
- 每个资料项目单独一个文件夹

**示例**（对应截图表单 `REG-202607-0128`）：

```
\\192.168.1.8\国内登记资料\
└── 杨燕玲\
    └── 10% 环丙氟虫胺可分散液剂\
        ├── 农药登记变更申请表\
        │   └── 农药登记变更申请表_v1.pdf
        ├── 营业执照及生产许可证\
        │   └── 营业执照及生产许可证_v1.pdf
        └── 资料真实合法性声明\
            └── 资料真实合法性声明_v1.pdf
```

---

### 5.2 系统侧：自动同步流程（集成中台）

**触发方式**：定时任务（默认 cron）+ 可选手动 API 触发。

```
【步骤 1】任务启动
    ├─ 检查 share-drive 挂载/连通性
    ├─ 检查 OA 库数据源连通性
    ├─ 检查 oa.attachment-sync.enabled = true
    └─ 获取 OA Rest Token（仅上传附件用）

【步骤 2】SQL 拉取待同步资料行
    ├─ JOIN formmain_4070 + formson_5464（见 §4.7.2）
    ├─ 过滤条件（待确认）：表单状态、修改时间等
    └─ 返回 owner_name / ipdp_name / item_name / sub_row_id

【步骤 3】逐行处理
    FOR EACH 资料行 row:
        ├─ 3.1 拼路径 sharePath = root / field0223 / field0160 / field0214
        ├─ 3.2 目录不存在 → SKIPPED / MISSING_DIR
        ├─ 3.3 取最新版 file；无文件 → NO_FILE
        ├─ 3.4 查中间表幂等 → 已 SUCCESS 且未变 → SKIP
        ├─ 3.5 REST 上传附件 → 取响应 fileurl
        ├─ 3.6 SQL UPDATE field0218 = fileurl WHERE id = sub_row_id
        └─ 3.7 中间表 → SUCCESS

【步骤 4】收尾（汇总、告警）
```

**单条资料行状态流转**：

```
（无记录）→ PENDING → SUCCESS
              ↓           ↑
            RETRY ────────┘（重试成功）
              ↓
            FAILED（达最大重试次数）
SKIPPED（目录缺失 / 无文件）
```

---

### 5.3 运维侧：部署与配置

| 步骤 | 操作 | 负责 |
|------|------|------|
| 1 | 在中台服务器挂载共享盘（SMB/NFS）至配置路径 | 运维 |
| 2 | 创建共享盘只读服务账号，授权 `{根目录}` 读权限 | 运维 |
| 3 | 配置 `share-drive.*`、`oa.reg-report.*`、**`oa.datasource.*`（OA 库）** | 开发 |
| 4 | 执行 Flyway 创建中间表（**仅 PG 集成库**） | 开发 |
| 5 | 配置 OA REST 账号（**仅附件上传**） | 运维 |
| 6 | 配置 OA 库只读+field0218 写权限账号 | 运维/DBA |
| 6 | 启用定时任务 `oa.attachment-sync.enabled=true` | 运维 |
| 7 | 配置企微 Webhook（失败告警，复用现有能力） | 运维 |
| 8 | 用测试表单 + 测试目录跑通首轮同步 | 开发/业务 |

---

### 5.4 手动触发与查询（上线后）

| 场景 | 操作 |
|------|------|
| 业务刚上传新版本，不想等定时任务 | 调用 `POST /api/v1/oa/reg-reports/attachment-sync/trigger?formBizNo=REG-xxx` |
| 查看某表单各资料项目同步情况 | 调用 `GET /api/v1/oa/reg-reports/{bizNo}/attachment-sync/records` |
| 排查某行一直未同步 | 查中间表 `sync_message`（MISSING_DIR / NO_FILE / ITEM_MAP_FAILED 等） |
| OA 接口持续失败 | 查日志 `【OaRegAttachmentSync】`；确认 Token、附件 API、网络 |

---

### 5.5 异常处理手册

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| OA 附件列一直为空 | 目录路径不匹配 | 核对三级目录名与 field0223/field0160/field0214 |
| 同步了旧版文件 | 版本号命名不规范 | 统一 `_vN`；检查同目录文件 |
| 重复上传同一文件 | 中间表未写入或版本号变化 | 查 PG 记录；确认幂等键 |
| 任务全部 SKIP | 共享盘目录均无文件或路径不匹配 | 核对三级目录名与 OA 名称字段 |
| 共享盘不可达 | 挂载失效 / 账号过期 | 运维恢复挂载；任务会 fail-fast 告警 |
| 新版本未覆盖旧附件 | OA 侧为追加模式 | 确认 §12.4 Q9 覆盖/追加策略 |

---

## 6. 功能点

| 序号 | 功能点 | 描述 | 必选/可选 |
|------|--------|------|-----------|
| F1 | SQL 查 OA 资料列表 | JOIN formmain_4070 + formson_5464 拉取待同步行 | 必选 |
| F2 | 共享盘路径匹配 | 按 field0223 / field0160 / field0214 拼三级路径 | 必选 |
| F3 | 最新版本解析 | 同目录多版本时取最新版 | 必选 |
| F4 | 幂等同步上传 | 中间表记录已同步版本，相同版本不重复上传 | 必选 |
| F5 | REST 上传 + SQL 回写 | REST 上传附件；SQL UPDATE field0218 | 必选 |
| F6 | 同步状态与重试 | SUCCESS/RETRY/FAILED/SKIPPED；失败重试 + 告警 | 必选 |
| F7 | 名称映射 | 资料项目、负责人姓名标准化 | 必选 |
| F8 | 手动触发同步 | 按表单编号或全量触发 | 可选 |
| F9 | 同步记录查询 API | 查询同步状态、版本、失败原因 | 可选 |
| F10 | 缺失目录告警 | 企微通知负责人 | 可选 |

**明确不做**：

| 项 | 说明 |
|----|------|
| 「需要」字段 | 不参与同步逻辑，不读取、不判断 |
| 「完成时间」字段 | 不同步、不回写 |
| 其他子表字段 | 除「附件」外均不写入 |

---

## 7. 中间表设计

```
t_integration_oa_reg_attachment_sync（登记报告附件同步记录）
├─ id                      BIGINT PK
├─ form_record_id          VARCHAR   -- OA 表单/流程实例 ID（日志追踪，非幂等键）
├─ owner_name              VARCHAR   -- field0223 登记负责人（幂等键）
├─ ipdp_name               VARCHAR   -- field0160 IPDP名称（幂等键）
├─ item_name               VARCHAR   -- field0214 资料项目名称（幂等键）
├─ item_row_id             VARCHAR   -- formson_5464 行 ID（写 OA 定位用）
├─ share_path              VARCHAR   -- 共享盘完整目录路径
├─ file_name               VARCHAR   -- 同步的文件名
├─ file_version            INTEGER   -- 解析版本号
├─ file_size               BIGINT
├─ file_checksum           VARCHAR   -- SHA-256
├─ file_modified_at        TIMESTAMP
├─ oa_file_id              VARCHAR   -- OA 文件 ID（上传响应 fileurl）
├─ sync_status             VARCHAR   -- PENDING/SUCCESS/RETRY/FAILED/SKIPPED
├─ sync_message            VARCHAR
├─ retry_count             INT
├─ last_sync_at            TIMESTAMP
├─ created_at / updated_at / is_deleted
```

**唯一索引（按名称，已确认）**：

```sql
UNIQUE (owner_name, ipdp_name, item_name, file_version) WHERE is_deleted = 0
```

---

## 8. 涉及系统与模块

| 系统 | 模块/接口 | 说明 |
|------|-----------|------|
| 共享盘 | SMB 挂载 | 业务源 |
| **致远 OA 数据库** | formmain_4070 / formson_5464 | **SQL 读列表、写 field0218** |
| 致远 OA REST | 附件上传 API | **仅**上传文件 |
| 集成中台 integration | `ShareDriveClient`、`OaRegReportDbClient`、`OaClient` | 读盘、查表、上传 |
| 集成中台 async | `OaRegAttachmentSyncTask` | 定时任务 |
| 集成中台 manager | `OaRegAttachmentSyncManager` | 编排 |
| 集成中台 dao（PG） | 中间表 Mapper | 幂等记录 |
| 企微 Webhook | 告警 | 失败通知 |

---

## 9. 接口草案

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/oa/reg-reports/attachment-sync/trigger` | 手动触发（可选 `formBizNo`） |
| GET | `/api/v1/oa/reg-reports/{bizNo}/attachment-sync/records` | 按表单查同步记录 |
| GET | `/api/v1/oa/reg-reports/attachment-sync/records` | 分页查询（按状态/时间） |

---

## 10. 技术方案（确认信息后细化）

### 10.1 配置项

```yaml
oa:
  reg-report:
    form-main-table: formmain_4070
    form-sub-table: formson_5464
    field-owner: field0223
    field-ipdp-name: field0160
    field-item-name: field0214
    field-attachment: field0218
    sub-table-fk: formmain_id          # 已确认
  datasource:
    url: jdbc:sqlserver://...          # OA 库连接（类型/地址待确认，配置注入）
    username:
    password: ${OA_DB_PASSWORD}
  attachment-sync:
    enabled: true
    cron: "0 */10 * * * *"
    batch-size: 50
    max-retry: 3
share-drive:
  protocol: smb
  root-path: "\\\\192.168.1.8\\国内登记资料"
  username: liuyanmiao
  password: ${SHARE_DRIVE_PASSWORD}
  version-pattern: "_v(\\d+)"
  allowed-extensions: [pdf, doc, docx, xls, xlsx, png, jpg, jpeg, zip]
  max-file-size-bytes: 104857600
```

> 集成 PG 数据源沿用现有 `spring.datasource`；OA 库为**独立第二数据源**。

### 10.2 代码模块（预估）

| 模块 | 类/文件 | 说明 |
|------|---------|------|
| common | `OaRegReportProperties`、`OaDataSourceProperties` | 配置绑定 |
| dao（PG） | `OaRegAttachmentSyncDO` | 中间表 |
| integration | `ShareDriveClient` | 共享盘 |
| integration | **`OaRegReportDbClient`** | OA 库 SELECT / UPDATE field0218 |
| integration | `OaClient`（扩展） | **仅**附件 REST 上传 |
| manager | `OaRegAttachmentSyncManager` | 编排 |
| async | `OaRegAttachmentSyncTask` | 定时任务 |
| web | `OaRegAttachmentSyncController` | 触发/查询 |
| web/config | `OaDataSourceConfiguration` | OA 第二数据源 Bean |

---

## 11. 验收标准

| 序号 | 验收点 | 通过标准 |
|------|--------|----------|
| 1 | 路径匹配 | 共享盘按规范存放后，中台能定位到正确目录 |
| 2 | 最新版本 | v1/v2/v3 并存时仅同步 v3 |
| 3 | 幂等 | 同版本重复跑任务不重复上传 OA |
| 4 | 新版本 | 新增 v4 后下轮自动同步 |
| 5 | OA 展示 | 资料列表对应行附件列可见正确文件 |
| 6 | 缺失目录 | 目录不存在记 SKIPPED，不误传 |
| 7 | 失败重试 | REST 或 SQL 失败记 RETRY，下轮可成功 |
| 8 | 告警 | 达最大重试次数触发企微告警 |
| 9 | SQL 回写 | formson_5464.field0218 已更新且 OA 页可打开 |
| 10 | 查表读 | JOIN 能正确拉出待同步行 |

---

## 12. 待确认信息清单

> **使用说明**：请逐项向对应责任方咨询，将「确认结果」列补充完整。全部必填项（★）确认后方可进入开发。

### 12.1 共享盘（咨询：IT 运维 / 基础设施）

| 编号 | 确认项 | 咨询对象 | 确认结果 | 备注 |
|------|--------|----------|----------|------|
| S1 ★ | 共享盘协议（SMB / NFS / 其他） | IT 运维 | **SMB** | 基于 UNC 路径 |
| S2 ★ | 共享盘根路径（UNC 或挂载路径） | IT 运维 | **`\\192.168.1.8\国内登记资料`** | 已确认 |
| S3 ★ | 中台服务器是否可挂载该共享盘 | IT 运维 | | 待运维验证 |
| S4 ★ | 中台服务账号（只读）及授权范围 | IT 运维 | **账号 `liuyanmiao`**；密码配置于 yml | 密码不入库 |
| S5 | 单文件大小上限 | IT 运维 | | 默认 100MB |
| S6 | 是否有防病毒/扫描延迟 | IT 运维 | | 上传后文件何时可读 |

### 12.2 业务规则（咨询：国内登记部 / 业务负责人）

| 编号 | 确认项 | 咨询对象 | 确认结果 | 备注 |
|------|--------|----------|----------|------|
| B1 ★ | 版本命名规范（`_vN` / `Vn_` / 其他） | 登记部 | | 建议 `_vN` |
| B2 | ~~「需要=不需要」的行是否跳过同步~~ | — | **已确认：不参与同步逻辑，全部资料行均扫描** | |
| B3 | ~~登记负责人目录名~~ | — | **已确认：与 field0223 名称完全一致** | |
| B4 | ~~资料项目目录名~~ | — | **已确认：与 field0214 名称完全一致** | |
| B13 ★ | IPDP 名称含特殊字符时共享盘文件夹名是否与 OA 一致 | 登记部 | **已确认：完全一致** | 如 `10% 环丙氟虫胺可分散液剂` |
| B5 | 同一资料项目目录内是否允许多个非版本并列文件 | 登记部 | | |
| B6 ★ | 新版本同步后，OA 旧附件：**覆盖**还是**追加** | 登记部 | | |
| B7 | 同版本号但文件内容变更（checksum 变）是否重新上传 | 登记部 | | |
| B8 | 表单哪些状态下继续同步（草稿/审批中/已完成/作废） | 登记部 | | |
| B9 | ~~同步成功后是否自动回写「完成时间」~~ | — | **已确认：不回写** | |
| B10 | 目录缺失/无文件是否需企微通知负责人 | 登记部 | | |
| B11 ★ | 允许上传的文件扩展名列表 | 登记部 | | |
| B12 | 同步频率期望（如每 10 分钟 / 每小时） | 登记部 | | |

### 12.3 致远 OA（咨询：OA 管理员 / OA 实施）

| 编号 | 确认项 | 咨询对象 | 确认结果 | 备注 |
|------|--------|----------|----------|------|
| O1 ★ | 国内登记报告表单模板编码 / formId | OA 管理员 | | 主表物理名已确认为 formmain_4070 |
| O2 ★ | 主表字段名 | OA 管理员 | **field0223 登记负责人、field0160 IPDP名称** | 已确认 |
| O3 ★ | 资料列表子表表名 | OA 管理员 | **formson_5464** | 已确认 |
| O4 ★ | 子表字段名 | OA 管理员 | **field0214 资料项目、field0218 资料附件** | 已确认 |
| O5 | ~~资料项目编码~~ | — | **已确认：不使用编码，按 field0214 名称匹配** | |
| O6 ★ | formson_5464 主键 id（UPDATE WHERE 条件） | OA/DBA | **默认 formson_5464.id** | 实施时核对 |
| O7 | ~~登记报告列表 REST~~ | — | **已确认：改 OA 库 SQL 查询** | 见 §4.7.2 |
| O8 | ~~表单详情 REST~~ | — | **已确认：改 OA 库 JOIN 查询** | 见 §4.7.2 |
| O9 ★ | 附件上传 REST API | OA 管理员 | | **唯一 REST 调用** |
| O10 | ~~子表 REST 更新~~ | — | **已确认：改 SQL UPDATE field0218** | 见 §4.7.3 |
| O14 | ~~field0218 格式~~ | — | **已确认：文件 ID，取上传响应 fileurl** | |
| O15 ★ | OA 库 JDBC 连接串（host/port/database） | DBA | | D1–D3 连通性已确认 |
| O16 | ~~formson 外键列~~ | — | **已确认：formmain_id** | |
| O11 | 附件大小/类型限制（OA 侧） | OA 管理员 | | |
| O12 | 列表查询过滤条件（按状态/修改时间/部门） | OA 管理员 | | |
| O13 | Token 使用方式（复用现有 REST 账号或按发起人） | OA 管理员 | | |

### 12.4 集成中台（内部确认）

| 编号 | 确认项 | 确认结果 | 备注 |
|------|--------|----------|------|
| P1 ★ | 定时任务 cron 表达式 | | 默认 `0 */10 * * * *` |
| P2 ★ | 单轮最多处理资料行数 batch-size | | 默认 50 |
| P3 ★ | 最大重试次数 max-retry | | 默认 3 |
| P4 | 是否提供手动触发 API | | |
| P5 | 失败告警企微 Webhook（复用现有） | | |
| P6 | 资料项目名称映射表放 DB 还是 yml | | |

### 12.5 OA 字段映射表（待 OA 管理员填写）

| 业务含义 | 表 / 字段 | 示例值 | 已确认 |
|----------|-----------|--------|--------|
| 登记信息主表 | formmain_4070 | — | ☑ |
| 资料列表子表 | formson_5464 | — | ☑ |
| 登记负责人 | formmain_4070.**field0223** | 杨燕玲 | ☑ |
| IPDP 名称 | formmain_4070.**field0160** | 10% 环丙氟虫胺可分散液剂 | ☑ |
| 资料项目 | formson_5464.**field0214** | 农药登记变更申请表 | ☑ |
| 资料附件（唯一写入） | formson_5464.**field0218** | — | ☑ |
| 子表行 ID | formson_5464.**id** | | ☑（默认，DBA 核对） |

### 12.6 OA REST 接口（仅附件上传）

| 接口用途 | HTTP 方法 | URL 路径 | 已确认 |
|----------|-----------|----------|--------|
| 附件文件上传 | | | 响应含 **fileurl** | ☐ |

> 列表查询、field0218 回写**均走 OA 库 SQL**，见 §4.7、§12.8。

### 12.8 OA 数据库（咨询：DBA / OA 实施）

| 编号 | 确认项 | 确认结果 | 备注 |
|------|--------|----------|------|
| D1 ★ | OA 库类型（SQL Server / Oracle / MySQL） | | JDBC 类型待配置 |
| D2 ★ | JDBC 连接串（host/port/database） | | 写入 application yml |
| D3 ★ | 中台服务器能否访问 OA 库 | **已确认：能访问** | |
| D4 ★ | 集成中台专用 DB 账号 | | SELECT 两表 + UPDATE field0218 |
| D5 ★ | formson_5464 关联 formmain_4070 的外键列 | | **已确认：formmain_id** | |
| D6 ★ | field0218 存储格式 | | **已确认：文件 ID，取上传响应 fileurl** | |
| D7 | 同步 SQL 的 WHERE 条件（状态/时间） | | 见 O12 |
| D8 | 子表是否有逻辑删除列需过滤 | | 如 state / is_deleted |

### 12.7 名称映射表（备用）

> B4 已确认：field0214 与共享盘 L3 文件夹名**完全一致**。下表仅当个别名称无法一致时备用。

| field0214 资料项目名称 | 共享盘 L3 目录名 | 已确认 |
|------------------------|------------------|--------|
| 农药登记变更申请表 | 农药登记变更申请表 | ☑ |
| … | | |

---

## 13. 风险与依赖

| 风险/依赖 | 影响 | 应对措施 |
|-----------|------|----------|
| §12 必填项未确认 | 无法开发 | 优先 O9（上传 API URL）、O15/D2（JDBC）、B6 |
| OA 库直连 | 与 OA 版本耦合 | 限定表/列权限；POC 验证 UPDATE 后 UI 可打开 |
| REST 成功、SQL 失败 | field0218 未更新 | 记 RETRY |
| 共享盘不可用 | 无法读文件 | 挂载监控 + fail-fast 告警 |

---

## 14. 变更记录

| 日期 | 修改人 | 修改内容 |
|------|--------|----------|
| 2026-07-23 | — | 创建文档 |
| 2026-07-24 | — | 新增共享盘驱动方案并调整为推荐 |
| 2026-07-24 | — | 确认 D3 可访问 OA 库、formmain_id 外键、field0218 取上传响应 fileurl |
