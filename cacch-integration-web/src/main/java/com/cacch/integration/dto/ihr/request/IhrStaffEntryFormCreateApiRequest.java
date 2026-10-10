package com.cacch.integration.dto.ihr.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * IHR 新增待入职 — API 请求体（带校验注解）
 *
 * <p>字段校验严格对齐 iHR 接口文档（2026-10-10 在线核实），
 * 经 MapStruct {@code IhrStaffEntryFormConverter} 转为 integration 层的
 * {@code IhrStaffEntryFormCreateRequest} 后下发 Client。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrStaffEntryFormCreateApiRequest {

    /**
     * 员工姓名（必填，最大 20 位）
     */
    @NotBlank(message = "staffName 不能为空")
    @Size(max = 20, message = "staffName 最大 20 位")
    private String staffName;

    /**
     * 性别：MALE / FEMALE（可空）
     */
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "sex 可选值: MALE / FEMALE")
    private String sex;

    /**
     * 证照类型（可空）
     * IDENTITY_CARD / CHINA_PASSPORT / FOREIGN_PASSPORT / MTP / MTPFHKAMR / OTHER
     */
    @Pattern(regexp = "^(IDENTITY_CARD|CHINA_PASSPORT|FOREIGN_PASSPORT|MTP|MTPFHKAMR|OTHER)$",
            message = "idCardType 可选值: IDENTITY_CARD / CHINA_PASSPORT / FOREIGN_PASSPORT / MTP / MTPFHKAMR / OTHER")
    private String idCardType;

    /**
     * 证件号（可空，敏感字段——Server 层日志经 Client 脱敏）
     */
    private String idCardNo;

    /**
     * 手机号（必填）
     */
    @NotBlank(message = "mobile 不能为空")
    private String mobile;

    /**
     * 邮箱（可空）
     */
    private String email;

    /**
     * 入职时间（必填，格式 yyyy-MM-dd）
     */
    @NotBlank(message = "expectEntryDate 不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "expectEntryDate 格式必须为 yyyy-MM-dd")
    private String expectEntryDate;

    /**
     * 部门 ID（必填，Long 类型数字）
     */
    @NotNull(message = "departmentId 不能为空")
    private Long departmentId;

    /**
     * 职位 ID（可空，最大 36 位）
     */
    @Size(max = 36, message = "positionId 最大 36 位")
    private String positionId;

    /**
     * 合同类型（可空）：LABOR_CONTRACT / SERVICE_CONTRACT / TRAINING_CONTRACT / OTHER_CONTRACT
     */
    @Pattern(regexp = "^(LABOR_CONTRACT|SERVICE_CONTRACT|TRAINING_CONTRACT|OTHER_CONTRACT)$",
            message = "contractType 可选值: LABOR_CONTRACT / SERVICE_CONTRACT / TRAINING_CONTRACT / OTHER_CONTRACT")
    private String contractType;

    /**
     * 员工类型（可空）
     * FULLTIME / PARTTIME / INTERSHIP / EXPATRIATE / TEMPORARY / REHIRE_RETIREMENT
     */
    @Pattern(regexp = "^(FULLTIME|PARTTIME|INTERSHIP|EXPATRIATE|TEMPORARY|REHIRE_RETIREMENT)$",
            message = "staffType 可选值: FULLTIME / PARTTIME / INTERSHIP / EXPATRIATE / TEMPORARY / REHIRE_RETIREMENT")
    private String staffType;

    /**
     * 备注（可空，最大 255 位）
     */
    @Size(max = 255, message = "remark 最大 255 位")
    private String remark;
}
