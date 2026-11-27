package com.cacch.integration.integration.heart.client;

import com.cacch.integration.common.config.heart.HeartProperties;
import com.cacch.integration.common.constant.heart.HeartAiConstants;
import com.cacch.integration.common.constant.heart.HeartAiLogConstants;
import com.cacch.integration.integration.heart.client.dto.HeartPoInfo;
import com.cacch.integration.integration.heart.client.dto.HeartPoParseResult;
import com.cacch.integration.integration.heart.support.HeartPdfTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heart PO PDF 文本兜底解析器
 *
 * <p>AI 不可用或置信度不足时启用：抽取文本层后按固定版式正则取关键字段，
 * 明细行不做猜测，统一低置信度转人工复核。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfTextPoParser implements IHeartPoParser {

    private static final String LOG_BIZ = HeartAiLogConstants.LOG_BIZ;

    /**
     * 兜底置信度：仅关键字段可读，必须人工复核
     */
    private static final BigDecimal FALLBACK_CONFIDENCE = new BigDecimal("0.5");

    private static final Pattern PO_NO_PATTERN = Pattern.compile(
            "(?i)\\bPO\\s*(?:NO\\.?|NUMBER)?\\s*[:：]?\\s*([A-Z]{1,4}[A-Z0-9\\-]{3,})");

    private static final Pattern TOTAL_AMOUNT_PATTERN = Pattern.compile(
            "(?i)TOTAL\\s*(?:AMOUNT)?\\s*[:：]?\\s*(?:USD|JPY|CNY|EUR)?\\s*([\\d,]+\\.\\d{2})");

    private static final Pattern ETD_PATTERN = Pattern.compile(
            "(?i)ETD\\s*[:：]?\\s*(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})");

    private final HeartProperties heartProperties;

    private final HeartPdfTextExtractor heartPdfTextExtractor;

    @Override
    public HeartPoParseResult parse(byte[] pdfBytes, String fileName) {
        long start = System.currentTimeMillis();
        String text = heartPdfTextExtractor.extract(pdfBytes, fileName,
                heartProperties.getAi().getMaxTextChars());

        HeartPoInfo poInfo = new HeartPoInfo();
        poInfo.setPoNo(findGroup(PO_NO_PATTERN, text));
        poInfo.setEtd(findGroup(ETD_PATTERN, text));
        String total = findGroup(TOTAL_AMOUNT_PATTERN, text);
        if (StringUtils.hasText(total)) {
            poInfo.setTotalAmount(new BigDecimal(total.replace(",", "")));
        }

        HeartPoParseResult result = new HeartPoParseResult();
        result.setParseSuccess(StringUtils.hasText(poInfo.getPoNo()));
        result.setConfidence(FALLBACK_CONFIDENCE);
        result.setParserType(HeartAiConstants.PARSER_TEXT_FALLBACK);
        result.setPromptVersion(heartProperties.getAi().getPromptVersion());
        result.setDurationMs(System.currentTimeMillis() - start);
        result.setPoInfo(poInfo);
        result.getWarnings().add("AI 解析不可用, 已降级文本兜底, 明细行需人工补录");

        log.info("【{}】文本兜底解析完成, file={}, poNo={}, durationMs={}",
                LOG_BIZ, fileName, poInfo.getPoNo(), result.getDurationMs());
        return result;
    }

    @Override
    public String getParserType() {
        return HeartAiConstants.PARSER_TEXT_FALLBACK;
    }

    /**
     * 取第一个匹配分组
     *
     * @param pattern 正则
     * @param text    文本
     * @return 匹配值，未命中返回 null
     */
    private String findGroup(Pattern pattern, String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
}
