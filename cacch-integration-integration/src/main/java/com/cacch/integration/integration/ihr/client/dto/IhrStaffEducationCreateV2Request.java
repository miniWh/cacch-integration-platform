package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

/**
 * iHR 创建教育经历 v2 — 请求体
 *
 * <p>POST {@code /openapi/thirdparty/api/staff/v1/educations/create}。
 * 「是否全日制」等扩展属性放在 {@link #flexAttributes}，key 须与花名册教育经历字段定义一致。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IhrStaffEducationCreateV2Request {

    /**
     * iHR 员工 ID（UUID，最大 36 位），非待入职 entryId
     */
    private String staffId;

    /**
     * 学校名称（必填，须为 iHR 认可的学校名称）
     */
    private String schoolName;

    /**
     * 入校日期（必填，格式 {@code yyyy-MM-dd}）
     */
    private String entryDate;

    /**
     * 结业/毕业日期（必填，格式 {@code yyyy-MM-dd}）
     */
    private String graduateDate;

    /**
     * 专业名称（必填，最大 60 位）
     */
    private String major;

    /**
     * 是否取得学位：{@code true} 是 / {@code false} 否
     */
    private Boolean hasGotDegree;

    /**
     * 学历类型枚举，如 {@code UNDER_GRADUATE}、{@code MASTER}，
     * 参见 {@link com.cacch.integration.common.constant.ihr.IhrStaffConstants} 中 {@code HIGHEST_EDUCATION_*} 常量
     */
    private String diplomaType;

    /**
     * 教育经历子集 flex 自定义字段（如是否全日制），key/value 含义参见「查询花名册字段信息」
     */
    private Map<String, Object> flexAttributes;
}