# 客如云订单查询接口文档

> 版本：v1.0  
> 适用模块：`cacch-integration-platform` 客如云业务域  
> 服务前缀：`/api/v1/keruyun/orders`

---

## 〇、环境信息

| 环境 | 地址 | 说明 |
|------|------|------|
| 测试环境 | `http://10.80.68.10:8081` | 联调测试使用 |
| 生产环境 | `http://10.80.68.11:8081` | 正式运行环境 |

完整接口 URL = `{环境地址}` + `{服务前缀}` + `{接口路径}`，例如：

```
http://10.80.68.10:8081/api/v1/keruyun/orders?pageNum=1&pageSize=20
```

---

## 〇·二、鉴权说明

本服务对外接口**不做独立鉴权**，所有请求的身份认证由**前置网关统一处理**。

- 本服务内部无 `SecurityFilterChain`、无 API Key 校验拦截器、无 Token 过滤器；
- 调用方在请求到达本服务前，须通过前置网关的认证；
- 认证通过后，网关将请求透传至本服务，本服务直接处理业务逻辑并返回结果；
- 若调用方未通过网关认证，请求将在网关层被拒绝，无法到达本服务。

**对接方需关注**：
1. 调用时需携带网关要求的认证凭据（具体认证方式与 Header 名称以网关约定为准，常见方式如 `Authorization: Bearer <token>` 或 `X-API-Key: <api-key>`）；
2. 测试环境与生产环境的认证凭据相互独立，不可混用；
3. 认证凭据由系统管理员统一申请与分配，请勿在代码或文档中硬编码。

---

## 一、统一返回体

所有接口统一返回 `Result<T>` 包装结构：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 业务码，`0` 表示成功，非 `0` 表示失败 |
| `message` | string | 业务消息，成功时为 `操作成功`，失败时为错误描述 |
| `data` | object / array / null | 业务数据，失败时可能为 `null` |

**调用方判定规则**：`code == 0` 才认为业务成功，再解析 `data`。

---

## 二、金额单位约定

| 字段后缀 | 单位 | 类型 | 精度 |
|----------|------|------|------|
| `...Yuan` | 元 | BigDecimal | 保留 2 位小数，HALF_UP |

所有金额字段已在服务端从「分」转换为「元」，调用方无需再做单位换算。

---

## 三、接口列表

### 3.1 分页查询订单

| 项 | 内容 |
|---|---|
| URL | `GET /api/v1/keruyun/orders` |
| 功能 | 按条件分页查询本地已落库的客如云订单，用于列表展示 |
| 排序 | 默认按 `openTime`（下单时间）**倒序** |

#### 请求参数

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `orderId` | string | 否 | — | 客如云订单 ID，精确匹配 |
| `shopId` | string | 否 | — | 门店 ID，精确匹配 |
| `orderStatus` | string | 否 | — | 订单状态，精确匹配 |
| `orderType` | string | 否 | — | 订单类型，精确匹配 |
| `openTimeFrom` | LocalDateTime | 否 | — | 下单时间起始（含），格式 `yyyy-MM-ddTHH:mm:ss` |
| `openTimeTo` | LocalDateTime | 否 | — | 下单时间结束（含），格式 `yyyy-MM-ddTHH:mm:ss` |
| `createdAtFrom` | LocalDateTime | 否 | — | 记录创建时间起始（含） |
| `createdAtTo` | LocalDateTime | 否 | — | 记录创建时间结束（含） |
| `pageNum` | int | 否 | `1` | 页码，从 1 开始 |
| `pageSize` | int | 否 | `20` | 每页条数，最大 `100`（超出自动截断为 100） |

#### 响应

