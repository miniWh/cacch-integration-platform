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
