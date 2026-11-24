# REQ-HEART-001 Heart 客户 PO 邮件自动化与 CRM 建单

---

## 1. 基本信息

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

## 2. 需求背景

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

## 3. 需求描述

### 3.0 总体原则（已确认）

| 决策项 | 结论 |
|--------|------|
| 实现方式 | **以本平台代码为主干**；AI 仅承担「PDF → 结构化字段」解析，不参与事务编排、金额计算与接口调用 |
| 邮件来源 | **企微邮箱**（腾讯企业邮），定时轮询指定邮箱 |
| 自动化程度 | **允许全自动**（解析通过即自动建单并触发审批流），保留人工复核兜底与全链路开关 |
| 台账载体 | **维持 Excel 母本 + 输出新副本**，不改为企微智能表格 |
| 附件地址 | CRM 可达的 `crmimage` 文件服务 URL，不使用操作员 PC 内网地址 |

### 3.1 功能点

| 序号 | 功能点 | 描述 | 必选/可选 |
|------|--------|------|-----------|
| F1 | 邮箱定时轮询 | 每 5 分钟通过 IMAP 拉取指定邮箱未读邮件；按 `UID/Message-ID` 去重，断点续拉；单轮限流（默认 20 封） | 必选 |
| F2 | 邮件识别与附件提取 | 发件人白名单（`ycho@heart-jp.com`）且主题/正文含 PO 标识时，提取 PDF 附件；非 Heart 邮件记 `SKIP` 不入库主流程 | 必选 |
| F3 | 附件归档与去重 | 附件落盘（`heart.attach.root`），计算 `SHA256` 去重；同名文件加时间戳，避免覆盖 | 必选 |
| F4 | PDF 结构化解析 | 调用 AI 解析适配器，将 PO PDF 转为 JSON（PO 号、客户、贸易条款、目的港、柜型、产品明细行、数量、单价、金额等），返回**置信度** | 必选 |
| F5 | 规则校验（V1.3） | 代码侧确定性校验：产品简称→13 位 `pdCode` 映射、目的港→对象 ID、箱数×单箱量=总量、行金额汇总=总额、包干费按报价表、毛利率重算；任一不符转人工复核 | 必选 |
| F6 | 台账自动登记 | 以母本为模板，按 V1.3 规则插入明细行 + 小计行 + 全量汇总，保留公式体系；输出新副本 `HeartSR-...-更新至YYYYMMDD.xlsx`，**不修改母本** | 必选 |
| F7 | 附件上传 | 通过 SFTP 上传至 `10.80.88.38:/data/UploadFiles/oafiles/heartorderfile`，按「PO号_时间戳_原名」命名，返回 `https://crmimage.cacch.com:50002/...` 并校验 HTTP 200 | 必选 |
| F8 | CRM 查客户 | 按固定 `customerCode=S1212000018`（HEART CO., LTD.）查询客户，校验存在性与状态 | 必选 |
| F9 | CRM 取号 | 调用 `saveObjectData`（`object_70cY4__c`）生成 CAC 流水号（26CACCPOTHxxx）；**同一 PO 只取一次**，失败重试复用已取号 | 必选 |
| F10 | CRM 港口/目的地解析 | 目的港按对象 `object_yEQGF__c` 转对象 ID；「目的地」字段 `field_RHVAG__c` 待确认后启用 | 必选 |
| F11 | CRM 建单 | 组装 `contractAdd` 报文（V2 签名、`isTrigger=true` 触发审批流、附件字段 `field_8s0nt__c` 填 crmimage URL），提交并回写合同号/ID | 必选 |
| F12 | 状态机与重试 | 全流程状态落库，按步骤失败可重试；单条失败不阻塞同批其余 PO；超过最大重试次数转人工 | 必选 |
| F13 | 告警与审计 | 建单成功/失败、人工复核、取号异常等通过企微 Webhook 告警；全链路 `traceId` 可查，三方 Client 记录完整入参/出参（脱敏） | 必选 |
| F14 | 人工复核与补偿 | 低置信度/校验失败/重试耗尽生成复核任务；提供手动触发、重试、跳过接口 | 必选 |
| F15 | 台账 CAC PO 回填 | 取号后回填台账 CAC PO 列（当前决策为人工补，本需求提供开关，默认关闭） | 可选 |
| F16 | NCC 客户余额 | 打通用友 NCC `V_QC_customerarye` 视图，补「客户余额」字段；**不阻塞主链路** | 可选 |

