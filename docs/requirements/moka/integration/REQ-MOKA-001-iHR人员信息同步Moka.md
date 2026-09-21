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
| 1 | Moka 角色查询 & 本地落库 | 新建 `t_integration_moka_role` 表，调用 Moka 角色查询接口（GET `/api-platform/v1/users/roles?type=all`）拉取全量角色数据落库；提供接口供 ESB 定时触发，仅做数据落库不做推送 | 必选 |
| 2 | Moka 人员中间表 | 在 PG 库新建 `t_integration_moka_person` 表，存储从 persondetail + organizationsdepartment 映射的人员数据 | 必选 |
| 3 | 接口 1：iHR → Moka 中间表同步 | `POST /api/v1/moka/persons/sync-from-ihr`，查询 persondetail + 通过 departmentId 关联 organizationsdepartment 获取 departmentcode，批量 upsert 到中间表 | 必选 |
| 4 | 接口 2：中间表 → Moka 开放平台推送 | `POST /api/v1/moka/persons/push-to-moka`，读取中间表批量调用 Moka API，更新本地同步状态 | 必选 |

### 3.2 交互流程

**整体链路**（ESB 按依赖顺序定时调度）：

```
ESB 定时调度器
    │
    │ ⓪ POST /api/v1/moka/roles/sync-from-moka
    ▼
集成中台
    ├─ 调用 Moka GET /api-platform/v1/users/roles?type=all
    └─ 全量 upsert 到 t_integration_moka_role

    │
    │ ① POST /api/v1/moka/persons/sync-from-ihr
    ▼
集成中台
    │
    ├─ 直接查询 PG.persondetail（全量）
    │   字段：id / staffNo / staffName / nickName /
    │         workEmail / mobileNo / departmentId / staffStatus
    ├─ 通过 persondetail.departmentId
    │   关联查询 organizationsdepartment.departmentcode
    ├─ 字段映射（iHR → 中间表）
    ├─ role_id 暂写固定值 223379（Moka 默认角色）
    ├─ deactivated 转换（staffStatus=QUIT → 1，其余 → 0）
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
    │   固定参数：uniqueType=phone / autoActivated=0 /
    │           updateDepartment=false / updateSuperiorEmail=false /
    │           thirdPartyId="" / locale=zh-CN / timezone=Asia/Shanghai
    └─ 批量更新 moka_sync_status：成功→1 失败→2
    │
    ▼
Moka 开放平台
    （syncInfo 自动执行 新增/更新/标记删除，
     uniqueType="phone" 指定以手机号做唯一性匹配）
```

---

## 4. 业务流程

### 4.0 功能点 0 — Moka 角色查询 & 本地落库

> Moka 新增人员时，首次创建需要传入 `roleId`（自定义角色 ID）。角色列表不是硬编码的固定值，而是 Moka 后台可配置的动态数据。因此在推送人员之前，需要先拉取最新的角色列表到本地表。

**接口**：`POST /api/v1/moka/roles/sync-from-moka`

1. 调用 Moka 角色查询接口（**GET** `https://api.mokahr.com/api-platform/v1/users/roles?type=all`，固定传 `type=all` 拉取全部角色）
2. 解析响应体中的角色列表（字段：`id` / `name` / `role` / `description`）
3. 批量 upsert 到 `t_integration_moka_role` 表（以 `role_id` 为业务冲突键）

**返回**：`{ totalFetched, personUpserted, deptCodeSkipped, invalidSkipped }`

**Moka 角色查询接口**（已联调确认）：

| 项目 | 详情 |
|------|------|
| 文档地址 | https://www.mokahr.com/docs/api/?shell#-75 |
| HTTP 方法 | **GET** |
| 完整路径 | `/api-platform/v1/users/roles?type=all` |
| 响应字段 | `id`（角色 ID，对应 DB role_id）、`name`（角色名称）、`role`（整数角色值）、`description`（描述） |
| 鉴权 | Basic Auth（API Key 为 username） |

