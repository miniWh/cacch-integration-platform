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

**目标**：定时从勤策CRM拉取订单数据，按接口返回的全部参数存储到PG库；同步OA表单时统一从PG库读取数据，经字段映射后在致远OA中发起表单审批流程。通过PG库中间存储实现数据采集与OA同步的解耦。

---

## 3. 需求描述

### 3.1 功能点

| 序号 | 功能点 | 描述 | 必选/可选 |
|------|--------|------|-----------|
| F1 | 定时拉取CRM订单并入库 | 按**每5分钟**周期从勤策CRM查询订单，将接口返回的全部字段存入PG库订单表。未指定时间范围时**默认只同步当天数据** | 必选 |
| F2 | 拉取订单明细（子订单） | 查询到订单后，根据订单ID调用明细查询接口，获取该订单下的全部子订单（明细）数据并存入PG库 | 必选 |
| F3 | 订单数据全量存储 | CRM接口返回的订单主表及明细全部参数原样存入PG库，不做字段筛选 | 必选 |
| F4 | 仅同步已结束流程的订单 | 只有CRM中审批状态为"已结束"（审批通过）的订单才进入OA同步队列，审批中的订单仅采集入库不发起OA流程 | 必选 |
| F5 | 从PG库读取并映射OA字段 | 从PG库读取订单数据，按映射规则转换为OA表单字段 | 必选 |
| F6 | 发起OA表单流程 | 在致远OA中自动发起对应的表单流程，**无需配置审批人** | 必选 |
| F7 | 同步状态记录 | 记录每笔订单的OA同步状态（待同步/同步中/成功/失败），支持重试 | 必选 |
| F8 | 失败重试机制 | OA同步失败的订单在下一周期自动重试，达到最大重试次数后告警 | 必选 |
| F9 | 同步去重 | 同一笔订单不会重复发起OA流程（基于CRM订单编号唯一标识） | 必选 |
| F10 | 同步结果告警 | 同步失败或达到最大重试次数时，通过企微Webhook发送告警通知 | 必选 |
| F11 | 手动触发同步 | 提供API接口，支持手动触发指定订单或全量订单的OA同步 | 可选 |
| F12 | 同步记录查询 | 提供API接口查询同步记录列表及详情 | 可选 |

### 3.2 交互流程

**阶段一：CRM订单采集入库**

```
定时任务触发（采集，每5分钟）
    ↓
确定查询时间范围：
    ├─ 已指定时间范围 → 按指定时间查询
    └─ 未指定时间范围 → 默认查询当天 00:00:00 ~ 23:59:59 的数据
    ↓
调用勤策CRM订单查询接口：按时间范围查询订单列表
    ↓
逐笔处理订单主表数据：
    ├─ 按订单编号(name)查询PG库 → 已存在？
    │   ├─ 是 → 更新订单数据（全量覆盖，同步状态字段不变）
    │   └─ 否 → 新增订单记录（全量存储）
    ├─ CRM接口返回的全部字段写入PG库订单主表
    └─ 根据订单ID调用CRM订单明细查询接口，获取该订单下全部子订单
        ├─ 逐笔写入PG库订单明细子表（按明细ID去重/更新）
        └─ 一笔订单可能对应多条明细记录
    ↓
分页处理：如有下一页继续拉取
    ↓
本轮采集完成
```

**阶段二：OA表单同步**

```
定时任务触发（同步，每5分钟）
    ↓
查询PG库：oa_sync_status = 'PENDING' 或 'RETRY'，且 CRM审批状态为"已结束"
    ↓
逐笔处理：
    ├─ 从PG库读取订单完整数据（主表+明细）
    ├─ 字段映射：PG库字段 → OA表单字段
    ├─ 调用致远OA接口 → 发起表单流程（无需审批人配置）
    ├─ 更新PG库同步状态（成功/失败）+ 记录OA流程实例ID
    └─ 失败时 → 重试次数 + 1，下一周期自动重试
    ↓
本轮同步完成
    ↓
检查是否有失败达到最大重试次数的订单
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
  │    (默认当天数据,     │                          │
  │     每5分钟轮询)      │                          │
  │ ────────────────────→│                          │
  │  返回订单列表         │                          │
  │ ←────────────────────│                          │
  │                      │                          │
  │  ② 按订单ID查询明细   │                          │
  │ ────────────────────→│                          │
  │  返回订单明细列表     │                          │
  │ ←────────────────────│                          │
  │                      │  ③ 全量存入PG库           │
  │                      │     (订单主表+明细子表)   │
  │                      │                          │
  │                      │  ④ 查询待同步订单         │
  │                      │     (审批已结束+待同步)   │
  │                      │  ⑤ 字段映射 PG→OA         │
  │                      │  ⑥ 发起表单流程(无审批人) │
  │                      │ ─────────────────────────→│
  │                      │    返回流程实例ID         │
  │                      │ ←─────────────────────────│
  │                      │  ⑦ 更新同步状态           │
  │                      │                          │
```

