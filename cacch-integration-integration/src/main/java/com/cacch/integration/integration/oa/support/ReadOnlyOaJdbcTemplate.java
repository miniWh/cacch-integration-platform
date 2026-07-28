package com.cacch.integration.integration.oa.support;

import com.cacch.integration.common.constant.oa.OaRegReportConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
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

    private static final String BIZ = "OaRegReportDb";

    public ReadOnlyOaJdbcTemplate(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public int update(String sql) throws DataAccessException {
        rejectWrite("update", sql);
        return 0;
    }

    @Override
    public int update(String sql, PreparedStatementSetter pss) throws DataAccessException {
        rejectWrite("update", sql);
        return 0;
    }

    @Override
    public int update(String sql, Object... args) throws DataAccessException {
        rejectWrite("update", sql);
        return 0;
    }

    @Override
    public int update(PreparedStatementCreator psc) throws DataAccessException {
        rejectWrite("update", "PreparedStatementCreator");
        return 0;
    }

    @Override
    public int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) throws DataAccessException {
        rejectWrite("update", "PreparedStatementCreator+KeyHolder");
        return 0;
    }

    @Override
    public int[] batchUpdate(String sql, BatchPreparedStatementSetter pss) throws DataAccessException {
        rejectWrite("batchUpdate", sql);
        return new int[0];
    }

    @Override
    public int[] batchUpdate(String... sql) throws DataAccessException {
        rejectWrite("batchUpdate", sql != null && sql.length > 0 ? sql[0] : "");
        return new int[0];
    }

    @Override
    public void execute(String sql) throws DataAccessException {
        rejectWrite("execute", sql);
    }

    @Override
    public <T> T execute(StatementCallback<T> action) throws DataAccessException {
        rejectWrite("execute(StatementCallback)", "StatementCallback");
        return null;
    }

    @Override
    public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
        rejectWrite("execute(ConnectionCallback)", "ConnectionCallback");
        return null;
    }

    @Override
    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action)
            throws DataAccessException {
        rejectWrite("execute(PreparedStatementCallback)", "PreparedStatementCallback");
        return null;
    }

    @Override
    public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
        assertSelectOnly(sql);
        return super.execute(sql, action);
    }

    /**
     * 校验 SQL 仅为 SELECT（允许外层 ROWNUM / 子查询包装）
     *
     * @param sql SQL 文本
     */
    public static void assertSelectOnly(String sql) {
        if (sql == null || sql.isBlank()) {
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
