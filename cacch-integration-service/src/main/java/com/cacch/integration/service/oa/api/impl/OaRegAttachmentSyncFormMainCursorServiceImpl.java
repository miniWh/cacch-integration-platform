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
    public long getLastFormMainId() {
        String raw = stringRedisTemplate.opsForValue().get(OaRegReportConstants.FORM_MAIN_CURSOR_REDIS_KEY);
        if (!StringUtils.hasText(raw)) {
            return 0L;
        }
        try {
            long cursor = Long.parseLong(raw.trim());
            return Math.max(cursor, 0L);
        } catch (NumberFormatException e) {
            log.info("【{}】主表游标解析失败，将重置为 0, raw={}", BIZ, raw);
            resetCursor();
            return 0L;
        }
    }

    @Override
    public void saveLastFormMainId(long lastFormMainId) {
        long normalized = Math.max(lastFormMainId, 0L);
        stringRedisTemplate.opsForValue().set(
                OaRegReportConstants.FORM_MAIN_CURSOR_REDIS_KEY,
                Long.toString(normalized));
        log.info("【{}】主表游标已更新, lastFormMainId={}", BIZ, normalized);
    }

    @Override
    public void resetCursor() {
        stringRedisTemplate.delete(OaRegReportConstants.FORM_MAIN_CURSOR_REDIS_KEY);
        log.info("【{}】主表游标已重置", BIZ);
    }
}