> **关键设计**：
> - **同步频率**：采集与同步均为**每5分钟**执行一次
> - 采集阶段拉取订单主表后，需逐笔调用明细查询接口获取子订单数据
> - 未指定时间范围时默认只同步当天数据
> - 仅审批状态为"已结束"的订单才进入OA同步队列
> - 采集与同步两阶段解耦，各自独立定时执行，失败互不影响

### 4.2 阶段一：CRM订单采集入库

```
定时任务触发（每5分钟）
    ↓
确定查询时间范围：
    ├─ 配置了时间范围参数 → 按指定时间查询
    └─ 未配置 → 默认查询当天 00:00:00 ~ 23:59:59
    ↓
调用勤策CRM订单查询接口（orderQuery）
    ├─ 请求参数：page/rows 分页 + query_group 时间范围过滤
    └─ 按创建时间倒序排列
    ↓
返回订单列表（分页）
    ↓
逐笔处理订单主表：
    ├─ 按订单编号(name)查询PG库
    │   ├─ 不存在 → INSERT（全量存储接口返回的全部字段）
    │   └─ 已存在 → UPDATE（全量覆盖，同步状态字段不变）
    ├─ 调用勤策CRM订单明细查询接口（orderDetailQuery）
    │   ├─ 请求参数：query_group 按 order_id 过滤
    │   └─ 返回该订单下全部明细记录（可能多条）
    └─ 明细逐笔写入PG库订单明细子表（按明细ID去重/更新）
    ↓
判断是否有下一页 → 是则继续分页拉取
    ↓
更新采集时间游标
    ↓
本轮采集完成
```

### 4.3 阶段二：OA表单同步

```
定时任务触发（每5分钟）
    ↓
查询PG库：oa_sync_status = 'PENDING' 或 'RETRY'
    且 CRM审批状态(approval_status) = 已结束（APPROVED）
    ↓
逐笔处理：
    ├─ 从PG库读取订单完整数据（主表+明细）
    ├─ 按字段映射规则转换为OA表单数据
    ├─ 调用致远OA发起表单流程（无需审批人）
    │   ├─ 成功 → 更新 oa_sync_status = 'SUCCESS'，记录流程实例ID
    │   └─ 失败 → 更新 oa_sync_status = 'RETRY'，retry_count + 1
    └─ retry_count >= 3 → 更新 oa_sync_status = 'FAILED'，触发告警
    ↓
本轮同步完成
```

### 4.4 异常处理流程

```
OA同步失败
    ↓
PG库记录：失败原因 + 重试次数 + 1 + 状态标记为 RETRY
    ↓
下一轮OA同步任务
    ↓
查询状态为 RETRY 的记录，重试次数 < 3？
    ├─ 是 → 从PG库重新读取数据，重新映射并同步OA
    └─ 否 → 状态改为 FAILED，发送企微Webhook告警，标记"需人工介入"
```

> **注意**：OA同步失败时不需要重新调CRM接口，直接从PG库读取数据重试即可。

### 4.5 订单状态与同步策略

| CRM审批状态(approval_status) | 含义 | 采集入库 | OA同步 |
|------------------------------|------|----------|--------|
| 审批中 | 订单审批流程进行中 | ✅ 采集入库 | ❌ 不同步（等待审批结束） |
| 已通过(APPROVED) | 订单审批流程已结束 | ✅ 采集入库 | ✅ 进入OA同步队列 |
| 已拒绝/已作废 | 订单被拒绝或作废 | ✅ 采集入库 | ❌ 不同步 |

