package com.cacch.integration.service.heart.api.impl;

import com.cacch.integration.common.constant.heart.HeartMailLogConstants;
import com.cacch.integration.common.exception.BizException;
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
    public List<HeartMailInfo> queryMails(LocalDateTime since) {
        LocalDateTime querySince = since == null ? LocalDate.now().atStartOfDay() : since;
        log.info("【{}】{}", LOG_BIZ, HeartMailLogConstants.buildAction("开始拉取邮件, since=" + querySince));
        try {
            List<HeartMailInfo> mails = heartMailClient.queryMails(querySince);
            log.info("【{}】{}", LOG_BIZ, HeartMailLogConstants.buildAction("拉取邮件完成, 共 " + mails.size() + " 封"));
            return mails;
        } catch (BizException e) {
            log.error("【{}】拉取邮件失败, since={}, error={}", LOG_BIZ, querySince, e.getMessage());
            throw e;
        }
    }
}
