# REQ-CRM-001 CRM订单同步OA表单

---

## 1. 基本信息

| 项目 | 内容 |
|------|------|
| 需求编号 | REQ-CRM-001 |
| 需求标题 | CRM(勤策)订单数据同步至OA(致远A8)发起表单流程 |
| 需求类型 | integration |
| 所属系统 | crm / oa |
| 优先级 | P1(高) |
| 状态 | reviewing |
| 提出人 | — |
| 提出日期 | 2026-07-11 |
| 负责人 | — |
| 预计上线 | 待定 |

---

## 2. 需求背景

目前销售人员在勤策CRM系统中录入客户订单后，需要在致远OA系统中手工重新填写一遍表单并提交审批流程。存在以下问题：

1. **重复劳动**：同一笔订单数据需要在两个系统中各录入一次，耗时且易出错
2. **数据不一致**：人工转录容易出现金额、客户名称等关键字段录入错误
3. **时效滞后**：CRM中订单创建后，OA审批流程的发起依赖人工操作，存在数小时甚至更长的延迟
4. **难以追踪**：无法自动关联CRM订单与OA审批流程的状态，对账困难

**目标**：定时从勤策CRM按 `modify_time` 拉取已审批通过的订单及明细，以「结构化关键列 + JSONB 原始报文」完整落库（仅新增不更新）；OA同步时经独立的 CRM↔OA 人员映射解析业务员/发起人，再以**明细**为粒度映射并发起致远OA表单（一条明细一个表单）。字段取值路径与实施人员确认后固化；采集与OA同步通过PG库解耦。

---

## 3. 需求描述

### 3.1 功能点

| 序号 | 功能点 | 描述 | 必选/可选 |
|------|--------|------|-----------|
| F1 | 定时拉取CRM订单并入库 | 按**每5分钟**周期从勤策CRM查询订单；按 **`modify_time`（修改时间）** 过滤；未指定时间范围时默认查询**当天**（时区 `Asia/Shanghai`）数据；**单轮最多处理100笔订单**；接口返回数据默认已是审批通过数据，**不做审批状态判断** | 必选 |
| F2 | 拉取订单明细（子订单） | 查询到订单后，根据订单ID调用明细查询接口，获取该订单下的全部子订单（明细）数据并存入PG库。明细拉取失败时**主表不回滚**，下轮继续补拉；明细未拉取成功前**不发起OA同步** | 必选 |
| F3 | 订单/人员等接口全量原始存储 | CRM（及人员映射相关 OA）接口返回数据**完整保留**：表结构除必要业务/同步字段外，须预留 **JSONB 原始报文**（如 `raw_payload`）保存接口全部返回内容；文档未列出的字段一并落入原始报文。业务取值与 OA 映射字段在实施阶段与实施人员确认后再解析 | 必选 |
| F4 | 订单/明细仅新增不更新 | CRM侧订单数据入库后**不会变更**；PG库按订单编号/明细ID判重，**已存在则跳过，不做UPDATE**；CRM明细行不删除，未入库的子订单直接新增 | 必选 |
| F5 | 从PG库读取并映射OA字段 | 从PG库读取订单主表+明细数据，按映射规则转换为OA表单字段；**一条明细对应一个OA表单** | 必选 |
| F6 | 发起OA表单流程 | 在致远OA中自动发起对应的表单流程，**无需配置审批人**；**业务员(field0006)与流程发起人均取 CRM→OA 人员映射结果**；**销售公司(field0003)本期传空**；`formson_2818`（费用利润子表）**本期不传** | 必选 |
| F7 | 同步状态记录 | 以**明细（子订单）**为粒度记录OA同步状态（待同步/成功/重试中/失败），支持重试 | 必选 |
| F8 | 失败重试机制 | OA同步失败的明细在下一周期自动重试，达到最大重试次数后告警 | 必选 |
| F9 | 同步去重 | 同一条CRM明细（明细ID）不会重复发起OA流程 | 必选 |
| F10 | 同步结果告警 | 同步失败或达到最大重试次数时，通过企微Webhook发送告警通知 | 必选 |
| F11 | 手动触发同步 | 提供API接口，支持手动触发指定订单/明细或全量OA同步 | 可选 |
| F12 | 同步记录查询 | 提供API接口查询同步记录列表及详情 | 可选 |
| F13 | CRM↔OA人员映射 | **独立能力**：订单OA同步时，按订单负责人员工ID解析并落库 CRM员工↔OA人员映射；业务员与发起人共用该映射结果 | 必选 |

### 3.2 交互流程

**阶段一：CRM订单采集入库**

```
定时任务触发（采集，每5分钟）
    ↓
确定查询时间范围（时区 Asia/Shanghai，过滤字段 modify_time）：
    ├─ 已指定时间范围 → 按指定时间查询
    └─ 未指定时间范围 → 默认查询当天 00:00:00 ~ 23:59:59
    ↓
调用勤策CRM订单查询接口：按 modify_time 时间范围查询订单列表
（接口返回数据默认已审批通过，不做审批状态过滤）
    ↓
逐笔处理订单主表（本轮最多100笔）：
    ├─ 按订单编号(name)查询PG库 → 已存在？
    │   ├─ 是 → 跳过主表写入（数据不变，不做UPDATE）
    │   └─ 否 → 新增订单记录（全量存储）
    └─ 根据订单ID调用CRM订单明细查询接口
        ├─ 成功 → 逐笔写入明细子表：
        │         按明细ID判重，已存在则跳过，不存在则新增；
        │         新明细 oa_sync_status = PENDING
        └─ 失败 → 主表不回滚，记录明细拉取失败，下轮继续补拉；
                  该订单明细未就绪，不进入OA同步
    ↓
分页处理：如有下一页且未达本轮上限继续拉取
    ↓
补拉：扫描 PG 中明细未拉取成功的订单，重试明细接口
    ↓
本轮采集完成
```

**阶段二：OA表单同步**

```
定时任务触发（同步，每5分钟）
    ↓
查询PG库明细：oa_sync_status = 'PENDING' 或 'RETRY'
（仅明细已成功入库的记录；明细未拉取成功的订单不同步）
本轮最多处理100条明细（即最多发起100个OA表单）
    ↓
逐条明细处理：
    ├─ 从PG库读取主表 + 当前明细
    ├─ 解析订单负责人员工ID（owner.id）
    ├─ 调用【独立】CRM↔OA人员映射能力 → 得到 OA人员ID
    │   （优先读映射表；未命中则查CRM员工账号取 emp_code，再查OA按编码取人员，并落库）
    │   └─ 映射失败 → 本条记 RETRY，不发起OA
    ├─ 字段映射：PG库字段 → OA表单字段（一条明细 → 一个表单）
    │   ├─ field0006(业务员) = 映射得到的OA人员ID
    │   ├─ 流程发起人 = 同一OA人员ID
    │   └─ field0003(销售公司) = 空
    ├─ 调用致远OA接口 → 发起表单流程（无需审批人）
    ├─ 更新该明细同步状态（成功/失败）+ 记录OA流程实例ID
    └─ 失败时 → 重试次数 + 1，下一周期自动重试
    ↓
本轮同步完成
    ↓
检查是否有失败达到最大重试次数的明细
    ├─ 是 → 发送企微Webhook告警
    └─ 否 → 结束
```

---

## 4. 业务流程

### 4.1 整体架构

```
勤策CRM                集成平台(PG库)              致远OA
  │                      │                          │
  │  ① 定时拉取订单       │                          │
  │    (按modify_time,   │                          │
  │     默认当天/上海时区, │                          │
  │     单轮≤100笔)       │                          │
  │ ────────────────────→│                          │
  │  返回订单列表         │                          │
  │ ←────────────────────│                          │
  │                      │                          │
  │  ② 按订单ID查询明细   │                          │
  │ ────────────────────→│                          │
  │  返回订单明细列表     │                          │
  │ ←────────────────────│                          │
  │                      │  ③ 仅新增入库             │
  │                      │     (主表+明细，不更新)   │
  │                      │                          │
  │                      │  ④ 查询待同步明细         │
  │                      │     (PENDING/RETRY,≤100) │
  │  ⑤a 查员工账号        │  ⑤ 人员映射(独立能力)    │
  │ ←────────────────────│     owner.id→emp_code    │
  │  返回 emp_code        │     →OA人员ID并落库      │
  │                      │  ⑤b 按code查人员 ────────→│
  │                      │     返回OA人员ID         │
  │                      │  ⑥ 字段映射 PG→OA         │
  │                      │     (一条明细一个表单)     │
  │                      │  ⑦ 发起表单流程(无审批人) │
  │                      │     业务员/发起人=映射ID  │
  │                      │ ─────────────────────────→│
  │                      │    返回流程实例ID         │
  │                      │ ←─────────────────────────│
  │                      │  ⑧ 更新明细同步状态       │
  │                      │                          │
```