### 3.2 交互流程

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
┌──────── 可选旁路：NCC 客户余额（F16，heart.ncc.enabled）────────┐
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

## 4. 业务流程

### 4.1 PO 处理状态机

```
MAIL_RECEIVED
   → PARSED
   → VALIDATED
   → LEDGER_WRITTEN
   → FILE_UPLOADED
   → CUSTOMER_RESOLVED
   → NCC_BALANCE_FETCHED（可选旁路；失败不改变状态，仅记 WARN）
   → SERIAL_NUMBERED
   → ORDER_CREATED
   → DONE
异常分支：
   → MANUAL_REVIEW（低置信度 / 校验失败 / 重试耗尽）
   → FAILED（不可重试的配置或凭证错误）
```

### 4.2 关键业务规则（V1.3，来自《HeartSR自动填写逻辑设计.md》）

1. CAC PO 列留空人工补（F15 开关默认关闭）；
2. 船公司写英文原文（如 `NAMSUNG`），不翻译；
3. 目的港大写英文、去括号道县名（`SHIBUSHI`）；
4. 中信保/结算单价等公式以工作簿 CN27 区现有公式为准（中信保 = `(X-Z*F*0.45)*0.315%*0.65`）；
5. 包干费按简化报价表取数（上海小柜 4900/5000、上海大柜 6800/6900、太仓小柜 3900、太仓大柜 4700），不分主偏港、不分货代；
6. 每个 PO 块生成小计行，计算列全量汇总，毛利率按汇总值重算；
7. 辅助列（H）只保留数字与符号（`40 cartons × 2 pallets` → `40 × 2`），备注列保留原文；
8. 输出新副本，不改原文件。

### 4.3 幂等与并发约束

| 场景 | 幂等键 | 处理 |
|------|--------|------|
| 邮件重复拉取 | `mail_uid` / `message_id` | 已存在直接跳过 |
| 同一 PO 重复邮件 | `po_no` + 附件 `sha256` | 已建单则跳过并告警 |
| 取号重试 | `po_id` | 复用库中已存 `serial_no`，不重复取号 |
| 建单重试 | `po_id` | 已存在 `crm_contract_id` 则不再提交 |
| 台账写入 | `po_id` | 已写入则跳过 |

---

## 5. 涉及系统与模块

| 系统 | 模块/接口 | 说明 |
|------|-----------|------|
| 企微邮箱（腾讯企业邮） | IMAP `imap.exmail.qq.com:993`（待确认） | 拉取未读邮件与 PDF 附件 |
| 文件服务 | SFTP `10.80.88.38` + `crmimage` HTTP | 附件上传与可访问 URL |
| 勤策 CRM | `queryCustomer` / `saveObjectData`(object_70cY4__c) / 港口查询 / `contractAdd` | V2 签名，生产写操作 |
| 用友 NCC | `V_QC_customerarye` 查询 | 客户余额（可选，待 token） |
| 企微机器人 | Webhook | 成功/失败/复核告警 |
| AI 解析服务 | PDF→JSON | 可插拔适配器，支持降级为人工 |

---

## 6. 字段映射

### 6.1 PO PDF → 台账关键列

| 来源（PDF 字段） | 台账列 | 转换规则 |
|------------------|--------|----------|
| PO No. | PO 号列 | 原文 |
| Customer | 客户 | 固定 HEART CO., LTD. |
| Port of Discharge | 目的港 | 大写英文、去括号道县名 |
| Shipping Line | 船公司 | 英文原文 |
| Container Type | 柜型 | 小柜/大柜 |
| Product / Qty / Unit Price | 明细行 | 产品简称→`pdCode` 映射 |
| Amount | 金额 | 数量 × 单价，校验与 PO 总额一致 |
| — | CAC PO | 取号流水号（F15 开关，默认空） |
| — | 小计行 | 每 PO 块生成，计算列汇总 |

### 6.2 产品编码映射（HeartSR 简称 → CRM）