`data` 为 MyBatis-Plus `IPage<T>` 结构：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1829214567890123456,
        "brandId": "B1001",
        "shopId": "80250510",
        "orderId": "18292145678901",
        "busiOrderNo": "BO20261021001",
        "orderStatus": "ORDER_RECEIVED",
        "orderType": "FOR_HERE",
        "orderAmtYuan": 128.00,
        "promoAmtYuan": 10.00,
        "orderReceivedAmtYuan": 118.00,
        "openTime": "2026-10-21T12:30:00",
        "finishTime": "2026-10-21T13:05:00",
        "thirdOrderNo": "T3rd001",
        "serialNo": "S20261021001",
        "thirdSerialNo": "TS001",
        "orderModeTag": "DINE_IN",
        "orderMode": "DINE_IN",
        "syncBatch": "OPEN_TIME-2026-10-21-163010",
        "syncedAt": "2026-10-21T16:30:10",
        "createdAt": "2026-10-21T16:30:10",
        "updatedAt": "2026-10-21T16:30:10"
      }
    ],
    "total": 156,
    "size": 20,
    "current": 1,
    "pages": 8
  }
}
```

**`data` 字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `records` | array | 当前页订单列表，元素见订单对象字段表 |
| `total` | long | 满足条件的总记录数 |
| `size` | int | 每页条数 |
| `current` | int | 当前页码 |
| `pages` | int | 总页数 |

#### 空结果

无数据时 `records` 为 `[]`，`total` 为 `0`，`code` 仍为 `0`。

---

### 3.2 全量导出查询（Excel 导出专用）

| 项 | 内容 |
|---|---|
| URL | `GET /api/v1/keruyun/orders/export` |
| 功能 | 按条件查询**全部**订单，供调用方生成 Excel 使用（不分页） |
| 排序 | 按 `openTime`（下单时间）**升序** |
| 上限 | 默认最多返回 **50000** 条，超出返回错误码提示缩小范围 |

#### 请求参数

与分页查询的过滤条件完全一致，**不含** `pageNum` / `pageSize`：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `orderId` | string | 否 | 客如云订单 ID，精确匹配 |
| `shopId` | string | 否 | 门店 ID，精确匹配 |
| `orderStatus` | string | 否 | 订单状态，精确匹配 |
| `orderType` | string | 否 | 订单类型，精确匹配 |
| `openTimeFrom` | LocalDateTime | 否 | 下单时间起始（含） |
| `openTimeTo` | LocalDateTime | 否 | 下单时间结束（含） |
| `createdAtFrom` | LocalDateTime | 否 | 记录创建时间起始（含） |
| `createdAtTo` | LocalDateTime | 否 | 记录创建时间结束（含） |

#### 响应

`data` 为订单对象数组：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": [
    { ... 订单对象，字段同分页接口 records 元素 ... }
  ]
}
```

#### 超限错误

满足条件的数据超过 50000 条时：

```json
{
  "code": 40001,
  "message": "导出数据量超过上限 50000 条，请缩小查询条件范围",
  "data": null
}
```

**处理建议**：按时间范围拆分（如按月、按周）多次调用后合并。

---

## 四、订单对象字段表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 内部主键（雪花算法） |
| `brandId` | string | 品牌 ID |
| `shopId` | string | 门店 ID |
| `orderId` | string | 客如云订单 ID（业务唯一键） |
| `busiOrderNo` | string | 业务订单号 |
| `orderStatus` | string | 订单状态 |
| `orderType` | string | 订单类型 |
| `orderAmtYuan` | BigDecimal | 订单金额（元，2 位小数） |
| `promoAmtYuan` | BigDecimal | 优惠金额（元，2 位小数） |
| `orderReceivedAmtYuan` | BigDecimal | 实收金额（元，2 位小数） |
| `openTime` | LocalDateTime | 下单时间 |
| `finishTime` | LocalDateTime | 完结时间 |
| `thirdOrderNo` | string | 第三方订单号 |
| `serialNo` | string | 订单流水号 |
| `thirdSerialNo` | string | 第三方流水号 |
| `orderModeTag` | string | 订单模式标签 |
| `orderMode` | string | 订单模式 |
| `syncBatch` | string | 同步批次号 |
| `syncedAt` | LocalDateTime | 最近一次同步成功时间 |
| `createdAt` | LocalDateTime | 记录创建时间 |
| `updatedAt` | LocalDateTime | 记录更新时间 |