> **关键设计**：
> - **同步频率**：采集与同步均为**每5分钟**执行一次
> - **查询条件**：按 CRM 字段 `modify_time` 过滤；默认当天；时区 `Asia/Shanghai`
> - **单轮上限**：采集最多处理 **100** 笔订单；OA同步最多处理 **100** 条明细（100个表单）
> - **数据不变**：CRM 返回数据默认已审批通过；入库后不做审批状态判断；主表/明细**仅新增不更新**
> - **一条明细一个OA表单**（与现有 ESB 规则 `ESB_T00001_DI95TDSF1B` 一致）
> - **人员映射独立**：CRM员工↔OA人员映射为独立能力，在订单OA同步时触发，结果落库复用
> - 明细拉取失败：主表不回滚，下轮补拉；明细未就绪不发起OA
> - 采集与同步两阶段解耦，各自独立定时执行

### 4.2 阶段一：CRM订单采集入库

```
定时任务触发（每5分钟）
    ↓
确定查询时间范围（Asia/Shanghai，字段 modify_time）：
    ├─ 配置了时间范围参数 → 按指定时间查询
    └─ 未配置 → 默认查询当天 00:00:00 ~ 23:59:59
    ↓
调用勤策CRM订单查询接口（orderQuery）
    ├─ 请求参数：page/rows 分页 + query_group 按 modify_time 过滤
    └─ 本轮累计处理订单数 ≤ 100
    ↓
返回订单列表（分页）
    ↓
逐笔处理订单主表：
    ├─ 按订单编号(name)查询PG库
    │   ├─ 不存在 → INSERT（全量存储接口返回的全部字段）
    │   └─ 已存在 → SKIP（不做UPDATE，数据不会变）
    ├─ 调用勤策CRM订单明细查询接口（orderDetailQuery）
    │   ├─ 请求参数：query_group 按 order_id 过滤
    │   ├─ 成功 → 返回该订单下全部明细记录（可能多条）
    │   │         按明细ID判重：不存在则 INSERT（oa_sync_status=PENDING），
    │   │         已存在则 SKIP；CRM明细行不删除，PG侧亦不删除
    │   └─ 失败 → 主表不回滚；标记该订单明细待补拉；本单不进入OA同步
    ↓
判断是否有下一页且未达本轮100笔上限 → 是则继续分页拉取
    ↓
补拉：查询PG库中 detail_fetch_status ≠ SUCCESS 的订单（不依赖 modify_time 再次出现），
      重新调用明细接口；成功则写入缺失明细并更新拉取状态
    ↓
更新采集时间游标（基于 modify_time）
    ↓
本轮采集完成
```

> **补拉说明**：明细拉取失败的订单，其 `modify_time` 可能不再变化，因此不能只靠 CRM 时间窗口重试；每轮采集需额外扫描 PG 中待补拉订单并重试明细接口。

### 4.3 阶段二：OA表单同步

```
定时任务触发（每5分钟）
    ↓
查询PG库明细表：oa_sync_status = 'PENDING' 或 'RETRY'
（关联主表已存在；明细已成功入库）
本轮最多取 100 条明细
    ↓
逐条明细处理：
    ├─ 从PG库读取主表 + 当前明细
    ├─ 取订单负责人 CRM 员工ID（owner.id）
    ├─ 调用独立人员映射能力（见 4.6）→ OA人员ID
    │   └─ 失败 → oa_sync_status=RETRY，本条不发起OA
    ├─ 按字段映射规则转换为OA表单数据（主表字段 + 单行 formson_2819）
    │   ├─ field0006 = OA人员ID；发起人 = 同一OA人员ID
    │   ├─ field0003 = 空
    │   └─ 不传 formson_2818
    ├─ 调用致远OA发起表单流程（无需审批人；Token/发起人使用映射到的OA人员）
    │   ├─ 成功 → 更新该明细 oa_sync_status = 'SUCCESS'，记录流程实例ID
    │   └─ 失败 → 更新 oa_sync_status = 'RETRY'，retry_count + 1
    └─ retry_count >= 3 → 更新 oa_sync_status = 'FAILED'，触发告警
    ↓
本轮同步完成
```

### 4.4 异常处理流程

```
【明细拉取失败】
    ↓
主表保留（不回滚）→ 记录失败原因 → 下轮采集对该订单重新查明细
    ↓
明细未入库成功前，该订单不进入OA同步

【人员映射失败】
    ↓
明细记 RETRY + 错误信息（如员工ID为空 / emp_code 查无 / OA人员不存在）
    ↓
下轮同步再次触发人员映射（可复用已落库的部分结果）

【OA同步失败】
    ↓
明细记录：失败原因 + 重试次数 + 1 + 状态标记为 RETRY
    ↓
下一轮OA同步任务
    ↓
查询状态为 RETRY 的明细，重试次数 < 3？
    ├─ 是 → 从PG库重新读取主表+明细，重新解析人员映射并同步OA
    └─ 否 → 状态改为 FAILED，发送企微Webhook告警，标记"需人工介入"
```

> **注意**：
> - 订单字段映射失败重试时，优先使用已落库的 CRM↔OA 人员映射，避免重复打接口。
> - 人员映射本身失败时，仍需按链路重试查询 CRM/OA。

### 4.5 入库与同步策略

| 场景 | 处理策略 |
|------|----------|
| CRM订单主表首次出现 | INSERT 全量字段 |
| CRM订单主表已存在于PG | **跳过**（不做UPDATE） |
| 明细拉取成功且明细ID不存在 | INSERT，`oa_sync_status=PENDING` |
| 明细拉取成功且明细ID已存在 | **跳过**（不做UPDATE、不删除） |
| 明细拉取失败 | 主表不回滚，下轮补拉；**不发起OA** |
| 明细 PENDING/RETRY | 进入OA同步队列（一条明细一个表单） |
| 明细 SUCCESS | 不再发起OA（去重） |
| 审批状态 | **不判断**（CRM接口返回数据默认已审批通过） |
| 人员映射已存在 | 直接复用映射表中的 OA人员ID |
| 人员映射不存在 | 现场解析并落库后再发起OA |
| 人员映射失败 | 本条明细不发起OA，记 RETRY |

> **关键规则**：
> - 同步粒度 = **CRM明细ID**（非订单编号）
> - 一笔订单有 N 条明细 → 发起 N 个 OA 表单流程
> - `formson_2818` 本期不传
> - **业务员与发起人**均使用 CRM↔OA 人员映射结果；**field0003 本期传空**

### 4.6 CRM↔OA 人员映射（独立能力）

> 人员映射与订单字段映射解耦，作为独立能力在**订单 OA 同步时触发**，并将映射关系持久化到 PG，供后续同步复用。

**触发时机**：阶段二处理每条待同步明细时（发起 OA 表单前）。

**输入**：CRM 订单主表中的负责人员工ID（`owner.id`）。

**处理流程**：

```
取 CRM 员工ID（owner.id）
    ↓
查询 PG 人员映射表（按 crm_employee_id）
    ├─ 已存在且 oa_user_id 有效 → 直接返回 OA人员ID
    └─ 未命中 / 无效 → 现场解析：
          ├─ ① 调用 CRM 查询员工帐号
          │     POST .../api/employee/v3/queryEmployee/{openid}/{timestamp}/{digest}/{msg_id}
          │     Body：`{ "id": "<owner.id>" }`（勤策员工唯一标识）
          │     └─ 取返回 **`emp_code`**（员工登录帐号）
          ├─ ② 调用 OA 按编码取人员信息
          │     GET .../seeyon/rest/orgMembers/code/{code}?pageNo=&pageSize=
          │     └─ 路径参数 **`code` = emp_code**
          │     └─ 取返回人员 **`id`**（及可选 `loginName`）
          ├─ ③ 将映射关系写入 PG（crm_employee_id / emp_code / oa_user_id / oa_login_name 等）
          └─ ④ 返回 OA人员ID
    ↓
业务员(field0006) = OA人员ID
流程发起人 = 同一 OA人员ID
```

