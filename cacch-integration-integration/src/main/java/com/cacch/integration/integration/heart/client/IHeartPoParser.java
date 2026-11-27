package com.cacch.integration.integration.heart.client;

import com.cacch.integration.integration.heart.client.dto.HeartPoParseResult;

/**
 * Heart PO PDF 解析策略接口
 *
 * <p>实现方只负责 PDF → 结构化字段，禁止做金额计算、编码转换与接口调用。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IHeartPoParser {

    /**
     * 解析 PO PDF
     *
     * @param pdfBytes PDF 字节
     * @param fileName 文件名（用于日志与告警定位）
     * @return 解析结果
     */
    HeartPoParseResult parse(byte[] pdfBytes, String fileName);

    /**
     * 解析器类型标识
     *
     * @return AI / TEXT_FALLBACK
     */
    String getParserType();
}
