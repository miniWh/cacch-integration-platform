package com.cacch.integration.integration.heart.client;

import com.cacch.integration.common.config.heart.HeartProperties;
import com.cacch.integration.common.constant.heart.HeartAiConstants;
import com.cacch.integration.common.constant.heart.HeartAiLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.heart.client.dto.HeartPoInfo;
import com.cacch.integration.integration.heart.client.dto.HeartPoParseResult;
import com.cacch.integration.integration.heart.support.HeartPdfPageRenderer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Heart PO AI 解析客户端（OpenAI 兼容对话补全接口）
 *
 * <p>链路：PDF 文本层抽取 → 文本模型结构化抽取 → JSON 解析。
 * 模型只做抄录，禁止计算与推断；密钥仅从配置（环境变量）读取，任何日志不得输出。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiPoParseClient implements IHeartPoParser {

    private static final String LOG_BIZ = HeartAiLogConstants.LOG_BIZ;

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    /**
     * 系统提示词：固定 JSON Schema 与抄录约束
     */
    private static final String SYSTEM_PROMPT = """
            你是采购订单（PO）结构化抽取器，只做抄录，禁止任何计算、四舍五入、单位换算或推断。
            请从用户提供的 PO 图片（可能为 PDF 扫描件，可能多页）中抽取字段，严格输出 JSON 对象，不要输出任何解释文字。
            
            输出 JSON 结构：
            {
              "poNo": "PO 号，找不到填 null",
              "poDate": "PO 日期 yyyy-MM-dd，找不到填 null",
              "customerName": "客户名称",
              "tradeTerms": "贸易条款，如 FOB/CIF",
              "portOfLoading": "起运港",
              "portOfDischarge": "目的港",
              "shippingLine": "船公司",
              "containerType": "柜型，如 20GP/40HQ",
              "packingType": "包装方式",
              "etd": "预计开船日 yyyy-MM-dd",
              "currency": "币种，如 USD",
              "totalAmount": 0.00,
              "remark": "整单备注",
              "items": [
                {
                  "lineNo": 1,
                  "productName": "产品名称原文",
                  "productCode": "产品编码，找不到填 null",
                  "quantity": 0,
                  "unit": "计量单位",
                  "unitPrice": 0.00,
                  "amount": 0.00,
                  "cartons": 0,
                  "unitsPerCarton": 0,
                  "volume": 0,
                  "remark": "行备注"
                }
              ],
              "confidence": 0.0,
              "warnings": ["字段缺失或文本模糊的说明"]
            }
            
            规则：
            1. 只抄录原文，禁止计算、四舍五入、补全或改写；
            2. 找不到的字段填 null，禁止编造；
            3. 数字不带千分位与货币符号；
            4. confidence 为整体抽取置信度（0~1），字段缺失或文本模糊时降低；
            5. 只输出 JSON，不要输出 markdown 代码块标记。
            """;

    private final HeartProperties heartProperties;

    private final HeartPdfPageRenderer heartPdfPageRenderer;

    private RestTemplate restTemplate;

    /**
     * 按配置初始化带超时的 RestTemplate
     */
    @PostConstruct
    void initRestTemplate() {
        HeartProperties.Ai ai = heartProperties.getAi();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(ai.getTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(ai.getTimeoutMs()));
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public HeartPoParseResult parse(byte[] pdfBytes, String fileName) {
        HeartProperties.Ai ai = heartProperties.getAi();
        validateConfig(ai);
        long start = System.currentTimeMillis();
        List<byte[]> pageImages = heartPdfPageRenderer.render(pdfBytes, fileName, ai.getRenderDpi(), ai.getMaxPages());
        if (pageImages.isEmpty()) {
            log.info("【{}】解析终止, reason=PDF 无可用页面, file={}", LOG_BIZ, fileName);
            throw new BizException(ResultCode.INTEGRATION_ERROR, "PDF 无可用页面, 无法解析");
        }
        String responseBody = requestModel(ai, pageImages, fileName);
        return convertResponse(ai, responseBody, fileName, start);
    }

    @Override
    public String getParserType() {
        return HeartAiConstants.PARSER_AI;
    }

    /**
     * 校验模型配置
     *
     * @param ai 解析配置
     */
    private void validateConfig(HeartProperties.Ai ai) {
        if (!StringUtils.hasText(ai.getBaseUrl()) || !StringUtils.hasText(ai.getApiKey())
                || !StringUtils.hasText(ai.getModel())) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "AI 解析配置缺失: base-url / api-key / model 均不可为空");
        }
    }

    /**
     * 调用模型（带退避重试）
     *
     * @param ai         解析配置
     * @param pageImages PDF 每页 PNG 图片
     * @param fileName   文件名（仅日志）
     * @return 模型响应体
     */
    private String requestModel(HeartProperties.Ai ai, List<byte[]> pageImages, String fileName) {
        String url = StringUtils.trimTrailingCharacter(ai.getBaseUrl().trim(), '/')
                + HeartAiConstants.CHAT_COMPLETIONS_PATH;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(ai.getApiKey());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", ai.getModel());
        body.put("temperature", ai.getTemperature());
        body.put("response_format", Map.of("type", "json_object"));
        List<Object> userContent = new ArrayList<>();
        userContent.add(Map.of("type", "text", "text",
                "PO 文件名：" + fileName + "，共 " + pageImages.size() + " 页，请逐页读取并合并为一张采购订单。"));
        for (byte[] pageImage : pageImages) {
            String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(pageImage);
            userContent.add(Map.of("type", "image_url", "image_url", Map.of("url", dataUri)));
        }
        body.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userContent)));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        int attempts = Math.max(1, ai.getRetryTimes() + 1);
        RuntimeException lastError = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                if (response.getStatusCode() == HttpStatus.OK && StringUtils.hasText(response.getBody())) {
                    return response.getBody();
                }
                lastError = new RestClientException("模型响应异常, httpStatus=" + response.getStatusCode());
            } catch (Exception e) {
                lastError = new RestClientException("模型调用失败: " + e.getMessage(), e);
            }
            log.warn("【{}】模型调用失败, 第 {}/{} 次, file={}, error={}",
                    LOG_BIZ, i, attempts, fileName, lastError.getMessage());
            if (i < attempts) {
                sleep(HeartAiConstants.RETRY_BACKOFF_MS * i, fileName);
            }
        }
        throw new BizException(ResultCode.INTEGRATION_ERROR,
                "AI PO 解析调用失败: " + lastError.getMessage(), lastError);
    }

    /**
     * 响应 JSON → 解析结果
     *
     * @param ai           解析配置
     * @param responseBody 模型响应体
     * @param fileName     文件名（仅日志）
     * @param start        开始时间戳
     * @return 解析结果
     */
    private HeartPoParseResult convertResponse(HeartProperties.Ai ai, String responseBody,
                                               String fileName, long start) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            String json = stripJsonFence(content);
            if (!StringUtils.hasText(json)) {
                throw new BizException(ResultCode.INTEGRATION_ERROR, "AI 返回内容为空");
            }
            JsonNode data = OBJECT_MAPPER.readTree(json);
            HeartPoInfo poInfo = OBJECT_MAPPER.treeToValue(data, HeartPoInfo.class);

            HeartPoParseResult result = new HeartPoParseResult();
            result.setParseSuccess(poInfo != null && StringUtils.hasText(poInfo.getPoNo()));
            result.setConfidence(resolveConfidence(data));
            result.setParserType(HeartAiConstants.PARSER_AI);
            result.setModelName(ai.getModel());
            result.setPromptVersion(ai.getPromptVersion());
            result.setDurationMs(System.currentTimeMillis() - start);
            result.setPoInfo(poInfo);
            result.setRawJson(json);
            result.getWarnings().addAll(resolveWarnings(data));

            int totalTokens = root.path("usage").path("total_tokens").asInt(-1);
            log.info("【{}】AI 解析完成, file={}, durationMs={}, confidence={}, tokens={}",
                    LOG_BIZ, fileName, result.getDurationMs(),
                    result.getConfidence(), totalTokens);
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "AI 返回内容解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析置信度
     *
     * @param data 模型 JSON
     * @return 置信度
     */
    private BigDecimal resolveConfidence(JsonNode data) {
        JsonNode node = data.path("confidence");
        if (!node.isNumber()) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(node.asDouble());
    }

    /**
     * 解析告警数组
     *
     * @param data 模型 JSON
     * @return 告警列表
     */
    private List<String> resolveWarnings(JsonNode data) {
        List<String> warnings = new ArrayList<>();
        JsonNode node = data.path("warnings");
        if (node.isArray()) {
            node.forEach(item -> {
                if (StringUtils.hasText(item.asText())) {
                    warnings.add(item.asText());
                }
            });
        }
        return warnings;
    }

    /**
     * 去掉 ```json 代码块包裹
     *
     * @param content 模型返回内容
     * @return 纯 JSON
     */
    private String stripJsonFence(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String json = content.trim();
        if (json.startsWith("```")) {
            int firstLineEnd = json.indexOf('\n');
            json = firstLineEnd < 0 ? "" : json.substring(firstLineEnd + 1);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        return json.trim();
    }

    /**
     * 退避等待
     *
     * @param millis   等待毫秒数
     * @param fileName 文件名（仅日志）
     */
    private void sleep(long millis, String fileName) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("【{}】重试等待被中断, file={}", LOG_BIZ, fileName);
        }
    }
}
