package com.cacch.integration.integration.oa.support;

import org.apache.commons.lang3.StringUtils;

import com.cacch.integration.common.constant.oa.OaRegReportConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.jdbc.support.KeyHolder;

import javax.sql.DataSource;
import java.util.Locale;

/**
 * OA 库只读 JdbcTemplate：禁止 UPDATE / INSERT / DELETE / DDL 等写操作
 *
 * @author hongfu_zhou@cacch.com
 */
public class ReadOnlyOaJdbcTemplate extends JdbcTemplate {

    public ReadOnlyOaJdbcTemplate(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public int update(@NonNull String sql) throws DataAccessException {
        rejectWrite("update", sql);
        return 0;
    }

    @Override
    public int update(@NonNull String sql, PreparedStatementSetter pss) throws DataAccessException {
        rejectWrite("update", sql);
        return 0;
    }

    @Override
    public int update(@NonNull String sql, Object... args) throws DataAccessException {
        rejectWrite("update", sql);
        return 0;
    }

    @Override
    public int update(@NonNull PreparedStatementCreator psc) throws DataAccessException {
        rejectWrite("update", "PreparedStatementCreator");
        return 0;
    }

    @Override
    public int update(@NonNull PreparedStatementCreator psc, @NonNull KeyHolder generatedKeyHolder) throws DataAccessException {
        rejectWrite("update", "PreparedStatementCreator+KeyHolder");
        return 0;
    }

    @Override
    public int @NonNull [] batchUpdate(@NonNull String sql, @NonNull BatchPreparedStatementSetter pss) throws DataAccessException {
        rejectWrite("batchUpdate", sql);
        return new int[0];
    }

    @Override
    public int @NonNull [] batchUpdate(String @NonNull ... sql) throws DataAccessException {
        rejectWrite("batchUpdate", sql.length > 0 ? sql[0] : "");
        return new int[0];
    }

    @Override
    public void execute(@NonNull String sql) throws DataAccessException {
        rejectWrite("execute", sql);
    }

    @Override
    public <T> T execute(@NonNull StatementCallback<T> action) throws DataAccessException {
        rejectWrite("execute(StatementCallback)", "StatementCallback");
        return null;
    }

    @Override
    public <T> T execute(@NonNull ConnectionCallback<T> action) throws DataAccessException {
        rejectWrite("execute(ConnectionCallback)", "ConnectionCallback");
        return null;
    }

    @Override
    public <T> T execute(@NonNull PreparedStatementCreator psc, @NonNull PreparedStatementCallback<T> action)
            throws DataAccessException {
        rejectWrite("execute(PreparedStatementCallback)", "PreparedStatementCallback");
        return null;
    }

    @Override
    public <T> T execute(@NonNull String sql, @NonNull PreparedStatementCallback<T> action) throws DataAccessException {
        assertSelectOnly(sql);
        return super.execute(sql, action);
    }

    /**
     * 校验 SQL 仅为 SELECT（允许外层 ROWNUM / 子查询包装）
     *
     * @param sql SQL 文本
     */
    public static void assertSelectOnly(String sql) {
        if (StringUtils.isBlank(sql)) {
            throw new BizException(ResultCode.PARAM_INVALID, "OA 库 SQL 不能为空");
        }
        String normalized = sql.stripLeading().toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("SELECT")) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    "OA 库禁止非 SELECT 操作，当前 SQL 前缀: " + normalized.substring(0, Math.min(20, normalized.length())));
        }
        assertNoForbiddenKeywords(normalized);
    }

    private static void assertNoForbiddenKeywords(String normalizedSql) {
        String[] forbidden = {" UPDATE ", " INSERT ", " DELETE ", " MERGE ", " DROP ", " ALTER ", " TRUNCATE "};
        String padded = " " + normalizedSql + " ";
        for (String keyword : forbidden) {
            if (padded.contains(keyword)) {
                throw new BizException(ResultCode.PARAM_INVALID, "OA 库 SQL 含禁止关键字: " + keyword.trim());
            }
        }
    }

    private static void rejectWrite(String operation, String sql) {
        throw new BizException(ResultCode.PARAM_INVALID,
                OaRegReportConstants.OA_DB_READ_ONLY_POLICY + "; 禁止 " + operation + "; sql=" + abbreviate(sql));
    }

    private static String abbreviate(String sql) {
        if (sql == null) {
            return "";
        }
        String trimmed = sql.trim();
        return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 120) + "...";
    }
}
