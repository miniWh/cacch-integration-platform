package com.cacch.integration.common.constant.mdm;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 岗位主数据（MDM）岗位子域 —— 公共业务参数常量
 *
 * <p>本类仅承载跨模块复用的业务参数与工具方法。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmPositionCommonConstants {

    private MdmPositionCommonConstants() {
    }

    /**
     * 来源系统编码：iHR
     */
    public static final String SRC_SYS_CODE_IHR = "IHR";

    /**
     * 源系统实体类型：岗位（src_ref 映射用）
     */
    public static final String SRC_ENTITY_TYPE_POSITION = "POSITION";

    /**
     * 岗位停用状态
     */
    public static final String POSITION_STATUS_DISABLED = "DISABLE";

    /**
     * ETL 批次号时间格式（精确到毫秒，保证单轮批次唯一）
     */
    private static final DateTimeFormatter BATCH_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 生成 iHR 岗位拉取 ETL 批次号
     *
     * <p>格式：{@code POS + yyyyMMddHHmmssSSS}（如 POS20260420103000123），
     * 毫秒精度保证同一轮拉取共用一个批次号、不同轮次批次号不同。</p>
     *
     * @return 本轮拉取的 ETL 批次号
     */
    public static String buildEtlBatchNo() {
        return "POS" + LocalDateTime.now().format(BATCH_NO_FMT);
    }
}
