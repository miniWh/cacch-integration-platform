package com.cacch.integration.integration.tencentmeeting.client;

import com.cacch.integration.common.config.tencentmeeting.TencentMeetingProperties;
import com.cacch.integration.integration.tencentmeeting.client.dto.TencentMeetingSmartMinutesResponse;
import com.tencentcloudapi.wemeet.Client;
import com.tencentcloudapi.wemeet.core.Config;
import com.tencentcloudapi.wemeet.core.Constants;
import com.tencentcloudapi.wemeet.core.authenticator.AuthenticatorBuilder;
import com.tencentcloudapi.wemeet.core.authenticator.JWTAuthenticator;
import com.tencentcloudapi.wemeet.core.exception.ClientException;
import com.tencentcloudapi.wemeet.core.exception.ServiceException;
import com.tencentcloudapi.wemeet.core.xhttp.ApiRequest;
import com.tencentcloudapi.wemeet.core.xhttp.ApiResponse;
import com.tencentcloudapi.wemeet.core.xhttp.Authentication;
import com.tencentcloudapi.wemeet.core.xhttp.DefaultHttpClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 腾讯会议 OpenAPI 客户端（基于官方 wemeet-openapi-sdk）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TencentMeetingClient {

    private final TencentMeetingProperties properties;

    private Config config;

    @PostConstruct
    void init() {
        if (!properties.isEnabled()) {
            log.info("【TencentMeeting】未启用腾讯会议 API（tencent-meeting.enabled=false）");
            return;
        }
        validateCredentials();
        this.config = buildConfig();
        log.info("【TencentMeeting】SDK 客户端已初始化, appId={}", properties.getAppId());
    }

    /**
     * 查询单个云录制的智能纪要
     *
     * @param recordFileId 录制文件 ID（来自企微 record/list）
     * @param operatorId   操作者 userid
     * @return 智能纪要响应
     */
    public TencentMeetingSmartMinutesResponse getSmartMinutes(String recordFileId, String operatorId)
            throws ClientException, ServiceException {
        ensureReady();
        if (!StringUtils.hasText(recordFileId)) {
            throw new IllegalArgumentException("recordFileId 不能为空");
        }
        if (!StringUtils.hasText(operatorId)) {
            throw new IllegalArgumentException("operatorId 不能为空");
        }
        log.info("【TencentMeeting】获取智能纪要, recordFileId={}, operatorId={}", recordFileId, operatorId);

        ApiRequest apiReq = new ApiRequest.Builder("/v1/smart/minutes/{record_file_id}").build();
        apiReq.getPathParams().set("record_file_id", recordFileId);
        apiReq.getQueryParams().set("operator_id", operatorId);
        apiReq.getQueryParams().set("operator_id_type", String.valueOf(properties.getOperatorIdType()));
        apiReq.getQueryParams().set("text_type", String.valueOf(properties.getSmartMinutes().getTextType()));
        apiReq.getQueryParams().set("llm", String.valueOf(properties.getSmartMinutes().getLlm()));
        if (StringUtils.hasText(properties.getSmartMinutes().getLang())) {
            apiReq.getQueryParams().set("lang", properties.getSmartMinutes().getLang());
        }
        apiReq.getAuthenticators().add(Constants.DEFAULT_AUTHENTICATOR);
        apiReq.getAuthenticators().add(buildJwtAuthenticator());

        ApiResponse apiRsp;
        try {
            apiRsp = config.getClt().get(apiReq);
        } catch (Exception e) {
            throw new ClientException("调用腾讯会议智能纪要接口失败", e);
        }
        if (apiRsp.getStatusCode() >= 300) {
            throw new ServiceException(apiRsp);
        }
        try {
            return apiRsp.translate(TencentMeetingSmartMinutesResponse.class, Constants.JSON_SERIALIZER);
        } catch (Exception e) {
            throw new ClientException("解析智能纪要响应失败", e);
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled() && config != null;
    }

    private void ensureReady() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("腾讯会议 API 未启用");
        }
        if (config == null) {
            throw new IllegalStateException("腾讯会议 SDK 未初始化，请检查 tencent-meeting 配置");
        }
    }

    private Authentication buildJwtAuthenticator() throws ClientException {
        BigInteger nonce = BigInteger.valueOf(Math.abs(new SecureRandom().nextInt()));
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        AuthenticatorBuilder<JWTAuthenticator> builder =
                new JWTAuthenticator.Builder().nonce(nonce).timestamp(timestamp);
        return builder.build(config);
    }

    private Config buildConfig() {
        Config sdkConfig = new Config();
        sdkConfig.setAppId(properties.getAppId());
        sdkConfig.setSdkId(properties.getSdkId());
        sdkConfig.setSecretID(properties.getSecretId());
        sdkConfig.setSecretKey(properties.getSecretKey());
        sdkConfig.setClt(new DefaultHttpClient.Builder(Constants.OPEN_API_DOMAIN)
                .withProtocol(Constants.DEFAULT_PROTOCOL)
                .withSerializer(Constants.DEFAULT_SERIALIZER)
                .build());
        new Client.Builder(sdkConfig).build();
        return sdkConfig;
    }

    private void validateCredentials() {
        if (!StringUtils.hasText(properties.getAppId())) {
            throw new IllegalStateException("tencent-meeting.app-id 未配置");
        }
        if (!StringUtils.hasText(properties.getSdkId())) {
            throw new IllegalStateException("tencent-meeting.sdk-id 未配置");
        }
        if (!StringUtils.hasText(properties.getSecretId())) {
            throw new IllegalStateException("tencent-meeting.secret-id 未配置");
        }
        if (!StringUtils.hasText(properties.getSecretKey())) {
            throw new IllegalStateException("tencent-meeting.secret-key 未配置");
        }
    }
}
