package com.cacch.integration.integration.oa.support;

import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * OA 只读 JDBC 结果集列读取（兼容 Oracle 列名大写、别名不一致等驱动差异）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class OaJdbcResultSetSupport {

    private OaJdbcResultSetSupport() {
    }

    /**
     * 按列别名或列名（忽略大小写）读取 Long 值
     *
     * @param rs          结果集
     * @param columnLabel 列别名，如 form_main_id
     * @return Long 值；列为 NULL 时返回 null
     * @throws SQLException JDBC 异常
     */
    public static Long readLong(ResultSet rs, String columnLabel) throws SQLException {
        Object value = rs.getObject(resolveColumnIndex(rs, columnLabel));
        return switch (value) {
            case null -> null;
            case Number number -> number.longValue();
            case String text when StringUtils.hasText(text) -> Long.parseLong(text.trim());
            default -> {
                Long typed = rs.getObject(resolveColumnIndex(rs, columnLabel), Long.class);
                yield typed;
            }
        };
    }

    /**
     * 按列别名或列名（忽略大小写）读取字符串
     *
     * @param rs          结果集
     * @param columnLabel 列别名
     * @return trim 后的字符串；空值时返回 null
     * @throws SQLException JDBC 异常
     */
    public static String readString(ResultSet rs, String columnLabel) throws SQLException {
        String value = rs.getString(resolveColumnIndex(rs, columnLabel));
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 解析列索引（优先 columnLabel，其次 columnName，均忽略大小写）
     *
     * @param rs          结果集
     * @param columnLabel 期望列别名
     * @return 1-based 列索引
     * @throws SQLException 未找到列时抛出
     */
    public static int resolveColumnIndex(ResultSet rs, String columnLabel) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String label = meta.getColumnLabel(i);
            if (label != null && label.equalsIgnoreCase(columnLabel)) {
                return i;
            }
        }
        for (int i = 1; i <= columnCount; i++) {
            String name = meta.getColumnName(i);
            if (name != null && name.equalsIgnoreCase(columnLabel)) {
                return i;
            }
        }
        throw new SQLException("Column not found: " + columnLabel);
    }
}
