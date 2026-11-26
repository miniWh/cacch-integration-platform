package com.cacch.integration.service.heart.api.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cacch.integration.common.constant.heart.HeartAttachmentConstants;
import com.cacch.integration.common.constant.heart.HeartAttachmentLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.heart.HeartPoDO;
import com.cacch.integration.integration.fileserver.client.FileServerUploadClient;
import com.cacch.integration.mapper.heart.HeartPoMapper;
import com.cacch.integration.service.heart.api.IHeartAttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Heart 客户 PO 附件上传服务实现
 *
 * <p>读取固定本地文件 → 落盘 NAS 挂载目录（{@code {mount-path}/{sub-dir}/{yyyyMMdd}/{poId}/}）
 * → 以 {@code url-prefix + 编码后绝对路径} 回写 {@code attachment_url}。
 * 回写以「该列仍为空」为条件，并发场景下重查并复用已写入值。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartAttachmentServiceImpl implements IHeartAttachmentService {

    private final HeartPoMapper heartPoMapper;

    private final FileServerUploadClient fileServerUploadClient;

    @Override
    public void resolveAndSaveAttachment(Long poId) {
        if (poId == null) {
            log.info("【{}】附件上传终止, reason=poId为空", HeartAttachmentLogConstants.LOG_BIZ);
            throw new BizException(ResultCode.PARAM_MISSING, "poId 不能为空");
        }
        HeartPoDO po = heartPoMapper.selectById(poId);
        if (po == null) {
            log.info("【{}】附件上传终止, reason=PO不存在, poId={}", HeartAttachmentLogConstants.LOG_BIZ, poId);
            throw new BizException(ResultCode.PARAM_INVALID, "PO 不存在, poId=" + poId);
        }
        if (StringUtils.hasText(po.getAttachmentUrl())) {
            log.info("【{}】复用已有附件URL, poId={}, url={}",
                    HeartAttachmentLogConstants.LOG_BIZ, poId, po.getAttachmentUrl());
            return;
        }

        Path localPath = Paths.get(HeartAttachmentConstants.DEFAULT_ATTACHMENT_FILE_PATH);
        if (!Files.isReadable(localPath)) {
            log.info("【{}】附件上传终止, reason=本地文件不存在或不可读, poId={}, path={}",
                    HeartAttachmentLogConstants.LOG_BIZ, poId, localPath);
            throw new BizException(ResultCode.SYSTEM_ERROR, "附件文件不存在或不可读: " + localPath);
        }
        byte[] content;
        try {
            content = Files.readAllBytes(localPath);
        } catch (IOException e) {
            log.error("【{}】附件读取失败, poId={}, path={}",
                    HeartAttachmentLogConstants.LOG_BIZ, poId, localPath, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "附件文件读取失败: " + e.getMessage(), e);
        }

        String fileName = localPath.getFileName().toString();
        String url;
        try {
            url = fileServerUploadClient.uploadFile(content, fileName, String.valueOf(poId));
        } catch (IllegalStateException e) {
            log.error("【{}】附件落盘失败, poId={}, fileName={}",
                    HeartAttachmentLogConstants.LOG_BIZ, poId, fileName, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "附件落盘失败: " + e.getMessage(), e);
        }

        int rows = heartPoMapper.update(null, Wrappers.<HeartPoDO>lambdaUpdate()
                .eq(HeartPoDO::getId, poId)
                .and(w -> w.isNull(HeartPoDO::getAttachmentUrl).or().eq(HeartPoDO::getAttachmentUrl, ""))
                .set(HeartPoDO::getAttachmentUrl, url)
                .set(HeartPoDO::getUpdatedAt, LocalDateTime.now()));
        if (rows > 0) {
            log.info("【{}】附件上传完成, poId={}, fileName={}, url={}",
                    HeartAttachmentLogConstants.LOG_BIZ, poId, fileName, url);
            return;
        }

        HeartPoDO latest = heartPoMapper.selectById(poId);
        if (latest == null || !StringUtils.hasText(latest.getAttachmentUrl())) {
            log.info("【{}】附件URL回写失败, poId={}", HeartAttachmentLogConstants.LOG_BIZ, poId);
            throw new BizException(ResultCode.SYSTEM_ERROR, "附件URL回写失败, poId=" + poId);
        }
        log.info("【{}】附件URL并发回写, 复用已有值, poId={}, url={}",
                HeartAttachmentLogConstants.LOG_BIZ, poId, latest.getAttachmentUrl());
    }
}
