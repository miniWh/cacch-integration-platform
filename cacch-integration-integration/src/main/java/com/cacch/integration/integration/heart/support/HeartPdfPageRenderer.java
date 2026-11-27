package com.cacch.integration.integration.heart.support;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Heart PO PDF 页面渲染器
 *
 * <p>将 PDF 每页渲染为 PNG 字节，供支持图像输入的模型直接读取，
 * 从而适配扫描件（无文本层）场景。通过页数上限与最大宽度控制单次请求的图片体量。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
public class HeartPdfPageRenderer {

    /**
     * 单页图片最大宽度（像素），超出等比缩放以控制 token 消耗
     */
    private static final int MAX_IMAGE_WIDTH = 1600;

    /**
     * 渲染 PDF 每页为 PNG 字节
     *
     * @param pdfBytes PDF 字节
     * @param fileName 文件名（仅日志）
     * @param dpi      渲染 DPI，建议 150
     * @param maxPages 最多渲染页数，超出部分丢弃
     * @return 每页 PNG 字节列表，按页序排列；无页面时返回空列表
     * @throws BizException PDF 读取失败、加密或渲染失败时抛出
     */
    public List<byte[]> render(byte[] pdfBytes, String fileName, int dpi, int maxPages) {
        List<byte[]> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.isEncrypted()) {
                throw new BizException(ResultCode.INTEGRATION_ERROR, "PDF 已加密, 无法解析");
            }
            int total = document.getNumberOfPages();
            int limit = Math.min(total, Math.max(1, maxPages));
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < limit; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, Math.max(1, dpi), ImageType.RGB);
                pages.add(toPng(scaleIfNeeded(image)));
            }
            log.info("渲染 PDF 页面完成, file={}, totalPages={}, renderedPages={}, dpi={}",
                    fileName, total, pages.size(), dpi);
            return pages;
        } catch (IOException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "PDF 渲染失败: " + e.getMessage(), e);
        }
    }

    /**
     * 超过最大宽度时等比缩放
     *
     * @param image 原始页面图
     * @return 缩放后的图片
     */
    private BufferedImage scaleIfNeeded(BufferedImage image) {
        if (image.getWidth() <= MAX_IMAGE_WIDTH) {
            return image;
        }
        int height = (int) (image.getHeight() * (MAX_IMAGE_WIDTH * 1.0 / image.getWidth()));
        BufferedImage scaled = new BufferedImage(MAX_IMAGE_WIDTH, height, BufferedImage.TYPE_INT_RGB);
        scaled.getGraphics().drawImage(image, 0, 0, MAX_IMAGE_WIDTH, height, null);
        return scaled;
    }

    /**
     * 图片转 PNG 字节
     *
     * @param image 页面图
     * @return PNG 字节
     */
    private byte[] toPng(BufferedImage image) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BizException(ResultCode.INTEGRATION_ERROR, "PDF 页面转 PNG 失败", e);
        }
    }
}