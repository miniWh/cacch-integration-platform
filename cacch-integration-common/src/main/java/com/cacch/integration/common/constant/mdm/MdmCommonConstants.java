package com.cacch.integration.common.constant.mdm;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 主数据（MDM）域级公共业务参数常量 —— 人员 / 部门 / 岗位子域共用
 *
 * <p>仅承载跨子域复用的业务参数与批次号工具方法，子域特有参数仍保留在各子域
 * {@code Mdm{Sub}CommonConstants} 中。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MdmCommonConstants {

    private MdmCommonConstants() {
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
     * 生成 iHR 拉取 ETL 批次号
     *
     * <p>格式：{@code 前缀 + yyyyMMddHHmmssSSS}（如 IHR20260420103000123），
     * 毫秒精度保证同一轮拉取共用一个批次号、不同轮次批次号不同。</p>
     *
     * @param prefix 子域前缀（如 IHR / DEPT / POS），不可为空
     * @return 本轮拉取的 ETL 批次号
     */
    public static String buildEtlBatchNo(String prefix) {
        return prefix + LocalDateTime.now().format(BATCH_NO_FMT);
    }

    /**
     * 生成 iHR 增量同步 Pull 批次号（入队 stg_staff_record 的 pull_batch_no）
     *
     * <p>格式：{@code pull- + yyyyMMddHHmmssSSS}，同一轮 pull 任务所有入队行共享。</p>
     *
     * @return 本轮 pull 任务的批次号
     */
    public static String buildPullBatchNo() {
        return "pull-" + LocalDateTime.now().format(BATCH_NO_FMT);
    }

    /**
     * 生成 iHR 增量同步 Process 批次号（stg_staff_record 的 process_batch_no）
     *
     * <p>格式：{@code proc- + yyyyMMddHHmmssSSS}，同一轮 process 任务所有已处理行共享。</p>
     *
     * @return 本轮 process 任务的批次号
     */
    public static String buildProcessBatchNo() {
        return "proc-" + LocalDateTime.now().format(BATCH_NO_FMT);
    }
}
