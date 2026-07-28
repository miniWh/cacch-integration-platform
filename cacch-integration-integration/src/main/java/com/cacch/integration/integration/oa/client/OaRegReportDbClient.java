package com.cacch.integration.integration.oa.client;

import com.cacch.integration.common.config.oa.OaRegReportProperties;
import com.cacch.integration.integration.oa.client.dto.OaRegReportItemRow;
import com.cacch.integration.integration.oa.support.OaDbDialectSupport;
import com.cacch.integration.integration.oa.support.OaDbDialectSupport.DbProduct;
import com.cacch.integration.integration.oa.support.ReadOnlyOaJdbcTemplate;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 国内登记报告 OA 库查询客户端（只读 JOIN 主表与子表）
 *
 * <p><strong>禁止</strong>对 OA 库执行 UPDATE / INSERT / DELETE；附件绑定仅走 OA REST + CAP4 batch-update。</p>
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
     * 拉取资料列表行（主表 JOIN 子表，登记负责人由 org_member 解析为姓名）
     *
     * <p>未指定 {@code formMainId} 时按<strong>项目（主表）</strong>分批：先取最多 {@code formBatchSize} 个主表，
     * 再拉取这些主表下的全部资料子表行，避免单项目资料行占满 {@code subRowBatchSize} 导致其他项目被截断。</p>
     *
     * @param formMainId       可选主表 ID 过滤；null 表示按项目批次扫描
     * @param formBatchSize    单轮最多扫描主表（项目）数；{@code formMainId} 有值时忽略
     * @param subRowBatchSize  单表扫描时子表行上限；按项目批次扫描时不截断子表行
     * @param ownerNameFilter  可选登记负责人姓名过滤（测试联调用）；空表示不过滤
     * @return 资料行列表；OA 数据源未配置时返回空列表
     */
    public List<OaRegReportItemRow> listRegReportItems(Long formMainId,
                                                        int formBatchSize,
                                                        int subRowBatchSize,
                                                        String ownerNameFilter) {
        if (formBatchSize <= 0 && (formMainId == null || formMainId <= 0)) {
            log.info("【{}】查询终止, reason=formBatchSize无效, formBatchSize={}", BIZ, formBatchSize);
            return Collections.emptyList();
        }
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            log.info("【{}】查询终止, reason=OA数据源未配置(oa.datasource.url)", BIZ);
            return Collections.emptyList();
        }

        DbProduct product = OaDbDialectSupport.detect(jdbc);
        if (formMainId != null && formMainId > 0) {
            int rowLimit = Math.max(1, subRowBatchSize);
            return queryItemRows(jdbc, product, List.of(formMainId), rowLimit, ownerNameFilter, true);
        }

        List<Long> formMainIds = listFormMainIds(jdbc, product, formBatchSize, ownerNameFilter);
        if (formMainIds.isEmpty()) {
            log.info("【{}】未匹配到含资料子表的主表, ownerFilter={}", BIZ, ownerNameFilter);
            return Collections.emptyList();
        }
        log.info("【{}】本批次主表数={}, formMainIds={}", BIZ, formMainIds.size(), formMainIds);
        return queryItemRows(jdbc, product, formMainIds, 0, ownerNameFilter, false);
    }

    private List<Long> listFormMainIds(JdbcTemplate jdbc,
                                       DbProduct product,
                                       int formBatchSize,
                                       String ownerNameFilter) {
        String mainTable = regReportProperties.getFormMainTable();
        String subTable = regReportProperties.getFormSubTable();
        String fieldOwner = regReportProperties.getFieldOwner();
        String subFk = regReportProperties.getSubTableFk();
        String orgMemberTable = regReportProperties.getOrgMemberTable();
        String ownerJoin = OaDbDialectSupport.buildOwnerMemberJoin(orgMemberTable, "m", fieldOwner, product);

        String sql = """
                SELECT DISTINCT m.id AS form_main_id
                FROM %s m
                %s
                INNER JOIN %s s ON s.%s = m.id
                """.formatted(mainTable, ownerJoin, subTable, subFk);

        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(ownerNameFilter)) {
            sql += " WHERE om.name = ?";
            args.add(ownerNameFilter.trim());
        }
        sql += " ORDER BY m.id";

        sql = OaDbDialectSupport.appendPagination(sql, args, formBatchSize, product);
        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);

        List<Long> ids = jdbc.query(sql, (rs, rowNum) -> rs.getLong("form_main_id"), args.toArray());
        return ids.stream().distinct().toList();
    }

    private List<OaRegReportItemRow> queryItemRows(JdbcTemplate jdbc,
                                                   DbProduct product,
                                                   List<Long> formMainIds,
                                                   int subRowBatchSize,
                                                   String ownerNameFilter,
                                                   boolean applySubRowLimit) {
        if (formMainIds.isEmpty()) {
            return Collections.emptyList();
        }

        String mainTable = regReportProperties.getFormMainTable();
        String subTable = regReportProperties.getFormSubTable();
        String fieldOwner = regReportProperties.getFieldOwner();
        String fieldIpdp = regReportProperties.getFieldIpdpName();
        String fieldItem = regReportProperties.getFieldItemName();
        String fieldAttachment = regReportProperties.getAttachmentField();
        String subFk = regReportProperties.getSubTableFk();
        String orgMemberTable = regReportProperties.getOrgMemberTable();
        String ownerJoin = OaDbDialectSupport.buildOwnerMemberJoin(orgMemberTable, "m", fieldOwner, product);

        String inPlaceholders = formMainIds.stream().map(id -> "?").collect(Collectors.joining(", "));

        String sql = """
                SELECT m.id AS form_main_id,
                       om.name AS owner_name,
                       m.%s AS ipdp_name,
                       s.id AS sub_row_id,
                       s.%s AS item_name,
                       s.%s AS current_attachment_ref
                FROM %s m
                %s
                INNER JOIN %s s ON s.%s = m.id
                WHERE m.id IN (%s)
                """.formatted(fieldIpdp, fieldItem, fieldAttachment, mainTable, ownerJoin, subTable, subFk,
                inPlaceholders);

        List<Object> args = new ArrayList<>(formMainIds);
        if (StringUtils.hasText(ownerNameFilter)) {
            sql += " AND om.name = ?";
            args.add(ownerNameFilter.trim());
        }
        sql += " ORDER BY m.id, s.id";

        if (applySubRowLimit && subRowBatchSize > 0) {
            sql = OaDbDialectSupport.appendPagination(sql, args, subRowBatchSize, product);
        }

        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);
        log.info("【{}】查询资料子表, formCount={}, subRowLimit={}, ownerNameFilter={}",
                BIZ, formMainIds.size(), applySubRowLimit ? subRowBatchSize : "无", ownerNameFilter);

        try {
            List<OaRegReportItemRow> rows = jdbc.query(sql, new ItemRowMapper(), args.toArray());
            logDistinctProjects(rows);
            log.info("【{}】查询资料列表完成, formCount={}, subRowCount={}, ownerFilter={}",
                    BIZ, formMainIds.size(), rows.size(), ownerNameFilter);
            return rows;
        } catch (Exception e) {
            log.info("【{}】查询资料列表失败, formMainIds={}, reason={}", BIZ, formMainIds, e.getMessage());
            log.error("【{}】查询资料列表异常, formMainIds={}", BIZ, formMainIds, e);
            throw e;
        }
    }

    private void logDistinctProjects(List<OaRegReportItemRow> rows) {
        Set<String> projects = new LinkedHashSet<>();
        for (OaRegReportItemRow row : rows) {
            if (row.formMainId() != null && StringUtils.hasText(row.ipdpName())) {
                projects.add(row.formMainId() + ":" + row.ipdpName());
            }
        }
        log.info("【{}】本批次覆盖 IPDP 项目数={}, 项目={}", BIZ, projects.size(), projects);
    }

    private static final class ItemRowMapper implements RowMapper<OaRegReportItemRow> {

        @Override
        public OaRegReportItemRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            Long formMainId = readLong(rs, "form_main_id");
            String ownerName = trimToNull(rs.getString("owner_name"));
            String ipdpName = trimToNull(rs.getString("ipdp_name"));
            Long subRowId = readLong(rs, "sub_row_id");
            String itemName = trimToNull(rs.getString("item_name"));
            String attachmentRef = trimToNull(rs.getString("current_attachment_ref"));
            return new OaRegReportItemRow(
                    formMainId,
                    ownerName,
                    ipdpName,
                    subRowId,
                    itemName,
                    attachmentRef);
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
            return switch (value) {
                case null -> null;
                case Number number -> number.longValue();
                case String text when StringUtils.hasText(text) -> Long.parseLong(text.trim());
                default -> rs.getObject(columnLabel, Long.class);
            };
        }

        private static String trimToNull(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            return value.trim();
        }
    }
}
