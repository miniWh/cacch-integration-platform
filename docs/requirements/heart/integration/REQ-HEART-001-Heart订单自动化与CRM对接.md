<a id="top"></a>

# REQ-HEART-001 Heart 客户 PO 邮件自动化与 CRM 建单

---

## 目录

- [1. 基本信息](#s1)
- [2. 需求背景](#s2)
- [3. 需求描述](#s3)
  - [3.0 总体原则（已确认）](#s3-0)
  - [3.1 功能点](#s3-1)
  - [3.2 交互流程](#s3-2)
- [4. 业务流程](#s4)
  - [4.1 PO 处理状态机](#s4-1)
  - [4.2 关键业务规则（V1.3，来自《HeartSR自动填写逻辑设计.md》）](#s4-2)
  - [4.3 幂等与并发约束](#s4-3)
- [5. 涉及系统与模块](#s5)
- [6. 字段映射](#s6)
  - [6.1 PO PDF → 台账关键列](#s6-1)
  - [6.2 产品编码映射（HeartSR 简称 → CRM）](#s6-2)
  - [6.3 CRM 固定字段](#s6-3)
- [7. 技术方案](#s7)
  - [7.1 架构影响](#s7-1)
  - [7.2 数据库变更](#s7-2)
  - [7.3 配置变更](#s7-3)
  - [7.4 依赖变更（需父 POM 审批）](#s7-4)
  - [7.5 涉及代码](#s7-5)
  - [7.6 异常、日志与安全](#s7-6)
  - [7.7 性能与容量](#s7-7)
  - [7.8 AI 解析适配器实现](#s7-8)
- [8. 验收标准](#s8)
- [9. 风险与依赖](#s9)
- [10. 待确认事项](#s10)
- [11. 变更记录](#s11)

## <a id="s1"></a>1. 基本信息

| 项目 | 内容 |
|------|------|
| 需求编号 | REQ-HEART-001 |
| 需求标题 | 企微邮箱 PO 自动抓取 → 台账登记 → 附件上传 → 勤策 CRM 自动建单 |
| 需求类型 | integration |
| 所属系统 | heart（企微邮箱 / 微盘台账 / 勤策 CRM / 用友 NCC） |
| 优先级 | P1(高) |
| 状态 | draft |
| 提出人 | 张仁杰（业务交接人） |
| 提出日期 | 2026-11-24 |
| 负责人 | 待定 |
| 预计上线 | 待定 |

---

[↑ 返回顶部](#top)

## <a id="s2"></a>2. 需求背景

南通泰禾化工国际贸易业务中，日本客户 **Heart Co., Ltd.** 通过邮件（`ycho@heart-jp.com`）发送 PO（采购订单 PDF）。业务侧当前完全手工处理两个环节：

1. 把 PO 信息登记到 Excel 台账《HeartSR-截止到2027年1月订单.xlsx》主表「5. SR」（38 列、约 1301 行明细、416 个 PO、总货值约 9400 万元）；
2. 在勤策 CRM（私有化部署 `crm.cacch.com:50001`）中查客户、取号、上传附件、创建销售订单并触发审批流。

存在问题：

- **效率低**：单个 PO 需人工解析 PDF、逐行录入台账、再逐字段填 CRM 表单；
- **易出错**：金额、箱数、13 位物料编码、港口对象 ID 等字段人工转录错误率高，错误直接流入审批与履约；
- **不可追溯**：无处理记录、无失败重试、无异常告警，出现错单难以定位；
- **口径依赖个人**：台账填写规则（V1.3 八条）仅存在于文档与个人经验中。

前置调研已完成：台账填写规则 V1.3 定稿、CRM OpenAPI V2 签名与 `contractAdd` 调通、取号接口验证、SFTP→crmimage 附件链路验证。本需求将这些验证性成果固化为平台内可运维的生产链路。

---

[↑ 返回顶部](#top)

## <a id="s3"></a>3. 需求描述

### <a id="s3-0"></a>3.0 总体原则（已确认）

| 决策项 | 结论 |
|--------|------|
| 实现方式 | **以本平台代码为主干**；AI 仅承担「PDF → 结构化字段」解析，不参与事务编排、金额计算与接口调用 |
| 邮件来源 | **企微邮箱**（腾讯企业邮），定时轮询指定邮箱 |
| 自动化程度 | **允许全自动**（解析通过即自动建单并触发审批流），保留人工复核兜底与全链路开关 |
| 台账载体 | **维持 Excel 母本 + 输出新副本**，不改为企微智能表格 |
| 附件地址 | CRM 可达的 `crmimage` 文件服务 URL，不使用操作员 PC 内网地址 |

[↑ 返回顶部](#top)

### <a id="s3-1"></a>3.1 功能点

| 序号 | 功能点 | 描述 | 实现阶段 | 必选/可选 |
|------|--------|------|----------|-----------|
| F1 | 邮箱定时轮询 | 每 5 分钟通过 IMAP 拉取指定邮箱未读邮件；按 `UID/Message-ID` 去重，断点续拉；单轮限流（默认 20 封） | 阶段一 邮件接入 | 必选 |
| F2 | 邮件识别与附件提取 | 发件人白名单（`ycho@heart-jp.com`）且主题/正文含 PO 标识时，提取 PDF 附件；非 Heart 邮件记 `SKIP` 不入库主流程。邮件摘要与正文一并入库，PDF 附件流式落盘 NAS（不设单附件大小上限），失败按 `retry_count` 重试 3 次后转人工 | 阶段一 | 必选 |
| F3 | 附件归档与去重 | 附件落盘（`heart.attach.root`），计算 `SHA256` 去重；同名文件加时间戳，避免覆盖 | 阶段一 | 必选 |
| F4 | PDF 结构化解析 | 调用 AI 解析适配器，将 PO PDF 转为 JSON（PO 号、客户、贸易条款、目的港、柜型、产品明细行、数量、单价、金额等），返回**置信度**（实现见 7.8） | 阶段二 解析校验 | 必选 |
| F5 | 规则校验（V1.3） | 代码侧确定性校验：产品简称→13 位 `pdCode` 映射、目的港→对象 ID、箱数×单箱量=总量、行金额汇总=总额、包干费按报价表、毛利率重算；任一不符转人工复核 | 阶段二 | 必选 |
| F6 | 台账自动登记 | 以母本为模板，按 V1.3 规则插入明细行 + 小计行 + 全量汇总，保留公式体系；输出新副本 `HeartSR-...-更新至YYYYMMDD.xlsx`，**不修改母本** | 阶段三 台账与附件 | 必选 |
| F7 | 附件上传 | 通过 SFTP 上传至 `10.80.88.38:/data/UploadFiles/oafiles/heartorderfile`，按「PO号_时间戳_原名」命名，返回 `https://crmimage.cacch.com:50002/...` 并校验 HTTP 200 | 阶段三 | 必选 |
| F8 | CRM 查客户 | 按固定 `customerCode=S1212000018`（HEART CO., LTD.）查询客户，校验存在性与状态 | 阶段四 CRM 建单 | 必选 |
| F9 | NCC 客户余额（旁路） | 用友 NCC `V_QC_customerarye` 视图查客户余额；成功并入建单报文，失败降级告警、**不阻塞主链路** | 阶段四 | 可选 |
| F10 | CRM 取号 | 调用 `saveObjectData`（`object_70cY4__c`）生成 CAC 流水号（26CACCPOTHxxx）；**同一 PO 只取一次**，失败重试复用已取号 | 阶段四 | 必选 |
| F11 | CRM 港口/目的地解析 | 目的港按对象 `object_yEQGF__c` 转对象 ID；「目的地」字段 `field_RHVAG__c` 待确认后启用 | 阶段四 | 必选 |
| F12 | CRM 建单 | 组装 `contractAdd` 报文（V2 签名、`isTrigger=true` 触发审批流、附件字段 `field_8s0nt__c` 填字符串化 JSON 数组 `[{path,name}]`，`path` 为 crmimage URL），提交并回写合同号/ID | 阶段四 | 必选 |
| F13 | 状态机与重试 | 全流程状态落库，按步骤失败可重试；单条失败不阻塞同批其余 PO；超过最大重试次数转人工 | 阶段五 运维兜底 | 必选 |
| F14 | 告警与审计 | 建单成功/失败、人工复核、取号异常等通过企微 Webhook 告警；全链路 `traceId` 可查，三方 Client 记录完整入参/出参（脱敏） | 阶段五 | 必选 |
| F15 | 人工复核与补偿 | 低置信度/校验失败/重试耗尽生成复核任务；提供手动触发、重试、跳过接口 | 阶段五 | 必选 |
| F16 | 台账 CAC PO 回填 | 取号后回填台账 CAC PO 列（当前决策为人工补，本需求提供开关，默认关闭） | 阶段五 | 可选 |

> **实现顺序约定**：上表即为开发与验收顺序（F1 → F16），每阶段完成并验证后再进入下一阶段；F13~F15 为横切能力，在阶段一先落「状态落库 + 基础日志」最小版本，随阶段逐级增强，避免最后一轮补锅。


[↑ 返回顶部](#top)

### <a id="s3-2"></a>3.2 交互流程

```
定时任务(5min)
  ↓
拉取企微邮箱未读邮件 ──(非 Heart / 无附件)──→ 标记 SKIP
  ↓
附件落盘 + SHA256 去重 ──(重复)──→ 标记 DUPLICATE
  ↓
AI 解析 PDF → JSON(含置信度)
  ↓
规则校验 V1.3 ──(不通过/低置信度)──→ 人工复核任务 + 企微告警
  ↓
写台账新副本（插行 + 小计 + 汇总）
  ↓
SFTP 上传附件 → crmimage URL（HTTP 200 校验）
  ↓
CRM 查客户（customerCode=S1212000018）
  ↓
┌──────── 可选旁路：NCC 客户余额（F9，heart.ncc.enabled）────────┐
│ 用友 NCC POST /ncc/v1/uapws/rest/uapbd/dataquerymanage/query    │
│ （视图 V_QC_customerarye，按 customerCode 查客户余额等 ERP 字段） │
│   ├─ 成功 → 余额写入建单报文（及台账对应列，按需）                 │
│   └─ 失败/超时/token 失效 → WARN + 字段置空，**不阻断主流程**      │
└────────────────────────────────────────────────────────────────┘
  ↓
取号（saveObjectData / object_70cY4__c）→ 查港口/目的地对象 ID
  ↓
contractAdd 建单（isTrigger=true，报文含附件 URL 与余额字段）→ 审批流
  ↓
回写合同号/流水号 + 告警 + 归档
```

> **NCC 旁路说明**：客户余额是**补充字段**而非建单前置条件，且 NCC 接口当前卡在 token 未打通（见第七节）。因此设计为「主流程内的可选旁路」——成功后参与报文组装，失败仅降级告警、字段置空；token 打通后只需打开 `heart.ncc.enabled` 与配置凭证，**无需改动流程编排**。若业务确认余额为建单必填，则改为强依赖分支（失败即转 `MANUAL_REVIEW`）。

---

[↑ 返回顶部](#top)

## <a id="s4"></a>4. 业务流程

### <a id="s4-1"></a>4.1 PO 处理状态机

```
MAIL_RECEIVED
   → PARSED
   → VALIDATED
   → LEDGER_WRITTEN
   → FILE_UPLOADED
   → CUSTOMER_RESOLVED
   → NCC_BALANCE_FETCHED（可选旁路；失败不改变状态，仅记 WARN）
   → SERIAL_NUMBERED
   → PORT_RESOLVED
   → ORDER_CREATED
   → DONE
异常分支：
   → MANUAL_REVIEW（低置信度 / 校验失败 / 重试耗尽）
   → FAILED（不可重试的配置或凭证错误）
```

[↑ 返回顶部](#top)

### <a id="s4-2"></a>4.2 关键业务规则（V1.3，来自《HeartSR自动填写逻辑设计.md》）

1. CAC PO 列留空人工补（F16 开关默认关闭）；
2. 船公司写英文原文（如 `NAMSUNG`），不翻译；
3. 目的港大写英文、去括号道县名（`SHIBUSHI`）；
4. 中信保/结算单价等公式以工作簿 CN27 区现有公式为准（中信保 = `(X-Z*F*0.45)*0.315%*0.65`）；
5. 包干费按简化报价表取数（上海小柜 4900/5000、上海大柜 6800/6900、太仓小柜 3900、太仓大柜 4700），不分主偏港、不分货代；
6. 每个 PO 块生成小计行，计算列全量汇总，毛利率按汇总值重算；
7. 辅助列（H）只保留数字与符号（`40 cartons × 2 pallets` → `40 × 2`），备注列保留原文；
8. 输出新副本，不改原文件。

[↑ 返回顶部](#top)

### <a id="s4-3"></a>4.3 幂等与并发约束

| 场景 | 幂等键 | 处理 |
|------|--------|------|
| 邮件重复拉取 | `mail_uid` / `message_id` | 已存在直接跳过 |
| 同一 PO 重复邮件 | `po_no` + 附件 `sha256` | 已建单则跳过并告警 |
| 取号重试 | `po_id` | 复用库中已存 `serial_no`，不重复取号 |
| 建单重试 | `po_id` | 已存在 `crm_contract_id` 则不再提交 |
| 台账写入 | `po_id` | 已写入则跳过 |

---

[↑ 返回顶部](#top)

## <a id="s5"></a>5. 涉及系统与模块

| 系统 | 模块/接口 | 说明 |
|------|-----------|------|
| 企微邮箱（腾讯企业邮） | IMAP `imap.exmail.qq.com:993`（待确认） | 拉取未读邮件与 PDF 附件 |
| 文件服务 | SFTP `10.80.88.38` + `crmimage` HTTP | 附件上传与可访问 URL |
| 勤策 CRM | `queryCustomer` / `saveObjectData`(object_70cY4__c) / 港口查询 / `contractAdd` | V2 签名，生产写操作 |
| 用友 NCC | `V_QC_customerarye` 查询 | 客户余额（可选，待 token） |
| 企微机器人 | Webhook | 成功/失败/复核告警 |
| AI 解析服务 | PDF→JSON | 可插拔适配器，支持降级为人工 |

---

[↑ 返回顶部](#top)

## <a id="s6"></a>6. 字段映射

### <a id="s6-1"></a>6.1 PO PDF → 台账关键列

| 来源（PDF 字段） | 台账列 | 转换规则 |
|------------------|--------|----------|
| PO No. | PO 号列 | 原文 |
| Customer | 客户 | 固定 HEART CO., LTD. |
| Port of Discharge | 目的港 | 大写英文、去括号道县名 |
| Shipping Line | 船公司 | 英文原文 |
| Container Type | 柜型 | 小柜/大柜 |
| Product / Qty / Unit Price | 明细行 | 产品简称→`pdCode` 映射 |
| Amount | 金额 | 数量 × 单价，校验与 PO 总额一致 |
| — | CAC PO | 取号流水号（F16 开关，默认空） |
| — | 小计行 | 每 PO 块生成，计算列汇总 |

[↑ 返回顶部](#top)

### <a id="s6-2"></a>6.2 产品编码映射（HeartSR 简称 → CRM）

| 简称 | pdCode | 产品 ID |
|------|--------|---------|
| GA500 | 5104020060002 | 7330516969341456853 |
| GA1L | 5104020060011 | 4833261628867502350 |
| GA5L | 5104020060012 | 7166866426435759276 |
| HWB500 | 5104020130006 | 8149382311877000439 |
| HWB1L | 5104020130005 | 6260739979620738781 |
| HWB5L | 5104020130007 | 5099883925488335461 |

[↑ 返回顶部](#top)

### <a id="s6-3"></a>6.3 CRM 固定字段

| 字段 | 取值 | 说明 |
|------|------|------|
| `customerCode` | `S1212000018` | HEART CO., LTD. |
| `owner` | `6291185993388435139` | 固定负责人 |
| `triggerEmployee` | `6288291750959183118` | 固定触发人（注意拼写，非 `tiggerEmployee`） |
| 港口对象 `object_yEQGF__c` | SHIBUSHI=`6864166600199432698`、TOKYO=`6961607334618785258`、SHANGHAI=`5801559508238571560` | 传对象 ID，非文本 |
| 报关口岸 | 上海 ID `5801559508238571560` | 太仓港主数据缺失，暂代 |
| 目的地 `field_RHVAG__c` | **待确认** | 引用的对象未知，暂省略 |
| 附件字段 | `field_8s0nt__c` | 填字符串化 JSON 数组 `[{"path":"crmimage URL","name":"文件名"}]` |
| BOM 版本 `field_6mBjw__c` | **不传** | 传值报「字段 id 值不存在」 |
| `isTrigger` | `true` | 触发审批流 |

---

[↑ 返回顶部](#top)

## <a id="s7"></a>7. 技术方案

### <a id="s7-1"></a>7.1 架构影响

新增业务域 **`heart`**，按模块分层落位（遵循单向依赖）：

```
web        controller.heart / dto.heart / convert.heart / config.heart
async      async.heart.task.HeartMailPollingTask
manager    manager.heart.api.IHeartOrderFlowManager + impl（状态机与事务入口）
service    service.heart.api.IHeartLedgerService + impl（POI 台账写入、规则校验）
integration  integration.heart.client.MailClient（IMAP）
             integration.heart.client.HeartSftpUploadClient（附件上传）
             integration.heart.client.AiPoParseClient（AI 解析）
             integration.heart.adapter.HeartPoParseAdapter（AI 结果 → 领域对象）
             integration.crm.client.CrmClient（扩展：查客户 / 取号 / 查港口 / contractAdd）
             integration.ncc.client.NccQueryClient（客户余额查询，可选旁路）
dao        entity.heart / mapper.heart
common     common.config.heart.HeartProperties / common.constant.heart.*
```

AI 边界：`AiPoParseClient` 只接收 PDF 字节并返回 JSON + 置信度，禁止在其内做金额计算、编码转换、接口调用；解析失败或置信度低于阈值（默认 0.9）直接转 `MANUAL_REVIEW`。

[↑ 返回顶部](#top)

### <a id="s7-2"></a>7.2 数据库变更

新增 Flyway 脚本 `V47` ~ `V52`（当前最新为 V46），均含标准字段 `id/created_at/updated_at/is_deleted`，主键雪花算法。

| 表名 | 核心列 |
|------|--------|
| `t_integration_heart_mail` | `mail_key`（去重键：Message-ID，缺失时 `UID:{uid}`）、`mail_uid`、`message_id`、`subject`、`from_address`、`received_at`、`body_text`、`body_html`、`attach_files`（JSONB，元素为 name/sha256/path/size）、`process_status`、`fail_reason`、`retry_count` |
| `t_integration_crm_heart_po` | PO 级阶段数据：`po_no`、`cac_po_no`、`customer_code`、`destination_country`、`port_of_loading`、`port_of_discharge`、`port_of_loading_id`、`port_of_discharge_id`、`shipping_line`、`container_type`、`packing_type`、`etd`；汇总列 `total_cartons`/`total_liters`/`total_volume`/`total_amount`/`total_cost`/`total_gross_profit`/`gross_margin_rate`；`mail_id`、`attachment_url`、`parse_confidence`、`parse_payload(JSONB)`、`process_status`、`crm_payload(JSONB)`、`submit_count`、`fail_reason`、`retry_count`、`version` |
| `t_integration_crm_heart_po_item` | 明细行（对应台账一产品一行）：`po_id`、`line_no`、`product_name`、`spec`、`pd_code`、`product_id`、`carton_count`、`liters`、`unit_price`、`amount`、`aux_text`、`density`、`net_weight`、`net_weight_with_carton`、`volume`、`carton_gross_weight`、`gross_weight`、`empty_carton_sets`、`gross_weight_with_extra_carton`、`cost_price`、`no_pallet_unit_price`、`package_fee`、`credit_insurance_fee`、`other_fee`、`cost_total`、`gross_profit`、`gross_margin_rate`、`remark`、`data_source` |
| `t_integration_heart_ledger_record` | `po_id`、`ledger_file_path`、`sheet_name`、`start_row`、`end_row`、`subtotal_row`、`write_status` |
| `t_integration_crm_heart_order_submit` | 提交与溯源（每次提交一条）：`po_id`、`attempt_no`、`submit_status`、`crm_contract_id`、`crm_contract_no`、`crm_serial_no`、`return_code`、`return_msg`、`http_status`、`error_message`、`request_payload(JSONB)`、`response_payload(JSONB)`、`duration_ms`、`trace_id`、`submitted_at` |
| `t_integration_heart_review_task` | `po_id`、`reason`、`confidence`、`status`、`handler`、`handled_at` |

`V47` 已落地 `t_integration_crm_heart_po` / `t_integration_crm_heart_po_item` / `t_integration_crm_heart_order_submit` 三张表：

- 主表与明细表列结构对齐台账「5. SR」（PO、CAC PO、产品名称、规格、箱数、L、单价、辅助、目的国、启运港、目的港、船公司、大小柜、备注、ETD、打托方式、密度、净重…USD考核价），台账小计行对应主表汇总列；
- 阶段演进只更新主表 `process_status`（对应 4.1 状态机），明细行按 `(po_id, line_no)` 幂等写入；
- 提交 CRM 前把最终报文写入主表 `crm_payload` 作为快照；每次实际调用在 `t_integration_crm_heart_order_submit` 追加一条记录（请求报文、响应报文、`return_code`/`return_msg`、合同号、耗时、`trace_id`），实现逐次溯源。

`V48` 调整主表港口列（F11 港口解析）：

- 新增 `port_of_loading_id` / `port_of_discharge_id`，分别承载启运港、目的港在 `object_yEQGF__c` 中的对象 ID；
- 删除原单一列 `port_object_id`（无法同时承载启运港与目的港）；
- 港口查询关键字取自名称列 `port_of_loading` / `port_of_discharge`（当前人工维护，后续由 AI 解析结果写入）；两列均已有对象 ID 时跳过查询，保证重试幂等。

[↑ 返回顶部](#top)

### <a id="s7-3"></a>7.3 配置变更

```yaml
heart:
  mail:
    host: imap.exmail.qq.com
    port: 993
    ssl: true
    username: ${HEART_MAIL_USER}
    password: ${HEART_MAIL_PASSWORD}   # 授权码，禁止硬编码
    from-whitelist: [ ycho@heart-jp.com ]
    poll-cron: "0 */5 * * * ?"
    max-per-round: 20
  attach:
    root: ${HEART_ATTACH_ROOT}
  sftp:
    host: 10.80.88.38
    port: 22
    username: ${HEART_SFTP_USER}
    password: ${HEART_SFTP_PASSWORD}
    dir: /data/UploadFiles/oafiles/heartorderfile
    url-prefix: https://crmimage.cacch.com:50002/oafiles/heartorderfile
  ledger:
    template-path: ${HEART_LEDGER_TEMPLATE}
    output-dir: ${HEART_LEDGER_OUTPUT}
  ai:
    endpoint: ${HEART_AI_ENDPOINT}
    model-name: ${HEART_AI_MODEL}
    prompt-version: v1
    confidence-threshold: 0.9
    timeout-ms: 60000
    max-retry: 2
    render-dpi: 200
  crm:
    customer-code: S1212000018
    owner: "6291185993388435139"
    trigger-employee: "6288291750959183118"
    is-trigger: true
  ncc:
    enabled: false                    # F9 开关，token 打通后置为 true
    base-url: http://10.80.86.94
    query-path: /ncc/v1/uapws/rest/uapbd/dataquerymanage/query
    view: V_QC_customerarye
    token: ${NCC_TOKEN}               # 取 token 方式待确认（Q7）
    timeout-ms: 3000                  # 超时即降级，不阻断建单
  auto-run: true          # 全链路总开关（灰度用）
  backfill-cac-po: false  # F16
```

[↑ 返回顶部](#top)

### <a id="s7-4"></a>7.4 依赖变更（需父 POM 审批）

| 依赖 | 用途 | 说明 |
|------|------|------|
| `spring-boot-starter-mail`（`jakarta.mail`） | IMAP 收信 | 父 POM 未声明 |
| `org.apache.poi:poi-ooxml` | 台账读写、插行、公式 | 父 POM 未声明 |
| `org.apache.pdfbox:pdfbox` | PDF 文本/渲染（AI 前置或降级） | 父 POM 未声明 |
| SFTP 客户端（`com.hierynomus:sshj` 或 `jsch`） | 附件上传 | 父 POM 未声明（现有 `smbj` 为 SMB，不适用） |

[↑ 返回顶部](#top)

### <a id="s7-5"></a>7.5 涉及代码

| 模块 | 新增/修改文件 | 说明 |
|------|--------------|------|
| common | `common/config/heart/HeartProperties.java`、`common/constant/heart/HeartPathConstants.java`、`HeartConstants.java`、`HeartLogConstants.java` | 配置与常量（Path/业务/日志三类隔离） |
| integration | `integration/heart/client/MailClient.java` | IMAP 收信、附件下载 |
| integration | `integration/heart/client/HeartSftpUploadClient.java` | SFTP 上传 + URL 拼接 + 200 校验 |
| integration | `integration/heart/client/AiPoParseClient.java` | AI 解析调用 |
| integration | `integration/heart/adapter/HeartPoParseAdapter.java` | AI JSON → 领域对象 + 置信度 |
| integration | `integration/crm/client/CrmClient.java`（修改） | 新增 `queryCustomer`、`saveSerialNumber`、`queryPort`、`contractAdd` |
| integration | `integration/ncc/client/NccQueryClient.java` | NCC 客户余额查询（可选旁路） |
| dao | `entity/heart/*`、`mapper/heart/*` | 6 张表实体与 Mapper |
| service | `service/heart/api/IHeartLedgerService.java` + `impl/HeartLedgerServiceImpl.java` | POI 台账写入、V1.3 校验 |
| manager | `manager/heart/api/IHeartOrderFlowManager.java` + `impl/HeartOrderFlowManagerImpl.java` | 状态机编排、事务入口 |
| async | `async/heart/task/HeartMailPollingTask.java` | 5 分钟轮询 |
| web | `controller/heart/HeartOrderController.java`、`dto/heart/*`、`convert/heart/*`、`config/heart/HeartConfiguration.java` | 手动触发/查询/重试 |
| web | `controller/heart/HeartOrderOpenApiController.java`、`dto/heart/vo/HeartOrderSubmitVO.java` | ESB 对外提交接口（当前空实现，无入参，参数全部取 `heart.*` 配置）：`POST /api/v1/heart/orders/submit` |
| web | `db/migration/V47~V52__heart_*.sql` | 建表脚本 |
| common | `common/constant/heart/HeartSerialNoConstants.java`、`HeartSerialNoLogConstants.java`、`HeartOrderConstants.java`、`HeartOrderLogConstants.java`、`HeartPoProcessStatusConstants.java`、`common/dto/heart/HeartOrderSubmitResult.java` | F10 取号固定参数、订单提交状态、PO 处理状态、日志域常量与提交流程结果 DTO |
| dao | `entity/heart/HeartPoDO.java`、`mapper/heart/HeartPoMapper.java` | PO 阶段主表实体与 Mapper（`po_no` 为幂等键，`cac_po_no`/`process_status` 为取号回写与状态推进载体） |
| service | `service/heart/api/IHeartSerialNoService.java` + `impl/HeartSerialNoServiceImpl.java` | F10 单 PO 取号：固定参数、同 PO 只取一次、并发复用；成功后回写 `cac_po_no` 并推进 `process_status=SERIAL_NUMBERED` |
| manager | `manager/heart/api/IHeartOrderFlowManager.java` + `impl/HeartOrderFlowManagerImpl.java` | 流程推进入口：取待处理 PO（`CUSTOMER_RESOLVED` 未取号，或 `SERIAL_NUMBERED` 待港口解析）→ 取号 → 港口解析（成功推进 `PORT_RESOLVED`）；后续阶段在此扩展 |
| integration | `integration/crm/client/CrmCustomObjectClient.java`（复用，未修改） | F10/F11 直接复用 `saveObjectData` / `queryObjectData`，不新增 CRM Client |
| service | `service/heart/api/IHeartPortService.java` + `impl/HeartPortServiceImpl.java` | F11 港口解析：按 PO 港口名称查询 `object_yEQGF__c`，精确匹配唯一记录后回写对象 ID；两侧均已有值则跳过 |
| common | `common/constant/heart/HeartPortConstants.java`、`HeartPortLogConstants.java` | F11 港口查询固定参数与日志域常量 |
| web | `controller/heart/HeartOrderOpenApiController.java`（修改） | `submit` 仅触发流程推进并转换 VO，返回 `SUCCESS` / `NO_DATA` |

[↑ 返回顶部](#top)

### <a id="s7-6"></a>7.6 异常、日志与安全

- 每条 PO 独立 try-catch，失败记录 `current_step` 与 `last_error`，不阻塞同批；
- 逻辑提前终止（跳过/重复/低置信度/重试耗尽）必须打 INFO 并说明原因与 `poNo`；
- 三方 Client 入参/出参统一走 `ThirdPartyHttpLogSupport` 脱敏（appkey、SFTP 密码、邮箱授权码）；
- 日志前缀统一 `【Heart】`；
- 凭证全部走配置/环境变量，禁止入库与写日志；
- 生产写操作（取号、建单）加分布式锁（`integration:heart:po:{poNo}`）防并发重复。

[↑ 返回顶部](#top)

### <a id="s7-7"></a>7.7 性能与容量

- 单 PO 处理目标 ≤ 60s（解析 + 台账 + 上传 + 建单）；
- 台账母本 1301 行、38 列，写入前需删除 100 列后的空列（规避 16384 列网格上限），写入后保存为副本；
- 邮箱轮询单轮 ≤ 20 封，避免阻塞调度线程。

[↑ 返回顶部](#top)

### <a id="s7-8"></a>7.8 AI 解析适配器实现

**定位**：`integration` 层的可插拔解析组件，只做「PDF → 结构化字段」，**不做金额计算、不写库、不调业务接口**；计算与规则判定全部由 Service 层（F5）确定性代码完成。

**组件结构**

| 组件 | 位置 | 职责 |
|------|------|------|
| `IHeartPoParser` | `integration.heart.client` | 解析能力接口（策略模式，可替换） |
| `AiPoParseClient` | `integration.heart.client` | 主实现：PDF 渲染为图片 → 调用模型（OpenAI 兼容 `/v1/chat/completions` 或内网推理服务），含超时与重试 |
| `PdfTextPoParser` | `integration.heart.client` | 兜底实现：PDFBox 提取文本 + 固定模板正则（版式稳定时可用） |
| `HeartPoParseAdapter` | `integration.heart.adapter` | 协议转换：PDF 预处理、Schema 校验、DTO 映射、置信度汇总 |
| `HeartPoParseResult` | `integration.heart.client.dto` | 解析结果 DTO（`confidence` / `warnings` / `promptVersion`） |

**处理流程**

1. **预处理**：PDFBox 按 200dpi 渲染 PNG（兼容扫描件/加密件）；>10 页按页分批，避免超出上下文；
2. **调用模型**：system prompt 固化 JSON Schema + few-shot（`CN27-051SHB` 样例），`temperature=0`，明确「只抄录原文、禁止计算」；
3. **结构化校验**：Jackson 反序列化 + JSON Schema 校验（必填、类型、枚举）；
4. **交叉校验**：明细金额合计 = PO 总额、数量 × 单价 = 行金额、箱数 × 单箱量 = 总数量；
5. **结果输出**：`parse_payload` 落库 + `parse_confidence`；低置信度或校验失败转 `MANUAL_REVIEW`。

**输出契约（Schema 摘要）**

```json
{
  "poNo": "CN27-051SHB",
  "customerName": "HEART CO., LTD.",
  "tradeTerm": "FOB",
  "portOfDischarge": "SHIBUSHI",
  "shippingLine": "NAMSUNG",
  "containerType": "40HQ",
  "currency": "USD",
  "totalAmount": 149016.00,
  "items": [
    { "lineNo": 1, "productName": "GLYPHOSATE 500ML", "shortName": "GA500",
      "quantity": "560 CARTONS", "unitPrice": "266.10", "amount": "149016.00",
      "cartons": "560", "volume": "8976", "auxText": "40 × 14", "remark": "" }
  ],
  "confidence": 0.97,
  "warnings": []
}
```

**代码骨架**

```java
/** PO PDF 解析能力接口（策略） */
public interface IHeartPoParser {
    HeartPoParseResult parse(byte[] pdf, String fileName);
}

/** 适配器：预处理 + Schema 校验 + 映射，不含业务规则 */
@Component
@RequiredArgsConstructor
public class HeartPoParseAdapter {
    private final IHeartPoParser parser;
    private final HeartPoSchemaValidator schemaValidator;

    public HeartPoParseResult parseWithGuard(byte[] pdf, String fileName) {
        HeartPoParseResult raw = parser.parse(pdf, fileName);
        schemaValidator.validate(raw);      // 仅结构与类型校验
        return raw;                          // V1.3 业务校验由 Service 层 F5 负责
    }
}
```

**置信度与路由**

| 条件 | 路由 |
|------|------|
| `confidence ≥ 0.9` 且 Schema / 交叉校验通过 | 自动进入 F5 规则校验 |
| `0.7 ≤ confidence < 0.9` | 人工复核（标注低置信字段） |
| `confidence < 0.7` 或 Schema 失败 | 降级 `PdfTextPoParser`，仍失败转人工 |
| 模型超时 / 5xx | 退避重试 2 次（2s、5s），仍失败降级 |

**缓存与成本**

- 以 PDF `SHA256` 为键缓存解析结果（Caffeine 本地 7 天 + `parse_payload` 持久化），重复投递不重复计费；
- 记录 `promptVersion`、`modelName`、`promptTokens/completionTokens`、`durationMs`，用于效果回溯与成本核算。

**安全与合规**

- PDF 不出内网：优先内网推理服务；使用公网模型须经脱敏与合规审批；
- 日志只输出 `sha256`、页数、耗时、置信度，不输出 PDF 全文与客户敏感信息；
- Prompt 与 Schema 纳入版本管理（`heart.ai.prompt-version`），变更须回归样例集。

**测试与验收**

- 离线样例集：历史 10 份 PO（含扫描件、多页、手写批注），字段级准确率 ≥ 99%，**金额/编码字段 100%**；
- 单元测试覆盖：Schema 校验、交叉校验、降级路径、缓存命中；
- 集成测试：Mock 模型服务，验证超时 / 5xx / 空响应三条异常链路。


---

[↑ 返回顶部](#top)

## <a id="s8"></a>8. 验收标准

| 序号 | 验收点 | 验收方式 | 通过标准 |
|------|--------|----------|----------|
| 1 | 邮件拉取 | 自动 | 新 PO 邮件 5 分钟内入库，重复邮件不重复处理 |
| 2 | PDF 解析 | 自动 | 样例 `CN27-051SHB` 解析出 6 行明细，字段完整 |
| 3 | 规则校验 | 自动 | 560 箱 / 8976L / ¥149,016 / 包干费 3900 / 毛利率 3.16% 全部一致 |
| 4 | 台账写入 | 自动 | 生成新副本，母本未被修改，公式与小计行正确 |
| 5 | 附件上传 | 自动 | 返回 crmimage URL 且 HTTP 200 可下载 |
| 6 | 取号 | 自动 | 生成 26CACCPOTHxxx，重试不重复取号 |
| 7 | 建单 | 自动 | `contractAdd` 返回成功，CRM 可见订单并进入审批流 |
| 8 | 幂等 | 自动 | 同一 PO 重复投递不产生第二张订单 |
| 9 | 异常隔离 | 自动 | 单条解析/建单失败不影响同批其余 PO，生成复核任务并告警 |
| 10 | 可追溯 | 手动 | 通过 `poNo` 可查完整状态流转与三方入参/出参日志 |

---

[↑ 返回顶部](#top)

## <a id="s9"></a>9. 风险与依赖

| 风险/依赖 | 影响 | 应对措施 |
|-----------|------|----------|
| 生产 3 张测试单未清理（0316/0317/0318，其中 0317 已触发真实审批） | 被误当真实订单履约 | **上线前 P0 清理**，0317 先处理审批再作废 |
| 「目的地」字段 `field_RHVAG__c` 对象未知 | 字段长期空缺，若变必填则建单失败 | 尽快向勤策确认；未确认前不传该字段 |
| 太仓港主数据缺失 | 报关口岸用上海 ID 代，费用口径偏差 | 主数据补齐后切回太仓 ID |
| 企微邮箱 IMAP 授权/频率限制 | 拉取失败、延迟 | 复用连接、失败退避、告警；必要时改企业邮 OpenAPI |
| AI 解析幻觉导致金额/编码错误 | 错单 | 代码侧强校验（F5）+ 置信度阈值 + 人工复核兜底 |
| 台账母本公式变更 | 写入错位 | 母本只读、版本号管理；模板路径配置化 |
| NCC token 未打通 | 客户余额缺失 | 不阻塞主链路，F9 独立排期 |
| 新增 4 类依赖 | 构建与安全审计 | 父 POM 统一声明版本，走依赖审批 |
| 全自动建单无人工确认 | 错误直接进审批 | 保留 `heart.auto-run` 总开关与单 PO 人工复核；首周灰度观察 |

---

[↑ 返回顶部](#top)

## <a id="s10"></a>10. 待确认事项

| 序号 | 事项 | 影响 |
|------|------|------|
| Q1 | 企微邮箱 IMAP 账号、授权码、是否开启 IMAP 服务 | 阻塞 F1 |
| Q2 | 「目的地」`field_RHVAG__c` 引用的对象 | 阻塞 F11 完整字段 |
| Q3 | 报关口岸是否可在本期补太仓港主数据 | 影响 F12 口径 |
| Q4 | AI 解析服务选型与内网可达性（自建/大模型 API） | 阻塞 F4 |
| Q5 | 台账母本存放位置与只读权限（微盘/共享盘） | 阻塞 F6 |
| Q6 | 3 张测试单的清理责任人与时间 | 阻塞上线 |
| Q7 | NCC 取 token 方式（账号/接口文档）及余额字段是否必填 | 影响 F9 是否升级为强依赖 |

---

[↑ 返回顶部](#top)

## <a id="s11"></a>11. 变更记录

| 日期 | 修改人 | 修改内容 |
|------|--------|----------|
| 2026-11-24 | TraeCode | 创建文档（依据《项目交接总结-Heart订单自动化与CRM对接.md》与三方决策） |
| 2026-11-24 | TraeCode | 补充 3.2 中 NCC 客户余额可选旁路流程、状态机 `NCC_BALANCE_FETCHED` 节点、NCC 客户端与配置、待确认 Q7 |
| 2026-11-24 | TraeCode | 3.1 功能点按执行顺序重排（NCC 调整为 F9），新增实现阶段列；新增 7.8 AI 解析适配器实现 |
| 2026-11-24 | TraeCode | 新增文档目录（含最末一级标题）、章节内嵌锚点与「返回顶部」链接 |
| 2026-11-25 | TraeCode | 新增 V47 建表脚本：PO 阶段主表/明细表/CRM 提交记录表；7.2 同步表结构说明 |
| 2026-11-25 | TraeCode | 完成 F10 CAC PO 取号：新增 `IHeartSerialNoService`/`HeartSerialNoServiceImpl`、`HeartPoDO`/`HeartPoMapper`、取号参数与日志域常量；取号业务由 `IHeartOrderFlowManager` 编排，ESB 提交接口 `POST /api/v1/heart/orders/submit` 仅转发；7.5 同步 |
| 2026-11-25 | TraeCode | 修正 F10 取号时序：明确 PO 记录先落库后取号，`submitOrder` 改为按状态机取 `CUSTOMER_RESOLVED` 的 PO，取号成功后回写 `cac_po_no` 并推进 `process_status=SERIAL_NUMBERED`、累加 `submit_count`；新增 `HeartPoProcessStatusConstants` |
| 2026-11-25 | TraeCode | 完成 F11 港口解析：新增 `V48` 主表港口列调整（新增 `port_of_loading_id`/`port_of_discharge_id`、删除 `port_object_id`）、`IHeartPortService`/`HeartPortServiceImpl` 与港口常量；取号后按港口名称查询 `object_yEQGF__c` 并回写对象 ID，两阶段幂等（港口解析失败时保持 `SERIAL_NUMBERED`，下次提交只重试本阶段） |
| 2026-11-25 | TraeCode | 状态机新增 `PORT_RESOLVED`：`SERIAL_NUMBERED` 语义收敛为「已取号待港口解析」，港口 ID 回写成功后推进 `PORT_RESOLVED`，后续建单阶段按该状态捞取；4.1 状态机、7.5 同步 |

[↑ 返回顶部](#top)
