package com.cacch.integration.integration.oa.client;

import com.cacch.integration.common.config.oa.OaRegReportProperties;
import com.cacch.integration.integration.oa.client.dto.OaRegReportItemRow;
import com.cacch.integration.integration.oa.support.OaDbDialectSupport;
import com.cacch.integration.integration.oa.support.OaDbDialectSupport.DbProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 国内登记报告 OA 库查询客户端（只读 JOIN 主表与子表）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
public class OaRegReportDbClient {

    private static final String BIZ = "OaRegReportDb";

    private final ObjectProvider<JdbcTemplate> oaJdbcTemplateProvider;
    private final OaRegReportProperties regReportProperties;

    public OaRegReportDbClient(@Qualifier("oaJdbcTemplate") ObjectProvider<JdbcTemplate> oaJdbcTemplateProvider,
                               OaRegReportProperties regReportProperties) {
        this.oaJdbcTemplateProvider = oaJdbcTemplateProvider;
        this.regReportProperties = regReportProperties;
    }

    /**
     * 拉取资料列表行（主表 JOIN 子表）
     *
     * @param formMainId 可选主表 ID 过滤；null 表示全量
     * @param limit      最大行数，须 &gt; 0
     * @return 资料行列表；OA 数据源未配置时返回空列表
     */
    public List<OaRegReportItemRow> listRegReportItems(Long formMainId, int limit) {
        if (limit <= 0) {
            log.info("【{}】查询终止, reason=limit无效, limit={}", BIZ, limit);
            return Collections.emptyList();
        }
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            log.info("【{}】查询终止, reason=OA数据源未配置(oa.datasource.url)", BIZ);
            return Collections.emptyList();
        }

        String mainTable = regReportProperties.getFormMainTable();
        String subTable = regReportProperties.getFormSubTable();
        String fieldOwner = regReportProperties.getFieldOwner();
        String fieldIpdp = regReportProperties.getFieldIpdpName();
        String fieldItem = regReportProperties.getFieldItemName();
        String fieldAttachment = regReportProperties.getAttachmentField();
        String subFk = regReportProperties.getSubTableFk();

        String sql = """
                SELECT m.id AS form_main_id,
                       m.%s AS owner_name,
                       m.%s AS ipdp_name,
                       s.id AS sub_row_id,
                       s.%s AS item_name,
                       s.%s AS current_attachment_ref
                FROM %s m
                INNER JOIN %s s ON s.%s = m.id
                """.formatted(fieldOwner, fieldIpdp, fieldItem, fieldAttachment, mainTable, subTable, subFk);

        List<Object> args = new ArrayList<>();
        if (formMainId != null && formMainId > 0) {
            sql += " WHERE m.id = ?";
            args.add(formMainId);
        }
        sql += " ORDER BY m.id, s.id";

        DbProduct product = OaDbDialectSupport.detect(jdbc);
        sql = OaDbDialectSupport.appendPagination(sql, args, limit, product);
        log.info("【{}】使用分页方言, product={}", BIZ, product);

        try {
            List<OaRegReportItemRow> rows = jdbc.query(sql, new ItemRowMapper(), args.toArray());
            log.info("【{}】查询资料列表完成, formMainId={}, limit={}, count={}",
                    BIZ, formMainId, limit, rows.size());
            return rows;
        } catch (Exception e) {
            log.info("【{}】查询资料列表失败, formMainId={}, reason={}", BIZ, formMainId, e.getMessage());
            log.error("【{}】查询资料列表异常, formMainId={}", BIZ, formMainId, e);
            throw e;
        }
    }

    private static final class ItemRowMapper implements RowMapper<OaRegReportItemRow> {

        @Override
        public OaRegReportItemRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long formMainId = readLong(rs, "form_main_id");
            String ownerName = rs.getString("owner_name");
            String ipdpName = rs.getString("ipdp_name");
            Long subRowId = readLong(rs, "sub_row_id");
            String itemName = rs.getString("item_name");
            String attachmentRef = rs.getString("current_attachment_ref");
            return new OaRegReportItemRow(
                    formMainId,
                    trimToNull(ownerName),
                    trimToNull(ipdpName),
                    subRowId,
                    trimToNull(itemName),
                    trimToNull(attachmentRef));
        }

        /**
         * 读取数值型主键（兼容 Oracle NUMBER 与 SQL Server BIGINT）
         *
         * @param rs          结果集
         * @param columnLabel 列别名
         * @return Long 值；列为 NULL 时返回 null
         * @throws SQLException JDBC 异常
         */
        private static Long readLong(ResultSet rs, String columnLabel) throws SQLException {
            Object value = rs.getObject(columnLabel);
            if (value == null) {
                return null;
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && StringUtils.hasText(text)) {
                return Long.parseLong(text.trim());
            }
            return rs.getObject(columnLabel, Long.class);
        }

        private static String trimToNull(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            return value.trim();
        }
    }
}