| 简称 | pdCode | 产品 ID |
|------|--------|---------|
| GA500 | 5104020060002 | 7330516969341456853 |
| GA1L | 5104020060011 | 4833261628867502350 |
| GA5L | 5104020060012 | 7166866426435759276 |
| HWB500 | 5104020130006 | 8149382311877000439 |
| HWB1L | 5104020130005 | 6260739979620738781 |
| HWB5L | 5104020130007 | 5099883925488335461 |

### 6.3 CRM 固定字段

| 字段 | 取值 | 说明 |
|------|------|------|
| `customerCode` | `S1212000018` | HEART CO., LTD. |
| `owner` | `6291185993388435139` | 固定负责人 |
| `triggerEmployee` | `6288291750959183118` | 固定触发人（注意拼写，非 `tiggerEmployee`） |
| 港口对象 `object_yEQGF__c` | SHIBUSHI=`6864166600199432698`、TOKYO=`6961607334618785258`、SHANGHAI=`5801559508238571560` | 传对象 ID，非文本 |
| 报关口岸 | 上海 ID `5801559508238571560` | 太仓港主数据缺失，暂代 |
| 目的地 `field_RHVAG__c` | **待确认** | 引用的对象未知，暂省略 |
| 附件字段 | `field_8s0nt__c` | 填 crmimage URL |
| BOM 版本 `field_6mBjw__c` | **不传** | 传值报「字段 id 值不存在」 |
| `isTrigger` | `true` | 触发审批流 |

---

## 7. 技术方案

### 7.1 架构影响

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

### 7.2 数据库变更

新增 Flyway 脚本 `V47` ~ `V51`（当前最新为 V46），均含标准字段 `id/created_at/updated_at/is_deleted`，主键雪花算法。

| 表名 | 核心列 |
|------|--------|
| `t_integration_heart_mail` | `mail_uid`、`message_id`、`subject`、`from_address`、`received_at`、`attach_file_name`、`attach_sha256`、`attach_path`、`process_status`、`fail_reason`、`retry_count` |
| `t_integration_heart_po` | `po_no`、`customer_code`、`trade_term`、`port_code`、`port_object_id`、`container_type`、`total_quantity`、`total_amount`、`currency`、`mail_id`、`parse_confidence`、`parse_payload(JSONB)`、`validate_status` |
| `t_integration_heart_po_item` | `po_id`、`line_no`、`product_short_name`、`pd_code`、`product_id`、`quantity`、`unit_price`、`amount`、`cartons`、`volume`、`aux_text`、`remark` |
| `t_integration_heart_ledger_record` | `po_id`、`ledger_file_path`、`sheet_name`、`start_row`、`end_row`、`subtotal_row`、`write_status` |
| `t_integration_heart_order_sync` | `po_id`、`serial_no`、`crm_contract_id`、`crm_contract_no`、`attach_url`、`sync_status`、`current_step`、`retry_count`、`last_error` |
| `t_integration_heart_review_task` | `po_id`、`reason`、`confidence`、`status`、`handler`、`handled_at` |

### 7.3 配置变更

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
    confidence-threshold: 0.9
  crm:
    customer-code: S1212000018
    owner: "6291185993388435139"
    trigger-employee: "6288291750959183118"
    is-trigger: true
  ncc:
    enabled: false                    # F16 开关，token 打通后置为 true
    base-url: http://10.80.86.94
    query-path: /ncc/v1/uapws/rest/uapbd/dataquerymanage/query
    view: V_QC_customerarye
    token: ${NCC_TOKEN}               # 取 token 方式待确认（Q7）
    timeout-ms: 3000                  # 超时即降级，不阻断建单
  auto-run: true          # 全链路总开关（灰度用）
  backfill-cac-po: false  # F15
