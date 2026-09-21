# REQ-MOKA-001 iHR 人员信息同步 Moka

> 复制自 `_template/requirement-template.md`，按项目规范填写。

---

## 1. 基本信息

| 项目 | 内容 |
|------|------|
| 需求编号 | REQ-MOKA-001 |
| 需求标题 | iHR 人员信息同步 Moka |
| 需求类型 | integration |
| 所属系统 | moka |
| 优先级 | P1（高，Moka 组织架构已上线，人员同步是配套必选项） |
| 状态 | draft |
| 提出人 | hongfu_zhou@cacch.com |
| 提出日期 | 2026-09-20 |
| 负责人 | hongfu_zhou@cacch.com |
| 预计上线 | — |

---

## 2. 需求背景

当前 CACCH 已完成以下两条链路：

1. **iHR 人员数据 → PG 库 `persondetail` 表**：由独立同步流程负责（不在本项目范围），数据已稳定落库。
2. **Moka 组织架构（部门）同步**：已上线，采用「两阶段、双接口」模式（`IHR 快照表 → Moka 中间表 → Moka 开放平台`），由 ESB 定时调度。

本次需补齐**人员信息**的同步能力，形成 Moka 侧完整的「组织架构 + 人员」全量数据通道。

### 为什么要落本地中间表？

而不是直接 `persondetail → Moka` 透传：

1. **业务解耦 & 可复用性**：本项目作为统一的「iHR → Moka」集成枢纽，中间表可被其他系统/流程复用（如未来 Moka People 模块对接、数据校验后台等）
2. **幂等 & 可追溯**：中间表记录 `moka_sync_status`（0-待推送 / 1-已同步 / 2-同步失败），推送失败可重试，成功可对账
3. **ESB 调度独立**：ESB 只负责「什么时候调接口」，不关心「数据怎么转换」，本项目接口是无状态的纯业务操作
4. **统一项目内多个同步子流程的实现范式**：与已上线的 Moka 部门同步保持相同的代码组织风格和落地节奏，便于后续迭代时快速对照

---

## 3. 需求描述

### 3.1 功能点

| 序号 | 功能点 | 描述 | 必选/可选 |
|------|--------|------|-----------|
| 1 | Moka 角色查询 & 本地落库 | 新建 `t_integration_moka_role` 表，调用 Moka 角色查询接口拉取全量角色数据落库；提供接口供 ESB 定时触发，仅做数据落库不做推送 | 必选 |
| 2 | Moka 人员中间表 | 在 PG 库新建 `t_integration_moka_person` 表，存储从 persondetail 映射的人员数据 | 必选 |
| 3 | 接口 1：iHR → Moka 中间表同步 | `POST /api/v1/moka/persons/sync-from-ihr`，查询 persondetail + 通过 departmentId 关联 ihr_department 表获取 department_code，批量 upsert 到中间表 | 必选 |
| 4 | 接口 2：中间表 → Moka 开放平台推送 | `POST /api/v1/moka/persons/push-to-moka`，读取中间表批量调用 Moka API，更新本地同步状态 | 必选 |

### 3.2 交互流程

**整体链路**（ESB 按依赖顺序定时调度）：

```
ESB 定时调度器
    │
    │ ⓪ POST /api/v1/moka/roles/sync-from-moka
    ▼
集成中台
    ├─ 调用 Moka GET 角色查询接口
    └─ 全量 upsert 到 t_integration_moka_role

    │
    │ ① POST /api/v1/moka/persons/sync-from-ihr
    ▼
集成中台
    │
    ├─ 直接查询 PG.persondetail（全量）
    ├─ 通过 persondetail.departmentId
    │   关联查询 t_integration_ihr_department.ihr_dept_id
    │   → 得到 department_code
    ├─ superiorEmail 提取（从 persondetail.superiorsInfo 字段）
    ├─ 字段映射（iHR → Moka）
    ├─ role_id 先写固定值（等角色拉取后再根据 jobTitle 匹配）
    ├─ deactivated 转换（在职 → 0，离职/停用 → 1）
    └─ 批量 upsert 到 t_integration_moka_person
    │     （moka_sync_status 重置为 0-PENDING）
    ▼
PG 数据库
    │
    │ ② POST /api/v1/moka/persons/push-to-moka
    ▼
集成中台
    │
    ├─ 读取 t_integration_moka_person（全量 或 PENDING+FAILED）
    ├─ DO → Moka API DTO 字段映射
    ├─ 分批推送（每批 ≤ 100 条）
    ├─ 调用 Moka POST /api-platform/v1/users/syncInfo
    └─ 批量更新 moka_sync_status：成功→1 失败→2
    │
    ▼
Moka 开放平台
    （syncInfo 自动执行 新增/更新/标记删除，
     uniqueType="number" 指定以工号做唯一性匹配）
```

---

## 4. 业务流程

### 4.0 功能点 0（新增）— Moka 角色查询 & 本地落库

> Moka 新增人员时，首次创建需要传入 `roleId`（自定义角色 ID）。角色列表不是硬编码的固定值，而是 Moka 后台可配置的动态数据。因此在推送人员之前，需要先拉取最新的角色列表到本地表。

