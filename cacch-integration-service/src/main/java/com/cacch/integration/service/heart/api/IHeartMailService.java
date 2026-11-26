package com.cacch.integration.service.heart.api;

import com.cacch.integration.integration.heart.client.dto.HeartMailInfo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Heart 邮件拉取服务
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IHeartMailService {

    /**
     * 拉取指定时间区间内的邮件摘要
     *
     * @param startTime 开始时间，为空时取当天 00:00:00
     * @param endTime   结束时间，为空时取当天 23:59:59
     * @return 邮件摘要列表
     */
    List<HeartMailInfo> queryMails(LocalDateTime startTime, LocalDateTime endTime);
}