> **关键规则**：只有审批状态为"已结束"（已通过/APPROVED）的订单才发起OA表单流程。
> 审批中的订单仍然采集入库（保留数据），但不触发OA同步，等后续采集轮询发现审批状态变为已结束后再进入同步队列。

---

## 5. 涉及系统与模块

### 5.1 勤策CRM

| 项目 | 内容 |
|------|------|
| 接口Base URL | `https://api.waiqin365.com` |
| **openId** | `8858965636174056137` |
| **appKey** | `0H4aAHGY0htrsglQKm` |
| 认证方式 | SHA256签名认证（openid + appkey + timestamp + msg_id），每次请求动态计算digest |
| 订单查询接口 | `POST /api/ig/v1/orderQuery/{openid}/{timestamp}/{digest}/{msg_id}` |
| 订单明细查询接口 | `POST /api/ig/v1/orderDetailQuery/{openid}/{timestamp}/{digest}/{msg_id}` |
| 同步频率 | **每5分钟**拉取一次 |

### 5.2 致远OA

| 项目 | 内容 |
|------|------|
| 表单模板编号 | `CRM_ZYXS_001` |
| 主表 | `formmain_2817`（销售订单主表，约100个字段） |
| 子表1 | `formson_2818`（销售费用与利润明细表，10个字段） |
| 子表2 | `formson_2819`（物料清单明细表，约85个字段） |
| 发起表单接口 | `POST /seeyon/rest/bpm/process/start` |
| Token认证接口 | `GET /seeyon/rest/token/{userName}/{password}?loginName={loginName}` |
| 流程状态查询 | `GET /seeyon/rest/flow/state/{flowId}` |
| 审批人 | **无需配置审批人** |

### 5.3 其他系统

| 系统 | 模块/接口 | 说明 |
|------|-----------|------|
| PG数据库 | 订单主表 | 全量存储CRM订单查询接口返回的全部字段 |
| PG数据库 | 订单明细子表 | 全量存储CRM订单明细查询接口返回的全部字段，按order_id关联主表 |
| 集成平台 | 采集定时任务 | 每5分钟从CRM拉取订单主表+明细，全量写入PG库；默认查询当天数据 |
| 集成平台 | 同步定时任务 | 每5分钟从PG库读取审批已结束且待同步的订单，映射后发起OA流程 |
| 企业微信 | Webhook告警 | 同步异常时发送告警通知 |

---

## 6. 数据存储与字段映射

### 6.1 PG库订单表设计说明

> CRM接口返回的**全部字段**原样存入PG库，不做字段筛选。
> 订单主表（`orderQuery` 接口返回）存一行，订单明细（`orderDetailQuery` 接口返回）存子表（一对多，通过 `order_id` 关联）。
> 同时增加OA同步状态管理字段。

**存储原则**：
- CRM返回什么字段，PG库就存什么字段
- 字段名按CRM接口返回的原始字段名存储（如 `id`、`name`、`customer`、`order_date` 等）
- 嵌套对象（如 `customer`、`owner`、`department`）展开存储或以JSONB存储
- 额外增加同步状态管理字段（非CRM字段）

### 6.2 CRM订单主表字段（orderQuery 接口返回）

> 以下为CRM订单查询接口返回的**全部字段**，均需存入PG库。

| 序号 | CRM字段名 | 字段说明 | 数据类型 | 备注 |
|------|-----------|----------|----------|------|
| 1 | id | 订单ID | String | CRM内部唯一标识 |
| 2 | name | 订单编号 | String | 业务唯一标识，用于去重 |
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
| 25 | **approval_status** | **审批状态** | Object | **APPROVED=已通过，仅此状态的订单同步OA** |
| 26 | source_type | 来源类型 | Object | |
| 27 | business_type | 业务类型 | Object | |
| 28 | owner | 负责人 | Object | 包含 id、name |
| 29 | department | 部门 | Object | 包含 id、name |
| 30 | creator_id | 创建人 | Object | |
| 31 | modifyier_id | 修改人 | Object | |
| 32 | create_time | 创建时间 | Object | |
| 33 | modify_time | 修改时间 | Object | |
| 34+ | field_xxx | 自定义字段 | Any | 按实际返回存储 |

### 6.3 CRM订单明细子表字段（orderDetailQuery 接口返回）

> 以下为CRM订单明细查询接口返回的**全部字段**，均需存入PG库明细子表。