**接口**：`POST /api/v1/moka/roles/sync-from-moka`

1. 调用 Moka 角色查询接口（[API 文档](https://www.mokahr.com/docs/api/?shell#-75)，**GET** 方法）
2. 解析响应体中的角色列表
3. 批量 upsert 到 `t_integration_moka_role` 表（以 `role_id` 为业务冲突键）

**返回**：`{ totalFetched, upserted }`

**Moka 角色查询接口**：

| 项目 | 详情 |
|------|------|
| 文档地址 | https://www.mokahr.com/docs/api/?shell#-75 |
| HTTP 方法 | **GET** |
| 完整路径 | 由 Moka 后台提供（角色查询接口） |
| 鉴权 | Basic Auth（与部门同步一致） |

### 4.1 接口 1 — iHR persondetail → Moka 中间表

1. 查询 PG 库 `persondetail` 全量记录（或按后续扩展参数过滤）
2. **部门关联查询**：通过 `persondetail.departmentId` 关联查询 `t_integration_ihr_department` 表（WHERE `ihr_dept_id = departmentId`），获取对应的 `department_code`
3. **直属领导提取**：从 `persondetail.superiorsInfo` 字段中提取直属上级的邮箱（`superior_email`）
4. 字段映射（详见第六节）
5. `role_id` **先暂定写固定值**（如 `40`，管理员角色），等角色拉取接口（功能点 0）稳定后，改为从 `t_integration_moka_role` 按 `jobTitle` 匹配得到对应的 `role_id`
6. `employeeStatus` → `deactivated` 转换：在职 → 0，离职/停用 → 1
7. 确定推送时固定参数：`uniqueType` = `"number"`（以工号做唯一性匹配），`locale` = `"zh-CN"`，`timezone` = `"Asia/Shanghai"`
8. 批量 upsert 落库（以 `user_id` 为业务冲突键），`moka_sync_status` 重置为 0（PENDING）

**返回**：`{ totalFetched, upserted }`

### 4.2 接口 2 — Moka 中间表 → Moka 开放平台

1. 查询 `t_integration_moka_person` 全量（或 `moka_sync_status IN (0, 2)`）
2. DO → Moka API DTO 字段映射（含固定参数：`uniqueType="number"`、`locale="zh-CN"`、`timezone="Asia/Shanghai"`、`updateDepartment=true`、`autoActivated=1`）
3. **分批推送**（每批 ≤ 100 条，保守设值避免请求体过大）调用 Moka API
4. Moka syncInfo 同步语义：以 `uniqueType` 指定的字段（number）做唯一性匹配，自动 **新增 / 更新 / 标记删除**
5. **注意**：`deactivated=1` 时首次创建用户会导致创建失败（Moka API 约束）；因此离职员工若为系统内**新用户**（Moka 侧此前不存在），推送时需特殊处理
6. 根据推送结果批量更新本地 `moka_sync_status`：成功 → 1，失败 → 2（同时记录 `last_sync_time` + `last_sync_result`）

**返回**：`{ totalRead, batchCount, mokaApiSuccess, syncedCount, syncFailedCount, dbUpdateFailedCount }`

---

## 5. 涉及系统与模块

| 系统 | 模块/接口 | 说明 |
|------|-----------|------|
| **PG 数据库** | `persondetail` 表 | iHR 人员数据源（外部表，仅查询） |
| **PG 数据库** | `t_integration_ihr_department` 表 | iHR 部门快照表（已存在），通过 `ihr_dept_id` 关联获取 `department_code` |
| **PG 数据库** | `t_integration_moka_role` 表 | Moka 角色中间表（**本次新增**，功能点 0） |
| **PG 数据库** | `t_integration_moka_person` 表 | Moka 人员中间表（**本次新增**，功能点 1） |
| **Moka 开放平台** | 角色查询接口（[文档](https://www.mokahr.com/docs/api/?shell#-75)） | 拉取 Moka 后台全部自定义角色，用于 roleId 匹配 |
| **Moka 开放平台** | 人员同步接口（[文档](https://www.mokahr.com/docs/api/#-72)） | `POST https://api.mokahr.com/api-platform/v1/users/syncInfo` |
| **Moka 开放平台** | Basic Auth（API Key 为 username） | 鉴权方式与部门同步一致 |
| **集成中台（本项目）** | Controller / Manager / Service / Mapper / Client | 按现有分层架构新增代码 |
| **ESB** | 定时调度 | 负责触发两个接口的调用时机，建议顺序：先拉角色 → 再同步人员 → 再推 Moka |

---

## 6. 字段映射

### 6.0 功能点 0：Moka 角色查询响应 → `t_integration_moka_role`

| Moka 角色查询响应字段 | 中间表字段 | 类型 | 转换规则 | 备注 |
|----------------------|-----------|------|----------|------|
| `roleId`（或 `id`） | `role_id` | INTEGER | 直接映射 | **业务唯一键**，UNIQUE |
| `name`（或 `roleName`） | `role_name` | VARCHAR(128) | 直接映射 | 角色显示名 |
| `description`（可选） | `description` | VARCHAR(500) | 直接映射 | 角色描述 |

> ⚠️ 以上字段为通用 REST 风格假设，开发前需实际调用确认 Moka 角色查询接口的**真实响应结构**（路径：`GET https://api.mokahr.com/api-platform/v1/xxx/roles`，具体路径以 Moka 文档为准）。

### 6.1 persondetail → Moka 中间表（接口 1）

| persondetail 字段 | 中间表字段 | 类型 | 转换规则 | 备注 |
|-------------------|-----------|------|----------|------|
| `userId` | `user_id` | VARCHAR(64) | 直接映射 | **业务唯一键**，UNIQUE |
| `employeeNo` | `employee_no` | VARCHAR(64) | 直接映射 | Moka number |
| `userName` | `user_name` | VARCHAR(128) | 直接映射 | Moka name |
| `userName` | `nickname` | VARCHAR(128) | 直接映射 | Moka nickname（昵称/花名，暂与 userName 同值） |
| `companyEmail` | `company_email` | VARCHAR(200) | 直接映射 | 允许为空（不做校验） |
| `contactPhone` | `contact_phone` | VARCHAR(32) | 直接映射 | Moka phone |
| `departmentId` | `department_code` | VARCHAR(64) | **关联查询** `t_integration_ihr_department`（WHERE `ihr_dept_id = persondetail.departmentId`）获取 `department_code` | 非直接映射 |
| `superiorsInfo` | `superior_email` | VARCHAR(200) | 从 persondetail.superiorsInfo 字段中提取直属上级邮箱 | 新增字段 |
| `jobTitle` | `role_id` | INTEGER | **① 当前先写固定值（如 40）② 角色拉取接口上线后**改为：jobTitle → 查询 `t_integration_moka_role` 匹配得到 `role_id` | 中间表只存 roleId 计算结果，不存原始 job_title |
| `employeeStatus` | `employee_status` | VARCHAR(32) | 直接映射（保留 iHR 原始值） | — |
| `employeeStatus` | `deactivated` | SMALLINT | 在职 → 0；离职/停用 → 1 | 直接存中间表，避免每次推 Moka 时再转换 |
| （固定值） | `locale` | VARCHAR(16) | `"zh-CN"` | Moka 用户语言 |
| （固定值） | `timezone` | VARCHAR(64) | `"Asia/Shanghai"` | Moka 用户时区 |
| — | `moka_sync_status` | SMALLINT | 固定初始值 0 | PENDING，等推送 Moka 后更新 |
| — | `last_sync_time` | TIMESTAMP | 初始 NULL | 推送 Moka 时写入 |
| — | `last_sync_result` | VARCHAR(500) | 初始 NULL | 推送 Moka 时写入结果摘要 |

### 6.2 Moka 中间表 → Moka API 请求体（接口 2）

**Moka API 完整请求体字段**（按官方文档，POST `/api-platform/v1/users/syncInfo`）：

| Moka API 字段 | 必填条件 | 类型 | 中间表字段 | 转换规则 | 说明 |
|--------------|----------|------|-----------|----------|------|
| `usersInfo` | ✅ | array | — | 数组包装 | 人员信息数组 |
| `usersInfo[].email` | uniqueType=email 时必填 | String | `company_email` | 直接映射 | 邮箱 |
| `usersInfo[].name` | — | String | `user_name` | 直接映射 | 姓名 |
| `usersInfo[].phone` | uniqueType=phone 时必填 | String | `contact_phone` | 直接映射 | 电话 |
| `usersInfo[].number` | uniqueType=number 时必填 | String | `employee_no` | 直接映射 | 工号 |
| `usersInfo[].roleId` | 首次创建用户必填 | Integer | `role_id` | 直接映射 | **Moka 自定义角色 ID**（当前固定值，后续从角色表匹配） |
| `usersInfo[].departmentCode` | ✅ | String[] | `department_code` | **数组包装**：`new String[]{ deptCode }` | 传空数组表示所有部门 |
| `usersInfo[].superiorEmail` | — | String | `superior_email` | 直接映射 | 上级邮箱，空字符串会清空汇报关系 |
| `usersInfo[].deactivated` | ✅ | Integer | `deactivated` | 直接映射（0=不禁用 1=禁用） | **首次创建用户传 1 则不会创建成功** |
| `usersInfo[].uniqueType` | ✅ | String | 固定值 | **`"number"`**（不在中间表存储，Manager 层硬编码） | 指定用哪个字段做唯一性匹配 |
| `usersInfo[].locale` | — | String | `locale` | `"zh-CN"` | 用户语言，不传默认中文 |
| `usersInfo[].timezone` | — | String | `timezone` | `"Asia/Shanghai"` | 用户时区，不传默认 Asia/Shanghai |
| `usersInfo[].updateDepartment` | — | Boolean | 固定值 | **true** | 是否更新部门，不传默认 true |
| `usersInfo[].updateSuperiorEmail` | — | Boolean | 固定值 | **true** | 是否更新上级汇报关系，不传默认 true |
| `usersInfo[].autoActivated` | — | Integer | 固定值 | **1** | 自动激活，不传默认 0 |
| `usersInfo[].nickname` | — | String | `nickname` | 直接映射 | 昵称、花名 |

### 6.3 role_id 取值策略（分两阶段）

| 阶段 | role_id 取值 | 说明 |
|------|-------------|------|
| **阶段一（当前）** | 写**固定值**，建议 `40`（管理员） | 先确保人员能推到 Moka 跑通链路；具体值可根据 Moka 后台实际角色调整 |
| **阶段二（角色拉取上线后）** | 按 `jobTitle` 匹配 `t_integration_moka_role` 表得到对应 role_id | 匹配失败则兜底固定值；需在代码中维护 jobTitle → roleName 的匹配关系 |

### 6.4 iHR persondetail → Moka 中间表 → Moka API 全链路映射总表

下表汇总 persondetail 原始字段如何经过中间表最终到达 Moka API：

| persondetail 字段 | 中间表字段 | Moka API 字段 | 类型 | 转换规则 |
|-------------------|-----------|--------------|------|----------|
| `userId` | `user_id` | — | VARCHAR(64) | 业务唯一键，内部用 |
| `employeeNo` | `employee_no` | `usersInfo[].number` | String | 直接映射 |
| `userName` | `user_name` | `usersInfo[].name` | String | 直接映射 |
| `userName` | `nickname` | `usersInfo[].nickname` | String | 暂与 userName 同值 |
| `companyEmail` | `company_email` | `usersInfo[].email` | String | 允许为空 |
| `contactPhone` | `contact_phone` | `usersInfo[].phone` | String | 直接映射 |
| `departmentId` | — | — | — | **关联查询** ihr_department 获取 department_code |
| — | `department_code` | `usersInfo[].departmentCode` | String[] | **数组包装** |
| `superiorsInfo` | `superior_email` | `usersInfo[].superiorEmail` | String | 提取直属上级邮箱 |
| `jobTitle` | — | — | — | roleId 计算输入，不存中间表 |
| — | `role_id` | `usersInfo[].roleId` | Integer | **阶段一固定值；阶段二 jobTitle → moka_role 匹配** |
| `employeeStatus` | `employee_status` | — | VARCHAR(32) | iHR 原始值保留 |
| （employeeStatus 转换） | `deactivated` | `usersInfo[].deactivated` | Integer | 在职→0，离职→1 |
| — | `locale` | `usersInfo[].locale` | String | 固定 "zh-CN" |
| — | `timezone` | `usersInfo[].timezone` | String | 固定 "Asia/Shanghai" |
| — | — | `usersInfo[].uniqueType` | String | Manager 层硬编码 "number" |
| — | — | `usersInfo[].updateDepartment` | Boolean | Manager 层硬编码 true |
| — | — | `usersInfo[].updateSuperiorEmail` | Boolean | Manager 层硬编码 true |
| — | — | `usersInfo[].autoActivated` | Integer | Manager 层硬编码 1 |
| — | `moka_sync_status` | — | SMALLINT | 中间表同步状态 |
| — | `last_sync_time` | — | TIMESTAMP | 推送 Moka 时更新 |
| — | `last_sync_result` | — | VARCHAR | 推送 Moka 时记录 |

---

## 7. 技术方案

### 7.1 架构影响

新增业务域 `moka` 下的 **角色 + 人员** 子模块，复用现有 Moka 部门同步的代码模式：

```
dao/
  entity/moka/MokaRoleDO.java                  ← 新增（功能点 0）
  entity/moka/MokaPersonDO.java
  mapper/moka/MokaRoleMapper.java               ← 新增（功能点 0）
  mapper/moka/MokaPersonMapper.java

service/
  moka/api/IMokaRoleService.java               ← 新增（功能点 0）
  moka/api/impl/MokaRoleServiceImpl.java
  moka/api/IMokaPersonService.java
  moka/api/impl/MokaPersonServiceImpl.java

manager/
  moka/api/IMokaRoleSyncManager.java            ← 新增（功能点 0）
  moka/api/impl/MokaRoleSyncManagerImpl.java
  moka/api/IMokaPersonSyncManager.java          ← 接口 1
  moka/api/impl/MokaPersonSyncManagerImpl.java
  moka/api/IMokaPersonPushManager.java           ← 接口 2
  moka/api/impl/MokaPersonPushManagerImpl.java

integration/
  moka/client/MokaUserClient.java               ← 新增（syncInfo）
  moka/client/MokaRoleClient.java               ← 新增（角色查询，功能点 0）
  moka/client/dto/
    MokaRoleListResponse.java
    MokaUserSyncRequest.java
    MokaUserSyncResponse.java
    MokaUserItem.java

web/
  controller/moka/MokaRoleController.java       ← 新增（功能点 0）
  controller/moka/MokaPersonController.java     ← 新增
  convert/moka/MokaRoleConverter.java            ← 新增
  convert/moka/MokaPersonConverter.java         ← 新增
  dto/moka/vo/
    MokaRoleSyncResultVO.java                   ← 新增
    MokaPersonSyncResultVO.java
    MokaPersonPushResultVO.java
```

**事务边界**（严格遵守项目规范第十二节）：

| 场景 | 处理方式 |
|------|----------|
| Manager 层 upsert 中间表 | `@Transactional(rollbackFor=Exception.class, propagation=REQUIRED, readOnly=false, timeout=30)` |
| Manager 层批量更新 sync_status | 同上 |
| persondetail 查询 + ihr_department 查询 | 纯 DB 只读，可选 `readOnly=true` |
| Moka HTTP 调用（角色查询 + 人员推送） | **禁止** `@Transactional` 包裹，放在事务外独立执行 |
| 分批推送中批次间 | 每批独立 try-catch，单条失败不阻断同批次其他条目 |

**MokaConstants 新增常量**：

```java
// 角色查询（拉取 Moka 全部自定义角色）
public static final String ROLE_LIST_PATH = "/api-platform/v1/xxx/roles"; // TODO: 开发前确认实际路径
// 同步人事信息给 Moka 系统（POST，自动新增/更新/标记删除）
public static final String USER_SYNC_INFO_PATH = "/api-platform/v1/users/syncInfo";
```

### 7.2 数据库变更

#### V23 — Moka 角色中间表（功能点 0）

**Flyway 脚本**：`V23__moka_role.sql`

```sql
CREATE TABLE IF NOT EXISTS t_integration_moka_role (
    id                    BIGINT       NOT NULL,   -- 内部主键（雪花生成）
    role_id               INTEGER      NOT NULL,   -- Moka 自定义角色 ID（业务唯一键，对应 Moka roleId）
    role_name             VARCHAR(128),             -- Moka 角色名称（对应 Moka roleName）
    description           VARCHAR(500),             -- 角色描述（对应 Moka description）
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted            SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_role PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_role_role_id UNIQUE (role_id)
);

COMMENT ON TABLE  t_integration_moka_role                     IS 'Moka角色信息中间表';
COMMENT ON COLUMN t_integration_moka_role.role_id            IS 'Moka 自定义角色 ID（业务唯一键，UNIQUE）';
COMMENT ON COLUMN t_integration_moka_role.role_name           IS 'Moka 角色名称';
COMMENT ON COLUMN t_integration_moka_role.description        IS 'Moka 角色描述';

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_role_name
    ON t_integration_moka_role(role_name) WHERE is_deleted = 0;
```

#### V24 — Moka 人员中间表（功能点 1）

**Flyway 脚本**：`V24__moka_person.sql`

```sql
CREATE TABLE IF NOT EXISTS t_integration_moka_person (
    id                    BIGINT       NOT NULL,   -- 内部主键（雪花生成）
    user_id               VARCHAR(64)  NOT NULL,   -- iHR 员工ID（persondetail.userId，业务唯一键）
    employee_no           VARCHAR(64),              -- 工号（Moka API number）
    user_name             VARCHAR(128),             -- 姓名（Moka API name）
    nickname              VARCHAR(128),             -- 昵称/花名（Moka API nickname，暂与 user_name 同值）
    company_email         VARCHAR(200),             -- 工作邮箱（Moka API email，允许为空）
    contact_phone         VARCHAR(32),              -- 工作电话（Moka API phone）
    role_id               INTEGER      NOT NULL DEFAULT 40, -- Moka 自定义角色 ID（Moka API roleId）。阶段一默认固定值 40；阶段二从 t_integration_moka_role 按 jobTitle 匹配后更新
    department_code       VARCHAR(64),              -- 部门编号（从 t_integration_ihr_department.department_code 关联查询获取）
    superior_email        VARCHAR(200),             -- 直属领导邮箱（从 persondetail.superiorsInfo 提取，Moka API superiorEmail）
    employee_status       VARCHAR(32),              -- 员工状态（iHR 原始值）
    deactivated           SMALLINT NOT NULL DEFAULT 0,  -- 0-不禁用 1-禁用（Moka API deactivated）
    locale                VARCHAR(16) NOT NULL DEFAULT 'zh-CN',   -- Moka 用户语言
    timezone              VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai', -- Moka 用户时区
    moka_sync_status      SMALLINT NOT NULL DEFAULT 0,  -- 0-PENDING 1-SYNCED 2-SYNC_FAILED
    last_sync_time        TIMESTAMP,
    last_sync_result      VARCHAR(500),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted            SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_person PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_person_user_id UNIQUE (user_id)
);

COMMENT ON TABLE  t_integration_moka_person                        IS 'Moka人员信息中间表';
COMMENT ON COLUMN t_integration_moka_person.user_id               IS 'iHR 员工ID（persondetail.userId，业务唯一键，UNIQUE）';
COMMENT ON COLUMN t_integration_moka_person.employee_no          IS '工号（Moka API number）';
COMMENT ON COLUMN t_integration_moka_person.user_name            IS '姓名（Moka API name）';
COMMENT ON COLUMN t_integration_moka_person.nickname             IS '昵称/花名（Moka API nickname）';
COMMENT ON COLUMN t_integration_moka_person.company_email        IS '工作邮箱（Moka API email，允许为空）';
COMMENT ON COLUMN t_integration_moka_person.contact_phone        IS '工作电话（Moka API phone）';
COMMENT ON COLUMN t_integration_moka_person.role_id              IS 'Moka 自定义角色 ID（Moka API roleId）。阶段一默认固定值 40（管理员）；阶段二从 t_integration_moka_role 按 jobTitle 匹配后更新';
COMMENT ON COLUMN t_integration_moka_person.department_code      IS '部门编号（关联查询 t_integration_ihr_department.ihr_dept_id 得到 department_code）';
COMMENT ON COLUMN t_integration_moka_person.superior_email       IS '直属领导邮箱（从 persondetail.superiorsInfo 提取，映射 Moka API superiorEmail）';
COMMENT ON COLUMN t_integration_moka_person.employee_status      IS '员工状态（iHR 原始值）';
COMMENT ON COLUMN t_integration_moka_person.deactivated          IS 'Moka API deactivated：0-不禁用 1-禁用。注意：首次创建用户传 1 则不会创建成功';
COMMENT ON COLUMN t_integration_moka_person.locale               IS 'Moka API locale：zh-CN 默认中文';
COMMENT ON COLUMN t_integration_moka_person.timezone             IS 'Moka API timezone：Asia/Shanghai 默认东八区';
COMMENT ON COLUMN t_integration_moka_person.moka_sync_status     IS 'Moka 开放平台同步状态：0-PENDING 1-SYNCED 2-SYNC_FAILED';
COMMENT ON COLUMN t_integration_moka_person.last_sync_time       IS '最近一次推送 Moka 的时间';
COMMENT ON COLUMN t_integration_moka_person.last_sync_result     IS '最近一次推送结果摘要';
COMMENT ON COLUMN t_integration_moka_person.created_at           IS '记录创建时间';
COMMENT ON COLUMN t_integration_moka_person.updated_at           IS '记录更新时间';
COMMENT ON COLUMN t_integration_moka_person.is_deleted           IS '逻辑删除：0-正常 1-删除';

-- 常用查询索引
CREATE INDEX IF NOT EXISTS idx_t_integration_moka_person_email
    ON t_integration_moka_person(company_email) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_person_sync_status
    ON t_integration_moka_person(moka_sync_status) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_person_emp_status
    ON t_integration_moka_person(employee_status) WHERE is_deleted = 0;

CREATE INDEX IF NOT EXISTS idx_t_integration_moka_person_employee_no
    ON t_integration_moka_person(employee_no) WHERE is_deleted = 0;
```

> **persondetail 表**为外部源表，不在本项目范围内。Mapper 层用 `@Select` 手写 SQL 直接查询，避免 MyBatis-Plus 自动注入逻辑删除等字段。
> **department_code 关联查询**：接口 1 的 Manager 需要调用 `IhrDepartmentMapper` 按 `ihr_dept_id` 批量查询 `department_code`（`WHERE ihr_dept_id IN (...)`），避免逐条查询的 N+1 问题。
> **superior_email 提取**：从 persondetail.superiorsInfo 字段中解析直属上级邮箱；若字段结构为 JSON 数组（常见），需解析后取第一层上级。

### 7.3 配置变更

无需新增配置项。Moka Base URL 和 API Key 复用现有 `MokaProperties`（`moka.base-url` + `moka.api-key`）。

### 7.4 涉及代码

| 模块 | 新增/修改文件 | 说明 |
|------|--------------|------|
| `common` | `constant/moka/MokaConstants.java`（修改） | 新增 `ROLE_LIST_PATH` 和 `USER_SYNC_INFO_PATH` |
| `dao` | `entity/moka/MokaRoleDO.java`（新增） | Moka 角色中间表实体 |
| `dao` | `entity/moka/MokaPersonDO.java`（新增） | Moka 人员中间表实体 |
| `dao` | `mapper/moka/MokaRoleMapper.java`（新增） | 角色 CRUD + upsert |
| `dao` | `mapper/moka/MokaPersonMapper.java`（新增） | 人员 CRUD + upsert + 按 sync_status 查询 |
| `service` | `moka/api/IMokaRoleService.java` + impl（新增） | 角色中间表读写服务 |
| `service` | `moka/api/IMokaPersonService.java` + impl（新增） | 人员中间表读写服务 |
| `service` | `ihr/api/IIhrDepartmentService.java`（**修改**，复用已有） | 需新增按 `ihr_dept_id` 批量查询的方法（`listByIhrDeptIds`） |
| `manager` | `moka/api/IMokaRoleSyncManager.java` + impl（新增） | 接口 0 编排（Moka API → 角色中间表） |
| `manager` | `moka/api/IMokaPersonSyncManager.java` + impl（新增） | 接口 1 编排（persondetail + ihr_department → 中间表） |
| `manager` | `moka/api/IMokaPersonPushManager.java` + impl（新增） | 接口 2 编排（中间表 → Moka API） |
| `integration` | `moka/client/MokaRoleClient.java`（新增） | Moka 角色查询 API 封装 |
| `integration` | `moka/client/MokaUserClient.java`（新增） | Moka users/syncInfo 调用封装 |
| `integration` | `moka/client/dto/*`（新增） | 角色 + 人员 请求/响应 DTO |
| `web` | `controller/moka/MokaRoleController.java`（新增） | 角色同步 REST 入口 |
| `web` | `controller/moka/MokaPersonController.java`（新增） | 人员同步 REST 入口 |
| `web` | `convert/moka/MokaRoleConverter.java`（新增） | 角色 MapStruct |
| `web` | `convert/moka/MokaPersonConverter.java`（新增） | 人员 MapStruct |
| `web` | `dto/moka/vo/*`（新增） | 接口返回 VO |
| `web` | `resources/db/migration/V23__moka_role.sql`（新增） | 角色表 Flyway DDL |
| `web` | `resources/db/migration/V24__moka_person.sql`（新增） | 人员表 Flyway DDL |

---

## 8. 验收标准

| 序号 | 验收点 | 验收方式 | 通过标准 |
|------|--------|----------|----------|
| 1 | 角色中间表 DDL 可执行 | 手动 | Flyway V23 启动无异常，`\d t_integration_moka_role` 表结构符合设计 |
| 2 | 人员中间表 DDL 可执行 | 手动 | Flyway V24 启动无异常，`\d t_integration_moka_person` 表结构符合设计 |
| 3 | 功能点 0 拉取 Moka 角色 | 手动调接口 | `POST /api/v1/moka/roles/sync-from-moka` 调用成功，角色列表落库 |
| 4 | 接口 1 同步 persondetail → 中间表 | 手动调接口 | 返回 `totalFetched` 与 persondetail 记录数一致，所有记录均 upsert 落库 |
| 5 | 接口 1 部门关联查询正确 | 数据校验 | 抽验 10 条记录，`department_code` 等于对应 `ihr_department` 记录的 `department_code` |
| 6 | 接口 1 superior_email 提取正确 | 数据校验 | 抽验 5 条有 superiorsInfo 的记录，`superior_email` 等于直属上级邮箱 |
| 7 | 接口 1 字段映射正确 | 数据校验 | 抽验 10 条记录，中间表各字段与 persondetail 源值一致 |
| 8 | 接口 2 推送 Moka 成功 | 手动调接口 + 查 Moka 后台 | 返回 `mokaApiSuccess=true`，Moka 后台人员数量与中间表一致，状态正确 |
| 9 | 接口 2 同步状态更新 | 数据校验 | 推送成功的记录 `moka_sync_status=1`，失败的记录 `moka_sync_status=2` |
| 10 | Moka API 分批推送 | 日志校验 | `batchCount ≥ totalRead/100`，每批独立不相互阻断 |
| 11 | 接口幂等性 | 重复调用 | 连续调用同一接口 3 次，数据库结果一致（无重复数据） |
| 12 | 敏感信息保护 | 日志审查 | 日志中不出现完整邮箱、手机号、身份证等敏感字段 |
| 13 | 异常场景处理 | 手动注入异常 | persondetail 查询异常 / Moka API 超时 / 批量 upsert 部分失败均不抛未捕获异常，有 INFO 日志说明 |
| 14 | 代码分层 & 依赖方向 | 静态审查 | 符合项目规范：Controller 不写业务逻辑、Manager 不直接操作 Mapper、DAO 无业务逻辑 |

---

## 9. 风险与依赖

| 风险/依赖 | 影响 | 应对措施 |
|-----------|------|----------|
| **persondetail 表结构变更**（iHR 接口字段变化） | 中间表映射需要调整 | 中间表独立于 persondetail，可通过 Flyway 加字段调整；代码层 Manager 集中维护映射逻辑 |
| **persondetail.superiorsInfo 结构不确定** | superior_email 提取失败 | 开发前先连库确认 superiorsInfo 字段的实际结构（JSON / 逗号分隔 / 嵌套对象），再选择合适的解析方式；解析失败时 superior_email 置空 |
| **部门关联查询 N+1 问题**（接口 1 每条员工都要查 department_code） | 大批量数据时性能下降 | Manager 层**批量查询**：先收集所有 `departmentId` → 一次性 `WHERE ihr_dept_id IN (...)` 查出 department_code → 内存 Map 匹配 |
| **persondetail.departmentId 无法匹配 ihr_department.ihr_dept_id** | 员工部门关联查不到，department_code 为空 | 查不到时 department_code 置空并打 WARN 日志，不阻断后续流程 |
| **Moka roleId 两阶段切换** | 阶段一固定值 40 → 阶段二按 jobTitle 匹配 | Manager 层将 roleId 映射逻辑封装为独立方法，后续修改逻辑时不影响其他流程；建议阶段二上线时全量刷新 role_id 列 |
| **Moka 角色查询接口响应结构需确认** | 字段名不确定（roleId vs id / roleName vs name） | 开发前先实际调用确认响应结构，DTO 字段按真实结构设计 |
| **Moka API `deactivated=1` 首次创建会失败** | 离职员工若为 Moka 新用户，推送时无法创建 | 推送前检查该员工是否已存在于 Moka（或中间表此前状态），对"首次创建即离职"的场景特殊处理（如先创建再禁用，或跳过并标记） |
| **Moka API 请求体大小**（syncInfo 单次上限未明示，保守设 100） | 大数据量推送失败 | Manager 层**分批推送**，每批 ≤ 100 条，批次间独立更新 sync_status |
| **persondetail 表直接依赖**（本项目 Mapper 写 SQL 查外部表） | MyBatis-Plus 自动注入可能产生问题 | Mapper 层用 `@Select` 手写 SQL 而非 BaseMapper，避免自动注入逻辑删除等字段 |
| **敏感字段暴露**（persondetail 含身份证、住址等） | 中间表如果超集存储会增加风险 | 中间表**只存 Moka 需要的字段**，不做超集存储；日志禁止打印完整邮箱/手机 |
| **ESB 调度编排顺序** | 需先拉角色 → 再同步人员 → 再推 Moka | ESB 建议编排：接口 0 → 接口 1 → 接口 2 顺序执行；接口 0 可低频（每天 1 次），接口 1 和 2 高频（30 分钟 / 1 小时） |
| **Moka API 限流** | 批量推送可能触发限流 | 观察 Moka API 响应时间和错误码，后续可考虑加批次间 delay |

---

## 10. 待确认事项（业务侧）

| 序号 | 问题 | 影响范围 | 建议 |
|------|------|----------|------|
| 1 | **Moka 角色查询接口实际响应结构**：roleId 字段名？roleName 字段名？是否分页？ | MokaRoleClient DTO 设计 | 开发前先实际调用 Moka 角色查询接口确认响应结构 |
| 2 | **Moka 角色查询接口完整 URL**：GET 路径？是否需要请求参数？ | MokaConstants.ROLE_LIST_PATH | 从 Moka 文档确认：https://www.mokahr.com/docs/api/?shell#-75 |
| 3 | **阶段二 roleId 映射表**：从 Moka 后台导出完整 roleId 列表后，persondetail.jobTitle 如何精确匹配？ | 接口 1 Manager 映射逻辑 | 阶段一先全部写固定值 40（管理员），阶段二提供 jobTitle → roleName → roleId 完整映射 |
| 4 | **persondetail.superiorsInfo 实际结构**：JSON 数组？逗号分隔？多层上级？ | 接口 1 superior_email 提取 | 开发前先连库验证，按实际结构写解析逻辑 |
| 5 | **Moka 人员推送是否全量**：是否像部门一样全量推送（不区分 PENDING/SYNCED），还是只推 PENDING + FAILED？ | 接口 2 推送策略 | 建议首次全量，后续只推 PENDING + FAILED |
| 6 | **ESB 调度频率**：角色查询 1 次/天 + 人员同步 30 分钟 + 推送 1 小时 是否合适？ | ESB 配置 | 根据数据变更量和 Moka API 限流调整 |
| 7 | **persondetail 表名/位置**：确认是同 PG 库、表名 `persondetail`、主键 `userId`、部门 ID 字段名为 `departmentId`、上级字段名为 `superiorsInfo`？ | 接口 1 SQL | 开发前需连库验证表结构 |
| 8 | **deactivated=1 首次创建失败**：离职员工如果是 Moka 新用户，推送时无法创建。是否需要特殊处理？ | 接口 2 推送策略 | 需业务确认离职员工是否需要在 Moka 创建账号 |

---

## 11. 变更记录

| 日期 | 修改人 | 修改内容 |
|------|--------|----------|
| 2026-09-20 | hongfu_zhou@cacch.com | 创建文档（draft） |
| 2026-09-20 | hongfu_zhou@cacch.com | 根据 Moka API 实际字段定义修订：删除接口 3、去 companyEmail 校验、role→roleId、新增 uniqueType/nickname/locale 字段、重写字段映射表 |
| 2026-09-20 | hongfu_zhou@cacch.com | 第三轮修订：API 文档链接改为 mokahr.com/docs/api/#-72、departmentId → ihr_department 关联、role_id 只存结果、新增 timezone、删除 job_title / department_name / third_party_id / unique_type |
| 2026-09-21 | hongfu_zhou@cacch.com | 第四轮修订：① 新增功能点 0（Moka 角色查询 + 本地落库 + 接口），原功能点顺延 ② role_id 暂定固定值 40，角色拉取后改为按 jobTitle 匹配 ③ 新增 superior_email 字段（取 persondetail.superiorsInfo） ④ Flyway 拆为 V23 角色表 + V24 人员表 ⑤ 说辞 "降低心智负担" → "统一项目内多个同步子流程的实现范式" ⑥ 新增风险/待确认事项覆盖角色查询 |
