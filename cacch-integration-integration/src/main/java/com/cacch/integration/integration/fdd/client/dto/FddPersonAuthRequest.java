package com.cacch.integration.integration.fdd.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * 法大大个人实名认证 URL 请求体
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FddPersonAuthRequest {

    /**
     * 法大大本地用户唯一标识（与 tpAccountId 二选一；创建用户后必传本字段）
     */
    private String accountId;

    /**
     * 第三方用户唯一标识（与 accountId 二选一；无 accountId 时使用）
     */
    private String tpAccountId;

    /**
     * 认证渠道，固定 0
     */
    private Integer verifiedChannel;

    /**
     * 验证方案，默认 0（三要素标准方案）
     */
    private Integer verifiedWay;

    /**
     * 1 首次认证，2 重新认证
     */
    private Integer verifiedType;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 证件类型：0 身份证
     */
    private String certType;

    /**
     * 证件号码
     */
    private String idCard;

    /**
     * 异步回调地址
     */
    private String notifyUrl;

    /**
     * 是否发送短信：0 否
     */
    private Integer isSendSms;

    /**
     * 是否支持其他证件：0 仅身份证
     */
    private Integer otherCertType;

    /**
     * 是否小程序：0 非小程序
     */
    private Integer miniProgram;
}
