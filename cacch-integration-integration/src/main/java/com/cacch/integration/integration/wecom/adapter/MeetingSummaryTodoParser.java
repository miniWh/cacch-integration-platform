package com.cacch.integration.integration.wecom.adapter;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 企微智能会议纪要待办解析适配器
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MeetingSummaryTodoParser {

    private static final int MAX_TODO_LINE_LENGTH = 120;

    private static final Pattern SECTION_HEADER = Pattern.compile(
            "^(#{1,6}\\s*)?(会议待办|待办事项|待办清单|Action Items|TODO)\\s*[:：]?\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern MARKDOWN_TODO_SECTION = Pattern.compile(
            "^#{1,6}\\s*(会议待办|待办事项|待办清单)\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern NEXT_SECTION = Pattern.compile(
            "^(#{1,6}\\s*)?(会议总结|会议摘要|摘要|关键词|发言人|文字记录|会议记录|附录)\\s*[:：]?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TODO_BLOCK = Pattern.compile(
            "(?:会议待办|待办事项|待办清单)\\s*[:：]?\\s*(?:\\r?\\n+)([\\s\\S]*?)"
                    + "(?=\\r?\\n(?:\\s*\\r?\\n)*(?:#{0,6}\\s*)?"
                    + "(?:会议总结|会议摘要|摘要|关键词|发言人|文字记录|附录)\\b|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TODO_ITEM = Pattern.compile(
            "^(?:[-*•●○◦▪]\\s+|\\d+[.)、、]\\s*)(.+)$");

    private static final Pattern TRANSCRIPT_LINE = Pattern.compile(".+\\(\\d{2}:\\d{2}:\\d{2}\\)\\s*[:：].*");

    private MeetingSummaryTodoParser() {
    }

    /**
     * 从会议纪要正文中解析待办事项列表
     *
     * @param content 纪要全文
     * @return 待办文本列表（不含场次前缀），无待办时返回 empty list
     */
    public static List<String> parseTodos(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        List<String> blockItems = parseFromTodoBlock(normalized);
        if (!blockItems.isEmpty()) {
            return blockItems;
        }
        List<String> sectionItems = parseFromTodoSection(normalized);
        if (!sectionItems.isEmpty()) {
            return sectionItems;
        }
        return parseFallbackLines(normalized);
    }

    private static List<String> parseFromTodoBlock(String content) {
        Matcher matcher = TODO_BLOCK.matcher(content);
        if (!matcher.find()) {
            return List.of();
        }
        return parseLinesInTodoBlock(matcher.group(1));
    }

    private static List<String> parseFromTodoSection(String content) {
        String[] lines = content.split("\n");
        boolean inSection = false;
        List<String> todos = new ArrayList<>();
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (!StringUtils.hasText(line)) {
                if (inSection && !todos.isEmpty()) {
                    break;
                }
                continue;
            }
            if (SECTION_HEADER.matcher(line).matches() || MARKDOWN_TODO_SECTION.matcher(line).matches()) {
                inSection = true;
                continue;
            }
            if (inSection && NEXT_SECTION.matcher(line).matches()) {
                break;
            }
            if (!inSection) {
                continue;
            }
            addTodoLine(todos, line);
        }
        return todos;
    }

    private static List<String> parseLinesInTodoBlock(String block) {
        List<String> todos = new ArrayList<>();
        for (String rawLine : block.split("\n")) {
            addTodoLine(todos, rawLine.trim());
        }
        return todos;
    }

    private static List<String> parseFallbackLines(String content) {
        List<String> todos = new ArrayList<>();
        for (String rawLine : content.split("\n")) {
            String line = rawLine.trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }
            Matcher matcher = TODO_ITEM.matcher(line);
            if (matcher.matches()) {
                String item = normalizeTodoText(matcher.group(1));
                if (StringUtils.hasText(item)) {
                    todos.add(item);
                }
            }
        }
        return todos;
    }

    private static void addTodoLine(List<String> todos, String line) {
        if (!looksLikeTodoLine(line)) {
            return;
        }
        Matcher matcher = TODO_ITEM.matcher(line);
        if (matcher.matches()) {
            String item = normalizeTodoText(matcher.group(1));
            if (StringUtils.hasText(item)) {
                todos.add(item);
            }
            return;
        }
        String item = normalizeTodoText(line);
        if (StringUtils.hasText(item)) {
            todos.add(item);
        }
    }

    private static boolean looksLikeTodoLine(String line) {
        if (!StringUtils.hasText(line)) {
            return false;
        }
        if (line.length() > MAX_TODO_LINE_LENGTH) {
            return false;
        }
        if (SECTION_HEADER.matcher(line).matches() || MARKDOWN_TODO_SECTION.matcher(line).matches()) {
            return false;
        }
        if (NEXT_SECTION.matcher(line).matches()) {
            return false;
        }
        if (TRANSCRIPT_LINE.matcher(line).matches()) {
            return false;
        }
        if (line.matches("^[.…。、,，\\-]+$")) {
            return false;
        }
        return !line.startsWith("##");
    }

    private static String normalizeTodoText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.trim()
                .replaceAll("^\\[(?:\\s|x|X)\\]\\s*", "")
                .replaceAll("^#+\\s*", "")
                .replaceAll("^[•●○◦▪]\\s*", "")
                .trim();
        return StringUtils.hasText(normalized) ? normalized : null;
    }
}
