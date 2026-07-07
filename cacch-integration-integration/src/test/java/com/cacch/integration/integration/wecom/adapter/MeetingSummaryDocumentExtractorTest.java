package com.cacch.integration.integration.wecom.adapter;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingSummaryDocumentExtractorTest {

    @Test
    void isTranscriptLike_detectsSpeakerTimestampFormat() {
        String transcript = """
                周洪福(00:00:41): 英雄归来
                周洪福(00:01:22): 12306
                """;
        assertTrue(MeetingSummaryDocumentExtractor.isTranscriptLike(transcript));
    }

    @Test
    void isTranscriptLike_falseForMinutesContent() {
        String minutes = """
                会议总结
                讨论了项目进度。

                会议待办
                - 帮助受灾群众
                - 展示了中国的军事实力
                """;
        assertFalse(MeetingSummaryDocumentExtractor.isTranscriptLike(minutes));
    }

    @Test
    void extractText_fromTxtBytes() {
        byte[] bytes = "会议待办\n- 测试".getBytes(StandardCharsets.UTF_8);
        assertEquals("会议待办\n- 测试", MeetingSummaryDocumentExtractor.extractText("txt", bytes));
    }
}
