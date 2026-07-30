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
     * 登记负责人字段（field0223 可能存 org_member.id 或人员姓名）与人员表 JOIN 片段
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
        String ownerText = castOwnerFieldAsText(ownerCol, product);
        return switch (product) {
            case ORACLE -> " LEFT JOIN " + orgMemberTable
                    + " om ON (TO_CHAR(om.id) = TRIM(" + ownerText + ")"
                    + " OR TRIM(om.name) = TRIM(" + ownerText + "))";
            case SQL_SERVER -> " LEFT JOIN " + orgMemberTable
                    + " om ON (CAST(om.id AS VARCHAR(32)) = LTRIM(RTRIM(" + ownerText + "))"
                    + " OR LTRIM(RTRIM(om.name)) = LTRIM(RTRIM(" + ownerText + ")))";
            case LIMIT -> " LEFT JOIN " + orgMemberTable
                    + " om ON (CAST(om.id AS CHAR) = TRIM(" + ownerText + ")"
                    + " OR TRIM(om.name) = TRIM(" + ownerText + "))";
        };
    }

    /**
     * SELECT 中登记负责人展示列（优先 org_member.name，否则取 field0223 原文）
     *
     * @param formMainAlias    主表别名
     * @param ownerFieldColumn 负责人字段列名
     * @param product          数据库类型
     * @return SELECT 片段，别名为 owner_name
     */
    public static String selectOwnerNameColumn(String formMainAlias,
                                               String ownerFieldColumn,
                                               DbProduct product) {
        String ownerText = castOwnerFieldAsText(formMainAlias + "." + ownerFieldColumn, product);
        return "COALESCE(om.name, TRIM(" + ownerText + ")) AS owner_name";
    }

    /**
     * 按登记负责人姓名过滤（匹配 org_member.name 或 field0223 存姓名场景）
     *
     * @param formMainAlias    主表别名
     * @param ownerFieldColumn 负责人字段列名
     * @param product          数据库类型
     * @return WHERE/AND 条件片段，须绑定两次同一姓名参数
     */
    public static String ownerNameEqualsClause(String formMainAlias,
                                               String ownerFieldColumn,
                                               DbProduct product) {
        String ownerText = castOwnerFieldAsText(formMainAlias + "." + ownerFieldColumn, product);
        return "(om.name = ? OR TRIM(" + ownerText + ") = ?)";
    }

    private static String castOwnerFieldAsText(String ownerColumnExpr, DbProduct product) {
        return switch (product) {
            case ORACLE -> "TO_CHAR(" + ownerColumnExpr + ")";
            case SQL_SERVER -> "CAST(" + ownerColumnExpr + " AS NVARCHAR(512))";
            case LIMIT -> "CAST(" + ownerColumnExpr + " AS CHAR)";
        };
    }

    /**
     * 将列转为文本以便 TRIM 比较（供 OA 反查 SQL 拼装）
     *
     * @param columnExpr 列表达式，如 m.field0160
     * @param product    数据库类型
     * @return 文本表达式
     */
    public static String castColumnAsText(String columnExpr, DbProduct product) {
        return castOwnerFieldAsText(columnExpr, product);
    }

    /**
     * SELECT 主表 ID 列（统一 TO_CHAR/CAST，避免 Oracle JDBC 按别名读 Long 失败）
     *
     * @param formMainAlias 主表别名
     * @param product       数据库类型
     * @return SELECT 片段，别名为 form_main_id
     */
    public static String selectFormMainIdColumn(String formMainAlias, DbProduct product) {
        return castColumnAsText(formMainAlias + ".id", product) + " AS form_main_id";
    }

    /**
     * SELECT 子表行 ID 列
     *
     * @param subTableAlias 子表别名
     * @param product       数据库类型
     * @return SELECT 片段，别名为 sub_row_id
     */
    public static String selectSubRowIdColumn(String subTableAlias, DbProduct product) {
        return castColumnAsText(subTableAlias + ".id", product) + " AS sub_row_id";
    }

    /**
     * SELECT 文本字段列（统一 CAST 后 JDBC 以字符串读取）
     *
     * @param tableAlias   表别名
     * @param columnName   列名
     * @param resultAlias  结果别名
     * @param product      数据库类型
     * @return SELECT 片段
     */
    public static String selectTextColumn(String tableAlias,
                                          String columnName,
                                          String resultAlias,
                                          DbProduct product) {
        return castColumnAsText(tableAlias + "." + columnName, product) + " AS " + resultAlias;
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
