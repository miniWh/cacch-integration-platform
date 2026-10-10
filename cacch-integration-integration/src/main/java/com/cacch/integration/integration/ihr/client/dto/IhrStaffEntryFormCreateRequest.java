package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;

/**
 * IHR 新增待入职-简略 — 请求体 DTO
 *
 * <p>严格对齐 iHR 接口文档（2026-10-10 在线核实）：
 * <pre>
 * POST /openapi/thirdparty/api/staff/v1/entryForms/create
 * Content-Type: application/json;charset=UTF-8; Authorization: Bearer access_token
 * </pre>
 *
 * <p>必填字段：staffName / mobile / expectEntryDate / departmentId。
 * 本类为 integration 层纯净 DTO，不含校验注解；校验在 web 层 ApiRequest 完成后经 MapStruct 转换。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrStaffEntryFormCreateRequest {

    /**
     * 员工姓名（必填，最大 20 位）
     */
    private String staffName;

    /**
     * 性别：MALE / FEMALE（可空）
     */
    private String sex;

    /**
     * 证照类型：IDENTITY_CARD / CHINA_PASSPORT / FOREIGN_PASSPORT / MTP / MTPFHKAMR / OTHER（可空）
     */
    private String idCardType;

    /**
     * 证件号（可空，敏感字段——Client 层日志必须脱敏）
     */
    private String idCardNo;

    /**
     * 手机号（必填，敏感字段——Client 层日志必须脱敏）
     */
    private String mobile;

    /**
     * 邮箱（可空）
     */
    private String email;

    /**
     * 入职时间（必填，格式 yyyy-MM-dd）
     */
    private String expectEntryDate;

    /**
     * 部门 ID（必填，Long 类型数字；可通过「获取部门清单」接口查询）
     */
    private Long departmentId;

    /**
     * 职位 ID（可空，最大 36 位 String UUID；可通过「获取公司职位清单」接口查询）
     */
    private String positionId;

    /**
     * 合同类型：LABOR_CONTRACT / SERVICE_CONTRACT / TRAINING_CONTRACT / OTHER_CONTRACT（可空）
     */
    private String contractType;

    /**
     * 员工类型：FULLTIME / PARTTIME / INTERSHIP / EXPATRIATE / TEMPORARY / REHIRE_RETIREMENT（可空）
     */
    private String staffType;

    /**
     * 备注（可空，最大 255 位）
     */
    private String remark;
}
