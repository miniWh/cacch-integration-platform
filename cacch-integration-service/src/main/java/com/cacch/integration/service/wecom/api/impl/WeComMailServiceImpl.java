package com.cacch.integration.service.wecom.api.impl;

import com.cacch.integration.common.constant.wecom.WeComMailConstants;
import com.cacch.integration.common.constant.wecom.WeComMailLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.wecom.client.WeComMailClient;
import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListRequest;
import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListResponse;
import com.cacch.integration.service.wecom.api.IWeComMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * 企微邮件服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComMailServiceImpl implements IWeComMailService {

    private final WeComMailClient weComMailClient;

    @Override
    public WeComMailListResponse getMailList(String accessToken, WeComMailListRequest request) {
        if (!StringUtils.hasText(accessToken)) {
            log.info("【{}】获取收件箱邮件列表终止, reason=access_token为空", WeComMailLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_INVALID, "access_token 不能为空");
        }
        if (request == null || request.getBeginTime() == null || request.getEndTime() == null) {
            log.info("【{}】获取收件箱邮件列表终止, reason=begin_time/end_time为空", WeComMailLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_INVALID, "begin_time 与 end_time 不能为空");
        }
        if (request.getBeginTime() > request.getEndTime()) {
            log.info("【{}】获取收件箱邮件列表终止, reason=begin_time大于end_time", WeComMailLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_INVALID, "begin_time 不能大于 end_time");
        }
        normalizeRequest(request);
        try {
            WeComMailListResponse response = weComMailClient.getMailList(accessToken.trim(), request);
            if (!response.isSuccess()) {
                log.info("【{}】获取收件箱邮件列表终止, errcode={}, errmsg={}",
                        WeComMailLogConstants.LOG_BIZ, response.getErrCode(), response.getErrMsg());
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        String.format("企业微信获取收件箱邮件列表失败, errcode=%d, errmsg=%s",
                                response.getErrCode(), response.getErrMsg()));
            }
            if (response.getMailList() == null) {
                response.setMailList(List.of());
            }
            log.info("【{}】获取收件箱邮件列表成功, size={}, hasMore={}",
                    WeComMailLogConstants.LOG_BIZ, response.getMailList().size(), response.getHasMore());
            return response;
        } catch (BizException e) {
            log.error("【{}】获取收件箱邮件列表失败", WeComMailLogConstants.LOG_BIZ, e);
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】获取收件箱邮件列表终止, reason={}", WeComMailLogConstants.LOG_BIZ, e.getMessage());
            log.error("【{}】获取收件箱邮件列表调用失败", WeComMailLogConstants.LOG_BIZ, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "企业微信获取收件箱邮件列表调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 参数兜底：limit 缺省 100、超上限取 1000；cursor 空白视为未传
     *
     * @param request 查询条件
     */
    private void normalizeRequest(WeComMailListRequest request) {
        Integer limit = request.getLimit();
        if (limit == null || limit < 1) {
            request.setLimit(WeComMailConstants.DEFAULT_LIMIT);
        } else if (limit > WeComMailConstants.MAX_LIMIT) {
            request.setLimit(WeComMailConstants.MAX_LIMIT);
        }
        if (!StringUtils.hasText(request.getCursor())) {
            request.setCursor(null);
        }
    }
}