| 序号 | CRM字段名 | 字段说明 | 数据类型 | 备注 |
|------|-----------|----------|----------|------|
| 1 | id | 明细ID | String | 明细唯一标识 |
| 2 | name | 明细编号 | String | |
| 3 | **order_id** | **关联订单** | Object | **包含 id 和 name，关联主表** |
| 4 | pd_id | 商品信息 | Object | |
| 5 | unit_id | 单位信息 | Object | |
| 6 | pd_count | 商品数量 | String | |
| 7 | actual_price | 实际售价 | String | |
| 8 | pd_origin_price | 原价 | String | |
| 9 | suggested_selling_price | 建议售价 | String | |
| 10 | pd_origin_amount | 原价金额 | String | |
| 11 | suggested_selling_amount | 建议售价金额 | String | |
| 12 | actual_selling_amount | 实际售价金额 | String | |
| 13 | sys_discount | 系统折扣 | String | |
| 14 | additional_discount | 额外折扣 | String | |
| 15 | amount_after_the_offer | 优惠后金额 | String | |
| 16 | is_gift | 是否赠品 | Boolean | |
| 17 | delivery_count | 已发货数量 | String | |
| 18 | back_count | 退货数量 | String | |
| 19 | slave_pd_price | 价目表信息 | Object | |
| 20 | price_detail | 价目表明细 | Object | |
| 21 | pd_portfolio | 商品组合 | Object | |
| 22 | pd_portfolio_detail | 商品组合明细 | Object | |
| 23 | node_id | BOM节点ID | String | |
| 24 | parent_node_id | BOM父节点ID | String | |
| 25 | root_node_id | BOM根节点ID | String | |
| 26 | tree_data | BOM树数据 | String | |
| 27 | portfolio_group | 组合分组 | String | |
| 28 | slave_quotation_id | 报价单信息 | Object | |
| 29 | quotation_detail_id | 报价单明细 | Object | |
| 30 | slave_contract_id | 合同信息 | Object | |
| 31 | contract_detail_id | 合同明细 | Object | |
| 32 | currency_type | 币种代码 | String | |
| 33 | exchange_rate | 汇率 | Number | |
| 34 | status | 数据状态 | Object | |
| 35 | lock_status | 锁定状态 | Object | |
| 36 | approval_status | 审批状态 | Object | |
| 37 | source_type | 来源类型 | Object | |
| 38 | business_type | 业务类型 | Object | |
| 39 | owner | 负责人 | Object | |
| 40 | department | 部门 | Object | |
| 41 | creator_id | 创建人 | Object | |
| 42 | modifyier_id | 修改人 | Object | |
| 43 | create_time | 创建时间 | Object | |
| 44 | modify_time | 修改时间 | Object | |
| 45+ | field_xxx | 自定义字段 | Any | 按实际返回存储 |

### 6.4 订单主表 → OA表单主表字段映射（formmain_2817）

> 以下映射基于现有ESB规则代码 `ESB_T00001_DI95TDSF1B` 中的实际字段对应关系。
> OA表单模板编号：`CRM_ZYXS_001`，主表表名：`formmain_2817`。
> 一笔订单的每条子订单（明细）生成一个独立的OA表单流程。

| 序号 | CRM/PG库字段 | OA字段 | 字段说明 | 取值方式 | 备注 |
|------|-------------|--------|----------|----------|------|
| 1 | field_FXfm3__c | field0329 | NC销售订单号 | 直传String | CRM自定义字段 |
| 2 | field_HVwgS__c.label | field0003 | 销售公司 | 取嵌套对象label | |
| 3 | owner_oa_id | field0006 | 业务员 | 非CRM订单字段 | 需预先查询OA用户ID，通过CRM负责人关联 |
| 4 | 固定值 "CAC02" | field0007 | 销售组织 | 固定值 | 默认：上海泰禾国际贸易有限公司 |
| 5 | customer_name | field0008 | 客户名称 | 非CRM订单字段 | 需预先查询客户名称 |
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

### 6.5 订单明细子表 → OA表单子表字段映射（formson_2819）

> OA子表表名：`formson_2819`。每条子订单明细对应一行子表数据。

