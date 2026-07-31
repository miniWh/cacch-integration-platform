package com.cacch.integration.common.constant.sharedrive;

/**
 * 共享盘常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class ShareDriveConstants {

    public static final String LOG_BIZ = "ShareDrive";

    /**
     * Windows/SMB 目录名非法字符（不含 %，共享盘 IPDP 目录名可含百分号）
     */
    public static final String FORBIDDEN_DIR_CHARS = "\\/:*?\"<>|\u0000";

    public static final String SKIP_PATH_COLLISION = "IPDP_PATH_COLLISION";

    /**
     * 资料项目目录内待上传文件名关键字（主文件名不含扩展名须包含此子串），默认 {@code 最终版本}
     */
    public static final String DEFAULT_FINAL_VERSION_SUFFIX = "最终版本";

    private ShareDriveConstants() {
    }
}
