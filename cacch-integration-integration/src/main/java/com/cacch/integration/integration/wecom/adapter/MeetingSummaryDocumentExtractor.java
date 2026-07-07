package com.cacch.integration.integration.wecom.adapter;

import com.cacch.integration.common.constant.wecom.WeComConstants;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 企微会议纪要文件文本提取（TXT / DOCX / PDF）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MeetingSummaryDocumentExtractor {

    private static final Pattern TRANSCRIPT_LINE = Pattern.compile(".+\\(\\d{2}:\\d{2}:\\d{2}\\)\\s*[:：].*");

    private static final Pattern DOCX_TEXT = Pattern.compile("<w:t(?:[^>]*)>([^<]*)</w:t>");

    private static final Pattern TODO_MARKER = Pattern.compile("会议待办|待办事项|待办清单");

    private MeetingSummaryDocumentExtractor() {
    }

    /**
     * 按文件类型从二进制内容提取纯文本
     *
     * @param fileType 文件类型（txt/docx/pdf）
     * @param bytes    文件字节
     * @return 提取后的文本，不支持的类型返回 null
     */
    public static String extractText(String fileType, byte[] bytes) {
        if (bytes == null || bytes.length == 0 || !StringUtils.hasText(fileType)) {
            return null;
        }
        String normalizedType = fileType.trim().toLowerCase();
        return switch (normalizedType) {
            case String s when WeComConstants.MEETING_SUMMARY_FILE_TYPE_TXT.equals(s) ->
                    new String(bytes, StandardCharsets.UTF_8);
            case String s when WeComConstants.MEETING_SUMMARY_FILE_TYPE_DOCX.equals(s) ->
                    extractDocxText(bytes);
            case String s when WeComConstants.MEETING_SUMMARY_FILE_TYPE_PDF.equals(s) ->
                    extractPdfText(bytes);
            default -> null;
        };
    }

    /**
     * 判断文本是否为录制转写（发言人+时间戳格式），而非智能纪要
     *
     * @param content 文本内容
     * @return 转写特征明显时返回 true
     */
    public static boolean isTranscriptLike(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        if (TODO_MARKER.matcher(content).find()) {
            return false;
        }
        int transcriptLines = 0;
        int contentLines = 0;
        for (String rawLine : content.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = rawLine.trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }
            contentLines++;
            if (TRANSCRIPT_LINE.matcher(line).matches()) {
                transcriptLines++;
            }
        }
        return contentLines > 0 && transcriptLines * 2 >= contentLines;
    }

    private static String extractDocxText(byte[] bytes) {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!"word/document.xml".equals(entry.getName())) {
                    continue;
                }
                String xml = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                return extractDocxParagraphText(xml);
            }
        } catch (IOException e) {
            throw new IllegalStateException("解析 DOCX 纪要失败", e);
        }
        throw new IllegalStateException("DOCX 中未找到 word/document.xml");
    }

    private static String extractDocxParagraphText(String xml) {
        String[] paragraphs = xml.split("</w:p>");
        StringBuilder document = new StringBuilder();
        for (String paragraph : paragraphs) {
            Matcher matcher = DOCX_TEXT.matcher(paragraph);
            StringBuilder line = new StringBuilder();
            while (matcher.find()) {
                line.append(matcher.group(1));
            }
            if (!line.isEmpty()) {
                document.append(line).append('\n');
            }
        }
        return document.toString().trim();
    }

    private static String extractPdfText(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document).trim();
        } catch (IOException e) {
            throw new IllegalStateException("解析 PDF 纪要失败", e);
        }
    }
}