> 说明：`orderStatus` / `orderType` / `orderMode` 取值沿用客如云官方枚举值，调用方按客如云文档释义展示。

---

## 五、错误码

| code | message | 触发场景 |
|------|---------|----------|
| `0` | 操作成功 | 成功 |
| `40001` | 参数错误 | 参数不合法（如 orderId 为空） |
| `40002` | 参数缺失 | 必填参数缺失 |
| `40001` | 导出数据量超过上限 N 条 | 导出接口结果 > 50000 |

---

## 六、调用示例

> 以下示例以**测试环境** `http://10.80.68.10:8081` 为例，生产环境请替换为 `http://10.80.68.11:8081`。  
> 鉴权 Header 名称与取值以网关约定为准，示例中用 `{网关鉴权Header}` 占位。

### 6.1 分页查询（cURL）

```bash
curl -X GET "http://10.80.68.10:8081/api/v1/keruyun/orders?shopId=80250510&openTimeFrom=2026-10-01T00:00:00&openTimeTo=2026-10-31T23:59:59&pageNum=1&pageSize=20" \
  -H "{网关鉴权Header}: {网关鉴权凭据}"
```

### 6.2 导出查询（cURL）

```bash
curl -X GET "http://10.80.68.10:8081/api/v1/keruyun/orders/export?shopId=80250510&openTimeFrom=2026-10-01T00:00:00&openTimeTo=2026-10-31T23:59:59" \
  -H "{网关鉴权Header}: {网关鉴权凭据}" \
  -o orders.json
```

### 6.3 浏览器端生成 Excel（调用方实现）

```javascript
import * as XLSX from 'xlsx';

const { data } = await axios.get('/api/v1/keruyun/orders/export', {
  params: { shopId: '80250510', openTimeFrom: '2026-10-01T00:00:00' }
});
const ws = XLSX.utils.json_to_sheet(data);
XLSX.writeFile({ Sheets: { 订单: ws }, SheetNames: ['订单'] }, 'keruyun_orders.xlsx');
```

---

## 七、对接注意事项

1. **时间格式**：`LocalDateTime` 类型参数使用 ISO-8601 格式 `yyyy-MM-ddTHH:mm:ss`（如 `2026-10-01T00:00:00`），由 Spring 默认 Jackson 反序列化。
2. **金额精度**：服务端返回的金额已为「元」且保留 2 位小数，调用方直接展示即可，无需再除 100。
3. **导出上限**：单次导出最大 50000 条，建议调用方按时间范围拆分请求。
4. **空值处理**：数值 / 时间字段无值时为 `null`，调用方需做空值判断。
5. **鉴权**：本服务无独立鉴权，由前置网关统一处理；调用时须携带网关要求的认证凭据（Header 名称与格式以网关约定为准），测试环境与生产环境凭据不可混用。
6. **环境地址**：测试 `http://10.80.68.10:8081`，生产 `http://10.80.68.11:8081`。

---

## 八、同步任务接口

> 以下接口用于「客如云订单同步」页面：展示同步记录、手动触发跨天同步。

### 8.1 分页查询同步记录

| 项 | 内容 |
|---|---|
| URL | `GET /api/v1/keruyun/orders/sync/tasks` |
| 功能 | 查询 `stg_keruyun_order_sync_task` 表的同步记录，按 `syncDate` **倒序** |
| Content-Type | `application/json` |

#### 请求参数

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `pageNum` | int | 否 | `1` | 页码，从 1 开始 |
| `pageSize` | int | 否 | `20` | 每页条数，最大 `100`（超出自动截断为 100） |

#### 响应

