# 环境变量清单 — 敏感信息部署配置指南

> 本文档配套「敏感信息硬编码治理」（P0）改造：`application.yml` / `application-test.yml` / `application-prod.yml` 中的密码、密钥已全部改为 `${ENV}` 占位符，由部署环境注入。**本文件不存放任何明文密钥**，实际值请从内部密钥管理渠道获取。

## 目录

- [一、改造原则](#一改造原则)
- [二、环境变量总表](#二环境变量总表)
- [三、生产环境（10.80.68.11）配置步骤](#三生产环境10806811配置步骤)
- [四、测试环境（10.80.68.10）配置步骤](#四测试环境10806810配置步骤)
- [五、本地 IDEA 开发配置](#五本地-idea-开发配置)
- [六、缺失变量影响对照表](#六缺失变量影响对照表)

## 一、改造原则

| 原则 | 说明 |
|------|------|
| fail-fast | 生产环境（`application-prod.yml`）的密钥类变量**无默认值**，环境变量缺失时应用启动即失败，杜绝「明文兜底」 |
| 空默认保底 | 测试环境（`application-test.yml`）密钥类变量使用 `${ENV:}` 空默认，缺失时启动不崩、连接/鉴权失败，便于本地联调排障 |
| 用户名可保留 | 账号名/企业 ID/模板号等半敏感标识保留默认值（如 `DB_USERNAME:esb`），降低部署配置负担；密钥必须注入 |
| 语义化命名 | `wecom.apps` 为 List 结构，secret 使用语义化命名（`WECOM_SELF_BUILT_SECRET` / `WECOM_ADDRESS_BOOK_SECRET`）经 yml 占位符注入。**严禁**使用 `WECOM_APPS_0_SECRET` 这类与属性路径 `wecom.apps[0].secret` 同构的命名 —— Spring 会将 SystemEnvironment 映射为该属性并劫持整个 List 绑定（优先级高于 yml），导致 yml 中其余字段（corpid/app-key）绑定为 null（2026-09-01 测试环境事故根因） |
| export 位置 | 写入 `startup.sh` 时，export 块必须位于 `nohup java ... &` 启动行**之前** —— 脚本派生 java 子进程后即退出，追加到文件末尾的 export 永远不会被子进程继承（2026-09-01 测试环境事故根因之二） |

[返回顶部](#目录)

## 二、环境变量总表

| 环境变量 | 对应配置项 | 说明 | 生产缺失影响 |
|----------|-----------|------|--------------|
| `DB_URL` | `spring.datasource.url` | PG 连接串（默认 `jdbc:postgresql://10.80.86.93:5432/cdb`） | 走默认地址 |
| `DB_USERNAME` | `spring.datasource.username` | PG 用户（默认 `esb`） | 走默认用户名 |
| `DB_PASSWORD` | `spring.datasource.password` | **PG 密码** | 启动失败 |
| `REDIS_HOST` | `spring.data.redis.host` | Redis 地址（默认 `10.80.86.95`） | 走默认地址 |
| `REDIS_PASSWORD` | `spring.data.redis.password` | **Redis 密码** | 启动失败 |
| `WECOM_SELF_BUILT_SECRET` | `wecom.apps[0].secret`（经 yml 占位符） | **企微自建应用 Secret** | 对应应用鉴权不可用 |
| `WECOM_ADDRESS_BOOK_SECRET` | `wecom.apps[1].secret`（经 yml 占位符） | **企微通讯录应用 Secret** | 对应应用鉴权不可用 |
| `WECOM_WEBHOOK_KEY` | `wecom.webhook.key` | 企微群机器人 Webhook key | 告警失效 |
| `CRM_APP_KEY` | `crm.app-key` | **勤策 CRM 签名密钥** | 启动失败 |
| `OA_REST_USER_NAME` | `oa.rest-user-name` | OA REST 账号（默认 `zhouhufu`） | 走默认 |
| `OA_REST_PASSWORD` | `oa.rest-password` | **OA REST 密码** | 启动失败 |
| `OA_DB_USERNAME` | `oa.datasource.username` | OA Oracle 用户（默认 `V5`） | 走默认 |
| `OA_DB_PASSWORD` | `oa.datasource.password` | **OA Oracle 只读库密码** | 启动失败 |
| `SHARE_DRIVE_PASSWORD` | `share-drive.password` | **共享盘 SMB 密码** | 启动失败 |
| `FDD_APP_SECRET` | `fdd.app-secret` | **法大大 App Secret** | 启动失败 |
| `TENCENT_MEETING_SECRET_ID` | `tencent-meeting.secret-id` | **腾讯会议 SecretId** | 启动失败 |
| `TENCENT_MEETING_SECRET_KEY` | `tencent-meeting.secret-key` | **腾讯会议 SecretKey** | 启动失败 |
| `IHR_APP_KEY` | `ihr.app-key` | **IHR360 开放平台 AppKey** | 启动失败 |
| `IHR_APP_SECRET` | `ihr.app-secret` | **IHR360 开放平台 AppSecret** | 启动失败 |
| `CRM_BASE_URL` / `CRM_OPEN_ID` / `CRM_EMPLOYEE_BASE_URL` | `crm.*` | CRM 地址与租户标识（半敏感，有默认） | 走默认 |
| `OA_BASE_URL` / `OA_DEFAULT_LOGIN_NAME` / `OA_TOKEN_TTL_SECONDS` / `OA_TEMPLATE_CODE` / `OA_DB_URL` / `OA_DB_USERNAME` | `oa.*` | OA 地址与业务参数（有默认） | 走默认 |
| `TENCENT_MEETING_APP_ID` / `SDK_ID` / `ENABLED` / `DEFAULT_OPERATOR_ID` | `tencent-meeting.*` | 腾讯会议账号参数（有默认） | 走默认 |

> 加粗项为必须注入的密钥类变量；其余为可选覆盖项（yml 已提供默认值）。

[返回顶部](#目录)

## 三、生产环境（10.80.68.11）配置步骤

应用由 systemd 拉起（`/home/zhf/app/startup.sh`），环境变量须写入 `startup.sh` 且 **export 块必须位于 `nohup java ... &` 行之前**（追加到文件末尾无效，java 子进程继承不到）：

```bash
# 编辑启动脚本，在 nohup 启动行之前插入（示例）
vi /home/zhf/app/startup.sh

# ===== 敏感信息环境变量（严禁提交版本库/写日志；必须位于 nohup 之前） =====
export DB_PASSWORD='<PG 密码>'
export REDIS_PASSWORD='<Redis 密码>'
export WECOM_SELF_BUILT_SECRET='<企微自建应用 Secret>'
export WECOM_ADDRESS_BOOK_SECRET='<企微通讯录应用 Secret>'
export WECOM_WEBHOOK_KEY='<群机器人 Webhook key>'
export CRM_APP_KEY='<CRM App Key>'
export OA_REST_PASSWORD='<OA REST 密码>'
export OA_DB_PASSWORD='<OA Oracle 密码>'
export SHARE_DRIVE_PASSWORD='<共享盘密码>'
export FDD_APP_SECRET='<法大大 App Secret>'
export TENCENT_MEETING_SECRET_ID='<腾讯会议 SecretId>'
export TENCENT_MEETING_SECRET_KEY='<腾讯会议 SecretKey>'
export IHR_APP_KEY='<IHR360 AppKey>'
export IHR_APP_SECRET='<IHR360 AppSecret>'
```

校验：`source /home/zhf/app/startup.sh && env | grep -c 'DB_PASSWORD\|FDD_APP_SECRET'`，然后重启服务并观察 `actuator/health`。

> 备选：在 systemd unit 中使用 `EnvironmentFile=/home/zhf/app/env.conf` 加载。

[返回顶部](#目录)

## 四、测试环境（10.80.68.10）配置步骤

同生产，但密钥缺失时**不会阻止启动**（空默认），可通过 `/actuator/health` 判断各依赖连通性；若出现鉴权/连接类报错，优先检查下表环境变量是否已注入。

[返回顶部](#目录)

## 五、本地 IDEA 开发配置

本地以 `test` profile 运行（`application-test.yml`）时，在 Run Configuration → Environment variables 中补齐密钥变量即可（值从测试环境获取）：

```
DB_PASSWORD=...;REDIS_PASSWORD=...;WECOM_SELF_BUILT_SECRET=...;WECOM_ADDRESS_BOOK_SECRET=...;CRM_APP_KEY=...;OA_REST_PASSWORD=...;OA_DB_PASSWORD=...;SHARE_DRIVE_PASSWORD=...;FDD_APP_SECRET=...;TENCENT_MEETING_SECRET_ID=...;TENCENT_MEETING_SECRET_KEY=...;IHR_APP_KEY=...;IHR_APP_SECRET=...
```

[返回顶部](#目录)

## 六、缺失变量影响对照表

| 缺失变量 | 生产（fail-fast） | 测试（空默认） |
|----------|------------------|----------------|
| `DB_PASSWORD` / `REDIS_PASSWORD` / `OA_REST_PASSWORD` / `OA_DB_PASSWORD` / `SHARE_DRIVE_PASSWORD` / `FDD_APP_SECRET` / `CRM_APP_KEY` / `TENCENT_MEETING_SECRET_*` / `IHR_APP_KEY` / `IHR_APP_SECRET` | **启动失败**（占位符无法解析） | 启动正常，连接/调用失败 |
| `WECOM_SELF_BUILT_SECRET` / `WECOM_ADDRESS_BOOK_SECRET` | 启动正常，对应企微应用鉴权不可用 | 启动正常，对应企微应用鉴权不可用 |
| `WECOM_WEBHOOK_KEY` | 启动正常，企微告警失效 | 启动正常，企微告警失效 |

[返回顶部](#目录)
