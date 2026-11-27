package com.cacch.integration.common.constant.heart;

import java.math.BigDecimal;

/**
 * Heart PO 智能解析业务参数常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class HeartAiConstants {

    private HeartAiConstants() {
    }

    /**
     * 提示词版本（变更提示词必须同步升级，便于回溯解析口径）
     */
    public static final String PROMPT_VERSION = "heart-po-v1";

    /**
     * OpenAI 兼容对话补全接口路径
     */
    public static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    /**
     * 自动通过置信度阈值
     */
    public static final BigDecimal CONFIDENCE_THRESHOLD = new BigDecimal("0.9");

    /**
     * 降级置信度阈值（低于该值转文本兜底）
     */
    public static final BigDecimal FALLBACK_THRESHOLD = new BigDecimal("0.7");

    /**
     * 金额比对容差
     */
    public static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    /**
     * 置信度扣减步长（每条告警扣一次）
     */
    public static final BigDecimal CONFIDENCE_PENALTY = new BigDecimal("0.05");

    /**
     * 解析器类型：AI 模型
     */
    public static final String PARSER_AI = "AI";

    /**
     * 解析器类型：PDF 文本兜底
     */
    public static final String PARSER_TEXT_FALLBACK = "TEXT_FALLBACK";

    /**
     * 重试退避基础毫秒数（第 n 次重试等待 n 倍）
     */
    public static final long RETRY_BACKOFF_MS = 2000L;

    /**
     * 缓存过期天数
     */
    public static final long CACHE_EXPIRE_DAYS = 7L;

    /**
     * 缓存最大条目数
     */
    public static final long CACHE_MAX_SIZE = 1000L;

    /**
     * 默认单次调用超时毫秒数
     */
    public static final int DEFAULT_TIMEOUT_MS = 60000;

    /**
     * 默认重试次数
     */
    public static final int DEFAULT_RETRY_TIMES = 2;

    /**
     * 默认送入模型的最大文本字符数
     */
    public static final int DEFAULT_MAX_TEXT_CHARS = 100000;

    /**
     * 默认 PDF 渲染 DPI（扫描件走图片通道）
     */
    public static final int DEFAULT_RENDER_DPI = 150;

    /**
     * 默认单次最多渲染页数
     */
    public static final int DEFAULT_MAX_PAGES = 10;
}
