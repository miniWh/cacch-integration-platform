package com.cacch.integration.service.heart.api.impl;

import com.cacch.integration.common.constant.heart.HeartMailConstants;
import com.cacch.integration.common.constant.heart.HeartMailLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.heart.client.HeartMailClient;
import com.cacch.integration.integration.heart.client.dto.HeartMailInfo;
import com.cacch.integration.service.heart.api.IHeartMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Heart 邮件拉取服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartMailServiceImpl implements IHeartMailService {

    private static final String LOG_BIZ = HeartMailLogConstants.LOG_BIZ;

    private final HeartMailClient heartMailClient;

    @Override
    public List<HeartMailInfo> queryMails(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDate today = LocalDate.now();
        LocalDateTime queryStart = startTime == null ? today.atStartOfDay() : startTime;
        LocalDateTime queryEnd = endTime == null ? today.atTime(HeartMailConstants.DEFAULT_END_TIME) : endTime;
        if (queryEnd.isBefore(queryStart)) {
            log.info("【{}】{}", LOG_BIZ, HeartMailLogConstants.buildAction(
                    "拉取终止：结束时间早于开始时间, startTime=" + queryStart + ", endTime=" + queryEnd));
            throw new BizException(ResultCode.PARAM_INVALID, "结束时间不能早于开始时间");
        }
        log.info("【{}】{}", LOG_BIZ, HeartMailLogConstants.buildAction(
                "开始拉取邮件, startTime=" + queryStart + ", endTime=" + queryEnd));
        try {
            List<HeartMailInfo> mails = heartMailClient.queryMails(queryStart, queryEnd);
            log.info("【{}】{}", LOG_BIZ, HeartMailLogConstants.buildAction("拉取邮件完成, 共 " + mails.size() + " 封"));
            return mails;
        } catch (BizException e) {
            log.error("【{}】拉取邮件失败, startTime={}, endTime={}, error={}",
                    LOG_BIZ, queryStart, queryEnd, e.getMessage());
            throw e;
        }
    }
}
