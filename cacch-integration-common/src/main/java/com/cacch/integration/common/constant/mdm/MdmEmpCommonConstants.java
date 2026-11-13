package com.cacch.integration.common.constant.mdm;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 人员主数据（MDM）人员子域 —— 公共业务参数常量
 *
 * <p>本类仅承载跨模块复用的业务参数与工具方法。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmEmpCommonConstants {

    private MdmEmpCommonConstants() {
    }

    /**
     * 来源系统编码：iHR
     */
    public static final String SRC_SYS_CODE_IHR = "IHR";

    /**
     * ETL 批次号时间格式（精确到毫秒，保证单轮批次唯一）
     */
    private static final DateTimeFormatter BATCH_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 生成 iHR 人员拉取 ETL 批次号
     *
     * <p>格式：{@code IHR + yyyyMMddHHmmssSSS}（如 IHR20260420103000123），
     * 毫秒精度保证同一轮拉取共用一个批次号、不同轮次批次号不同。</p>
     *
     * @return 本轮拉取的 ETL 批次号
     */
    public static String buildEtlBatchNo() {
        return SRC_SYS_CODE_IHR + LocalDateTime.now().format(BATCH_NO_FMT);
    }

    /**
     * 生成 iHR 增量同步 Pull 批次号（入队 stg_staff_record 的 pull_batch_no）
     *
     * <p>格式：{@code pull- + yyyyMMddHHmmssSSS}，同一轮 pull 任务所有入队行共享。</p>
     */
    public static String buildPullBatchNo() {
        return "pull-" + LocalDateTime.now().format(BATCH_NO_FMT);
    }

    /**
     * 生成 iHR 增量同步 Process 批次号（stg_staff_record 的 process_batch_no）
     *
     * <p>格式：{@code proc- + yyyyMMddHHmmssSSS}，同一轮 process 任务所有已处理行共享。</p>
     */
    public static String buildProcessBatchNo() {
        return "proc-" + LocalDateTime.now().format(BATCH_NO_FMT);
    }
}
