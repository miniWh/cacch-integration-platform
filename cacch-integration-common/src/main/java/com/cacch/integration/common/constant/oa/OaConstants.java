package com.cacch.integration.common.constant.oa;

import com.cacch.integration.common.constant.redis.RedisConstants;

/**
 * 致远 OA REST 常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaConstants {

    private OaConstants() {
    }

    /**
     * 日志业务标识
     */
    public static final String LOG_BIZ = "OaOpenApi";

    /**
     * Token 请求 Header 名
     */
    public static final String TOKEN_HEADER = "token";

    /**
     * Token 获取路径模板：/seeyon/rest/token/{userName}/{password}
     */
    public static final String TOKEN_PATH = "/seeyon/rest/token/{userName}/{password}";

    /**
     * 按人员编码查询路径模板
     */
    public static final String ORG_MEMBER_BY_CODE_PATH = "/seeyon/rest/orgMembers/code/{code}";

    /**
     * 发起表单流程
     */
    public static final String BPM_PROCESS_START_PATH = "/seeyon/rest/bpm/process/start";

    /**
     * 流程状态查询路径模板
     */
    public static final String FLOW_STATE_PATH = "/seeyon/rest/flow/state/{flowId}";

    /**
     * 附件上传（multipart，form-data 字段名 {@code file}）
     */
    public static final String ATTACHMENT_UPLOAD_PATH = "/seeyon/rest/attachment";

    /**
     * multipart 上传表单字段名
     */
    public static final String ATTACHMENT_UPLOAD_FIELD = "file";

    /**
     * 默认协同应用名
     */
    public static final String APP_NAME_COLLABORATION = "collaboration";

    /**
     * 新建-发送（非待发）
     */
    public static final String DRAFT_SEND = "0";

    /**
     * 销售订单主表名
     */
    public static final String FORM_MAIN = "formmain_2817";

    /**
     * 物料清单子表名（本期使用）
     */
    public static final String FORM_SON_DETAIL = "formson_2819";

    /**
     * 按编码取人员默认页号
     */
    public static final int DEFAULT_PAGE_NO = 0;

    /**
     * 按编码取人员默认每页条数
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Token Redis Key 前缀：integration:oa:token:{loginName|_default}
     */
    public static final String TOKEN_REDIS_KEY_PREFIX = RedisConstants.KEY_PREFIX + "oa:token:";

    /**
     * 生成 Token Redis Key
     *
     * @param loginName 绑定登录名，可空
     * @return Redis Key
     */
    public static String tokenRedisKey(String loginName) {
        String suffix = (loginName == null || loginName.isBlank()) ? "_default" : loginName.trim();
        return TOKEN_REDIS_KEY_PREFIX + suffix;
    }
}
