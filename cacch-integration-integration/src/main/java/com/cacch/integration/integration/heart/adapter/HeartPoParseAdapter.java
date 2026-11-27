package com.cacch.integration.integration.heart.adapter;

import com.cacch.integration.common.config.heart.HeartProperties;
import com.cacch.integration.common.constant.heart.HeartAiConstants;
import com.cacch.integration.common.constant.heart.HeartAiLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.heart.client.AiPoParseClient;
import com.cacch.integration.integration.heart.client.PdfTextPoParser;
import com.cacch.integration.integration.heart.client.dto.HeartPoInfo;
import com.cacch.integration.integration.heart.client.dto.HeartPoItemInfo;
import com.cacch.integration.integration.heart.client.dto.HeartPoParseResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Heart PO 解析适配器
 *
 * <p>编排 AI 解析与文本兜底，统一做一致性校验（金额 = 数量 × 单价、明细合计 = 总额）、
 * 置信度扣减与 SHA-256 结果缓存；不做业务规则校验（由 F5 负责）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartPoParseAdapter {

    private static final String LOG_BIZ = HeartAiLogConstants.LOG_BIZ;

    /**
     * 解析结果缓存：同一 PDF 重复投递不重复调用模型
     */
    private static final Cache<String, HeartPoParseResult> PARSE_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofDays(HeartAiConstants.CACHE_EXPIRE_DAYS))
            .maximumSize(HeartAiConstants.CACHE_MAX_SIZE)
            .build();

    private final HeartProperties heartProperties;

    private final AiPoParseClient aiPoParseClient;

    private final PdfTextPoParser pdfTextPoParser;

    /**
     * 解析 PO PDF
     *
     * @param pdfBytes PDF 字节
     * @param fileName 文件名（仅日志）
     * @return 解析结果（含置信度与告警）
     */
    public HeartPoParseResult parse(byte[] pdfBytes, String fileName) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "PDF 内容为空, 无法解析");
        }
        String sha256 = sha256(pdfBytes);
        String cacheKey = heartProperties.getAi().getPromptVersion() + ":" + sha256;
        HeartPoParseResult cached = PARSE_CACHE.getIfPresent(cacheKey);
        if (cached != null) {
            log.info("【{}】命中解析缓存, file={}, sha256={}", LOG_BIZ, fileName, sha256);
            return cached;
        }
        HeartPoParseResult result = parseWithFallback(pdfBytes, fileName);
        validate(result);
        PARSE_CACHE.put(cacheKey, result);
        log.info("【{}】{}", LOG_BIZ, HeartAiLogConstants.buildAction(
                "解析完成, file=" + fileName + ", parser=" + result.getParserType()
                        + ", confidence=" + result.getConfidence()
                        + ", warnings=" + result.getWarnings().size()));
        return result;
    }

    /**
     * 优先 AI 解析，置信度不足或异常时降级文本兜底
     *
     * @param pdfBytes PDF 字节
     * @param fileName 文件名（仅日志）
     * @return 解析结果
     */
    private HeartPoParseResult parseWithFallback(byte[] pdfBytes, String fileName) {
        HeartProperties.Ai ai = heartProperties.getAi();
        if (Boolean.TRUE.equals(ai.getEnabled())) {
            try {
                HeartPoParseResult aiResult = aiPoParseClient.parse(pdfBytes, fileName);
                if (aiResult.getConfidence() != null
                        && aiResult.getConfidence().compareTo(ai.getFallbackThreshold()) >= 0) {
                    return aiResult;
                }
                log.warn("【{}】AI 解析置信度不足, 降级文本兜底, file={}, confidence={}",
                        LOG_BIZ, fileName, aiResult.getConfidence());
            } catch (Exception e) {
                log.warn("【{}】AI 解析异常, 降级文本兜底, file={}, error={}",
                        LOG_BIZ, fileName, e.getMessage());
            }
        } else {
            log.info("【{}】AI 解析已关闭, 使用文本兜底, file={}", LOG_BIZ, fileName);
        }
        return pdfTextPoParser.parse(pdfBytes, fileName);
    }

    /**
     * 一致性校验（仅断言，不做取数计算）
     *
     * @param result 解析结果
     */
    private void validate(HeartPoParseResult result) {
        HeartPoInfo info = result.getPoInfo();
        if (info == null || !StringUtils.hasText(info.getPoNo())) {
            result.setParseSuccess(false);
            addWarning(result, "未识别到 PO 号");
            return;
        }
        result.setParseSuccess(true);
        if (info.getItems() == null || info.getItems().isEmpty()) {
            addWarning(result, "未识别到明细行");
            return;
        }
        BigDecimal itemsTotal = BigDecimal.ZERO;
        int index = 0;
        for (HeartPoItemInfo item : info.getItems()) {
            index++;
            if (item.getAmount() != null) {
                itemsTotal = itemsTotal.add(item.getAmount());
            }
            if (item.getQuantity() == null || item.getUnitPrice() == null || item.getAmount() == null) {
                continue;
            }
            BigDecimal expected = item.getQuantity().multiply(item.getUnitPrice());
            if (expected.subtract(item.getAmount()).abs().compareTo(HeartAiConstants.AMOUNT_TOLERANCE) > 0) {
                addWarning(result, "第 " + index + " 行金额与数量×单价不一致: " + item.getAmount() + " vs " + expected);
            }
        }
        if (info.getTotalAmount() != null
                && itemsTotal.subtract(info.getTotalAmount()).abs().compareTo(HeartAiConstants.AMOUNT_TOLERANCE) > 0) {
            addWarning(result, "明细金额合计 " + itemsTotal + " 与 PO 总额 " + info.getTotalAmount() + " 不一致");
        }
    }

    /**
     * 记录告警并扣减置信度
     *
     * @param result  解析结果
     * @param warning 告警内容
     */
    private void addWarning(HeartPoParseResult result, String warning) {
        result.getWarnings().add(warning);
        BigDecimal confidence = result.getConfidence() == null ? BigDecimal.ZERO : result.getConfidence();
        result.setConfidence(confidence.subtract(HeartAiConstants.CONFIDENCE_PENALTY).max(BigDecimal.ZERO));
    }

    /**
     * 计算 PDF SHA-256
     *
     * @param pdfBytes PDF 字节
     * @return 十六进制摘要
     */
    private String sha256(byte[] pdfBytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdfBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "计算 PDF 摘要失败", e);
        }
    }
}
