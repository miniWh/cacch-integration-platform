package com.cacch.integration.service.oa.api.impl;

import com.cacch.integration.common.constant.oa.OaRegReportConstants;
import com.cacch.integration.service.oa.api.IOaRegAttachmentSyncFormMainCursorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 国内登记报告附件同步主表游标实现（Redis 持久化）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaRegAttachmentSyncFormMainCursorServiceImpl implements IOaRegAttachmentSyncFormMainCursorService {

    private static final String BIZ = OaRegReportConstants.LOG_BIZ;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String getLastFormMainId() {
        String raw = stringRedisTemplate.opsForValue().get(OaRegReportConstants.FORM_MAIN_CURSOR_REDIS_KEY);
        if (!StringUtils.hasText(raw)) {
            return "0";
        }
        return raw.trim();
    }

    @Override
    public void saveLastFormMainId(String lastFormMainId) {
        if (!StringUtils.hasText(lastFormMainId)) {
            log.info("【{}】主表游标保存终止, reason=lastFormMainId为空", BIZ);
            return;
        }
        stringRedisTemplate.opsForValue().set(
                OaRegReportConstants.FORM_MAIN_CURSOR_REDIS_KEY,
                lastFormMainId.trim());
        log.info("【{}】主表游标已更新, lastFormMainId={}", BIZ, lastFormMainId.trim());
    }

    @Override
    public void resetCursor() {
        stringRedisTemplate.delete(OaRegReportConstants.FORM_MAIN_CURSOR_REDIS_KEY);
        log.info("【{}】主表游标已重置", BIZ);
    }
}