### 4.1 接口 1 — iHR persondetail → Moka 中间表

1. 查询 PG 库 `persondetail` 全量记录（`@Select` 手写 SQL，列名为 camelCase：id / staffNo / staffName / nickName / workEmail / mobileNo / departmentId / staffStatus）
2. **部门关联查询**：通过 `persondetail.departmentId` 关联查询外部表 `organizationsdepartment`（WHERE `departmentId IN (...)`），获取对应的 `departmentcode`
3. 字段映射（详见第六节）
4. `role_id` **先写固定值 `223379`**（Moka 默认角色），等角色拉取接口（功能点 0）稳定后，改为按 `jobTitle` 匹配 `t_integration_moka_role` 得到对应的 `role_id`
5. `staffStatus` → `deactivated` 转换：`QUIT` → 1，其余（含 `IN_SERVICE`）→ 0
6. 批量 upsert 落库（以 `user_id` 为业务冲突键），`moka_sync_status` 重置为 0（PENDING）

**返回**：`{ totalFetched, personUpserted, deptCodeSkipped, invalidSkipped }`

### 4.2 接口 2 — Moka 中间表 → Moka 开放平台

1. 查询 `t_integration_moka_person` 全量（或 `moka_sync_status IN (0, 2)`）
2. DO → Moka API DTO 字段映射
3. **固定参数**（已确定，写入 `MokaConstants`）：

| Moka API 参数 | 值 | 常量名 |
|--------------|----|--------|
| uniqueType | `"phone"` | `USER_UNIQUE_TYPE` |
| autoActivated | `0` | `USER_AUTO_ACTIVATED` |
| updateDepartment | `false` | `USER_UPDATE_DEPARTMENT` |
| updateSuperiorEmail | `false` | `USER_UPDATE_SUPERIOR_EMAIL` |
| thirdPartyId | `""` | `USER_THIRD_PARTY_ID` |
| locale | `"zh-CN"` | `USER_LOCALE` |
| timezone | `"Asia/Shanghai"` | `USER_TIMEZONE` |

4. **分批推送**（每批 ≤ 100 条，保守设值避免请求体过大）调用 Moka API
5. Moka syncInfo 同步语义：以 `uniqueType="phone"` 指定的手机号字段做唯一性匹配，自动 **新增 / 更新 / 标记删除**
6. 根据推送结果批量更新本地 `moka_sync_status`：成功 → 1，失败 → 2（同时记录 `last_sync_time` + `last_sync_result`）

**返回**：`{ totalRead, batchCount, mokaApiSuccess, syncedCount, syncFailedCount, dbUpdateFailedCount }`

---

## 5. 涉及系统与模块

