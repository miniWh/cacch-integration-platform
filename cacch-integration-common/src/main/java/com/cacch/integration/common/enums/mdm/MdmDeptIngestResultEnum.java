package com.cacch.integration.common.enums.mdm;

import lombok.Getter;

/**
 * 单部门落库（ingestOne）结果枚举
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public enum MdmDeptIngestResultEnum {

    /**
     * 已完成 stg/ods/mdm_dept/事件全链路落库
     */
    INGESTED("已落库并产生事件"),

    /**
     * 行哈希未变化，幂等短路（仅写 stg 贴源）
     */
    SKIPPED_UNCHANGED("数据无变化，幂等跳过");

    /**
     * 结果中文描述（用于跳过原因记录）
     */
    private final String desc;

    MdmDeptIngestResultEnum(String desc) {
        this.desc = desc;
    }
}
