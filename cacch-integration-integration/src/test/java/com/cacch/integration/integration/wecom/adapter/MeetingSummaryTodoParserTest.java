package com.cacch.integration.integration.wecom.adapter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingSummaryTodoParserTest {

    @Test
    void parseTodos_fromSection() {
        String content = """
                会议摘要
                讨论了项目进度。

                会议待办
                - 完成需求文档
                - 跟进测试计划

                文字记录
                ...
                """;
        List<String> todos = MeetingSummaryTodoParser.parseTodos(content);
        assertEquals(2, todos.size());
        assertEquals("完成需求文档", todos.get(0));
        assertEquals("跟进测试计划", todos.get(1));
    }

    @Test
    void parseTodos_numberedItems() {
        String content = """
                待办事项：
                1. 提交周报
                2、安排评审
                """;
        List<String> todos = MeetingSummaryTodoParser.parseTodos(content);
        assertEquals(2, todos.size());
        assertEquals("提交周报", todos.get(0));
        assertEquals("安排评审", todos.get(1));
    }

    @Test
    void parseTodos_markdownSection() {
        String content = """
                ## 会议总结
                摘要内容

                ## 会议待办
                - 帮助受灾群众
                - 展示了中国的军事实力
                """;
        List<String> todos = MeetingSummaryTodoParser.parseTodos(content);
        assertEquals(2, todos.size());
        assertEquals("帮助受灾群众", todos.get(0));
        assertEquals("展示了中国的军事实力", todos.get(1));
    }

    @Test
    void parseTodos_plainLinesWithoutBullets() {
        String content = """
                会议待办
                帮助受灾群众
                展示了中国的军事实力
                """;
        List<String> todos = MeetingSummaryTodoParser.parseTodos(content);
        assertEquals(2, todos.size());
        assertEquals("帮助受灾群众", todos.get(0));
        assertEquals("展示了中国的军事实力", todos.get(1));
    }

    @Test
    void parseTodos_emptyWhenNoItems() {
        assertTrue(MeetingSummaryTodoParser.parseTodos("").isEmpty());
        assertTrue(MeetingSummaryTodoParser.parseTodos("只有摘要，没有待办").isEmpty());
    }
}