`data` 为 MyBatis-Plus `IPage<T>` 结构：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1829214567890123456,
        "syncDate": "2026-10-21",
        "dateType": "OPEN_TIME",
        "syncWindowStart": "2026-10-21T00:00:00",
        "syncWindowEnd": "2026-10-21T23:59:59",
        "status": "SUCCESS",
        "retryCount": 0,
        "lastPage": 3,
        "totalPage": 3,
        "totalCount": 128,
        "successCount": 128,
        "failureCount": 0,
        "errorMessage": null,
        "createdAt": "2026-10-22T10:00:00",
        "updatedAt": "2026-10-22T10:00:30",
        "syncedAt": "2026-10-22T10:00:30"
      }
    ],
    "total": 15,
    "size": 20,
    "current": 1,
    "pages": 1
  }
}
```

**`data.records` 单条字段说明**：见「九、同步任务对象字段表」。

---

### 8.2 手动触发跨天同步

| 项 | 内容 |
|---|---|
| URL | `POST /api/v1/keruyun/orders/sync/tasks/manual` |
| 功能 | 手动指定查询窗口，触发一次跨天同步（**dateType 固定 `OPEN_TIME`**，无需调用方传入） |
| Content-Type | `application/json` |
| 说明 | 以 `startTime` 的日期作为 `syncDate`，若该 `(syncDate, OPEN_TIME)` 已存在则重置查询窗口并重新执行，否则新建任务；**异步执行**，接口立即返回 |

#### 请求体

```json
{
  "startTime": "2026-10-01T00:00:00",
  "endTime": "2026-10-07T23:59:59"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `startTime` | LocalDateTime | 是 | 查询窗口起始时间 |
| `endTime` | LocalDateTime | 是 | 查询窗口结束时间，**必须晚于 startTime** |

#### 响应

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "createdTaskCount": 0,
    "pendingTaskCount": 1,
    "taskIds": [1829214567890123456],
    "message": "手动跨天同步已触发, dateType=OPEN_TIME, syncDate=2026-10-01, window=[2026-10-01T00:00, 2026-10-07T23:59:59]"
  }
}
```

**`data` 字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `createdTaskCount` | int | 本次新建任务数（手动同步固定为 0，因为走创建/重置逻辑） |
| `pendingTaskCount` | int | 本次待执行任务数（固定为 1） |
| `taskIds` | array | 任务 ID 列表（可用于后续查询状态） |
| `message` | string | 执行概览描述 |

#### 参数错误响应

| 场景 | code | message |
|------|------|---------|
| `startTime` 或 `endTime` 为空 | `40002` | 起始时间与结束时间均不可为空 / 查询窗口起始时间不可为空 |
| `endTime` 不晚于 `startTime` | `40001` | 结束时间必须晚于起始时间 |

---

## 九、同步任务对象字段表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 内部主键（雪花算法） |
| `syncDate` | LocalDate | 同步日期（`startTime` 的日期部分） |
| `dateType` | string | 查询时间类型，当前固定 `OPEN_TIME` |
| `syncWindowStart` | LocalDateTime | 查询窗口起始时间 |
| `syncWindowEnd` | LocalDateTime | 查询窗口结束时间 |
| `status` | string | 任务状态，见下方状态枚举 |
| `retryCount` | int | 已重试次数 |
| `lastPage` | int | 断点续传用，已拉取到的最后页码 |
| `totalPage` | int | 总页数（首次执行后回填） |
| `totalCount` | int | 总订单数（首次执行后回填） |
| `successCount` | int | 落库成功数 |
| `failureCount` | int | 落库失败数 |
| `errorMessage` | string | 失败原因（成功时为 null） |
| `createdAt` | LocalDateTime | 任务创建时间 |
| `updatedAt` | LocalDateTime | 任务最近状态/进度更新时间 |
| `syncedAt` | LocalDateTime | 任务执行完成时间（未完成时为 null） |

### 状态枚举（`status`）

| 值 | 含义 |
|----|------|
| `PENDING` | 待执行 |
| `IN_PROGRESS` | 执行中 |
| `SUCCESS` | 成功 |
| `FAILED` | 失败 |
| `PARTIAL` | 部分失败 |

---

## 十、同步任务接口调用示例

> 以**测试环境** `http://10.80.68.10:8081` 为例。

### 10.1 查询同步记录

