package com.cacch.integration.integration.heart.support;

import com.cacch.integration.common.constant.heart.HeartAiLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * PDF 文本抽取器（PDFBox 3）
 *
 * <p>按版面顺序抽取文本层并限长；加密或无文本层的 PDF 由调用方决定降级策略。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
public class HeartPdfTextExtractor {

    private static final String LOG_BIZ = HeartAiLogConstants.LOG_BIZ;

    /**
     * 抽取 PDF 文本层
     *
     * @param pdfBytes PDF 字节
     * @param fileName 文件名（仅日志）
     * @param maxChars 最大字符数，超出截断
     * @return 文本内容
     * @throws BizException PDF 为空、已加密或读取失败时抛出
     */
    public String extract(byte[] pdfBytes, String fileName, int maxChars) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "PDF 内容为空，无法解析");
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            int pages = document.getNumberOfPages();
            String normalized = text == null ? "" : text.trim();
            log.info("【{}】PDF 文本抽取完成, file={}, pages={}, chars={}",
                    LOG_BIZ, fileName, pages, normalized.length());
            if (normalized.isEmpty()) {
                log.info("【{}】PDF 无文本层, 可能为扫描件, file={}", LOG_BIZ, fileName);
                return "";
            }
            return normalized.length() > maxChars ? normalized.substring(0, maxChars) : normalized;
        } catch (InvalidPasswordException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "PDF 已加密, 无法解析", e);
        } catch (IOException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "PDF 读取失败: " + e.getMessage(), e);
        }
    }
}
