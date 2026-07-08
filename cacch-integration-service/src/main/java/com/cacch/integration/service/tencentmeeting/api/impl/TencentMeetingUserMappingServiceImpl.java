package com.cacch.integration.service.tencentmeeting.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.entity.tencentmeeting.QywxTxMeetingUserDO;
import com.cacch.integration.mapper.tencentmeeting.QywxTxMeetingUserMapper;
import com.cacch.integration.service.tencentmeeting.api.ITencentMeetingUserMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 企微 userid 与腾讯会议 userid 映射服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Service
@RequiredArgsConstructor
public class TencentMeetingUserMappingServiceImpl implements ITencentMeetingUserMappingService {

    private final QywxTxMeetingUserMapper qywxTxMeetingUserMapper;

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.SUPPORTS,
            readOnly = true,
            timeout = 10
    )
    public String resolveTxMeetingUserId(String wecomUserId) {
        if (!StringUtils.hasText(wecomUserId)) {
            return null;
        }
        QywxTxMeetingUserDO mapping = qywxTxMeetingUserMapper.selectOne(new LambdaQueryWrapper<QywxTxMeetingUserDO>()
                .eq(QywxTxMeetingUserDO::getUserId, wecomUserId.trim())
                .last("LIMIT 1"));
        if (mapping == null || !StringUtils.hasText(mapping.getTxMeetingUserId())) {
            return null;
        }
        return mapping.getTxMeetingUserId().trim();
    }
}
