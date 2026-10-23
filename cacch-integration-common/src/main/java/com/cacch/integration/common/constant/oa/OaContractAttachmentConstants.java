package com.cacch.integration.common.constant.oa;

/**
 * OA 货物购销合同附件回传 CRM 常量（REQ-OA-003）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaContractAttachmentConstants {

    private OaContractAttachmentConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "OA合同附件回传CRM";

    /**
     * CRM 推送销售订单表单的 NC 单号字段（formmain_2817.field0329）
     */
    public static final String SALES_ORDER_FIELD_NO = "field0329";

    /**
     * CRM 推送销售订单表单的订单来源字段（formmain_2817.field0493）
     */
    public static final String SALES_ORDER_FIELD_SOURCE = "field0493";

    /**
     * 销售订单来源判定值：来源于 CRM 推送
     */
    public static final String ORDER_SOURCE_CRM = "crm订单";

    /**
     * 购销合同表的销售订单编号字段（formmain_5296.field0104）
     */
    public static final String CONTRACT_FIELD_CONTRACT_NO = "field0104";

    /**
     * 购销合同表的流程完成标记字段（formmain_5296.finishedflag，数值 1=已完成 0=未完成）
     */
    public static final String CONTRACT_FIELD_FINISHED_FLAG = "finishedflag";

    /**
     * 流程状态快照：已完成
     */
    public static final String FLOW_STATUS_FINISHED = "已完成";

    /**
     * 流程状态快照：未完成
     */
    public static final String FLOW_STATUS_UNFINISHED = "未完成";

    /**
     * 流程状态快照：未知
     */
    public static final String FLOW_STATUS_UNKNOWN = "未知";

    /**
     * Oracle IN/NOT IN 列表表达式上限（规避 ORA-01795）
     */
    public static final int MAX_NOT_IN_CHUNK = 1000;

    /**
     * 默认 NOT IN 分片大小
     */
    public static final int DEFAULT_SCAN_CHUNK_SIZE = 500;

    /**
     * 默认单次执行处理条数
     */
    public static final int DEFAULT_BATCH_SIZE = 200;

    /**
     * 默认 PROCESSING 超时回收阈值（分钟）
     */
    public static final int DEFAULT_PROCESSING_TIMEOUT_MINUTES = 10;

    /**
     * 业务跳过原因：OA 附件缺失（按附件引用未查到 ctp_attachment 记录）
     */
    public static final String SKIP_REASON_ATTACHMENT_MISSING = "OA附件缺失";

    /**
     * 同步说明/失败原因统一截断长度
     */
    public static final int MESSAGE_MAX_LENGTH = 500;

    /**
     * OA 附件下载路径模板（与 {@code OaConstants.ATTACHMENT_FILE_DOWNLOAD_PATH} 一致，
     * 独立声明避免 common 内跨包耦合）
     */
    public static final String ATTACHMENT_FILE_DOWNLOAD_PATH = "/seeyon/rest/attachment/file/";

    /**
     * 拼装 OA 附件下载地址（Q2 口径：不转存文件，直接回传 OA REST 下载地址）
     *
     * @param oaBaseUrl OA 基础地址（OaProperties.resolvedBaseUrl()，无末尾斜杠）
     * @param fileId    附件文件标识（ctp_attachment.file_url）
     * @return 形如 {oaBaseUrl}/seeyon/rest/attachment/file/{fileId} 的下载地址；
     * fileId 为空时返回 null
     */
    public static String buildAttachmentDownloadUrl(String oaBaseUrl, String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return null;
        }
        String base = oaBaseUrl == null || oaBaseUrl.isBlank()
                ? ""
                : oaBaseUrl.endsWith("/") ? oaBaseUrl.substring(0, oaBaseUrl.length() - 1) : oaBaseUrl;
        return base + ATTACHMENT_FILE_DOWNLOAD_PATH + fileId.trim();
    }

    /**
     * 按 FINISHEDFLAG 转换流程状态快照（对齐原查询 SQL 的 CASE 规则）
     *
     * @param finishedFlag OA 购销合同 FINISHEDFLAG 值，可为 null
     * @return 已完成 / 未完成 / 未知
     */
    public static String flowStatusOf(Integer finishedFlag) {
        if (finishedFlag == null) {
            return FLOW_STATUS_UNKNOWN;
        }
        return switch (finishedFlag) {
            case 1 -> FLOW_STATUS_FINISHED;
            case 0 -> FLOW_STATUS_UNFINISHED;
            default -> FLOW_STATUS_UNKNOWN;
        };
    }
}
