# API-OA-002 国内登记报告附件同步记录查询

---

## 1. 概述

| 项目 | 内容 |
|------|------|
| 接口编号 | API-OA-002 |
| 接口名称 | 国内登记报告附件同步记录查询（管理端） |
| 关联需求 | [REQ-OA-001 国内登记报告资料列表附件上传](../../requirements/oa/feature/REQ-OA-001-国内登记报告资料列表附件上传.md) |
| 适用场景 | 管理人员按登记负责人、IPDP、资料项目等维度查询附件同步中间表记录 |
| 数据来源 | PostgreSQL 表 `t_integration_oa_reg_attachment_sync` |

---

## 2. 接口说明

### 2.1 基本信息

| 项目 | 内容 |
|------|------|
| 请求方法 | `GET` |
| 请求路径 | `/api/v1/oa/reg-reports/attachment-sync/records/search` |
| Content-Type | 无请求体 |
| 鉴权 | 与平台其他 REST 接口一致（API Key / 网关策略以部署环境为准） |

### 2.2 查询逻辑

- **登记负责人**、**IPDP 名称**、**资料项目** 三个参数均为可选，可只填一项、多项组合，或全部不填。
- 多个条件之间为 **AND** 关系。
- 名称类字段（登记负责人、IPDP 名称、资料项目）采用 **模糊匹配**（SQL `LIKE %keyword%`）。
- **同步状态** 为可选精确匹配。
- 结果按 **最近同步时间 `lastSyncAt` 降序**，其次按 **记录 ID 降序**。
- 仅返回未逻辑删除记录（`is_deleted = 0`）。

---

## 3. 请求参数

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `ownerName` | string | 否 | — | 登记负责人（OA field0223 解析姓名），模糊匹配 |
| `ipdpName` | string | 否 | — | IPDP 名称（OA field0160），模糊匹配 |
| `itemName` | string | 否 | — | 资料项目名称（OA field0214），模糊匹配 |
| `syncStatus` | string | 否 | — | 同步状态，精确匹配；见 [3.1 同步状态枚举](#31-同步状态枚举) |
| `page` | long | 否 | `1` | 页码，从 1 开始 |
| `size` | long | 否 | `20` | 每页条数，最大 `100` |

### 3.1 同步状态枚举

| 值 | 含义 |
|----|------|
| `PENDING` | 待同步 |
| `SUCCESS` | 已成功 |
| `RETRY` | 重试中 |
| `FAILED` | 已失败（达最大重试） |
| `SKIPPED` | 已跳过（如 OA 未匹配、幂等跳过等） |

---

## 4. 响应结构

统一包装为平台标准 `Result<T>`：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [ /* OaRegAttachmentSyncRecordVO 数组 */ ],
    "total": 128,
    "page": 1,
    "size": 20,
    "pages": 7
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | `0` 表示成功 |
| `message` | string | 提示信息 |
| `data.records` | array | 同步记录列表 |
| `data.total` | long | 符合条件的总条数 |
| `data.page` | long | 当前页码 |
| `data.size` | long | 当前页大小 |
| `data.pages` | long | 总页数 |

### 4.1 记录字段说明（`records[]`）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 中间表主键 |
| `formMainId` | long | OA 主表 formmain_4070.id |
| `ownerName` | string | 登记负责人 |
| `ipdpName` | string | IPDP 名称 |
| `itemName` | string | 资料项目名称 |
| `itemRowId` | long | OA 子表行 formson_5464.id |
| `sharePath` | string | 共享盘资料项目目录完整路径 |
| `fileName` | string | 已同步文件名 |
| `fileSize` | long | 文件大小（字节） |
| `fileChecksum` | string | 文件 SHA-256（流式上传后写入） |
| `fileCreatedAt` | string | 共享盘文件创建时间（ISO-8601） |
| `fileModifiedAt` | string | 共享盘文件修改时间 |
| `oaFileId` | string | OA REST 上传返回的 fileUrl（文件 ID） |
| `oaSubReference` | string | CAP4 绑定 subReference（field0218） |
| `syncStatus` | string | 同步状态 |
| `syncMessage` | string | 同步说明或失败/跳过原因 |
| `retryCount` | int | 重试次数 |
| `lastSyncAt` | string | 最近一次同步时间 |
| `createdAt` | string | 记录创建时间 |
| `updatedAt` | string | 记录更新时间 |

---

## 5. 请求示例

### 5.1 按登记负责人查询

```http
GET /api/v1/oa/reg-reports/attachment-sync/records/search?ownerName=李庆辉&page=1&size=20
```

### 5.2 按 IPDP + 资料项目组合查询

```http
GET /api/v1/oa/reg-reports/attachment-sync/records/search?ipdpName=环丙氟虫胺&itemName=农药登记变更申请表
```

### 5.3 查询全部失败记录

```http
GET /api/v1/oa/reg-reports/attachment-sync/records/search?syncStatus=FAILED&page=1&size=50
```

### 5.4 不传任何业务条件（分页浏览全部）

```http
GET /api/v1/oa/reg-reports/attachment-sync/records/search?page=1&size=20
```

---

## 6. 响应示例

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1987654321098765432,
        "formMainId": -6712378767102161000,
        "ownerName": "李庆辉",
        "ipdpName": "10% 环丙氟虫胺可分散液剂",
        "itemName": "农药登记变更申请表",
        "itemRowId": -6712378767102161227,
        "sharePath": "\\\\192.168.1.8\\国内登记资料\\李庆辉\\10% 环丙氟虫胺可分散液剂\\农药登记变更申请表",
        "fileName": "农药登记变更申请表_最终版本.pdf",
        "fileSize": 1048576,
        "fileChecksum": "a1b2c3...",
        "fileCreatedAt": "2026-07-28T14:30:00",
        "fileModifiedAt": "2026-07-28T14:30:00",
        "oaFileId": "-1234567890",
        "oaSubReference": "9077397064738097293",
        "syncStatus": "SUCCESS",
        "syncMessage": null,
        "retryCount": 0,
        "lastSyncAt": "2026-07-29T08:00:15",
        "createdAt": "2026-07-29T08:00:15",
        "updatedAt": "2026-07-29T08:00:15"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20,
    "pages": 1
  }
}
```

---

## 7. 错误码

| code | 说明 |
|------|------|
| `0` | 成功 |
| `400xx` | 参数错误（如 page/size 非法，一般由框架校验） |
| `999xx` | 系统内部错误 |

---

## 8. 与其他查询接口的区别

| 接口路径 | 主要用途 | 关键过滤条件 |
|----------|----------|--------------|
| `GET .../attachment-sync/records/search` | **管理端按业务维度查询** | ownerName / ipdpName / itemName（均可选） |
| `GET .../attachment-sync/records` | 按同步状态分页 | 仅 `syncStatus` |
| `GET .../attachment-sync/records/by-form/{formMainId}` | 按 OA 主表 ID 查询 | `formMainId` |
| `GET .../{bizNo}/attachment-sync/records` | 按表单标识查询 | `bizNo`（当前等同 formMainId） |

---

## 9. 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-07-29 | 新增管理端按登记负责人 / IPDP / 资料项目查询接口 |
