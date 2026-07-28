package com.cacch.integration.common.constant.oa;

/**
 * 国内登记报告附件同步常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaRegReportConstants {

    public static final String LOG_BIZ = "OaRegAttachmentSync";

    public static final int DEFAULT_BATCH_SIZE = 50;

    public static final int DEFAULT_MAX_RETRY = 3;

    public static final String SKIP_MISSING_DIR = "MISSING_DIR";

    public static final String SKIP_NO_FILE = "NO_FILE";

    public static final String SKIP_IDEMPOTENT = "ALREADY_SYNCED";

    private OaRegReportConstants() {
    }
}