```bash
curl -X GET "http://10.80.68.10:8081/api/v1/keruyun/orders/sync/tasks?pageNum=1&pageSize=20" \
  -H "{网关鉴权Header}: {网关鉴权凭据}"
```

### 10.2 手动跨天同步

```bash
curl -X POST "http://10.80.68.10:8081/api/v1/keruyun/orders/sync/tasks/manual" \
  -H "Content-Type: application/json" \
  -H "{网关鉴权Header}: {网关鉴权凭据}" \
  -d '{
    "startTime": "2026-10-01T00:00:00",
    "endTime": "2026-10-07T23:59:59"
  }'
```

---

## 十一、同步任务对接注意事项

1. **异步执行**：手动同步接口返回时任务仍在后台执行，前端需轮询「8.1 查询同步记录」接口查看 `status` 变化。
2. **幂等重置**：同一天重复触发手动同步不会新建任务，而是更新查询窗口并重置为 `PENDING` 重新执行。
3. **状态轮询**：建议前端每 3~5 秒轮询一次，直到 `status` 变为 `SUCCESS` / `FAILED` / `PARTIAL`。
4. **时间格式**：`startTime` / `endTime` 使用 ISO-8601 格式 `yyyy-MM-ddTHH:mm:ss`。

---

## 十二、订单菜品明细接口（SKU 维度）

> 以下接口以 SKU 为维度展示订单信息，**一行 = 一个订单 × 一个 SKU**。
> 数据来源：`stg_keruyun_order_item` JOIN `stg_keruyun_order`，依赖订单详情同步落库（订单列表同步完成后自动触发，仅同步已完结订单）。

### 12.1 分页查询菜品明细

| 项 | 内容 |
|---|---|
| URL | `GET /api/v1/keruyun/order-items` |
| 功能 | 按条件分页查询订单菜品明细（订单头信息 + SKU 数量/单价/金额） |
| 排序 | 下单时间**倒序**；同一订单的 SKU 行相邻排列 |

#### 请求参数

| 参数 | 类型 | 必填 | 匹配方式 | 说明 |
|------|------|------|------|------|
| `orderId` | string | 否 | 精确 | 客如云订单 ID |
| `itemCode` | string | 否 | 精确 | SKU 编码 |
| `itemName` | string | 否 | **模糊** | 菜品名称 |
| `orderStatus` | string | 否 | 精确 | 订单状态（客如云原始枚举值，见 12.3 映射表） |
| `orderType` | string | 否 | 精确 | 订单类型（客如云原始枚举值，见 12.3 映射表） |
| `openTimeFrom` | LocalDateTime | 否 | 范围 | 下单时间起始（含） |
| `openTimeTo` | LocalDateTime | 否 | 范围 | 下单时间结束（含） |
| `pageNum` | int | 否 | — | 页码，默认 `1` |
| `pageSize` | int | 否 | — | 每页条数，默认 `20`，最大 `100` |

