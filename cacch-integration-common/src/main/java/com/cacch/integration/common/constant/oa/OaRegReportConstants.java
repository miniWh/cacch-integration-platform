package com.cacch.integration.common.constant.oa;

/**
 * 国内登记报告附件同步常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaRegReportConstants {

    public static final String LOG_BIZ = "OaRegAttachmentSync";

    public static final int DEFAULT_BATCH_SIZE = 50;

    public static final int DEFAULT_FORM_BATCH_SIZE = 20;

    public static final int DEFAULT_MAX_RETRY = 3;

    public static final String SKIP_MISSING_DIR = "MISSING_DIR";

    public static final String SKIP_NO_FILE = "NO_FILE";

    public static final String SKIP_OWNER_UNRESOLVED = "OWNER_UNRESOLVED";

    public static final String SKIP_OWNER_FILTER = "OWNER_FILTER";

    public static final String SKIP_IDEMPOTENT = "ALREADY_SYNCED";

    /**
     * OA 库访问策略说明（只读 SELECT，禁止 JDBC 写库）
     */
    public static final String OA_DB_READ_ONLY_POLICY =
            "OA 库仅允许 SELECT 只读查询；业务写操作须走 OA REST（upload / batch-update 等）";

    private OaRegReportConstants() {
    }
}
