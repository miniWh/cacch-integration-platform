package com.cacch.integration.dto.moka.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Moka 标记候选人已离职 —— web 层请求体
 *
 * <p>对接测试接口 {@code PUT /api/v1/moka/applications/{applicationId}/resign}，
 * 经 MapStruct 转换为 integration 层 {@code MokaApplicationResignRequest} 后调用 Moka API。</p>
 *
 * <p>校验规则：
 * <ul>
 *     <li>{@code detail}（离职原因）必填</li>
 *     <li>{@code reason} 取值 0/1/2（已离职/主动离职/被动离职）</li>
 *     <li>{@code isResignWriteJob} 取值 0/1；为 1 时 {@code resignJobName} 必填
 *         （条件依赖校验在 Controller 中手动判断）</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaApplicationResignRequest {

    /**
     * 离职类型的归档原因 id（可选）
     */
    private Integer reasonType;

    /**
     * 离职原因（必填）
     */
    @NotBlank(message = "离职原因 detail 不可为空")
    private String detail;

    /**
     * 离职时间（可选，格式遵循 Moka 要求；不传时 Moka 默认取请求时间）
     */
    private String time;

    /**
     * 操作人的邮箱（可选）
     */
    private String email;

    /**
     * 操作人有权限的人才库 id 列表（可选，需先通过 Moka 人才库列表接口查询）
     */
    private List<Integer> talentPoolId;

    /**
     * 是否回写离职信息（可选）：0=不开启（默认），1=开启；
     * 开启后 resignJobName 必填
     */
    @Min(value = 0, message = "isResignWriteJob 取值 0 或 1")
    @Max(value = 1, message = "isResignWriteJob 取值 0 或 1")
    private Integer isResignWriteJob;

    /**
     * 离职职位（isResignWriteJob=1 时必填）
     */
    private String resignJobName;

    /**
     * 离职部门（可选）
     */
    private String resignDepartment;

    /**
     * 离职原因分类（可选）：0=已离职，1=主动离职，2=被动离职
     */
    @Min(value = 0, message = "reason 取值 0/1/2")
    @Max(value = 2, message = "reason 取值 0/1/2")
    private Integer reason;

    /**
     * 离职职类（可选，传入职类完全相同时 Moka 系统自动合并）
     */
    private String resignJobType;
}
