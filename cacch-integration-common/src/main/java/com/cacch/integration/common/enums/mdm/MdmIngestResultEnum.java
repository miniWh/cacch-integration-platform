package com.cacch.integration.common.enums.mdm;

import lombok.Getter;

/**
 * 主数据单条落库（ingestOne）结果枚举 —— 人员 / 部门 / 岗位子域共用
 *
 * <p>SKIPPED_MISSING_IDCARD 仅人员子域使用（证件号为致命质检项），
 * 部门 / 岗位子域不会产生该结果。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public enum MdmIngestResultEnum {

    /**
     * 已完成 stg/ods/主档/事件全链路落库
     */
    INGESTED("已落库并产生事件"),

    /**
     * 行哈希未变化，幂等短路（仅写 stg 贴源）
     */
    SKIPPED_UNCHANGED("数据无变化，幂等跳过"),

    /**
     * 证件号缺失（致命质检项，人员子域专用），仅入 stg/ods，不产主档与事件
     */
    SKIPPED_MISSING_IDCARD("证件号缺失，跳过主档与事件");

    /**
     * 结果中文描述（用于跳过原因记录）
     */
    private final String desc;

    MdmIngestResultEnum(String desc) {
        this.desc = desc;
    }
}
