package com.cacch.integration.integration.oa.client;

import com.cacch.integration.common.config.oa.OaProperties;
import com.cacch.integration.common.constant.oa.OaConstants;
import com.cacch.integration.integration.oa.client.dto.OaFileUploadResult;
import com.cacch.integration.integration.oa.client.dto.OaProcessStartRequest;
import com.cacch.integration.integration.oa.client.dto.OaTokenResponse;
import com.cacch.integration.integration.oa.support.OaResponseSupport;
import com.cacch.integration.integration.support.ThirdPartyHttpLogSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 致远 OA REST HTTP 客户端
 *
 * <p>Token 以外的业务请求统一在 Header 中携带 {@code token}；入参/出参走 {@link ThirdPartyHttpLogSupport}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OaClient {

    private static final String BIZ = OaConstants.LOG_BIZ;

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final RestTemplate restTemplate;
    private final OaProperties oaProperties;

    /**
     * 获取 Rest Token
     *
     * @param loginName 绑定 OA 登录名，可空（不传 loginName 查询参数）
     * @return Token 字符串
     * @throws RestClientException 网络或响应解析失败
     */
    public String fetchToken(String loginName) {
        String action = "获取 Token";
        String userName = oaProperties.getRestUserName();
        String password = oaProperties.getRestPassword();
        if (!StringUtils.hasText(userName) || !StringUtils.hasText(password)) {
            throw new RestClientException("致远 OA REST 账号未配置（oa.rest-user-name / oa.rest-password）");
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(oaProperties.resolvedBaseUrl() + OaConstants.TOKEN_PATH);
        if (StringUtils.hasText(loginName)) {
            builder.queryParam("loginName", loginName.trim());
        }
        URI uri = builder.buildAndExpand(userName, password).encode().toUri();
        String logUrl = maskPasswordInUrl(uri.toString(), password);

        Map<String, String> requestLog = ThirdPartyHttpLogSupport.queryParams(
                "loginName", StringUtils.hasText(loginName) ? loginName.trim() : "");
        ThirdPartyHttpLogSupport.logRequest(BIZ, action, logUrl, requestLog);

        try {
            HttpHeaders headers = jsonHeaders();
            ResponseEntity<String> entity = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String body = entity.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, action, body);
            String token = parseToken(body);
            if (!StringUtils.hasText(token)) {
                throw new RestClientException("致远 OA Token 响应为空或无法解析");
            }
            log.info("【{}】{}成功, loginName={}", BIZ, action,
                    StringUtils.hasText(loginName) ? loginName.trim() : "_default");
            return token;
        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, action, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, action, e);
            throw e;
        }
    }

    /**
     * 按人员编码查询人员信息
     *
     * @param token    Rest Token，不可为空
     * @param code     人员编号（CRM emp_code），不可为空
     * @param pageNo   页号
     * @param pageSize 每页条数
     * @return 原始 JSON 节点
     * @throws RestClientException 网络或 HTTP 错误
     */
    public JsonNode getOrgMembersByCode(String token, String code, int pageNo, int pageSize) {
        String action = "按编码取人员";
        URI uri = UriComponentsBuilder
                .fromUriString(oaProperties.resolvedBaseUrl() + OaConstants.ORG_MEMBER_BY_CODE_PATH)
                .queryParam("pageNo", pageNo)
                .queryParam("pageSize", pageSize)
                .buildAndExpand(Map.of("code", code))
                .encode()
                .toUri();
        return exchangeForJson(action, HttpMethod.GET, uri, token, null);
    }

    /**
     * 发起表单流程
     *
     * @param token   Rest Token，不可为空
     * @param request 发起请求（含主表/子表字段）
     * @return 原始响应 JSON
     * @throws RestClientException 网络或 HTTP 错误
     */
    public JsonNode startProcess(String token, OaProcessStartRequest request) {
        String action = "发起表单流程";
        Map<String, Object> body = request.toSeeyonBody(oaProperties.getTemplateCode());
        URI uri = URI.create(oaProperties.resolvedBaseUrl() + OaConstants.BPM_PROCESS_START_PATH);
        return exchangeForJson(action, HttpMethod.POST, uri, token, body);
    }

    /**
     * 查询流程状态
     *
     * @param token  Rest Token，不可为空
     * @param flowId 流程实例 ID，不可为空
     * @return 原始响应 JSON
     * @throws RestClientException 网络或 HTTP 错误
     */
    public JsonNode getFlowState(String token, String flowId) {
        String action = "查询流程状态";
        URI uri = UriComponentsBuilder
                .fromUriString(oaProperties.resolvedBaseUrl() + OaConstants.FLOW_STATE_PATH)
                .buildAndExpand(Map.of("flowId", flowId))
                .encode()
                .toUri();
        return exchangeForJson(action, HttpMethod.GET, uri, token, null);
    }

    /**
     * 上传附件文件
     *
     * <p>POST {@code /seeyon/rest/attachment}，multipart 字段名 {@code file}；响应 {@code atts[0].fileUrl} 为文件 ID。</p>
     *
     * @param token       Rest Token，不可为空
     * @param fileBytes   文件内容，不可为空
     * @param fileName    原始文件名，不可为空
     * @param contentType MIME 类型，可空
     * @return 上传结果，含 fileUrl
     * @throws RestClientException 网络、HTTP 非 2xx 或响应无法解析 fileUrl 时抛出
     */
    public OaFileUploadResult uploadAttachment(String token, byte[] fileBytes, String fileName, String contentType) {
        String action = "上传附件";
        if (fileBytes == null || fileBytes.length == 0) {
            throw new RestClientException("致远 OA 上传附件失败：文件内容为空");
        }
        if (!StringUtils.hasText(fileName)) {
            throw new RestClientException("致远 OA 上传附件失败：文件名为空");
        }
        URI uri = URI.create(oaProperties.resolvedBaseUrl() + OaConstants.ATTACHMENT_UPLOAD_PATH);

        Map<String, Object> requestLog = Map.of(
                "fileName", fileName.trim(),
                "byteLength", fileBytes.length,
                "contentType", StringUtils.hasText(contentType) ? contentType.trim() : "");
        ThirdPartyHttpLogSupport.logRequest(BIZ, action, uri.toString(), requestLog);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.add(OaConstants.TOKEN_HEADER, token);

            org.springframework.core.io.ByteArrayResource fileResource =
                    new org.springframework.core.io.ByteArrayResource(fileBytes) {
                        @Override
                        public String getFilename() {
                            return fileName.trim();
                        }
                    };

            org.springframework.util.LinkedMultiValueMap<String, Object> body =
                    new org.springframework.util.LinkedMultiValueMap<>();
            body.add(OaConstants.ATTACHMENT_UPLOAD_FIELD, fileResource);

            HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                    new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, action, responseBody);

            if (responseBody == null || responseBody.isBlank()) {
                log.info("【{}】{}终止, reason=响应体为空", BIZ, action);
                throw new RestClientException("致远 OA 上传附件响应体为空");
            }
            JsonNode json = OBJECT_MAPPER.readTree(responseBody);
            String fileUrl = OaResponseSupport.extractFileUrl(json);
            if (!StringUtils.hasText(fileUrl)) {
                log.info("【{}】{}终止, reason=响应未包含 fileUrl", BIZ, action);
                throw new RestClientException("致远 OA 上传附件响应未包含 fileUrl");
            }
            log.info("【{}】{}成功, fileName={}, fileUrl={}", BIZ, action, fileName.trim(), fileUrl);
            return new OaFileUploadResult(fileUrl.trim(), fileName.trim(), json);
        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, action, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, action, e);
            throw e;
        } catch (Exception e) {
            log.info("【{}】{}终止, reason={}", BIZ, action, e.getMessage());
            log.error("【{}】{} 处理失败", BIZ, action, e);
            throw new RestClientException("致远 OA 上传附件处理失败: " + e.getMessage(), e);
        }
    }

    private JsonNode exchangeForJson(String action, HttpMethod method, URI uri,
                                     String token, Object body) {
        ThirdPartyHttpLogSupport.logRequest(BIZ, action, uri.toString(), body);
        try {
            HttpHeaders headers = jsonHeaders();
            headers.add(OaConstants.TOKEN_HEADER, token);
            HttpEntity<?> entity = body == null
                    ? new HttpEntity<>(headers)
                    : new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, method, entity, String.class);
            String responseBody = response.getBody();
            ThirdPartyHttpLogSupport.logResponse(BIZ, action, responseBody);
            if (responseBody == null || responseBody.isBlank()) {
                log.info("【{}】{}终止, reason=响应体为空", BIZ, action);
                throw new RestClientException("致远 OA " + action + " 响应体为空");
            }
            return OBJECT_MAPPER.readTree(responseBody);
        } catch (RestClientException e) {
            log.info("【{}】{}终止, reason={}", BIZ, action, e.getMessage());
            log.error("【{}】{} HTTP 调用失败", BIZ, action, e);
            throw e;
        } catch (Exception e) {
            log.info("【{}】{}终止, reason={}", BIZ, action, e.getMessage());
            log.error("【{}】{} 处理失败", BIZ, action, e);
            throw new RestClientException("致远 OA " + action + " 处理失败: " + e.getMessage(), e);
        }
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String parseToken(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("{")) {
            OaTokenResponse response = OBJECT_MAPPER.readValue(trimmed, OaTokenResponse.class);
            return response.resolveToken();
        }
        // 纯文本 Token
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String encodePath(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    private static String maskPasswordInUrl(String url, String password) {
        if (!StringUtils.hasText(url) || !StringUtils.hasText(password)) {
            return url;
        }
        String encoded = encodePath(password);
        return url.replace(encoded, "****").replace(password, "****");
    }
}
