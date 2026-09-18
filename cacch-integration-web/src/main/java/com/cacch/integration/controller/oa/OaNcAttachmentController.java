package com.cacch.integration.controller.oa;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.oa.vo.OaNcAttachmentVO;
import com.cacch.integration.integration.oa.client.dto.OaFileDownloadResult;
import com.cacch.integration.service.oa.api.IOaAttachmentQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Optional;

/**
 * NC 单号附件查询下载 REST 接口
 *
 * <p>对外提供按 NC 单号（致远 OA 内部购销合同表 formmain_5296.field0273）下载关联附件的能力。
 * 所有场景统一返回 {@link Result} JSON 格式：命中附件时 data 含 Base64 编码文件内容；
 * 未命中附件或附件文件标识为空时 data 为 null（正常业务分支，非异常）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oa/nc-attachments")
public class OaNcAttachmentController {

    private final IOaAttachmentQueryService oaAttachmentQueryService;

    /**
     * 按 NC 单号查询并下载第一条关联附件
     *
     * @param ncNo NC 单号，对应致远 OA 内部购销合同表 formmain_5296.field0273，不可为空
     * @return 统一返回体：
     * <ul>
     *   <li>命中附件：{@code data} 含 fileName、contentType、fileSize、fileContent(Base64)</li>
     *   <li>未命中附件或附件文件标识为空：{@code data = null}</li>
     * </ul>
     * @throws com.cacch.integration.common.exception.BizException ncNo 为空或致远 OA 下载失败时抛出，
     *                                                             由全局异常处理器统一转换为 {@code Result} 返回
     */
    @GetMapping("/{ncNo}")
    public Result<OaNcAttachmentVO> downloadByNcNo(@PathVariable String ncNo) {
        Optional<OaFileDownloadResult> resultOpt = oaAttachmentQueryService.downloadAttachmentByNcNo(ncNo);
        if (resultOpt.isEmpty()) {
            log.info("【NC附件】按NC单号下载返回空, ncNo={}", ncNo);
            return Result.success();
        }
        OaFileDownloadResult result = resultOpt.get();
        String fileName = StringUtils.hasText(result.fileName()) ? result.fileName().trim() : ncNo.trim();
        String contentType = StringUtils.hasText(result.contentType())
                ? result.contentType().trim() : "application/octet-stream";
        String fileContent = Base64.getEncoder().encodeToString(result.content());
        OaNcAttachmentVO vo = new OaNcAttachmentVO(fileName, contentType, result.contentLength(), fileContent);
        log.info("【NC附件】按NC单号下载成功, ncNo={}, fileName={}, fileSize={}", ncNo.trim(), fileName, result.contentLength());
        return Result.success(vo);
    }
}