| 序号 | CRM子订单字段 | OA字段 | 字段说明 | 取值方式 | 备注 |
|------|---------------|--------|----------|----------|------|
| 1 | pd_count | field0074 | 销售-数量 | 直传String | |
| 2 | field_Mb25P__c | field0089 | 物料编号 | 直传String | CRM自定义字段 |
| 3 | null | field0091 | 销售税率 | null | OA计算公式字段 |
| 4 | actual_price | field0092 | 销售单价 | 直传String | |
| 5 | field_USMmk__c | field0093 | 考核单价 | 直传String | CRM自定义字段 |
| 6 | 空字符串 "" | field0129 | 发货日期 | 空字符串 | 暂用代码默认值，后续确认是否需要填充 |
| 7 | field_qx94q__c.value | field0130 | 要求到货日 | 取**主订单**嵌套对象value | 注意：取主订单字段，非子订单字段 |
| 8 | pd_code 判断 | field0443 | 零售包装 | pd_code 以 "53" 开头→"0001"(是)，否则→"0002"(否) | |

### 6.6 OA表单流程参数

> 以下为调用致远OA发起表单流程时的顶层请求参数。

| 参数 | 值 | 说明 |
|------|-----|------|
| appName | `collaboration` | 应用类型：协同 |
| templateCode | `CRM_ZYXS_001` | 表单模板编号 |
| draft | `0` | 新建-发送（非待发） |
| data.formmain_2817 | {字段KV} | 主表数据 |
| data.formson_2819 | {字段KV} | 子表数据（每个子订单一行） |
| attachments | [] | 附件ID列表（Long型），当前为空 |

> **重要**：
> - 一笔订单若有多条子订单明细，会生成多个OA表单流程（每条明细一个表单）
> - OA流程**无需配置审批人**

### 6.7 OA同步状态字段（PG库专用，非CRM字段）

| 字段名 | 类型 | 说明 |
|--------|------|------|
| oa_sync_status | 枚举 | PENDING(待同步) / SUCCESS(已成功) / RETRY(重试中) / FAILED(已失败) |
| oa_process_id | 文本 | OA流程实例ID（同步成功后记录） |
| oa_sync_time | 时间戳 | 最近一次OA同步时间 |
| retry_count | 数值 | OA同步重试次数（最大3次） |
| last_error_msg | 文本 | 最近一次同步失败的错误信息 |

> **说明**：6.2/6.3 为CRM接口返回的全部字段清单（全量存储到PG库）。6.4/6.5 为现有ESB规则代码中实际使用的字段映射关系。6.6 为OA表单流程的请求参数结构。6.7 的同步状态字段是PG库专用，不来自CRM。
>
> **字段映射分类**：
> - **CRM直传字段**：从CRM订单数据中直接取值（含嵌套对象取label/name/value）
> - **CRM自定义字段**：CRM中 field_xxx__c 格式的自定义字段
> - **非CRM字段**：owner_oa_id、customer_name 等，需在同步前预先查询准备
> - **固定值字段（暂用默认值）**：field0019(内外贸)、field0020(是否集中采购)、field0031(委托放行单)、field0035(委外方式)、field0327(客户主键)、field0129(发货日期)、field0408(原币信用额度)——以上字段暂用代码中默认值，后续业务方确认后更新
> - **计算公式字段**：OA表单通过公式自动计算，传null

### 6.8 OA表单完整字段定义（附件：销售订单.xlsx）

> 以下为致远OA "销售订单" 表单的全部字段定义，来源于附件 `销售订单.xlsx`，按表结构分类列出。

#### 6.8.1 主表 formmain_2817（销售订单，约100字段）

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

#### 6.8.2 子表1 formson_2818（销售费用与利润明细表）

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

#### 6.8.3 子表2 formson_2819（物料清单明细表）

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

> **说明**：6.8 为OA表单的完整字段定义（共约195个字段），来源于业务方提供的 `销售订单.xlsx`。
> 当前一期对接仅映射 6.4/6.5 中的字段（主表25个 + 子表8个），其余字段不使用或由OA系统侧计算/默认填充。

---

## 7. 技术方案

> 需求确认后补充技术设计。

---

## 8. 验收标准

