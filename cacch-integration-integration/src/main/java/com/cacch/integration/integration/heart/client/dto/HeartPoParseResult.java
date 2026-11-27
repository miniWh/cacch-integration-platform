package com.cacch.integration.integration.heart.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Heart PO 解析结果
 *
 * <p>承载解析主体、置信度、解析器信息与告警，供规则校验层路由使用。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class HeartPoParseResult {

    /**
     * 是否解析出结构化主体（PO 号非空）
     */
    private boolean parseSuccess;

    /**
     * 整体置信度（0~1）
     */
    private BigDecimal confidence;

    /**
     * 解析器类型：AI / TEXT_FALLBACK
     */
    private String parserType;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 提示词版本
     */
    private String promptVersion;

    /**
     * 解析耗时（毫秒）
     */
    private long durationMs;

    /**
     * 告警（字段缺失、交叉校验不一致、降级说明等）
     */
    private List<String> warnings = new ArrayList<>();

    /**
     * 结构化主体
     */
    private HeartPoInfo poInfo;

    /**
     * 模型返回的原始 JSON（落库 parse_payload 用）
     */
    private String rawJson;
}