**接口文档**：
- CRM：[查询员工帐号](https://api.waiqin365.com/SERVER/sysapp/employee_v3.html#%E6%9F%A5%E8%AF%A2%E5%91%98%E5%B7%A5%E5%B8%90%E5%8F%B7)
- OA：[按编码取人员信息](https://open.seeyoncloud.com/seeyonapi/728/732.html#%E6%8C%89%E7%BC%96%E7%A0%81%E5%8F%96%E4%BA%BA%E5%91%98%E4%BF%A1%E6%81%AF)
**落库原则**：
- 以 `crm_employee_id` 为业务唯一键，已存在则更新 `emp_code`、`oa_user_id` 及同步时间（人员映射允许更新，与订单「仅新增不更新」策略分离）
- 映射能力独立封装，订单同步只调用映射服务，不内联拼装 CRM/OA 人员接口细节

**失败策略**：任一环节失败（员工ID为空、CRM无 emp_code、OA按 code 查无等）→ 当前明细不同步 OA，记 RETRY，并写 `last_error_msg`。

---
## 5. 涉及系统与模块

### 5.1 勤策CRM

| 项目 | 内容 |
|------|------|
| 接口Base URL | `https://api.waiqin365.com` |
| **openId** | `8858965636174056137` |
| **appKey** | `0H4aAHGY0htrsglQKm` |
| 认证方式 | MD5 签名：`digest=MD5(jsonBody\|appkey\|timestamp)`，URL 携带 openid/timestamp/digest/msg_id |
| 订单查询接口 | `POST /api/ig/v1/orderQuery/{openid}/{timestamp}/{digest}/{msg_id}` |
| 订单明细查询接口 | `POST /api/ig/v1/orderDetailQuery/{openid}/{timestamp}/{digest}/{msg_id}` |
| 员工账号查询接口 | `POST /api/employee/v3/queryEmployee/{openid}/{timestamp}/{digest}/{msg_id}`（文档 Base：`https://openapi.qince.com`） |
| 员工账号查询文档 | [查询员工帐号](https://api.waiqin365.com/SERVER/sysapp/employee_v3.html#%E6%9F%A5%E8%AF%A2%E5%91%98%E5%B7%A5%E5%B8%90%E5%8F%B7) |
| 同步频率 | **每5分钟**拉取一次 |

**员工账号查询（人员映射用）要点**：

| 项 | 说明 |
|----|------|
| 方法 | POST（HTTPS） |
| 请求体关键入参 | 使用 **`id`**（勤策员工唯一标识）精确查询；对应订单 `owner.id`。`id` 与 `emp_id` 同时存在时优先 `id` |
| 响应关键出参 | **`emp_code`**（员工登录帐号，必填字段）；另可取 `emp_name`、`id` 等落库 |
| 认证 | 与订单接口相同：openid + timestamp + digest + msg_id |

### 5.2 致远OA

| 项目 | 内容 |
|------|------|
| 表单模板编号 | `CRM_ZYXS_001` |
| 主表 | `formmain_2817`（销售订单主表，约100个字段） |
| 子表1 | `formson_2818`（销售费用与利润明细表）——**本期不传** |
| 子表2 | `formson_2819`（物料清单明细表）——每表单仅传当前明细一行 |
| 发起表单接口 | `POST /seeyon/rest/bpm/process/start` |
| Token认证接口 | `GET /seeyon/rest/token/{userName}/{password}?loginName={loginName}` |
| 按编码取人员信息 | `GET /seeyon/rest/orgMembers/code/{code}?pageNo={pageNo}&pageSize={pageSize}` |
| 按编码取人员文档 | [按编码取人员信息](https://open.seeyoncloud.com/seeyonapi/728/732.html#%E6%8C%89%E7%BC%96%E7%A0%81%E5%8F%96%E4%BA%BA%E5%91%98%E4%BF%A1%E6%81%AF) |
| 流程状态查询 | `GET /seeyon/rest/flow/state/{flowId}` |
| 审批人 | **无需配置审批人** |
| 业务员 / 发起人 | 均使用人员映射得到的 OA 人员ID |

**按编码取人员信息（人员映射用）要点**：

| 项 | 说明 |
|----|------|
| 方法 | GET |
| 路径参数 **`code`** | 必填，人员编号；取值 = CRM 返回的 **`emp_code`** |
| 查询参数 | `pageNo`（必填，页号）、`pageSize`（必填，每页条数）；建议 `pageNo=0`（或文档约定首页）、`pageSize=20` |
| 响应 | JSON 人员信息；取人员 **`id`** 作为 OA人员ID；若有 **`loginName`** 一并落库（供 Token `loginName` / 发起人使用） |
| 前置 | 需先获取 Rest Token |

### 5.3 其他系统

| 系统 | 模块/接口 | 说明 |
|------|-----------|------|
| PG数据库 | 订单主表 | 结构化关键字段 + **`raw_payload`(JSONB) 完整原始报文**；仅新增不更新 |
| PG数据库 | 订单明细子表 | 同上；按 order_id 关联；OA同步状态挂在明细上 |
| PG数据库 | CRM↔OA人员映射表 | 关键映射字段 + **`crm_raw_payload` / `oa_raw_payload`** 预留完整响应 |
| 集成平台 | 采集定时任务 | 每5分钟按 modify_time 拉取；默认当天(Asia/Shanghai)；单轮≤100笔 |
| 集成平台 | 同步定时任务 | 每5分钟按明细 PENDING/RETRY 发起OA；同步前触发人员映射；单轮≤100条明细 |
| 集成平台 | 人员映射能力 | 独立服务：CRM员工ID → emp_code → OA人员ID，并落库 |
| 企业微信 | Webhook告警 | 同步异常时发送告警通知 |
---

## 6. 数据存储与字段映射

### 6.1 PG库订单表设计说明

> CRM接口返回的数据须**完整可追溯**。接口文档字段清单往往不完整，实际响应可能包含大量未文档化字段。
> 订单主表（`orderQuery`）存一行，订单明细（`orderDetailQuery`）存子表（一对多，通过 `order_id` 关联）。
> **OA同步状态管理字段挂在明细子表**（因一条明细对应一个OA表单）。
> 主表可额外记录明细拉取状态（便于补拉与门禁OA同步）。

**存储原则（结构化字段 + 原始报文）**：

| 层级 | 存什么 | 说明 |
|------|--------|------|
| 结构化列 | 业务主键、查询/同步必需字段、状态字段 | 如订单 `id`/`name`/`modify_time`、明细 `id`/`order_id`、`oa_sync_status` 等，便于索引与任务扫描 |
| **原始报文（必选预留）** | 接口返回的**完整 JSON** | 建议列名 `raw_payload`（JSONB）。文档未列出的字段、自定义字段、嵌套对象全部保留在此，**不做字段丢弃** |
| 可选展开列 | 已确认要高频使用的字段 | 可从 `raw_payload` 冗余展开为独立列；未确认前不必强行建全量表结构 |

**解析与映射原则**：
- 6.2 / 6.3 字段表仅为**接口文档参考清单**，**不代表实际返回的全部字段**
- OA 表单映射（6.4 / 6.5）及人员映射取值路径，在实施阶段与**实施人员确认**「取哪个字段、对应关系」后再固化到代码/配置
- 同步时优先从已确认的结构化列取值；未展开字段从 `raw_payload` 按确认路径解析
- 后续新增映射只需改解析配置并从原始报文取值，**无需因漏字段而重新拉数**（历史 `raw_payload` 已保留）

**其他规则**：
- **仅新增不更新**：主表按订单编号、明细按明细ID判重，已存在则跳过（含跳过覆盖 `raw_payload`）
- CRM明细行不删除，PG侧亦不做删除同步
- 额外增加同步状态管理字段（非CRM字段，见 6.7）

### 6.2 CRM订单主表字段（orderQuery 接口返回）

> 以下为CRM订单查询接口**文档中的参考字段**。实际入库以接口完整响应为准，全部写入 `raw_payload`；下表字段可按需展开为结构化列。
> **未在文档中出现但接口实际返回的字段，一律保留在原始报文中。**
| 序号 | CRM字段名 | 字段说明 | 数据类型 | 备注 |
|------|-----------|----------|----------|------|
| 1 | id | 订单ID | String | CRM内部唯一标识 |
| 2 | name | 订单编号 | String | 业务唯一标识，主表入库去重（已存在则跳过） |
| 3 | customer | 客户信息 | Object | 包含 id、name、status |
| 4 | order_date | 下单日期 | Object | 包含 format 和 value |
| 5 | order_total_amount | 订单总额 | String | |
| 6 | whole_order_discount | 整单折扣 | String | |
| 7 | receipt_info | 收货信息 | Object | |
| 8 | receipt_name | 收货人姓名 | String | |
| 9 | receipt_phone | 收货人手机 | String | |
| 10 | receipt_landline | 收货人固话 | String | |
| 11 | receipt_address | 收货地址 | String | |
| 12 | quotation_id | 报价单信息 | Object | |
| 13 | contract_id | 合同信息 | Object | |
| 14 | opportunities_info_id | 商机信息 | Object | |
| 15 | pd_price | 价目表信息 | Object | |
| 16 | currency_type | 币种代码 | String | 如 CNY |
| 17 | delivery_status | 订单发货状态 | Object | |
| 18 | delivered_amount_total | 已发货金额 | String | |
| 19 | pending_delivery_amount_total | 待发货金额 | String | |
| 20 | payment_received_amount_total | 已回款金额 | String | |
| 21 | confirm_receipt_amount_total | 确认收货金额 | String | |
| 22 | cm_useful_amount | 客户可用额度 | String | |
| 23 | status | 数据状态 | Object | |
| 24 | lock_status | 锁定状态 | Object | |
| 25 | approval_status | 审批状态 | Object | 接口返回数据默认已审批通过，**本期不做状态判断** |
| 26 | source_type | 来源类型 | Object | |
| 27 | business_type | 业务类型 | Object | |
| 28 | owner | 负责人 | Object | 包含 id、name；**id 为人员映射入口（CRM员工ID）** |
| 29 | department | 部门 | Object | 包含 id、name |
| 30 | creator_id | 创建人 | Object | |
| 31 | modifyier_id | 修改人 | Object | |
| 32 | create_time | 创建时间 | Object | |
| 33 | **modify_time** | **修改时间** | Object | **采集查询过滤字段**（默认当天，Asia/Shanghai） |
| 34+ | field_xxx | 自定义字段 | Any | 按实际返回存储 |

### 6.3 CRM订单明细子表字段（orderDetailQuery 接口返回）

> 以下为CRM订单明细查询接口**文档中的参考字段**。实际入库以接口完整响应为准，全部写入明细表 `raw_payload`；下表字段可按需展开为结构化列。
> **未在文档中出现但接口实际返回的字段，一律保留在原始报文中。**

| 序号 | CRM字段名 | 字段说明 | 数据类型 | 备注 |
|------|-----------|----------|----------|------|
| 1 | id | 明细ID | String | 明细唯一标识；**OA同步去重键** |
| 2 | name | 明细编号 | String | |
| 3 | **order_id** | **关联订单** | Object | **包含 id 和 name，关联主表** |
| 4 | pd_id | 商品信息 | Object | |
| 5 | **pd_code** | **商品编码** | String | **零售包装判断依据**（以"53"开头→是） |
| 6 | unit_id | 单位信息 | Object | |
| 7 | pd_count | 商品数量 | String | |
| 8 | actual_price | 实际售价 | String | |
| 9 | pd_origin_price | 原价 | String | |
| 10 | suggested_selling_price | 建议售价 | String | |
| 11 | pd_origin_amount | 原价金额 | String | |
| 12 | suggested_selling_amount | 建议售价金额 | String | |
| 13 | actual_selling_amount | 实际售价金额 | String | |
| 14 | sys_discount | 系统折扣 | String | |
| 15 | additional_discount | 额外折扣 | String | |
| 16 | amount_after_the_offer | 优惠后金额 | String | |
| 17 | is_gift | 是否赠品 | Boolean | |
| 18 | delivery_count | 已发货数量 | String | |
| 19 | back_count | 退货数量 | String | |
| 20 | slave_pd_price | 价目表信息 | Object | |
| 21 | price_detail | 价目表明细 | Object | |
| 22 | pd_portfolio | 商品组合 | Object | |
| 23 | pd_portfolio_detail | 商品组合明细 | Object | |
| 24 | node_id | BOM节点ID | String | |
| 25 | parent_node_id | BOM父节点ID | String | |
| 26 | root_node_id | BOM根节点ID | String | |
| 27 | tree_data | BOM树数据 | String | |
| 28 | portfolio_group | 组合分组 | String | |
| 29 | slave_quotation_id | 报价单信息 | Object | |
| 30 | quotation_detail_id | 报价单明细 | Object | |
| 31 | slave_contract_id | 合同信息 | Object | |
| 32 | contract_detail_id | 合同明细 | Object | |
| 33 | currency_type | 币种代码 | String | |
| 34 | exchange_rate | 汇率 | Number | |
| 35 | status | 数据状态 | Object | |
| 36 | lock_status | 锁定状态 | Object | |
| 37 | approval_status | 审批状态 | Object | 本期不做状态判断 |
| 38 | source_type | 来源类型 | Object | |
| 39 | business_type | 业务类型 | Object | |
| 40 | owner | 负责人 | Object | |
| 41 | department | 部门 | Object | |
| 42 | creator_id | 创建人 | Object | |
| 43 | modifier_id | 修改人 | Object | |
| 44 | create_time | 创建时间 | Object | |
| 45 | modify_time | 修改时间 | Object | |
| 46+ | field_xxx | 自定义字段 | Any | 按实际返回存储 |

### 6.4 订单主表 → OA表单主表字段映射（formmain_2817）

> 以下映射基于现有ESB规则代码 `ESB_T00001_DI95TDSF1B` 中的实际字段对应关系，作为**初版映射草案**。
> OA表单模板编号：`CRM_ZYXS_001`，主表表名：`formmain_2817`。
> **一条明细生成一个独立的OA表单流程**（主表字段取自订单主表，子表仅含当前这一条明细）。
> **实施阶段须与实施人员逐项确认**：CRM侧取数字段路径（含 `raw_payload` 内路径）与 OA 字段对应关系；确认后固化，未确认项不得臆造取值。

| 序号 | CRM/PG库字段 | OA字段 | 字段说明 | 取值方式 | 备注 |
|------|-------------|--------|----------|----------|------|
| 1 | field_FXfm3__c | field0329 | NC销售订单号 | 直传String | CRM自定义字段 |
| 2 | 空 | field0003 | 销售公司 | **本期传空** | 选单位；后续再定取值 |
| 3 | 人员映射→OA人员ID | field0006 | 业务员 | 见 4.6 | 选人；与流程发起人同一映射结果 |
| 4 | 固定值 "CAC02" | field0007 | 销售组织 | 固定值 | 默认：上海泰禾国际贸易有限公司 |
| 5 | customer.name | field0008 | 客户名称 | 取CRM客户对象name | 从CRM接口参数取值 |
| 6 | currency_type.label | field0009 | 销售币种 | 取嵌套对象label | |
| 7 | field_TwmQQ__c.label | field0010 | 贸易术语 | 取嵌套对象label | CRM自定义字段 |
| 8 | field_NjupX__c.name | field0011 | 目的地 | 取嵌套对象name | CRM自定义字段 |
| 9 | field_65xcf__c.label | field0012 | 运输方式 | 取嵌套对象label | CRM自定义字段 |
| 10 | field_TZKmt__c.label | field0013 | 收款方式 | 取嵌套对象label | CRM自定义字段 |
| 11 | field_9uwgg__c.label | field0015 | 订单账期 | 取嵌套对象label | CRM自定义字段 |
| 12 | field_X7vPP__c.value | field0017 | 出运日期 | 取嵌套对象value | CRM自定义字段 |
| 13 | field_Swpt6__c.value | field0018 | 收款日期 | 取嵌套对象value | CRM自定义字段 |
| 14 | 固定值 | field0019 | 内外贸 | 固定值（待确认） | 暂用代码默认值，后续确认具体取值方式 |
| 15 | 固定值 "1" | field0020 | 是否集中采购 | 固定值 "1" | 暂用代码默认值，后续确认具体取值方式 |
| 16 | name | field0025 | 客户订单号 | 直传 | CRM订单编号 |
| 17 | field_234i0__c.code | field0029 | 是否需要盖章 | code="1"→"1"，否则→"2" | OA(1:是, 2:否)，CRM(0:否, 1:是) |
| 18 | 固定值 "1" | field0031 | 委托放行单 | 固定值 "1" | 暂用代码默认值，后续确认具体取值方式 |
| 19 | 固定值 "1" | field0035 | 委外方式 | 固定值 "1" | 暂用代码默认值，后续确认具体取值方式 |
| 20 | null | field0222 | 是否超默认账期 | null | OA计算公式字段 |
| 21 | field_zjW8t__c | field0254 | 客户编码 | 直传String | CRM自定义字段 |
| 22 | 固定值 "S1201280054" | field0327 | 客户主键 | 固定值 | 暂用代码默认值，后续确认新的取值方式 |
| 23 | 固定值 "空值" | field0408 | 原币信用额度 | 固定值 | 暂用代码默认值，后续确认具体取值方式 |
| 24 | field_NjupX__c.name | field0461 | 目的国 | 取嵌套对象name | 与目的地(field0011)取值相同 |
| 25 | field_Z6A3J__c.name | field0462 | 出口港 | 取嵌套对象name | CRM自定义字段 |

### 6.5 订单明细 → OA表单子表字段映射（formson_2819）

> OA子表表名：`formson_2819`。**每个OA表单只含当前这一条明细的一行子表数据**（不传多行）。
> **`formson_2818`（销售费用与利润明细表）本期不传。**
> 下表为初版草案，实施时与实施人员确认取值路径（可来自结构化列或明细 `raw_payload`）。

| 序号 | CRM子订单字段 | OA字段 | 字段说明 | 取值方式 | 备注 |
|------|---------------|--------|----------|----------|------|
| 1 | pd_count | field0074 | 销售-数量 | 直传String | |
| 2 | field_Mb25P__c | field0089 | 物料编号 | 直传String | CRM自定义字段 |
| 3 | null | field0091 | 销售税率 | null | OA计算公式字段 |
| 4 | actual_price | field0092 | 销售单价 | 直传String | |
| 5 | field_USMmk__c | field0093 | 考核单价 | 直传String | CRM自定义字段 |
| 6 | 空字符串 "" | field0129 | 发货日期 | 空字符串 | 暂用代码默认值，后续确认是否需要填充 |
| 7 | field_qx94q__c.value | field0130 | 要求到货日 | 取**主订单**嵌套对象value | 注意：取主订单字段，非子订单字段 |
| 8 | pd_code 判断 | field0443 | 零售包装 | pd_code 以 "53" 开头→"0001"(是)，否则→"0002"(否) | pd_code 来自CRM明细接口 |

### 6.6 OA表单流程参数

> 以下为调用致远OA发起表单流程时的顶层请求参数（与 ESB `ESB_T00001_DI95TDSF1B` 一致）。

| 参数 | 值 | 说明 |
|------|-----|------|
| appName | `collaboration` | 应用类型：协同 |
| templateCode | `CRM_ZYXS_001` | 表单模板编号 |
| draft | `0` | 新建-发送（非待发） |
| data.formmain_2817 | {字段KV} | 主表数据 |
| data.formson_2819 | {字段KV} | 当前明细对应的单行子表数据 |
| attachments | [] | 附件ID列表（Long型），当前为空 |

> **重要**：
> - 一笔订单若有 N 条明细 → 发起 N 个 OA 表单（每条明细一个表单，每个表单仅含一行 formson_2819）
> - **不传** `formson_2818`
> - OA流程**无需配置审批人**
> - **业务员(field0006)与流程发起人**均使用 4.6 人员映射得到的 OA 人员ID
> - **field0003(销售公司)本期传空**

### 6.7 OA同步状态字段（挂在明细子表，非CRM字段）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| oa_sync_status | 枚举 | PENDING(待同步) / SUCCESS(已成功) / RETRY(重试中) / FAILED(已失败) |
| oa_process_id | 文本 | OA流程实例ID（该明细同步成功后记录） |
| oa_sync_time | 时间戳 | 最近一次OA同步时间 |
| retry_count | 数值 | OA同步重试次数（最大3次） |
| last_error_msg | 文本 | 最近一次同步失败的错误信息 |

> 主表可另增 `detail_fetch_status`（如 PENDING/SUCCESS/FAILED）标记明细是否拉取成功，用于门禁：明细未成功拉取前不进入OA同步。

### 6.8 CRM↔OA 人员映射表（独立，非订单字段）

> 表名建议：`t_integration_crm_oa_user_mapping`。与订单表解耦，由人员映射能力维护。
> 员工账号、OA人员接口同样可能返回文档未列全的字段，须预留原始报文列。

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键（雪花） |
| crm_employee_id | 文本 | CRM员工ID（来自订单 owner.id），业务唯一键 |
| emp_code | 文本 | CRM员工账号接口返回的 emp_code（已确认取值） |
| oa_user_id | 文本 | OA按 code 查询得到的人员ID（已确认取值） |
| oa_login_name | 文本 | OA登录名（若接口返回则保存，便于 Token loginName） |
| crm_employee_name | 文本 | CRM员工姓名（可选，便于排查） |
| crm_raw_payload | JSONB | CRM 查询员工帐号接口**完整响应**（或单条员工对象），预留未文档化字段 |
| oa_raw_payload | JSONB | OA 按编码取人员接口**完整响应**，预留未文档化字段 |
| last_mapped_at | 时间戳 | 最近一次成功映射时间 |
| created_at / updated_at / is_deleted | — | 标准审计与逻辑删除字段 |

**读写规则**：
- 同步前按 `crm_employee_id` 查询；命中且 `oa_user_id` 有效则直接使用
- 未命中则走 CRM→OA 解析链路，成功后 **UPSERT** 落库（结构化关键字段 + 两侧 `*_raw_payload`）
- 人员映射允许更新（与订单「仅新增不更新」分离）
- 后续若需改用其他返回字段（如改取 `employee_code` 而非 `emp_code`），与实施人员确认后从 `crm_raw_payload` / `oa_raw_payload` 调整解析即可

> **说明**：6.2/6.3 为CRM接口文档参考字段清单（**非穷尽**）；完整数据在各表 `raw_payload`。6.4/6.5 为初版映射草案，实施确认后固化。6.6 为OA表单流程请求参数结构。6.7 同步状态挂在**明细**上。6.8 为独立人员映射表（含原始报文预留）。
>
> **字段映射分类**：
> - **CRM直传字段**：从CRM订单/明细数据中直接取值（含嵌套对象取label/name/value，或从 `raw_payload` 按确认路径取值）；客户名称取 `customer.name`
> - **CRM自定义字段**：CRM中 field_xxx__c 格式的自定义字段（常仅存在于原始报文）
> - **人员映射字段**：field0006(业务员)、流程发起人——经独立人员映射能力得到 OA人员ID
> - **本期传空**：field0003(销售公司)
> - **固定值字段（暂用默认值）**：field0019(内外贸)、field0020(是否集中采购)、field0031(委托放行单)、field0035(委外方式)、field0327(客户主键)、field0129(发货日期)、field0408(原币信用额度)——以上字段暂用代码中默认值，后续业务方确认后更新
> - **计算公式字段**：OA表单通过公式自动计算，传null
> - **本期不传**：formson_2818 全部字段
> - **实施确认项**：凡文档未覆盖或 ESB 与现网不一致的取值路径，一律与实施人员确认后再解析

### 6.9 OA表单完整字段定义（附件：销售订单.xlsx）

> 以下为致远OA "销售订单" 表单的全部字段定义，来源于附件 `销售订单.xlsx`，按表结构分类列出。

#### 6.9.1 主表 formmain_2817（销售订单，约100字段）

| 字段名 | 字段类型 | 显示名称 | 输入类型 |
|--------|----------|----------|----------|
| field0002 | VARCHAR(100) | 表单编号 | 文本 |
| field0003 | VARCHAR(20) | 销售公司 | 选单位 |
| field0004 | VARCHAR(100) | 业务流程 | 文本 |
| field0005 | DATE | 日期 | 日期 |
| field0006 | VARCHAR(20) | 业务员 | 选人 |
| field0007 | VARCHAR(100) | 销售组织 | 文本 |
| field0008 | VARCHAR(255) | 客户名称 | 文本 |
| field0009 | VARCHAR(100) | 销售币种 | 文本 |
| field0010 | VARCHAR(100) | 贸易术语 | 文本 |
| field0011 | VARCHAR(255) | 目的地 | 文本 |
| field0012 | VARCHAR(100) | 运输方式 | 文本 |
| field0013 | VARCHAR(255) | 收款方式 | 文本 |
| field0014 | NUMBER(20,0) | 默认账期 | 文本 |
| field0015 | NUMBER(20,0) | 订单账期 | 文本 |
| field0016 | VARCHAR(255) | 工厂装箱 | 文本 |
| field0017 | DATE | 出运日期 | 日期 |
| field0018 | DATE | 收款日期 | 日期 |
| field0019 | VARCHAR(255) | 内外贸 | 文本 |
| field0020 | NUMBER(19,0) | 是否集中采购 | 下拉 |
| field0021 | NUMBER(20,2) | 信用额度 | 文本 |
| field0022 | NUMBER(20,2) | 应收款 | 文本 |
| field0023 | NUMBER(20,2) | 信用余额 | 文本 |
| field0024 | VARCHAR(100) | 是否信用超限 | 文本 |
| field0025 | VARCHAR(100) | 客户订单号 | 文本 |
| field0026 | VARCHAR(100) | 收货联系人 | 文本 |
| field0027 | VARCHAR(100) | 联系人电话 | 文本 |
| field0029 | NUMBER(19,0) | 是否需要盖章 | 下拉 |
| field0030 | VARCHAR(20) | 印章管理员 | 选人 |
| field0031 | NUMBER(19,0) | 委托放行单 | 下拉 |
| field0032 | NUMBER(21,1) | 20尺标箱数量 | 文本 |
| field0035 | NUMBER(19,0) | 委外方式 | 下拉 |
| field0136 | VARCHAR(100) | 是否超账期 | 文本 |
| field0145 | VARCHAR(255) | 表单转文档1 | 表单自定义控件 |
| field0192 | NUMBER(20,2) | 销售合计1 | 文本 |
| field0193 | NUMBER(20,2) | 考核合计 | 文本 |
| field0194 | NUMBER(20,2) | 费用合计 | 文本 |
| field0209 | NUMBER(20,2) | 利润 | 文本 |
| field0210 | NUMBER(20,2) | 利润率 | 文本 |
| field0211 | NUMBER(20,2) | 作废工厂承担费用 | 文本 |
| field0212 | VARCHAR(100) | 台账流水号 | 文本 |
| field0215 | VARCHAR(100) | 需盖章文件 | 文本 |
| field0220 | VARCHAR(20) | 已双方盖章合同附件 | 上传附件 |
| field0222 | VARCHAR(100) | 是否超默认账期 | 文本 |
| field0223 | VARCHAR(255) | 框架合同编号 | 文本 |
| field0224 | VARCHAR(20) | 合同附件 | 上传附件 |
| field0226 | VARCHAR(150) | 备注 | 文本 |
| field0236 | VARCHAR(100) | 国别 | 文本 |
| field0237 | VARCHAR(100) | 洲别 | 文本 |
| field0238 | VARCHAR(100) | 航线 | 文本 |
| field0242 | NUMBER(20,4) | 无税总销售合计 | 文本 |
| field0244 | VARCHAR(100) | 废弃字段1 | 文本 |
| field0245 | VARCHAR(20) | 职务级别 | 选职务级别 |
| field0247 | VARCHAR(100) | 可发货状态 | 文本 |
| field0252 | NUMBER(24,2) | 分摊金额 | 文本 |
| field0254 | VARCHAR(100) | 客户编码 | 文本 |
| field0256 | VARCHAR(100) | 业务流程主键 | 文本 |
| field0259 | VARCHAR(100) | 贸易术语主键 | 文本 |
| field0260 | VARCHAR(100) | 目的地主键 | 文本 |
| field0261 | VARCHAR(100) | 运输方式主键 | 文本 |
| field0262 | VARCHAR(100) | 收款方式主键 | 文本 |
| field0263 | VARCHAR(100) | 订单账期主键 | 文本 |
| field0264 | VARCHAR(100) | 内外贸主键 | 文本 |
| field0268 | VARCHAR(100) | 原币 | 文本 |
| field0269 | VARCHAR(100) | 原币-销售币种-主键 | 文本 |
| field0270 | NUMBER(25,5) | 总数量 | 文本 |
| field0272 | NUMBER(22,2) | 价税合计 | 文本 |
| field0279 | NUMBER(20,5) | 日汇率 | 文本 |
| field0291 | VARCHAR(100) | 默认美元 | 文本 |
| field0292 | VARCHAR(100) | 默认人民币 | 文本 |
| field0300 | VARCHAR(100) | 销售公司本位币 | 文本 |
| field0323 | VARCHAR(100) | 工厂装箱主键 | 文本 |
| field0327 | VARCHAR(100) | 客户主键 | 文本 |
| field0329 | VARCHAR(2000) | NC销售订单号 | 文本 |
| field0331 | VARCHAR(100) | 人员编号 | 文本 |
| field0356 | VARCHAR(100) | 本位币主键 | 文本 |
| field0368 | NUMBER(20,2) | 考核合计-无税 | 文本 |
| field0370 | NUMBER(20,5) | 美元原币汇率 | 文本 |
| field0371 | NUMBER(20,5) | 人民币原币汇率 | 文本 |
| field0374 | VARCHAR(100) | NC交易类型编码-销售 | 文本 |
| field0375 | VARCHAR(100) | NC交易类型编码-调拨 | 文本 |
| field0376 | VARCHAR(100) | NC交易类型编码-采购 | 文本 |
| field0377 | VARCHAR(100) | NC交易类型编码-入库 | 文本 |
| field0379 | NUMBER(20,5) | 利润人民币 | 文本 |
| field0383 | VARCHAR(1000) | NC_调拨_采购_入库_单号 | 文本 |
| field0385 | VARCHAR(100) | 销售公司转文本 | 文本 |
| field0387 | VARCHAR(100) | 制单人主键 | 文本 |
| field0389 | VARCHAR(2000) | NC采购订单号 | 文本 |
| field0390 | VARCHAR(2000) | NC调拨单号 | 文本 |
| field0391 | VARCHAR(2000) | NC采购入库单号 | 文本 |
| field0393 | VARCHAR(100) | 超默认账期提示 | 文本 |
| field0395 | NUMBER(20,2) | 现时信用余额 | 文本 |
| field0397 | VARCHAR(255) | 销售公司境内外属性 | 文本 |
| field0399 | VARCHAR(255) | 销售公司文本转换 | 文本 |
| field0401 | VARCHAR(20) | OA发起人 | 选人 |
| field0402 | VARCHAR(20) | OA审批人 | 选人 |
| field0404 | VARCHAR(255) | 默认账期主键 | 文本 |
| field0406 | VARCHAR(100) | OA发起人OA编号 | 文本 |
| field0408 | NUMBER(20,2) | 原币信用额度 | 文本 |
| field0409 | NUMBER(20,2) | 原币应收款 | 文本 |
| field0417 | VARCHAR(20) | 发起者部门 | 选部门 |
| field0419 | VARCHAR(100) | 发货公司or销售公司 | 文本 |
| field0421 | VARCHAR(2000) | 原跨公司销售订单 | 文本 |
| field0428 | VARCHAR(2000) | 原表单编号 | 文本 |
| field0432 | VARCHAR(2000) | 原外购直运销售订单 | 文本 |
| field0434 | VARCHAR(2000) | 原自有销售订单 | 文本 |
| field0436 | VARCHAR(100) | 变更原因 | 文本 |
| field0437 | VARCHAR(100) | 入库说明 | 文本 |
| field0439 | VARCHAR(100) | 国家地区名称 | 文本 |
| field0451 | NUMBER(20,5) | 锁汇汇率 | 文本 |
| field0453 | VARCHAR(100) | 关联销售跟踪表-单据编号 | 文本 |
| field0454 | VARCHAR(100) | 原头编号 | 文本 |
| field0456 | VARCHAR(20) | 业务员归属部门 | 选部门 |
| field0461 | VARCHAR(100) | 目的国 | 文本 |
| field0462 | VARCHAR(100) | 出口港 | 文本 |
| field0468 | VARCHAR(100) | 源头编号中转 | 文本 |
| field0470 | VARCHAR(100) | 特殊字符判断 | 文本 |
| field0472 | VARCHAR(100) | 结算方式 | 文本 |
| field0473 | NUMBER(20,0) | 承兑比列 | 文本 |
| field0478 | VARCHAR(4000) | 加签相关人 | 选多人 |
| field0480 | NUMBER(20,2) | 报关合计总金额 | 文本 |
| field0481 | VARCHAR(20) | 销售订单发起人 | 选人 |
| field0483 | NUMBER(20,1) | 40GP数量 | 文本 |
| field0484 | VARCHAR(255) | 订舱费供应商 | 文本 |
| field0485 | VARCHAR(255) | 包干费供应商 | 文本 |
| field0487 | NUMBER(20,0) | 数量合计 | 文本 |
| field0488 | VARCHAR(20) | 发起人 | 选人 |
| field0492 | NUMBER(20,2) | 仓储费合计 | 文本 |

#### 6.9.2 子表1 formson_2818（销售费用与利润明细表）

| 字段名 | 字段类型 | 显示名称 | 输入类型 |
|--------|----------|----------|----------|
| field0033 | NUMBER(20,0) | 序号1 | 序号 |
| field0069 | VARCHAR(255) | 费用类别 | 文本 |
| field0358 | VARCHAR(255) | 费用类别PK | 文本 |
| field0430 | VARCHAR(255) | 费用类别-变更 | 文本 |
| field0036 | NUMBER(20,2) | 金额 | 文本 |
| field0037 | VARCHAR(255) | 供应商 | 文本 |
| field0265 | VARCHAR(255) | 供应商PK | 文本 |
| field0360 | VARCHAR(255) | 币种PK | 文本 |
| field0372 | NUMBER(20,2) | 汇率折算金额 | 文本 |
| field0441 | VARCHAR(20) | 费用承担公司 | 选单位 |

#### 6.9.3 子表2 formson_2819（物料清单明细表）

| 字段名 | 字段类型 | 显示名称 | 输入类型 |
|--------|----------|----------|----------|
| field0074 | NUMBER(20,5) | 销售-数量 | 文本 |
| field0075 | VARCHAR(100) | 销售-单位 | 文本 |
| field0076 | NUMBER(20,2) | 销售合计 | 文本 |
| field0077 | VARCHAR(100) | 报关公司 | 文本 |
| field0078 | VARCHAR(100) | 报关单价 | 文本 |
| field0089 | VARCHAR(100) | 物料编号 | 文本 |
| field0091 | NUMBER(20,2) | 销售税率 | 文本 |
| field0092 | NUMBER(24,4) | 销售单价 | 文本 |
| field0093 | NUMBER(24,4) | 考核单价 | 文本 |
| field0114 | VARCHAR(100) | 境内外调拨 | 文本 |
| field0115 | VARCHAR(100) | 结算币种 | 文本 |
| field0118 | NUMBER(20,2) | 调拨合计 | 文本 |
| field0119 | VARCHAR(255) | 采购-供应商 | 文本 |
| field0120 | VARCHAR(100) | 采购币种 | 文本 |
| field0121 | NUMBER(22,2) | 采购税率 | 文本 |
| field0122 | VARCHAR(20) | 采购员 | 选人 |
| field0123 | VARCHAR(20) | 采购员部门 | 选部门 |
| field0124 | VARCHAR(255) | Y采购员归属 | 文本 |
| field0125 | VARCHAR(100) | Y采购组织 | 文本 |
| field0126 | NUMBER(22,4) | 采购单价 | 文本 |
| field0127 | VARCHAR(100) | 付款账期 | 文本 |
| field0128 | NUMBER(20,2) | 采购合计 | 文本 |
| field0129 | DATE | 发货日期 | 日期 |
| field0130 | DATE | 要求到货日 | 日期 |
| field0131 | DATE | 计划到货日 | 日期 |
| field0132 | VARCHAR(100) | 收发-备注 | 文本 |
| field0138 | VARCHAR(100) | Y件数 | 文本 |
| field0139 | VARCHAR(100) | Y包装种类 | 文本 |
| field0140 | VARCHAR(100) | Y净重千克 | 文本 |
| field0141 | VARCHAR(100) | Y毛重千克 | 文本 |
| field0168 | VARCHAR(200) | 物料名称 | 文本 |
| field0230 | VARCHAR(100) | 发货公司 | 文本 |
| field0239 | NUMBER(22,2) | 考核价税合计 | 文本 |
| field0266 | VARCHAR(100) | 物料编号PK | 文本 |
| field0273 | NUMBER(22,2) | 明细价税合计 | 文本 |
| field0274 | NUMBER(24,4) | 无税单价 | 文本 |
| field0275 | NUMBER(22,2) | 无税金额 | 文本 |
| field0276 | NUMBER(22,2) | 税额 | 文本 |
| field0282 | NUMBER(24,4) | 本币含税单价 | 文本 |
| field0283 | NUMBER(24,4) | 本币无税单价 | 文本 |
| field0284 | NUMBER(22,2) | 本币无税金额 | 文本 |
| field0285 | NUMBER(22,2) | 本币价税合计 | 文本 |
| field0287 | VARCHAR(100) | 结算路径 | 文本 |
| field0294 | VARCHAR(100) | 发货公司本位币 | 文本 |
| field0295 | VARCHAR(100) | 发货公司本位币主键 | 文本 |
| field0297 | NUMBER(22,2) | 无税考核金额合计 | 文本 |
| field0298 | NUMBER(22,4) | 利润明细 | 文本 |
| field0302 | NUMBER(20,2) | Y物料税率 | 文本 |
| field0304 | VARCHAR(100) | 原发货公司本位币 | 文本 |
| field0306 | NUMBER(20,2) | 结算税率 | 文本 |
| field0307 | NUMBER(22,4) | 结算单价 | 文本 |
| field0311 | VARCHAR(100) | 采购员归属主键 | 文本 |
| field0313 | VARCHAR(100) | 付款账期主键 | 文本 |
| field0315 | VARCHAR(100) | 采购组织主键 | 文本 |
| field0317 | VARCHAR(100) | 发货公司主键 | 文本 |
| field0319 | VARCHAR(100) | 结算币种主键 | 文本 |
| field0321 | VARCHAR(100) | 采购币种主键 | 文本 |
| field0325 | VARCHAR(100) | 采购-供应商主键 | 文本 |
| field0333 | VARCHAR(100) | 采购-供应商编码 | 文本 |
| field0335 | NUMBER(22,4) | 采购-无税单价 | 文本 |
| field0337 | NUMBER(20,2) | 采购-无税金额 | 文本 |
| field0338 | NUMBER(22,4) | 采购-本币无税单价 | 文本 |
| field0339 | NUMBER(20,5) | 采购-日汇率 | 文本 |
| field0340 | NUMBER(22,4) | 采购-本币含税单价 | 文本 |
| field0341 | NUMBER(20,2) | 采购-本币无税金额 | 文本 |
| field0342 | NUMBER(20,2) | 采购-本币价税合计 | 文本 |
| field0343 | NUMBER(20,2) | 采购-本币税额 | 文本 |
| field0345 | NUMBER(22,4) | 调拨-无税单价 | 文本 |
| field0347 | NUMBER(22,4) | 调拨-本币无税单价 | 文本 |
| field0348 | NUMBER(20,5) | 调拨-日汇率 | 文本 |
| field0349 | NUMBER(22,4) | 调拨-本币含税单价 | 文本 |
| field0350 | NUMBER(20,2) | 调拨-无税金额 | 文本 |
| field0351 | NUMBER(20,2) | 调拨-价税合计 | 文本 |
| field0352 | NUMBER(20,2) | 调拨-本币无税金额 | 文本 |
| field0353 | NUMBER(20,2) | 调拨-本币价税合计 | 文本 |
| field0354 | NUMBER(20,2) | 调拨-税额 | 文本 |
| field0362 | NUMBER(20,2) | 采购-原币税额 | 文本 |
| field0366 | NUMBER(20,2) | 采购-价税合计 | 文本 |
| field0381 | VARCHAR(100) | 存货名称_英 | 文本 |
| field0411 | VARCHAR(255) | 选择手册号 | 文本 |
| field0413 | VARCHAR(100) | 手册号 | 文本 |
| field0415 | VARCHAR(100) | 产品手册属性 | 文本 |
| field0423 | VARCHAR(100) | 报关合计 | 文本 |
| field0424 | NUMBER(20,2) | 结算合计 | 文本 |
| field0426 | VARCHAR(100) | 物料编号-变更 | 文本 |
| field0443 | NUMBER(19,0) | 零售包装 | 下拉 |
| field0444 | VARCHAR(255) | 自产一般 | 文本 |
| field0445 | VARCHAR(255) | 原药制剂 | 文本 |
| field0447 | VARCHAR(100) | 报关公司1 | 文本 |
| field0448 | VARCHAR(100) | 报关单价1 | 文本 |
| field0449 | VARCHAR(100) | 报关合计1 | 文本 |
| field0458 | NUMBER(24,4) | 报关单价-数字类型 | 文本 |
| field0459 | NUMBER(20,2) | 报关总计-数字类型 | 文本 |
| field0464 | NUMBER(20,2) | 销售数量1000倍 | 文本 |
| field0466 | VARCHAR(255) | 存货名称英 | 文本域 |
| field0475 | VARCHAR(20) | 是否预付 | 下拉 |
| field0476 | NUMBER(20,2) | 预付金额 | 文本 |
| field0482 | VARCHAR(100) | 结算单价文本 | 文本 |
| field0486 | NUMBER(20,2) | 作废字段 | 文本 |
| field0489 | VARCHAR(100) | BOM版本号 | 文本 |
| field0490 | VARCHAR(100) | 产品名 | 文本 |
| field0491 | VARCHAR(100) | 物料英文 | 文本 |

> **说明**：6.9 为OA表单的完整字段定义（共约195个字段），来源于业务方提供的 `销售订单.xlsx`。
> 当前一期对接仅映射 6.4/6.5 中的字段（主表25个 + formson_2819 共8个）；**formson_2818 本期不传**；其余字段不使用或由OA系统侧计算/默认填充。

---

## 7. 技术方案

> 需求确认后补充技术设计。

---

## 8. 验收标准

| 序号 | 验收点 | 验收方式 | 通过标准 |
|------|--------|----------|----------|
| V1 | 定时拉取CRM订单并入库 | 自动 | 按每5分钟周期执行；按 modify_time 过滤；CRM返回字段正确写入PG库；单轮≤100笔 |
| V2 | 订单明细拉取与存储 | 自动 | 明细通过 orderDetailQuery 获取并写入子表；与主表通过 order_id 关联；含 pd_code |
| V3 | 默认按当天 modify_time 查询 | 自动 | 未指定时间范围时，按 Asia/Shanghai 当天 00:00:00 ~ 23:59:59 的 modify_time 查询 |
| V4 | 原始报文全量保留 | 自动 | 主表/明细/人员映射均写入完整接口响应至 JSONB 原始列；文档未列字段不丢失 |
| V4a | 映射实施确认 | 手动 | OA/人员取值路径与实施人员确认后再固化；可从 raw_payload 调整解析而无需重拉历史数据 |
| V5 | 仅新增不更新 | 自动 | 主表/明细已存在则跳过，不做UPDATE；CRM明细不删除，PG侧亦不删 |
| V6 | 不做审批状态判断 | 自动 | 入库与OA同步均不依赖 approval_status；CRM返回数据默认已审批通过 |
| V7 | 明细拉取失败门禁 | 自动 | 明细拉取失败时主表不回滚、下轮补拉；明细未就绪前不发起OA |
| V8 | 字段映射准确性 | 自动 | 按6.4/6.5映射；customer.name、pd_code 判断正确；field0003为空；不传 formson_2818 |
| V9 | 一条明细一个OA表单 | 手动 | N条明细发起N个OA流程；每个表单仅含一行 formson_2819 |
| V10 | 去重机制 | 自动 | 同一CRM明细ID不会重复发起OA流程 |
| V11 | 失败重试 | 自动 | OA同步失败后下一周期从PG库重试，最多3次 |
| V12 | 告警通知 | 手动 | 达到最大重试次数后，企微Webhook收到告警消息 |
| V13 | 同步记录可追溯 | 手动 | 明细级可查采集/同步时间、状态、OA流程ID |
| V14 | 采集与同步解耦 | 自动 | 采集失败不影响已入库明细的OA同步；OA失败不影响新订单采集 |
| V15 | 异常不中断 | 自动 | 单笔订单或明细处理异常不影响其他记录 |
| V16 | 单轮上限 | 自动 | 采集单轮≤100笔订单；OA同步单轮≤100条明细 |
| V17 | CRM↔OA人员映射 | 自动 | 按 owner.id→emp_code→OA人员ID 解析；映射关系落库；再次同步可复用 |
| V18 | 业务员与发起人一致 | 手动 | field0006 与流程发起人均为同一映射得到的 OA人员ID |
| V19 | 人员映射失败门禁 | 自动 | 映射失败时不发起OA，明细记 RETRY |

---

## 9. 风险与依赖

| 序号 | 风险/依赖 | 影响 | 应对措施 |
|------|-----------|------|----------|
| R1 | 勤策CRM接口权限 | 无法查询订单数据 | openId/appKey已获取，确认IP白名单配置 |
| R2 | 致远OA表单模板未创建 | 无法发起流程 | 已确认模板编号 CRM_ZYXS_001，表单已存在 |
| R3 | 勤策CRM订单明细查询频率高 | 每笔订单需单独查明细，订单量大时API调用频繁 | 控制采集频率（每5分钟），必要时分页控制 |
| R4 | 致远OA Token过期 | 同步中断 | Token设置Redis缓存+自动刷新机制 |
| R5 | CRM订单量大，单次同步超时 | 同步周期内未完成 | 分页拉取 + 单轮上限100笔/100条明细 |
| R6 | OA接口频率限制 | 发起流程被限流 | 控制调用频率，必要时增加间隔 |
| R7 | 网络不稳定导致接口调用失败 | 同步失败 | 重试机制 + 告警通知 |
| R8 | 明细拉取失败导致OA延迟 | 主表已入库但长期无明细 | 下轮持续补拉；告警可后续增强 |
| R9 | CRM/OA人员编码不一致 | emp_code 在OA查无，业务员/发起人无法赋值 | 映射失败记RETRY+告警；人工核对主数据 |
| R10 | 固定值字段未最终确认 | 内外贸等字段可能需调整 | Q8-Q11 暂用默认值，确认后热更配置 |
| R11 | field0003 本期传空 | 销售公司为空可能影响OA必填校验 | 业务确认可空；若OA强校验需尽快补取值规则 |

---

## 10. 待确认事项

| 序号 | 待确认内容 | 确认方 | 当前状态 |
|------|-----------|--------|----------|
| Q1 | 勤策CRM的openid和appkey | 内部管理员 | ✅ 已确认：openId=`8858965636174056137`，appKey=`0H4aAHGY0htrsglQKm` |
| Q2 | 致远OA表单模板编号 | OA管理员 | ✅ 已确认：`CRM_ZYXS_001` |
| Q3 | 致远OA表单各字段(field0003等)的中文显示名 | OA管理员 | ✅ 已确认：见附件 `销售订单.xlsx` 完整字段定义 |
| Q4 | 同步周期 | 业务方 | ✅ 已确认：**每5分钟** |
| Q5 | OA审批流程的审批人配置方式 | 业务方 | ✅ 已确认：**无需配置审批人** |
| Q6 | CRM订单作废后是否需要同步通知OA撤回流程 | 业务方 | ✅ **不需要**（本期不做） |
| Q7 | 是否需要将OA审批结果回写到CRM | 业务方 | ✅ **不需要**（本期不做） |
| Q8 | field0019(内外贸)取值方式 | 业务方 | ⏳ **暂用代码默认值**，后续确认后更新 |
| Q9 | field0031(委托放行单)和field0035(委外方式)取值 | 业务方 | ⏳ **暂用代码默认值 "1"**，后续确认是否需要对接CRM字段 |
| Q10 | field0129(发货日期)取值 | 业务方 | ⏳ **暂用空字符串 ""**，后续确认是否需要填充 |
| Q11 | field0327(客户主键)取值 | 业务方 | ⏳ **暂用代码默认值 "S1201280054"**，后续确认新的取值方式 |
| Q12 | field0003 / field0006 / 发起人取值 | 业务方 | ✅ **field0003本期传空**；**field0006与发起人**走 CRM员工ID→emp_code→OA人员ID 映射并落库 |
| Q13 | 同步粒度 | 业务方 | ✅ **一条明细对应一个OA表单**（对齐 ESB） |
| Q14 | 采集时间字段 | 业务方 | ✅ **按 modify_time**；默认当天；时区 Asia/Shanghai |
| Q15 | 审批状态判断 | 业务方 | ✅ **不需要**（CRM返回默认已审批通过） |
| Q16 | 订单/明细是否更新 | 业务方 | ✅ **仅新增不更新**；明细不删除，未同步则新增 |
| Q17 | formson_2818 | 业务方 | ✅ **本期不传** |
| Q18 | 单轮处理上限 | 业务方 | ✅ **100**（采集订单/同步明细） |
| Q20 | 表结构：原始报文预留与字段解析确认 | 开发方/实施 | ✅ **结构化关键列 + JSONB 原始报文**；6.2/6.3/映射表为参考，实际取值路径与实施人员确认后再解析 |

---

## 11. 变更记录

| 日期 | 修改人 | 修改内容 |
|------|--------|----------|
| 2026-07-11 | — | 创建需求文档，梳理对接需求 |
| 2026-07-11 | — | 优化实现逻辑：CRM数据全量存储PG库，OA同步统一走PG库读取，采集与同步两阶段解耦 |
| 2026-07-11 | — | 完善需求：补充默认同步当天数据、订单明细(子订单)查询存储、仅同步已结束审批流程的订单、CRM接口实际URL和全部字段清单 |
| 2026-07-11 | — | 补充OA接口URL和实际字段映射：主表25个字段(formmain_2817)、子表8个字段(formson_2819)、模板编号CRM_ZYXS_001，基于现有ESB规则代码提取 |
| 2026-07-11 | — | 完善确认信息：①CRM凭证已确认(openId/appKey)；②OA完整字段定义(附件销售订单.xlsx，主表约100字段+子表1约10字段+子表2约85字段)；③同步频率确认为每5分钟；④OA流程无需审批人；⑤Q6/Q7确认不需要；⑥Q8-Q12暂用代码默认值，待后续确认 |
| 2026-07-11 | — | 按业务确认修订：①一条明细一个OA表单；②按 modify_time 查询（上海时区当天）；③不做审批状态判断、仅新增不更新；④明细拉取失败不回滚、不发起OA；⑤formson_2818不传；⑥选人/选单位/发起人用默认值；⑦customer.name、pd_code 从CRM取值；⑧单轮上限100；⑨同步状态挂明细；对齐 ESB_T00001_DI95TDSF1B |
| 2026-07-11 | — | 补充CRM↔OA人员映射：独立能力，订单同步时触发；owner.id→CRM员工账号emp_code→OA按code取人员ID并落库；业务员与发起人共用；field0003本期传空 |
| 2026-07-11 | — | 确认人员接口文档与路径：CRM queryEmployee（id→emp_code）；OA GET orgMembers/code/{code}（code=emp_code→人员id/loginName） |
| 2026-07-11 | — | 表设计明确：结构化关键列 + JSONB 原始报文预留（订单/明细/人员映射）；文档未列字段不丢；解析取值与实施人员确认后再固化 |
