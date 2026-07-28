package com.cacch.integration.integration.oa.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * OA 库 JDBC 方言识别与分页 SQL 拼装
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
public final class OaDbDialectSupport {

    private static final String BIZ = "OaRegReportDb";

    private OaDbDialectSupport() {
    }

    /**
     * OA 库产品类型
     */
    public enum DbProduct {

        /**
         * Microsoft SQL Server
         */
        SQL_SERVER,

        /**
         * Oracle Database
         */
        ORACLE,

        /**
         * 支持 LIMIT 语法的库（如 PostgreSQL/MySQL）
         */
        LIMIT
    }

    /**
     * 识别 JDBC 连接的数据库产品类型
     *
     * @param jdbc OA 库 JdbcTemplate
     * @return 数据库类型；识别失败时返回 {@link DbProduct#ORACLE}（致远 OA 常见部署）
     */
    public static DbProduct detect(JdbcTemplate jdbc) {
        try (Connection connection = jdbc.getDataSource().getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null) {
                log.info("【{}】识别数据库类型终止, reason=productName为空, 默认 Oracle", BIZ);
                return DbProduct.ORACLE;
            }
            String normalized = product.toLowerCase();
            if (normalized.contains("sql server")) {
                return DbProduct.SQL_SERVER;
            }
            if (normalized.contains("oracle")) {
                return DbProduct.ORACLE;
            }
            log.info("【{}】未识别数据库类型, product={}, 默认 LIMIT 分页", BIZ, product);
            return DbProduct.LIMIT;
        } catch (SQLException e) {
            log.info("【{}】识别数据库类型失败, reason={}, 默认 Oracle", BIZ, e.getMessage());
            return DbProduct.ORACLE;
        }
    }

    /**
     * 为查询 SQL 追加方言分页并写入参数列表
     *
     * @param baseSql 已含 SELECT / JOIN / 可选 WHERE / ORDER BY 的 SQL
     * @param args    已有绑定参数（会被追加 limit 参数）
     * @param limit   最大行数
     * @param product 数据库类型
     * @return 可执行的分页 SQL
     */
    public static String appendPagination(String baseSql, List<Object> args, int limit, DbProduct product) {
        return switch (product) {
            case SQL_SERVER -> {
                args.add(limit);
                yield baseSql + " OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
            }
            case ORACLE -> {
                args.add(limit);
                yield "SELECT * FROM (" + baseSql + ") oa_q WHERE ROWNUM <= ?";
            }
            case LIMIT -> {
                args.add(limit);
                yield baseSql + " LIMIT ?";
            }
        };
    }

    /**
     * 登记负责人字段（存 org_member.id）与人员表 JOIN 片段
     *
     * @param orgMemberTable   人员表名，如 org_member
     * @param formMainAlias    主表别名
     * @param ownerFieldColumn 负责人字段列名，如 field0223
     * @param product          数据库类型
     * @return LEFT JOIN SQL 片段
     */
    public static String buildOwnerMemberJoin(String orgMemberTable,
                                              String formMainAlias,
                                              String ownerFieldColumn,
                                              DbProduct product) {
        String ownerCol = formMainAlias + "." + ownerFieldColumn;
        return switch (product) {
            case ORACLE -> " LEFT JOIN " + orgMemberTable
                    + " om ON TO_CHAR(om.id) = TRIM(" + ownerCol + ")";
            case SQL_SERVER -> " LEFT JOIN " + orgMemberTable
                    + " om ON CAST(om.id AS VARCHAR(32)) = LTRIM(RTRIM(" + ownerCol + "))";
            case LIMIT -> " LEFT JOIN " + orgMemberTable
                    + " om ON CAST(om.id AS CHAR) = TRIM(" + ownerCol + ")";
        };
    }

    /**
     * 根据 JDBC URL 推断驱动类名（未显式配置 driver-class-name 时使用）
     *
     * @param jdbcUrl JDBC URL
     * @return 驱动类名
     */
    public static String inferDriverClassName(String jdbcUrl) {
        return com.cacch.integration.common.config.oa.OaDataSourceProperties.inferDriverClassName(jdbcUrl);
    }
}
