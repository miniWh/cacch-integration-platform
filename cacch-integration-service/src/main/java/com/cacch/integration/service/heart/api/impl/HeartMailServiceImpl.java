package com.cacch.integration.service.heart.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cacch.integration.common.constant.heart.HeartMailConstants;
import com.cacch.integration.common.constant.heart.HeartMailLogConstants;
import com.cacch.integration.common.constant.heart.HeartMailProcessStatusConstants;
import com.cacch.integration.common.dto.heart.HeartMailPullItem;
import com.cacch.integration.common.dto.heart.HeartMailPullResult;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.heart.HeartMailDO;
import com.cacch.integration.integration.fileserver.client.FileServerUploadClient;
import com.cacch.integration.integration.heart.client.HeartMailClient;
import com.cacch.integration.integration.heart.client.dto.HeartMailAttachmentInfo;
import com.cacch.integration.integration.heart.client.dto.HeartMailInfo;
import com.cacch.integration.mapper.heart.HeartMailMapper;
import com.cacch.integration.service.heart.api.IHeartMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Heart 邮件拉取与入库服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartMailServiceImpl implements IHeartMailService {

    private static final String LOG_BIZ = HeartMailLogConstants.LOG_BIZ;

    /**
     * 本次邮件新增入库
     */
    private static final int SAVE_INSERTED = 1;

    /**
     * 失败邮件重试成功（更新）
     */
    private static final int SAVE_UPDATED = 2;

    /**
     * 已存在或无法去重，跳过
     */
    private static final int SAVE_SKIPPED = 0;

    /**
     * 入库失败（已落失败状态）
     */
    private static final int SAVE_FAILED = -1;

    private final HeartMailClient heartMailClient;

    private final HeartMailMapper heartMailMapper;

    private final FileServerUploadClient fileServerUploadClient;

    @Override
    public List<HeartMailInfo> queryMails(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime queryStart = resolveStart(startTime);
        LocalDateTime queryEnd = resolveEnd(endTime);
        validateRange(queryStart, queryEnd);
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

    @Override
    public HeartMailPullResult pullAndSaveMails(LocalDateTime startTime, LocalDateTime endTime) {
        HeartMailPullResult result = new HeartMailPullResult();
        retryFailedMails(result);
        LocalDateTime queryStart = resolveStart(startTime);
        LocalDateTime queryEnd = resolveEnd(endTime);
        validateRange(queryStart, queryEnd);
        log.info("【{}】{}", LOG_BIZ, HeartMailLogConstants.buildAction(
                "开始拉取并入库邮件, startTime=" + queryStart + ", endTime=" + queryEnd));
        List<HeartMailInfo> mails = heartMailClient.fetchMailsWithContent(queryStart, queryEnd);
        result.setPulled(mails.size());
        for (HeartMailInfo mail : mails) {
            HeartMailPullItem item = new HeartMailPullItem(resolveMailKey(mail), mail.getSubject());
            try {
                recordSaveResult(result, item, saveMail(mail));
            } catch (Exception e) {
                result.recordFailure(item, "邮件入库异常: " + e.getMessage());
                log.error("【{}】邮件入库异常, messageId={}, uid={}, error={}",
                        LOG_BIZ, mail.getMessageId(), mail.getUid(), e.getMessage());
            }
        }
        log.info("【{}】{}", LOG_BIZ, HeartMailLogConstants.buildAction(
                "邮件入库完成, 拉取=" + result.getPulled() + " 封, 重试=" + result.getRetried()
                        + " 封, 成功=" + result.getSuccessCount() + " 封, 跳过=" + result.getSkipCount()
                        + " 封, 失败=" + result.getFailureCount() + " 封"));
        return result;
    }

    /**
     * 按保存结果码记录统计
     *
     * @param result   执行结果
     * @param item     处理明细主体
     * @param saveCode 保存结果码（{@code SAVE_*}）
     */
    private void recordSaveResult(HeartMailPullResult result, HeartMailPullItem item, int saveCode) {
        if (saveCode == SAVE_FAILED) {
            result.recordFailure(item, "入库失败, 详见日志");
        } else if (saveCode == SAVE_SKIPPED) {
            result.recordSkip(item, "重复邮件或缺少去重键");
        } else {
            result.recordSuccess(item);
        }
    }

    /**
     * 保存单封邮件（幂等）
     *
     * <p>已存在且非失败状态的邮件直接跳过；失败邮件重新拉取后覆盖更新。
     * 附件落盘失败时保留失败状态并累计重试次数。</p>
     *
     * @param mail 邮件信息（含正文与临时附件）
     * @return 保存结果码
     */
    private int saveMail(HeartMailInfo mail) {
        String mailKey = resolveMailKey(mail);
        if (mailKey == null) {
            log.warn("【{}】邮件缺少 Message-ID 与 UID, 无法去重入库", LOG_BIZ);
            deleteTempFiles(mail);
            return SAVE_SKIPPED;
        }
        HeartMailDO existing = heartMailMapper.selectOne(Wrappers.<HeartMailDO>lambdaQuery()
                .eq(HeartMailDO::getMailKey, mailKey));
        return doSaveMail(mail, mailKey, existing);
    }

    /**
     * 保存失败重试拉取的邮件（覆盖原有失败行）
     *
     * @param mail     邮件信息（按 UID 重拉）
     * @param existing 原失败邮件行，不为空
     * @return 保存结果码
     */
    private int saveMail(HeartMailInfo mail, HeartMailDO existing) {
        String mailKey = resolveMailKey(mail);
        if (mailKey == null) {
            log.info("【{}】重试邮件缺少去重键, 跳过, mailKey={}", LOG_BIZ, existing.getMailKey());
            deleteTempFiles(mail);
            return SAVE_SKIPPED;
        }
        return doSaveMail(mail, mailKey, existing);
    }

    /**
     * 按去重键保存邮件行
     *
     * @param mail     邮件信息
     * @param mailKey  去重键
     * @param existing 已存在的邮件行，为空表示首次入库
     * @return 保存结果码
     */
    private int doSaveMail(HeartMailInfo mail, String mailKey, HeartMailDO existing) {
        if (existing != null && !HeartMailProcessStatusConstants.FAILED.equals(existing.getProcessStatus())) {
            log.info("【{}】邮件已入库, 跳过, mailKey={}, status={}",
                    LOG_BIZ, mailKey, existing.getProcessStatus());
            deleteTempFiles(mail);
            return SAVE_SKIPPED;
        }
        HeartMailDO row = buildRow(mail, mailKey);
        boolean failed = false;
        try {
            if (mail.getFetchError() != null) {
                failRow(row, mail.getFetchError(), existing);
                failed = true;
            } else {
                row.setAttachFiles(uploadAttachments(mail, mailKey));
                row.setProcessStatus(resolveStatus(mail));
                row.setFailReason("");
                row.setRetryCount(existing == null || existing.getRetryCount() == null
                        ? 0 : existing.getRetryCount());
            }
        } catch (Exception e) {
            log.error("【{}】附件落盘失败, mailKey={}, error={}", LOG_BIZ, mailKey, e.getMessage());
            failRow(row, e.getMessage(), existing);
            failed = true;
        } finally {
            deleteTempFiles(mail);
        }
        if (existing == null) {
            heartMailMapper.insert(row);
            return failed ? SAVE_FAILED : SAVE_INSERTED;
        }
        row.setId(existing.getId());
        heartMailMapper.updateById(row);
        return failed ? SAVE_FAILED : SAVE_UPDATED;
    }

    /**
     * 重试历史失败邮件
     *
     * <p>按 IMAP UID 精确重拉，不受当天时间窗口限制；重试超次置人工处理状态。</p>
     *
     * @param result 执行结果（累加重试统计）
     */
    private void retryFailedMails(HeartMailPullResult result) {
        List<HeartMailDO> failedRows = heartMailMapper.selectList(Wrappers.<HeartMailDO>lambdaQuery()
                .eq(HeartMailDO::getProcessStatus, HeartMailProcessStatusConstants.FAILED)
                .lt(HeartMailDO::getRetryCount, HeartMailConstants.MAX_RETRY)
                .orderByAsc(HeartMailDO::getCreatedAt)
                .last("LIMIT " + HeartMailConstants.DEFAULT_MAX_PER_ROUND));
        if (failedRows.isEmpty()) {
            return;
        }
        result.setRetried(failedRows.size());
        log.info("【{}】开始重试失败邮件, 共 {} 封", LOG_BIZ, failedRows.size());
        for (HeartMailDO row : failedRows) {
            HeartMailPullItem item = new HeartMailPullItem(row.getMailKey(), row.getSubject());
            if (row.getMailUid() == null) {
                log.info("【{}】失败邮件缺少 IMAP UID, 终止重试, mailKey={}", LOG_BIZ, row.getMailKey());
                failRow(row, "缺少 IMAP UID, 无法重试", row);
                heartMailMapper.updateById(row);
                result.recordFailure(item, "缺少 IMAP UID, 无法重试");
                continue;
            }
            try {
                HeartMailInfo mail = heartMailClient.fetchMailByUid(row.getMailUid());
                if (mail == null) {
                    failRow(row, "IMAP 中已不存在 UID=" + row.getMailUid() + " 的邮件", row);
                    heartMailMapper.updateById(row);
                    result.recordFailure(item, row.getFailReason());
                    continue;
                }
                String mailKey = resolveMailKey(mail);
                if (mailKey != null && !mailKey.equals(row.getMailKey())) {
                    HeartMailDO duplicate = heartMailMapper.selectOne(Wrappers.<HeartMailDO>lambdaQuery()
                            .eq(HeartMailDO::getMailKey, mailKey));
                    if (duplicate != null) {
                        row.setProcessStatus(HeartMailProcessStatusConstants.SKIP);
                        row.setRetryCount(HeartMailConstants.MAX_RETRY);
                        row.setFailReason(truncate("重复邮件, 已存在 mailKey=" + mailKey, 1000));
                        heartMailMapper.updateById(row);
                        deleteTempFiles(mail);
                        log.info("【{}】重试邮件已存在同内容记录, 终止重试, mailKey={}, duplicateId={}",
                                LOG_BIZ, mailKey, duplicate.getId());
                        result.recordSkip(item, row.getFailReason());
                        continue;
                    }
                }
                recordSaveResult(result, item, saveMail(mail, row));
            } catch (Exception e) {
                failRow(row, e.getMessage(), row);
                heartMailMapper.updateById(row);
                result.recordFailure(item, "重试异常: " + e.getMessage());
                log.error("【{}】失败邮件重试异常, mailKey={}, error={}",
                        LOG_BIZ, row.getMailKey(), e.getMessage());
            }
        }
    }

    /**
     * 上传 PDF 附件至文件服务器，返回附件元信息数组
     *
     * <p>同一邮件内按 SHA-256 去重，落盘目录使用清洗后的 mailKey，重试覆盖写幂等。</p>
     *
     * @param mail    邮件信息
     * @param mailKey 去重键
     * @return 附件数组：[{name, sha256, path, size}]
     */
    private List<Map<String, Object>> uploadAttachments(HeartMailInfo mail, String mailKey) {
        List<Map<String, Object>> files = new ArrayList<>();
        if (mail.getAttachments() == null || mail.getAttachments().isEmpty()) {
            return files;
        }
        Set<String> shaSet = new HashSet<>();
        String fileIdPrefix = sanitizeFileId(mailKey);
        int index = 0;
        for (HeartMailAttachmentInfo attachment : mail.getAttachments()) {
            if (!shaSet.add(attachment.getSha256())) {
                continue;
            }
            String url = fileServerUploadClient.uploadFile(Path.of(attachment.getTempFilePath()),
                    attachment.getName(), fileIdPrefix + "_" + index++);
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("name", attachment.getName());
            file.put("sha256", attachment.getSha256());
            file.put("path", url);
            file.put("size", attachment.getSize());
            files.add(file);
        }
        return files;
    }

    /**
     * 组装邮件行
     *
     * @param mail    邮件信息
     * @param mailKey 去重键
     * @return 邮件数据 DO
     */
    private HeartMailDO buildRow(HeartMailInfo mail, String mailKey) {
        HeartMailDO row = new HeartMailDO();
        row.setMailKey(mailKey);
        row.setMailUid(mail.getUid());
        row.setMessageId(mail.getMessageId());
        row.setSubject(truncate(mail.getSubject(), 500));
        row.setFromAddress(truncate(mail.getFrom(), 255));
        row.setReceivedAt(mail.getReceivedAt());
        row.setBodyText(mail.getBodyText());
        row.setBodyHtml(mail.getBodyHtml());
        row.setRetryCount(0);
        return row;
    }

    /**
     * 标记邮件失败并累计重试次数
     *
     * @param row      邮件行
     * @param reason   失败原因
     * @param existing 已存在的邮件行，为空表示首次入库
     */
    private void failRow(HeartMailDO row, String reason, HeartMailDO existing) {
        int retry = existing == null || existing.getRetryCount() == null
                ? 1 : existing.getRetryCount() + 1;
        row.setRetryCount(retry);
        row.setProcessStatus(retry >= HeartMailConstants.MAX_RETRY
                ? HeartMailProcessStatusConstants.MANUAL_REVIEW
                : HeartMailProcessStatusConstants.FAILED);
        row.setFailReason(truncate(reason, 1000));
    }

    /**
     * 校验发件人白名单，返回流程状态
     *
     * @param mail 邮件信息
     * @return MAIL_RECEIVED 或 SKIP
     */
    private String resolveStatus(HeartMailInfo mail) {
        boolean heartMail = StringUtils.hasText(mail.getFrom())
                && HeartMailConstants.HEART_SENDER_ADDRESS.equalsIgnoreCase(mail.getFrom().trim());
        return heartMail
                ? HeartMailProcessStatusConstants.MAIL_RECEIVED
                : HeartMailProcessStatusConstants.SKIP;
    }

    /**
     * 解析去重键：优先 Message-ID，缺失时使用 UID
     *
     * @param mail 邮件信息
     * @return 去重键，Message-ID 与 UID 均缺失时返回 null
     */
    private String resolveMailKey(HeartMailInfo mail) {
        if (StringUtils.hasText(mail.getMessageId())) {
            return mail.getMessageId().trim();
        }
        if (mail.getUid() != null) {
            return HeartMailConstants.MAIL_KEY_UID_PREFIX + mail.getUid();
        }
        return null;
    }

    /**
     * 清洗邮件去重键，作为落盘子目录名
     *
     * @param mailKey 去重键
     * @return 仅含字母/数字/点/下划线/中划线的目录名，最长 80 字符
     */
    private String sanitizeFileId(String mailKey) {
        String sanitized = mailKey.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("[. ]+$", "");
        return sanitized.length() > 80 ? sanitized.substring(0, 80) : sanitized;
    }

    /**
     * 删除邮件临时附件文件（忽略异常）
     *
     * @param mail 邮件信息
     */
    private void deleteTempFiles(HeartMailInfo mail) {
        if (mail.getAttachments() == null) {
            return;
        }
        for (HeartMailAttachmentInfo attachment : mail.getAttachments()) {
            if (!StringUtils.hasText(attachment.getTempFilePath())) {
                continue;
            }
            try {
                Files.deleteIfExists(Path.of(attachment.getTempFilePath()));
            } catch (Exception e) {
                log.warn("【{}】删除临时附件失败, path={}, error={}",
                        LOG_BIZ, attachment.getTempFilePath(), e.getMessage());
            }
        }
    }

    /**
     * 解析开始时间
     *
     * @param startTime 入参开始时间
     * @return 实际开始时间
     */
    private LocalDateTime resolveStart(LocalDateTime startTime) {
        return startTime == null ? LocalDate.now().atStartOfDay() : startTime;
    }

    /**
     * 解析结束时间
     *
     * @param endTime 入参结束时间
     * @return 实际结束时间
     */
    private LocalDateTime resolveEnd(LocalDateTime endTime) {
        return endTime == null ? LocalDate.now().atTime(HeartMailConstants.DEFAULT_END_TIME) : endTime;
    }

    /**
     * 校验时间区间
     *
     * @param queryStart 开始时间
     * @param queryEnd   结束时间
     */
    private void validateRange(LocalDateTime queryStart, LocalDateTime queryEnd) {
        if (queryEnd.isBefore(queryStart)) {
            log.info("【{}】{}", LOG_BIZ, HeartMailLogConstants.buildAction(
                    "拉取终止：结束时间早于开始时间, startTime=" + queryStart + ", endTime=" + queryEnd));
            throw new BizException(ResultCode.PARAM_INVALID, "结束时间不能早于开始时间");
        }
    }

    /**
     * 按字符上限截断
     *
     * @param text  原始文本
     * @param limit 最大字符数
     * @return 截断后文本
     */
    private String truncate(String text, int limit) {
        if (text == null || text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit);
    }
}