```

### 7.4 依赖变更（需父 POM 审批）

| 依赖 | 用途 | 说明 |
|------|------|------|
| `spring-boot-starter-mail`（`jakarta.mail`） | IMAP 收信 | 父 POM 未声明 |
| `org.apache.poi:poi-ooxml` | 台账读写、插行、公式 | 父 POM 未声明 |
| `org.apache.pdfbox:pdfbox` | PDF 文本/渲染（AI 前置或降级） | 父 POM 未声明 |
| SFTP 客户端（`com.hierynomus:sshj` 或 `jsch`） | 附件上传 | 父 POM 未声明（现有 `smbj` 为 SMB，不适用） |

### 7.5 涉及代码

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
| web | `db/migration/V47~V51__heart_*.sql` | 建表脚本 |

### 7.6 异常、日志与安全

- 每条 PO 独立 try-catch，失败记录 `current_step` 与 `last_error`，不阻塞同批；
- 逻辑提前终止（跳过/重复/低置信度/重试耗尽）必须打 INFO 并说明原因与 `poNo`；
- 三方 Client 入参/出参统一走 `ThirdPartyHttpLogSupport` 脱敏（appkey、SFTP 密码、邮箱授权码）；
- 日志前缀统一 `【Heart】`；
- 凭证全部走配置/环境变量，禁止入库与写日志；
- 生产写操作（取号、建单）加分布式锁（`integration:heart:po:{poNo}`）防并发重复。

### 7.7 性能与容量

- 单 PO 处理目标 ≤ 60s（解析 + 台账 + 上传 + 建单）；
- 台账母本 1301 行、38 列，写入前需删除 100 列后的空列（规避 16384 列网格上限），写入后保存为副本；
- 邮箱轮询单轮 ≤ 20 封，避免阻塞调度线程。

---

## 8. 验收标准

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

## 9. 风险与依赖

| 风险/依赖 | 影响 | 应对措施 |
|-----------|------|----------|
| 生产 3 张测试单未清理（0316/0317/0318，其中 0317 已触发真实审批） | 被误当真实订单履约 | **上线前 P0 清理**，0317 先处理审批再作废 |
| 「目的地」字段 `field_RHVAG__c` 对象未知 | 字段长期空缺，若变必填则建单失败 | 尽快向勤策确认；未确认前不传该字段 |
| 太仓港主数据缺失 | 报关口岸用上海 ID 代，费用口径偏差 | 主数据补齐后切回太仓 ID |
| 企微邮箱 IMAP 授权/频率限制 | 拉取失败、延迟 | 复用连接、失败退避、告警；必要时改企业邮 OpenAPI |
| AI 解析幻觉导致金额/编码错误 | 错单 | 代码侧强校验（F5）+ 置信度阈值 + 人工复核兜底 |
| 台账母本公式变更 | 写入错位 | 母本只读、版本号管理；模板路径配置化 |
| NCC token 未打通 | 客户余额缺失 | 不阻塞主链路，F16 独立排期 |
| 新增 4 类依赖 | 构建与安全审计 | 父 POM 统一声明版本，走依赖审批 |
| 全自动建单无人工确认 | 错误直接进审批 | 保留 `heart.auto-run` 总开关与单 PO 人工复核；首周灰度观察 |

---

## 10. 待确认事项

| 序号 | 事项 | 影响 |
|------|------|------|
| Q1 | 企微邮箱 IMAP 账号、授权码、是否开启 IMAP 服务 | 阻塞 F1 |
| Q2 | 「目的地」`field_RHVAG__c` 引用的对象 | 阻塞 F10 完整字段 |
| Q3 | 报关口岸是否可在本期补太仓港主数据 | 影响 F11 口径 |
| Q4 | AI 解析服务选型与内网可达性（自建/大模型 API） | 阻塞 F4 |
| Q5 | 台账母本存放位置与只读权限（微盘/共享盘） | 阻塞 F6 |
| Q6 | 3 张测试单的清理责任人与时间 | 阻塞上线 |
| Q7 | NCC 取 token 方式（账号/接口文档）及余额字段是否必填 | 影响 F16 是否升级为强依赖 |

---

## 11. 变更记录

| 日期 | 修改人 | 修改内容 |
|------|--------|----------|
| 2026-11-24 | TraeCode | 创建文档（依据《项目交接总结-Heart订单自动化与CRM对接.md》与三方决策） |
| 2026-11-24 | TraeCode | 补充 3.2 中 NCC 客户余额可选旁路流程、状态机 `NCC_BALANCE_FETCHED` 节点、NCC 客户端与配置、待确认 Q7 |
