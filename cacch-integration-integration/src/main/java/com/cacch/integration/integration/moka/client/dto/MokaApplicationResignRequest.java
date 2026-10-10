package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Moka ATS —— 标记候选人已离职请求体
 *
 * <p>对接 Moka API {@code PUT /api-platform/v1/applications/{applicationId}/resign}。
 * 字段严格匹配 Moka 官方文档的请求 body 参数表；除 {@code detail} 必填外，
 * 其余字段均可选（序列化时 null 字段不输出，由 Moka 侧取默认值）。</p>
 *
 * <p>业务规则：
 * <ul>
 *     <li>{@code time} 不传时 Moka 默认取请求时间作为离职时间</li>
 *     <li>{@code isResignWriteJob=1} 时 {@code resignJobName} 必填（由上层校验）</li>
 *     <li>{@code talentPoolId} 需先通过「查询人才库列表接口」获取有效 id</li>
 *     <li>{@code resignJobType} 传入职类完全相同时 Moka 系统会自动合并</li>
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
    private String detail;

    /**
     * 离职时间（可选，默认取请求时间）
     */
    private String time;

    /**
     * 操作人的邮箱（可选）
     */
    private String email;

    /**
     * 操作人有权限的人才库 id 列表（可选，需先查人才库列表接口获取）
     */
    private List<Integer> talentPoolId;

    /**
     * 是否回写离职信息（可选）：0=不开启（默认），1=开启；
     * 开启后 {@link #resignJobName} 必填
     *
     * <p>Moka 文档中该字段为 boolean 语义但取值 0/1，故用 Integer 承接。</p>
     */
    @JsonProperty("isResignWriteJob")
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
    private Integer reason;

    /**
     * 离职职类（可选，传入职类完全相同时系统自动合并）
     */
    private String resignJobType;
}