| 序号 | 验收点 | 验收方式 | 通过标准 |
|------|--------|----------|----------|
| V1 | 定时拉取CRM订单并入库 | 自动 | 按每5分钟周期自动执行，CRM返回的全部字段正确写入PG库 |
| V2 | 订单明细拉取与存储 | 自动 | 每笔订单的明细数据通过 orderDetailQuery 接口获取并写入PG库子表，与主表通过 order_id 关联 |
| V3 | 默认查询当天数据 | 自动 | 未指定时间范围时，自动查询当天 00:00:00 ~ 23:59:59 的订单数据 |
| V4 | 订单数据全量存储 | 自动 | PG库订单主表字段与CRM接口返回字段一一对应，明细子表数据完整 |
| V5 | 已有订单更新 | 自动 | CRM中订单变更后，PG库中对应记录被全量更新，同步状态字段不被覆盖 |
| V6 | 仅同步已结束流程订单 | 自动 | 审批状态为"审批中"的订单仅入库不同步OA；审批状态变为"已通过"后才进入OA同步队列 |
| V7 | 字段映射准确性 | 自动 | PG库订单字段正确映射到OA表单字段，嵌套对象正确取值，数值/日期格式正确 |
| V8 | OA表单流程发起 | 手动 | OA中成功创建表单流程，无需审批人配置，表单数据完整 |
| V9 | 去重机制 | 自动 | 同一订单编号不会重复发起OA流程 |
| V10 | 失败重试 | 自动 | OA同步失败后下一周期从PG库重新读取数据重试，最多重试3次 |
| V11 | 告警通知 | 手动 | 达到最大重试次数后，企微Webhook收到告警消息 |
| V12 | 同步记录可追溯 | 手动 | 每笔订单的采集时间、同步时间、状态、OA流程ID可查询 |
| V13 | 采集与同步解耦 | 自动 | CRM采集失败不影响已入库订单的OA同步；OA同步失败不影响新订单采集 |
| V14 | 异常不中断 | 自动 | 单笔订单或明细处理异常不影响其他订单 |

---

## 9. 风险与依赖

| 序号 | 风险/依赖 | 影响 | 应对措施 |
|------|-----------|------|----------|
| R1 | 勤策CRM接口权限 | 无法查询订单数据 | openId/appKey已获取，确认IP白名单配置 |
| R2 | 致远OA表单模板未创建 | 无法发起流程 | 已确认模板编号 CRM_ZYXS_001，表单已存在 |
| R3 | 勤策CRM订单明细查询频率高 | 每笔订单需单独查明细，订单量大时API调用频繁 | 控制采集频率（每5分钟），必要时分页控制 |
| R4 | 致远OA Token过期 | 同步中断 | Token设置Redis缓存+自动刷新机制 |
| R5 | CRM订单量大，单次同步超时 | 同步周期内未完成 | 支持分页拉取（page/rows），控制单次处理量 |
| R6 | OA接口频率限制 | 发起流程被限流 | 控制调用频率，必要时增加间隔 |
| R7 | 网络不稳定导致接口调用失败 | 同步失败 | 重试机制 + 告警通知 |
| R8 | CRM审批状态未及时更新 | 审批中的订单迟迟未变为已通过 | 采集任务持续轮询，状态变更后自动进入同步队列 |

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
| Q12 | owner_oa_id 和 customer_name 取值方式 | 开发方 | ⏳ **暂用代码默认值**，后续确认新的取值方式 |

---

## 11. 变更记录

| 日期 | 修改人 | 修改内容 |
|------|--------|----------|
| 2026-07-11 | — | 创建需求文档，梳理对接需求 |
| 2026-07-11 | — | 优化实现逻辑：CRM数据全量存储PG库，OA同步统一走PG库读取，采集与同步两阶段解耦 |
| 2026-07-11 | — | 完善需求：补充默认同步当天数据、订单明细(子订单)查询存储、仅同步已结束审批流程的订单、CRM接口实际URL和全部字段清单 |
| 2026-07-11 | — | 补充OA接口URL和实际字段映射：主表25个字段(formmain_2817)、子表8个字段(formson_2819)、模板编号CRM_ZYXS_001，基于现有ESB规则代码提取 |
| 2026-07-11 | — | 完善确认信息：①CRM凭证已确认(openId/appKey)；②OA完整字段定义(附件销售订单.xlsx，主表约100字段+子表1约10字段+子表2约85字段)；③同步频率确认为每5分钟；④OA流程无需审批人；⑤Q6/Q7确认不需要；⑥Q8-Q12暂用代码默认值，待后续确认 |
