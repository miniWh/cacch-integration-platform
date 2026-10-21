package com.cacch.integration.integration.keruyun.support;

import com.cacch.integration.common.config.keruyun.KeruyunProperties;
import com.cacch.integration.common.constant.keruyun.KeruyunConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * 客如云 V2 签名支撑
 *
 * <p>签名规则：</p>
 * <ol>
 *     <li>公共参数（sign 除外）按参数名 ASCII 升序拼接为 {@code k1v1k2v2...}；</li>
 *     <li>获取 token：拼接公共参数 + secretKey；业务请求：拼接公共参数 + body（非空时）+ token；</li>
 *     <li>对拼接结果做 SHA-256，输出小写 hex。</li>
 * </ol>
 *
 * <p>签名原文包含 secretKey 与 token，<strong>禁止打印到日志</strong>。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
@RequiredArgsConstructor
public class KeruyunSignSupport {

    private final KeruyunProperties keruyunProperties;

    /**
     * 生成秒级时间戳（客如云服务端校验与当前时间差不超过 300 秒）
     *
     * @return 秒级时间戳字符串
     */
    public String currentTimestampSeconds() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    /**
     * 构造参与签名的公共参数（按参数名 ASCII 升序）
     *
     * @param timestamp 秒级时间戳，不可为空
     * @return 保序公共参数 Map，key 依次为 appKey / shopIdenty / timestamp / version
     */
    public Map<String, String> buildPublicParams(String timestamp) {
        Map<String, String> params = new TreeMap<>();
        params.put(KeruyunConstants.PARAM_APP_KEY, keruyunProperties.getAppKey());
        params.put(KeruyunConstants.PARAM_SHOP_IDENTY, keruyunProperties.getShopIdenty());
        params.put(KeruyunConstants.PARAM_TIMESTAMP, timestamp);
        params.put(KeruyunConstants.PARAM_VERSION, keruyunProperties.getVersion());
        return params;
    }

    /**
     * 计算获取 token 接口的签名：SHA-256(公共参数串 + secretKey)
     *
     * @param publicParams 公共参数（不含 sign）
     * @return 64 位小写 hex 签名
     * @throws IllegalStateException secretKey 未配置时抛出
     */
    public String signForToken(Map<String, String> publicParams) {
        if (!StringUtils.hasText(keruyunProperties.getAppSecret())) {
            throw new IllegalStateException("客如云 appSecret 未配置，无法计算 token 签名");
        }
        return sha256(concatParams(publicParams) + keruyunProperties.getAppSecret());
    }

    /**
     * 计算业务接口签名：SHA-256(公共参数串 + body[非空时] + token)
     *
     * @param compactBody  实际发送的请求体 JSON（已压缩空白），无请求体时传 null 或空串
     * @param publicParams 公共参数（不含 sign）
     * @param token        门店 token；token 阶段传 secretKey 请使用 {@link #signForToken(Map)}
     * @return 64 位小写 hex 签名
     */
    public String signForRequest(String compactBody, Map<String, String> publicParams, String token) {
        StringBuilder source = new StringBuilder(concatParams(publicParams));
        if (StringUtils.hasText(compactBody) && !"{}".equals(compactBody)) {
            source.append(KeruyunConstants.BODY_PREFIX).append(compactBody);
        }
        if (token != null) {
            source.append(token);
        }
        return sha256(source.toString());
    }

    /**
     * 压缩 JSON 报文：仅删除字符串字面量之外的空白、换行、回车、制表符
     *
     * <p>字符串内的空格必须保留（如 {@code 2025-07-20 04:30:00}），否则签名与原报文不一致。</p>
     *
     * @param json 原始 JSON 字符串
     * @return 压缩后的 JSON；入参为空时原样返回
     */
    public static String compactJson(String json) {
        if (!StringUtils.hasText(json)) {
            return json;
        }
        StringBuilder sb = new StringBuilder(json.length());
        boolean inQuote = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                sb.append(c);
            } else if (inQuote) {
                sb.append(c);
            } else if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 按 key ASCII 升序拼接 {@code k1v1k2v2...}，忽略 null key/value
     */
    private static String concatParams(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        if (params == null || params.isEmpty()) {
            return sb.toString();
        }
        new TreeMap<>(params).forEach((key, value) -> {
            if (key != null && value != null) {
                sb.append(key).append(value);
            }
        });
        return sb.toString();
    }

    /**
     * SHA-256 哈希，输出小写 hex
     */
    private static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
