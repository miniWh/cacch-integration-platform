package com.cacch.integration.integration.oa.support;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 致远 OA 分布式 ID 读写与 API 传参（避免 JSON Number 精度丢失）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaIdSupport {

    private OaIdSupport() {
    }

    /**
     * 将 OA 主/子表 ID 转为 CAP4 REST 请求体中的字符串 ID
     *
     * @param oaId OA 库 id 原文（TO_CHAR 结果）
     * @return 非空字符串；输入为空时返回 null
     */
    public static String toApiId(String oaId) {
        if (!StringUtils.hasText(oaId)) {
            return null;
        }
        return oaId.trim();
    }

    /**
     * 将 OA ID 字符串转为 Long 供 PG 中间表存储（超出 long 范围时返回 null）
     *
     * @param oaId OA 库 id 原文
     * @return Long 值；无法精确转换时返回 null
     */
    public static Long toStorageLong(String oaId) {
        if (!StringUtils.hasText(oaId)) {
            return null;
        }
        try {
            return new BigDecimal(oaId.trim()).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    /**
     * 比较两个 OA ID 是否相同（忽略首尾空白）
     *
     * @param left  左值
     * @param right 右值
     * @return true 表示相同
     */
    public static boolean equalsId(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return false;
        }
        return left.trim().equals(right.trim());
    }

    /**
     * 解析游标 ID 用于 SQL 比较（无效时返回 0）
     *
     * @param cursorId 游标字符串
     * @return long 值
     */
    public static long parseCursorId(String cursorId) {
        if (!StringUtils.hasText(cursorId)) {
            return 0L;
        }
        try {
            return new BigDecimal(cursorId.trim()).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return 0L;
        }
    }
}
