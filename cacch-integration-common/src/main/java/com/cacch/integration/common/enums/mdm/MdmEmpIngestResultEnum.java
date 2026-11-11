package com.cacch.integration.common.enums.mdm;

import lombok.Getter;

/**
 * 单员工落库（ingestOne）结果枚举
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public enum MdmEmpIngestResultEnum {

    /**
     * 已完成 stg/ods/md_emp/任职/事件全链路落库
     */
    INGESTED("已落库并产生事件"),

    /**
     * 行哈希未变化，幂等短路（仅写 stg 贴源）
     */
    SKIPPED_UNCHANGED("数据无变化，幂等跳过"),

    /**
     * 证件号缺失（致命质检项），仅入 stg/ods，不产主档与事件
     */
    SKIPPED_MISSING_IDCARD("证件号缺失，跳过主档与事件");

    /**
     * 结果中文描述（用于跳过原因记录）
     */
    private final String desc;

    MdmEmpIngestResultEnum(String desc) {
        this.desc = desc;
    }
}