| 系统 | 模块/接口 | 说明 |
|------|-----------|------|
| **PG 数据库（外部表）** | `persondetail` 表 | iHR 人员数据源（外部表，仅查询）。联调确认字段：id / staffNo / staffName / nickName / workEmail / mobileNo / departmentId / staffStatus |
| **PG 数据库（外部表）** | `organizationsdepartment` 表 | 部门关联表（外部表，仅查询）。通过 `departmentId` 关联获取 `departmentcode` |
| **PG 数据库（本地）** | `t_integration_moka_role` 表 | Moka 角色中间表（**本次新增**，Flyway V23，功能点 0） |
| **PG 数据库（本地）** | `t_integration_moka_person` 表 | Moka 人员中间表（**本次新增**，Flyway V24，功能点 1） |
| **Moka 开放平台** | 角色查询接口（[文档](https://www.mokahr.com/docs/api/?shell#-75)） | `GET /api-platform/v1/users/roles?type=all`，拉取 Moka 后台全部角色 |
| **Moka 开放平台** | 人员同步接口（[文档](https://www.mokahr.com/docs/api/#-72)） | `POST https://api.mokahr.com/api-platform/v1/users/syncInfo` |
| **Moka 开放平台** | Basic Auth（API Key 为 username） | 鉴权方式与部门同步一致 |
| **集成中台（本项目）** | Controller / Manager / Service / Mapper / Client | 按现有分层架构新增代码 |
| **ESB** | 定时调度 | 负责触发接口调用时机，建议顺序：先拉角色 → 再同步人员 → 再推 Moka |

---

## 6. 字段映射

### 6.0 功能点 0：Moka 角色查询响应 → `t_integration_moka_role`

| Moka 角色查询响应字段 | 中间表字段 | 类型 | 转换规则 | 备注 |
|----------------------|-----------|------|----------|------|
| `id` | `role_id` | INTEGER | 直接映射 | **业务唯一键**，UNIQUE |
| `name` | `role_name` | VARCHAR(128) | 直接映射 | 角色显示名 |
| `role` | `role` | INTEGER | 直接映射 | 整数角色值 |
| `description`（可选） | `description` | VARCHAR(500) | 直接映射 | 角色描述 |

> ✅ 以上字段已通过实际调用 Moka 角色查询接口 `/api-platform/v1/users/roles?type=all` 确认。

### 6.1 persondetail → Moka 中间表（接口 1）

| persondetail 字段 | 中间表字段 | 类型 | 转换规则 | 备注 |
|-------------------|-----------|------|----------|------|
| `id` | `user_id` | VARCHAR(64) | 直接映射（SQL 列别名 AS userId） | **业务唯一键**，UNIQUE |
| `staffNo` | `employee_no` | VARCHAR(64) | 直接映射（SQL 列别名 AS employeeNo） | Moka number |
| `staffName` | `user_name` | VARCHAR(128) | 直接映射（SQL 列别名 AS userName） | Moka name |
| `nickName` | `nickname` | VARCHAR(128) | 直接映射（SQL 列别名 AS nickname） | Moka nickname（暂与 userName 同值） |
| `workEmail` | `company_email` | VARCHAR(200) | 直接映射（SQL 列别名 AS companyEmail） | 允许为空 |
| `mobileNo` | `contact_phone` | VARCHAR(32) | 直接映射（SQL 列别名 AS contactPhone） | Moka phone（uniqueType=phone 时必填） |
| `departmentId` | `department_code` | VARCHAR(64) | **关联查询** 外部表 `organizationsdepartment`（WHERE `departmentId` = persondetail.departmentId）获取 `departmentcode` | 非直接映射 |
| `staffStatus` | `employee_status` | VARCHAR(32) | 直接映射（SQL 列别名 AS employeeStatus） | IN_SERVICE=在职 / QUIT=离职 |
| `staffStatus` | `deactivated` | SMALLINT | `staffStatus=QUIT` → 1；其余 → 0 | 直接存中间表 |
| （固定值） | `role_id` | INTEGER | **固定值 223379**（Moka 默认角色） | 阶段一固定；阶段二按 jobTitle（待 persondetail 确认是否有此字段）匹配 |
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
| `usersInfo[].phone` | uniqueType=phone 时必填 | String | `contact_phone` | 直接映射 | **唯一标识**（uniqueType=phone） |
| `usersInfo[].name` | — | String | `user_name` | 直接映射 | 姓名 |
| `usersInfo[].nickname` | — | String | `nickname` | 直接映射 | 昵称、花名 |
| `usersInfo[].email` | — | String | `company_email` | 直接映射 | 邮箱，允许为空 |
| `usersInfo[].number` | — | String | `employee_no` | 直接映射 | 工号 |
| `usersInfo[].roleId` | 首次创建用户必填 | Integer | `role_id` | 直接映射 | 当前固定值 223379 |
| `usersInfo[].departmentCode` | ✅ | String[] | `department_code` | **数组包装**：`new String[]{ deptCode }` | 传空数组表示所有部门 |
| `usersInfo[].deactivated` | ✅ | Integer | `deactivated` | 直接映射（0=不禁用 1=禁用） | |
| `usersInfo[].uniqueType` | ✅ | String | 固定值 | **`"phone"`** | 指定用手机号做唯一性匹配 |
| `usersInfo[].locale` | — | String | `locale` | `"zh-CN"` | 用户语言 |
| `usersInfo[].timezone` | — | String | `timezone` | `"Asia/Shanghai"` | 用户时区 |
| `usersInfo[].updateDepartment` | — | Boolean | 固定值 | **`false`** | 本次不同步部门 |
| `usersInfo[].updateSuperiorEmail` | — | Boolean | 固定值 | **`false`** | 本次不同步上级汇报关系 |
| `usersInfo[].autoActivated` | — | Integer | 固定值 | **`0`** | 不自动激活 |
| `usersInfo[].thirdPartyId` | — | String | 固定值 | **`""`** | SSO 未启用，传空串 |

### 6.3 role_id 取值策略（分两阶段）

| 阶段 | role_id 取值 | 说明 |
|------|-------------|------|
| **阶段一（当前）** | 写**固定值 `223379`**（Moka 默认角色 ID，已通过功能点 0 拉取确认） | 先确保人员能推到 Moka 跑通链路 |
| **阶段二（角色拉取上线后）** | 按 `jobTitle` 匹配 `t_integration_moka_role` 表得到对应 role_id | persondetail 当前无 jobTitle 字段，需确认是否有对应字段后实施 |

### 6.4 iHR persondetail → Moka 中间表 → Moka API 全链路映射总表

下表汇总 persondetail 原始字段如何经过中间表最终到达 Moka API：

| persondetail 字段 | 中间表字段 | Moka API 字段 | 类型 | 转换规则 |
|-------------------|-----------|--------------|------|----------|
| `id` | `user_id` | — | VARCHAR(64) | 业务唯一键，内部用 |
| `staffNo` | `employee_no` | `usersInfo[].number` | String | 直接映射 |
| `staffName` | `user_name` | `usersInfo[].name` | String | 直接映射 |
| `nickName` | `nickname` | `usersInfo[].nickname` | String | 直接映射 |
| `workEmail` | `company_email` | `usersInfo[].email` | String | 允许为空 |
| `mobileNo` | `contact_phone` | `usersInfo[].phone` | String | **唯一标识**（uniqueType=phone） |
| `departmentId` | — | — | — | **关联查询** `organizationsdepartment` 获取 departmentcode |
| — | `department_code` | `usersInfo[].departmentCode` | String[] | **数组包装** |
| `staffStatus` | `employee_status` | — | VARCHAR(32) | IN_SERVICE / QUIT |
| （staffStatus 转换） | `deactivated` | `usersInfo[].deactivated` | Integer | QUIT → 1，其余 → 0 |
| — | `role_id` | `usersInfo[].roleId` | Integer | **阶段一固定 223379；阶段二按 jobTitle 匹配** |
| — | `locale` | `usersInfo[].locale` | String | 固定 "zh-CN" |
| — | `timezone` | `usersInfo[].timezone` | String | 固定 "Asia/Shanghai" |
| — | — | `usersInfo[].uniqueType` | String | 硬编码 "phone" |
| — | — | `usersInfo[].updateDepartment` | Boolean | 硬编码 false |
| — | — | `usersInfo[].updateSuperiorEmail` | Boolean | 硬编码 false |
| — | — | `usersInfo[].autoActivated` | Integer | 硬编码 0 |
| — | — | `usersInfo[].thirdPartyId` | String | 硬编码 "" |
| — | `moka_sync_status` | — | SMALLINT | 中间表同步状态 |
| — | `last_sync_time` | — | TIMESTAMP | 推送 Moka 时更新 |
| — | `last_sync_result` | — | VARCHAR | 推送 Moka 时记录 |

---

## 7. 技术方案

### 7.1 架构影响

新增业务域 `moka` 下的 **角色 + 人员** 子模块：

```
dao/
  entity/ihr/
    PersondetailDO.java                          ← 新增（外部表，不继承 BaseMapper）
    OrganizationsdepartmentDO.java              ← 新增（外部表，不继承 BaseMapper）
  entity/moka/
    MokaRoleDO.java                              ← 新增（功能点 0）
    MokaPersonDO.java                            ← 新增
  mapper/ihr/
    PersondetailMapper.java                      ← 新增（@Select 手写 SQL，不继承 BaseMapper）
    OrganizationsdepartmentMapper.java            ← 新增（@Select 手写 SQL，不继承 BaseMapper）
  mapper/moka/
    MokaRoleMapper.java                          ← 新增（功能点 0）
    MokaPersonMapper.java                        ← 新增

service/
  moka/api/IMokaRoleService.java                 ← 新增（功能点 0）
  moka/api/impl/MokaRoleServiceImpl.java
  moka/api/IMokaPersonService.java
  moka/api/impl/MokaPersonServiceImpl.java

manager/
  moka/api/IMokaRoleSyncManager.java              ← 新增（功能点 0）
  moka/api/impl/MokaRoleSyncManagerImpl.java
  moka/api/IMokaPersonSyncManager.java           ← 接口 1
  moka/api/impl/MokaPersonSyncManagerImpl.java
  moka/api/IMokaPersonPushManager.java           ← 接口 2
  moka/api/impl/MokaPersonPushManagerImpl.java

integration/
  moka/client/MokaUserClient.java               ← 新增（syncInfo）
  moka/client/MokaRoleClient.java               ← 新增（功能点 0）
  moka/client/dto/
    MokaRoleListResponse.java
    MokaRoleItem.java
    MokaUserSyncRequest.java
    MokaUserSyncResponse.java

web/
  controller/moka/MokaRoleController.java       ← 新增（功能点 0）
  controller/moka/MokaPersonController.java     ← 新增
  dto/moka/vo/
    MokaRoleSyncResultVO.java                   ← 新增
    MokaPersonSyncResultVO.java
    MokaPersonPushResultVO.java
  resources/db/migration/V23__moka_role.sql     ← 新增（角色表）
  resources/db/migration/V24__moka_person.sql   ← 新增（人员表）
```

**事务边界**（严格遵守项目规范第十二节）：

| 场景 | 处理方式 |
|------|----------|
| Manager 层批量 upsert 中间表 | **外层不包事务**（外部表只读）；`batchUpsert()` 内部声明 `@Transactional(rollbackFor=Exception.class, propagation=REQUIRED, timeout=120)` |
| Manager 层批量更新 sync_status | 独立事务 `@Transactional(timeout=30)` |
| persondetail 查询 + organizationsdepartment 查询 | 纯 DB 只读，无事务 |
| Moka HTTP 调用（角色查询 + 人员推送） | **禁止** `@Transactional` 包裹，放在事务外独立执行 |
| 分批推送中批次间 | 每批独立 try-catch，单条失败不阻断同批次其他条目 |

**MokaConstants 常量**（已确认）：

```java
// 角色查询（拉取 Moka 全部角色，固定 type=all）
public static final String ROLE_LIST_PATH = "/api-platform/v1/users/roles";
// Moka 用户同步
public static final String USER_SYNC_INFO_PATH = "/api-platform/v1/users/syncInfo";
// 默认角色 ID（已通过功能点 0 拉取确认）
public static final int DEFAULT_ROLE_ID = 223379;
// Moka API 固定参数
public static final String USER_UNIQUE_TYPE = "phone";
public static final int USER_AUTO_ACTIVATED = 0;
public static final boolean USER_UPDATE_DEPARTMENT = false;
public static final boolean USER_UPDATE_SUPERIOR_EMAIL = false;
public static final String USER_THIRD_PARTY_ID = "";
public static final String USER_LOCALE = "zh-CN";
public static final String USER_TIMEZONE = "Asia/Shanghai";
```

### 7.2 数据库变更

#### V23 — Moka 角色中间表（功能点 0）

**Flyway 脚本**：`V23__moka_role.sql`（已创建）

```sql
CREATE TABLE IF NOT EXISTS t_integration_moka_role (
    id                    BIGINT       NOT NULL,   -- 内部主键（雪花生成）
    role_id               INTEGER      NOT NULL,   -- Moka 角色 ID（业务唯一键，UNIQUE；对应 API id）
    role_name             VARCHAR(128),             -- Moka 角色名称（对应 API name）
    role                  INTEGER,                  -- Moka 角色值（对应 API role）
    description           VARCHAR(500),             -- Moka 角色描述（对应 API description）
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted            SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_role PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_role_role_id UNIQUE (role_id)
);
```

#### V24 — Moka 人员中间表（功能点 1）

**Flyway 脚本**：`V24__moka_person.sql`（已创建，role_id DEFAULT 223379）

```sql
CREATE TABLE IF NOT EXISTS t_integration_moka_person (
    id                    BIGINT       NOT NULL,   -- 内部主键（雪花生成）
    user_id               VARCHAR(64)  NOT NULL,   -- persondetail.id（业务唯一键，UNIQUE）
    employee_no           VARCHAR(64),              -- 工号（persondetail.staffNo）
    user_name             VARCHAR(128),             -- 姓名（persondetail.staffName）
    nickname              VARCHAR(128),             -- 昵称（persondetail.nickName）
    company_email         VARCHAR(200),             -- 邮箱（persondetail.workEmail）
    contact_phone         VARCHAR(32),              -- 手机号（persondetail.mobileNo）
    role_id               INTEGER      NOT NULL DEFAULT 223379, -- Moka 默认角色
    department_code       VARCHAR(64),              -- 部门编号（关联 organizationsdepartment.departmentcode）
    superior_email        VARCHAR(200),             -- 预留列，当前版本不写入（updateSuperiorEmail=false）
    employee_status       VARCHAR(32),              -- persondetail.staffStatus（IN_SERVICE/QUIT）
    deactivated           SMALLINT NOT NULL DEFAULT 0,
    locale                VARCHAR(16) NOT NULL DEFAULT 'zh-CN',
    timezone              VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    moka_sync_status      SMALLINT NOT NULL DEFAULT 0,
    last_sync_time        TIMESTAMP,
    last_sync_result      VARCHAR(500),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted            SMALLINT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_t_integration_moka_person PRIMARY KEY (id),
    CONSTRAINT uk_t_integration_moka_person_user_id UNIQUE (user_id)
);
```

> **persondetail / organizationsdepartment** 均为外部表，Mapper 层用 `@Select` 手写 SQL，不继承 BaseMapper，避免 MyBatis-Plus 自动注入逻辑删除等条件。
> **department_code 关联**：Manager 层先收集所有 `departmentId` → 一次性 `WHERE departmentId IN (...)` 查 `departmentcode` → 内存 Map 匹配，避免 N+1。
> **superior_email**：表中保留列定义便于未来扩展，但当前版本 Moka API `updateSuperiorEmail=false`，接口 1 不写入此列。

### 7.3 配置变更

无需新增配置项。Moka Base URL 和 API Key 复用现有 `MokaProperties`（`moka.base-url` + `moka.api-key`）。

### 7.4 涉及代码

| 模块 | 文件 | 说明 |
|------|------|------|
| `common` | `constant/moka/MokaConstants.java` | 新增 `ROLE_LIST_PATH`（已确认为 `/api-platform/v1/users/roles`）、`USER_SYNC_INFO_PATH`、7 个 Moka API 固定参数、`DEFAULT_ROLE_ID=223379` |
| `dao` | `entity/ihr/PersondetailDO.java` | persondetail 外部表轻量 DO（不继承 BaseMapper） |
| `dao` | `entity/ihr/OrganizationsdepartmentDO.java` | organizationsdepartment 外部表轻量 DO |
| `dao` | `entity/moka/MokaRoleDO.java` | Moka 角色中间表 DO |
| `dao` | `entity/moka/MokaPersonDO.java` | Moka 人员中间表 DO（superiorEmail 标记 `@TableField(exist=false)`） |
| `dao` | `mapper/ihr/PersondetailMapper.java` | persondetail 查询（@Select 手写 SQL） |
| `dao` | `mapper/ihr/OrganizationsdepartmentMapper.java` | organizationsdepartment 查询（@Select 手写 SQL + IN 批量） |
| `dao` | `mapper/moka/MokaRoleMapper.java` | 角色 CRUD + upsert（含预生成雪花 id） |
| `dao` | `mapper/moka/MokaPersonMapper.java` | 人员 CRUD + upsert（不含 superior_email） |
| `service` | `moka/api/IMokaRoleService.java` + impl | 角色中间表读写 |
| `service` | `moka/api/IMokaPersonService.java` + impl | 人员中间表读写（引用 `MokaConstants.DEFAULT_ROLE_ID`） |
| `manager` | `moka/api/IMokaRoleSyncManager.java` + impl | 功能点 0 编排 |
| `manager` | `moka/api/IMokaPersonSyncManager.java` + impl | 接口 1 编排（persondetail + organizationsdepartment → 中间表） |
| `manager` | `moka/api/IMokaPersonPushManager.java` + impl | 接口 2 编排（中间表 → Moka API） |
| `integration` | `moka/client/MokaRoleClient.java` | 角色查询（GET + `?type=all`，HTTP Basic Auth） |
| `integration` | `moka/client/MokaUserClient.java` | syncInfo 推送 |
| `integration` | `moka/client/dto/*` | 角色 + 人员 请求/响应 DTO |
| `web` | `controller/moka/MokaRoleController.java` | 角色同步 REST |
| `web` | `controller/moka/MokaPersonController.java` | 人员同步 REST |
| `web` | `dto/moka/vo/*` | 接口返回 VO |

---

## 8. 验收标准

| 序号 | 验收点 | 验收方式 | 通过标准 |
|------|--------|----------|----------|
| 1 | 角色中间表 DDL 可执行 | 手动 | Flyway V23 启动无异常，表结构含 role INTEGER 列 |
| 2 | 人员中间表 DDL 可执行 | 手动 | Flyway V24 启动无异常，role_id DEFAULT 223379 |
| 3 | 功能点 0 拉取 Moka 角色 | 手动调接口 | 返回 totalFetched > 0，role_id=223379 的记录存在 |
| 4 | 接口 1 同步 persondetail → 中间表 | 手动调接口 | totalFetched 与 persondetail 记录数一致 |
| 5 | 接口 1 部门关联查询正确 | 数据校验 | department_code 等于 organizationsdepartment.departmentcode |
| 6 | 接口 1 deactivated 转换正确 | 数据校验 | QUIT → 1，IN_SERVICE → 0 |
| 7 | 接口 1 role_id 默认值正确 | 数据校验 | 所有新同步记录 role_id=223379 |
| 8 | 接口 2 推送 Moka 成功 | 手动调接口 + 查 Moka 后台 | Moka 后台人员数量与中间表一致 |
| 9 | 接口 2 同步状态更新 | 数据校验 | 成功的 moka_sync_status=1，失败=2 |
| 10 | Moka API 分批推送 | 日志校验 | 每批 ≤ 100 条 |
| 11 | 接口幂等性 | 重复调用 | 连续调 3 次，DB 结果一致 |
| 12 | 敏感信息保护 | 日志审查 | 不出现完整邮箱、手机号 |
| 13 | 异常场景处理 | 手动注入异常 | persondetail 查询异常 / Moka API 超时 / 批量 upsert 部分失败均有 INFO 日志说明 |
| 14 | 代码分层 | 静态审查 | Controller 不写业务逻辑、Manager 不直接操作 Mapper |

---

## 9. 风险与依赖

| 风险/依赖 | 影响 | 应对措施 |
|-----------|------|----------|
| **persondetail 表结构变更**（iHR 字段变化） | 中间表映射需要调整 | PersondetailMapper 用 `@Select` 手写 SQL，列别名集中维护；DO 字段与 SQL 列别名一一对应 |
| **organizationsdepartment 列名不确定**（假设为 departmentId / departmentcode 全小写） | 部门关联失败 | 联调前先执行 `SELECT * FROM organizationsdepartment LIMIT 1` 确认列名，若不同改 SQL 列别名即可 |
| **部门关联查不到**（persondetail.departmentId 在 organizationsdepartment 不存在） | 员工 department_code 为空 | 置空并打 INFO 日志，不阻断 |
| **Moka roleId 两阶段切换** | 阶段一 223379 → 阶段二按 jobTitle | roleId 兜底常量统一在 `MokaConstants.DEFAULT_ROLE_ID`；后续改一处即可 |
| **Moka API `deactivated=1` 首次创建会失败** | 离职员工若为 Moka 新用户，推送时无法创建 | 推送时先区分"中间表此前是否推过 Moka"，对新用户离职场景特殊处理 |
| **Moka API 请求体大小** | 大批量推送失败 | Manager 层分批 ≤ 100 条 |
| **外部表直接依赖**（persondetail + organizationsdepartment） | MyBatis-Plus 自动注入可能产生问题 | 两个 Mapper 均用 `@Select` 手写 SQL，不继承 BaseMapper |
| **敏感字段暴露**（persondetail 含邮箱、手机号等） | 中间表超集存储增加风险 | 中间表只存 Moka 需要的字段；日志脱敏 |
| **ESB 调度编排顺序** | 需先拉角色 → 再同步人员 → 再推 Moka | ESB 建议编排顺序；接口 0 低频（每天 1 次），接口 1/2 高频 |

---

## 10. 待确认事项

| 序号 | 问题 | 影响范围 | 建议 |
|------|------|----------|------|
| 1 | **organizationsdepartment 实际列名**：是否为 `departmentId` + `departmentcode`（全小写）？ | OrganizationsdepartmentMapper SQL | 联调前执行 `SELECT * FROM organizationsdepartment LIMIT 1` 确认；若不同改列别名即可 |
| 2 | **persondetail 是否有 jobTitle 字段**？（阶段二 roleId 匹配依赖此字段） | Manager 层 roleId 匹配逻辑 | 联调时顺便确认；若无则需业务提供 persondetail 到 Moka 角色的映射关系 |
| 3 | **Moka API `deactivated=1` 首次创建失败**：离职员工如果是 Moka 新用户如何处理？ | 接口 2 推送策略 | 需业务确认离职员工是否需要在 Moka 创建账号 |
| 4 | **ESB 调度频率**：角色 1 次/天 + 人员同步 30 分钟 + 推送 1 小时 是否合适？ | ESB 配置 | 根据数据变更量调整 |

---

## 11. 变更记录

| 日期 | 修改人 | 修改内容 |
|------|--------|----------|
| 2026-09-20 | hongfu_zhou@cacch.com | 创建文档（draft） |
| 2026-09-20 | hongfu_zhou@cacch.com | 第二轮：重写字段映射表，Moka API 按真实文档对齐 |
| 2026-09-20 | hongfu_zhou@cacch.com | 第三轮：新增功能点 0（角色查询），Flyway V23+V24 |
| 2026-09-21 | hongfu_zhou@cacch.com | 第四轮：Moka 角色查询接口联调确认（路径 + 响应字段 id/name/role/description），role_id 固定值 223379 |
| 2026-09-21 | hongfu_zhou@cacch.com | 第五轮（全链路 Breaking Change 对齐）：① persondetail 字段全对齐真实表（id→userId, staffNo→employeeNo, staffName→userName, nickName→nickname, workEmail→companyEmail, mobileNo→contactPhone, staffStatus→employeeStatus IN_SERVICE/QUIT）② 部门关联表从 ihr_department 改为外部表 organizationsdepartment（取 departmentcode）③ Moka API 固定参数全更新：uniqueType=phone, autoActivated=0, updateDepartment=false, updateSuperiorEmail=false, thirdPartyId="" ④ superior_email 当前版本不写入（updateSuperiorEmail=false）⑤ Flyway V24 role_id DEFAULT 223379 |