#### 响应示例

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "orderId": "18292145678901",
        "shopId": "80250510",
        "busiOrderNo": "BO20261021001",
        "orderStatus": "已结账",
        "orderType": "堂食",
        "openTime": "2026-10-21T12:30:00",
        "finishTime": "2026-10-21T13:05:00",
        "itemCode": "SKU001",
        "itemName": "宫保鸡丁",
        "bigTypeName": "热菜",
        "midTypeName": "川菜",
        "unitName": "份",
        "specName": "中份/微辣",
        "quantity": 1.0000,
        "giftFlag": false,
        "saleStatusTypeCode": "NORMAL",
        "itemPriceYuan": 38.00,
        "salePriceYuan": 38.00,
        "itemSaleAmtYuan": 38.00
      }
    ],
    "total": 326,
    "size": 20,
    "current": 1,
    "pages": 17
  }
}
```

#### 明细行字段表

| 字段 | 类型 | 说明 |
|------|------|------|
| `orderId` | string | 客如云订单 ID（同订单多行相同，可作前端分组依据） |
| `shopId` | string | 门店 ID |
| `busiOrderNo` | string | 业务订单号 |
| `orderStatus` | string | 订单状态（**中文展示值**，未识别枚举返回原始值） |
| `orderType` | string | 订单类型（**中文展示值**，未识别枚举返回原始值） |
| `openTime` | LocalDateTime | 下单时间 |
| `finishTime` | LocalDateTime | 完结时间 |
| `itemCode` | string | SKU 编码 |
| `itemName` | string | 菜品名称 |
| `bigTypeName` | string | 菜品大类 |
| `midTypeName` | string | 菜品中类 |
| `unitName` | string | 单位 |
| `specName` | string | 规格（拼接展示值） |
| `quantity` | BigDecimal | 数量 |
| `giftFlag` | boolean | 是否赠菜：`true` 赠菜 / `false` 非赠菜 |
| `saleStatusTypeCode` | string | 售卖状态：`NORMAL` 销售 / `DISCARD` 退菜 |
| `itemPriceYuan` | BigDecimal | 原始单价（元，2 位小数） |
| `salePriceYuan` | BigDecimal | 售价（元，2 位小数） |
| `itemSaleAmtYuan` | BigDecimal | 销售金额（元，2 位小数） |

### 12.2 全量导出查询（Excel 导出专用）

| 项 | 内容 |
|---|---|
| URL | `GET /api/v1/keruyun/order-items/export` |
| 功能 | 按条件查询**全部**菜品明细，供调用方生成 Excel（不分页） |
| 排序 | 与分页接口一致 |
| 上限 | 默认最多返回 **50000** 条，超出返回错误码提示缩小范围 |

请求参数与 12.1 一致（**不含** `pageNum` / `pageSize`）；响应 `data` 为明细行数组，超限错误码同「3.2」。

### 12.3 订单状态 / 类型中文映射表

**过滤参数使用客如云原始枚举值，返回结果中已转换为中文：**

| orderStatus 枚举 | 中文 | | orderType 枚举 | 中文 |
|------|------|---|------|------|
| `WAIT_PROCESSED` | 待处理 | | `FOR_HERE` | 堂食 |
| `ORDER_RECEIVED` | 已接单 | | `PLATFORM_TAKE_OUT` | 平台外卖 |
| `SUCCESS` | 成功 | | `SELF_TAKE_OUT` | 门店外带 |
| `WAIT_SETTLED` | 待结账 | | `SELF_TAKE` | 自提 |
| `SETTLED` | 已结账 | | `NO_ORDER_CASHIER` | 无单收银 |
| `REFUND` | 已退单 | | `MEMBER_STORE` | 会员储值 |
| `CLOSED` | 已关闭 | | `MEMBER_MANUAL_STORE` | 会员手工储值 |
| `INVALID` | 已作废 | | `REPAYMENT_ORDER` | 销账订单 |
| `CANCELLED` | 已取消 | | | |
| `REJECTED` | 已拒单 | | | |
| `ANTI_SETTLED` | 已反结 | | | |

未识别的枚举值原样返回（不报错）。

### 12.4 调用示例

```bash
curl -X GET "http://10.80.68.10:8081/api/v1/keruyun/order-items?itemName=宫保鸡丁&openTimeFrom=2026-10-01T00:00:00&openTimeTo=2026-10-31T23:59:59&pageNum=1&pageSize=20" \
  -H "{网关鉴权Header}: {网关鉴权凭据}"
```

### 12.5 对接注意事项

1. **数据时延**：菜品明细依赖详情同步（列表同步完成后自动触发、仅同步**已完结订单**），列表页可见订单的明细通常在几分钟内补齐；`finishTime` 为空的订单暂无明细。
2. **套餐子商品**：套餐内子商品已平铺为独立行，与普通菜品行结构一致。
3. **退菜 / 赠菜**：通过 `saleStatusTypeCode=DISCARD`、`giftFlag=true` 标记，做销量统计时调用方需自行排除退菜行。
4. **空值处理**：单价/金额无值时为 `null`（如赠菜行可能无售价），调用方需做空值判断。
