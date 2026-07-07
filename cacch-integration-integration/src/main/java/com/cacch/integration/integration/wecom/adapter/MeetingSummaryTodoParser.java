package com.cacch.integration.integration.wecom.adapter;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 企微智能会议纪要 TXT 待办解析适配器
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MeetingSummaryTodoParser {

    private static final Pattern SECTION_HEADER = Pattern.compile(
            "^(#{1,6}\\s*)?(会议待办|待办事项|待办清单|Action Items|TODO)\\s*[:：]?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern NEXT_SECTION = Pattern.compile(
            "^(#{1,6}\\s*)?(会议摘要|摘要|关键词|发言人|文字记录|会议记录|附录)\\s*[:：]?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TODO_ITEM = Pattern.compile(
            "^(?:[-*•]\\s+|\\d+[.)、、]\\s*)(.+)$");

    private MeetingSummaryTodoParser() {
    }

    /**
     * 从会议纪要 TXT 正文中解析待办事项列表
     *
     * @param content 纪要 TXT 全文
     * @return 待办文本列表（不含场次前缀），无待办时返回空列表
     */
    public static List<String> parseTodos(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        List<String> sectionItems = parseFromTodoSection(content);
        if (!sectionItems.isEmpty()) {
            return sectionItems;
        }
        return parseFallbackLines(content);
    }

    private static List<String> parseFromTodoSection(String content) {
        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n");
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
            if (SECTION_HEADER.matcher(line).matches()) {
                inSection = true;
                continue;
            }
            if (inSection && NEXT_SECTION.matcher(line).matches()) {
                break;
            }
            if (!inSection) {
                continue;
            }
            String item = extractTodoText(line);
            if (StringUtils.hasText(item)) {
                todos.add(item);
            }
        }
        return todos;
    }

    private static List<String> parseFallbackLines(String content) {
        List<String> todos = new ArrayList<>();
        for (String rawLine : content.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
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

    private static String extractTodoText(String line) {
        Matcher matcher = TODO_ITEM.matcher(line);
        if (matcher.matches()) {
            return normalizeTodoText(matcher.group(1));
        }
        if (line.startsWith("[")) {
            return normalizeTodoText(line);
        }
        return normalizeTodoText(line);
    }

    private static String normalizeTodoText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.trim()
                .replaceAll("^\\[(?:\\s|x|X)\\]\\s*", "")
                .replaceAll("^#+\\s*", "")
                .trim();
        return StringUtils.hasText(normalized) ? normalized : null;
    }
}
