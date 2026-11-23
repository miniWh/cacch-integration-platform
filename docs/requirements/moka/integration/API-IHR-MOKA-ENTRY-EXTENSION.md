# Moka 登记表字段 → iHR 扩展对接清单（二期）

> Step 1（本期已落地）：`listEhrApplicationsV2` → PG `candidate_base` / `entry_education`  
> Step 2（已有）：`entryForms/create` 简略待入职  
> **Step 3（本文）**：员工在册后回写紧急联系人与教育经历

## 1. 修改员工基础信息（紧急联系人）

| 项 | 内容 |
|----|------|
| 文档 | [修改员工基础信息](https://open.ihr360.com/#/%E4%BC%81%E4%B8%9A%E5%BA%94%E7%94%A8API/%E5%91%98%E5%B7%A5%E7%AE%A1%E7%90%86/%E5%91%98%E5%B7%A5/%E4%BF%AE%E6%94%B9%E5%91%98%E5%B7%A5%E5%9F%BA%E7%A1%80%E4%BF%A1%E6%81%AF) |
| 方法 | `PUT` |
| Path | `/openapi/thirdparty/api/v1/staff/{staffId}` |
| 代码 Client | `IhrStaffClient#updateStaffBasicInfo` |
| Service | `IIhrStaffService#updateStaffBasicInfo` |
| 请求 DTO | `IhrStaffUpdateBasicInfoRequest` |

### PG → iHR 字段映射（建议）

| PG 列 | iHR Body 字段 | 说明 |
|-------|---------------|------|
| `emergency_contact_name` | `emergencyContactName` | trim |
| `emergency_contact_phone` | `emergencyContactMobile` | 脱敏日志 |
| `emergency_contact_relation_ihr` 或 Q-02 | 花名册自定义 key | `setCustomField(key, value)` |
| — | 勿传 `mobileNo` | 除非有意改员工手机号（会企业解绑） |

### 前置条件

- 已取得 iHR **员工 staffId**（非 `entryId`）：如「根据员工手机号获取员工ID」或入职转正后花名册 ID。
- 自定义字段 key 通过「查询花名册字段信息」配置。

---

## 2. 创建教育经历 v2（含是否全日制）

| 项 | 内容 |
|----|------|
| 文档 | [创建教育经历v2](https://open.ihr360.com/#/%E4%BC%81%E4%B8%9A%E5%BA%94%E7%94%A8API/%E5%91%98%E5%B7%A5%E7%AE%A1%E7%90%86/%E5%91%98%E5%B7%A5%E6%95%99%E8%82%B2%E7%BB%8F%E5%8E%86/%E5%88%9B%E5%BB%BA%E6%95%99%E8%82%B2%E7%BB%8F%E5%8E%86v2) |
| 方法 | `POST` |
| Path | `/openapi/thirdparty/api/staff/v1/educations/create` |
| 代码 Client | `IhrStaffClient#createEducationV2` |
| Service | `IIhrStaffService#createStaffEducationV2` |
| 请求 DTO | `IhrStaffEducationCreateV2Request` |

### PG → iHR 字段映射（建议）

| `t_integration_moka_entry_education` | iHR 字段 | 转换 |
|--------------------------------------|----------|------|
| `school` | `schoolName` | 原样 |
| `speciality` | `major` | 原样 |
| `start_date` | `entryDate` | `YYYY-MM` → `yyyy-MM-01` 或补 `-01` |
| `end_date` | `graduateDate` | 同上 |
| `academic_degree` | `diplomaType` | 中文/文本 → `IhrStaffConstants` 枚举 |
| `is_full_time` | `flexAttributes.{key}` | 花名册教育经历下「是否全日制」字段 key |

### 相关只读接口（编排可选用）

| 接口 | 用途 |
|------|------|
| 获取教育经历清单 | 幂等：先查再决定是否创建 |
| 修改教育经历 / 删除教育经历 | 全量同步策略 |
| 根据员工ID删除教育经历 | 重建前先清空 |

---

## 3. 建议编排（未实现 Manager）

```text
Moka push-ihr SUCCESS
  → 等待员工入职/花名册生成 staffId（事件或定时扫 modifications）
  → 读 PG 紧急联系人 + education
  → PUT staff/{id} 写紧急联系人
  → 逐条 POST educations/create
  → 记录扩展同步状态（可扩 t_integration_moka_entry_sync 字段）
```

## 4. Path 常量

见 `IhrPathConstants#STAFF_UPDATE_BASIC_PATH`、`STAFF_EDUCATION_CREATE_V2_PATH`。