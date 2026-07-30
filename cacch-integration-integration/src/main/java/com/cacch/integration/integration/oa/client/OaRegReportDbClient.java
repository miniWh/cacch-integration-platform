package com.cacch.integration.integration.oa.client;

import com.cacch.integration.common.config.oa.OaRegReportProperties;
import com.cacch.integration.integration.oa.client.dto.OaRegReportItemRow;
import com.cacch.integration.integration.oa.support.OaDbDialectSupport;
import com.cacch.integration.integration.oa.support.OaDbDialectSupport.DbProduct;
import com.cacch.integration.integration.oa.support.OaRegReportItemMatcher;
import com.cacch.integration.integration.oa.support.ReadOnlyOaJdbcTemplate;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;
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

    /**
     * 拉取 OA 资料行供共享盘反查匹配（不做子表行数截断）
     *
     * @param formMainId      可选主表 ID；null 表示按负责人过滤拉取多主表
     * @param formBatchSize   formMainId 为空时最多拉取的主表数
     * @param ownerNameFilter 可选登记负责人姓名过滤
     * @return 资料行列表
     */
    public List<OaRegReportItemRow> listRegReportItemsForLookup(Long formMainId,
                                                                int formBatchSize,
                                                                String ownerNameFilter) {
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            log.info("【{}】反查终止, reason=OA数据源未配置(oa.datasource.url)", BIZ);
            return Collections.emptyList();
        }
        DbProduct product = OaDbDialectSupport.detect(jdbc);
        if (formMainId != null && formMainId > 0) {
            return queryItemRows(jdbc, product, List.of(formMainId), 0, ownerNameFilter, false);
        }
        int batch = formBatchSize > 0 ? formBatchSize : 20;
        List<Long> formMainIds = listFormMainIds(jdbc, product, batch, ownerNameFilter);
        if (formMainIds.isEmpty()) {
            return Collections.emptyList();
        }
        return queryItemRows(jdbc, product, formMainIds, 0, null, false);
    }

    /**
     * 按游标分页拉取下一批 OA 主表 ID（{@code id > afterFormMainId}，升序）
     *
     * @param afterFormMainIdExclusive 上一批最大主表 ID；0 表示从最小 ID 开始
     * @param limit                    本批最多主表数
     * @param ownerNameFilter          可选登记负责人精确过滤（测试联调）
     * @return 主表 ID 列表，无数据时返回空列表
     */
    public List<Long> listFormMainIdsAfterCursor(long afterFormMainIdExclusive,
                                                 int limit,
                                                 String ownerNameFilter) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            log.info("【{}】主表游标查询终止, reason=OA数据源未配置", BIZ);
            return Collections.emptyList();
        }
        DbProduct product = OaDbDialectSupport.detect(jdbc);
        return listFormMainIds(jdbc, product, limit, ownerNameFilter, Math.max(afterFormMainIdExclusive, 0L));
    }

    /**
     * 按登记负责人姓名拉取资料子表行（OA org_member.name 精确匹配）
     *
     * @param ownerName  登记负责人姓名（共享盘 L1 目录名）
     * @param formMainId 可选主表 ID；非空时仅查该主表
     * @return 资料行列表
     */
    public List<OaRegReportItemRow> listItemRowsByOwnerName(String ownerName, Long formMainId) {
        if (!StringUtils.hasText(ownerName)) {
            return Collections.emptyList();
        }
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            return Collections.emptyList();
        }
        DbProduct product = OaDbDialectSupport.detect(jdbc);
        return queryItemRowsByOwner(jdbc, product, ownerName.trim(), formMainId);
    }

    /**
     * 按主表 ID 列表拉取资料子表行（不做子表行数截断）
     *
     * @param formMainIds 主表 ID 列表，不可为空
     * @return 资料行列表
     */
    public List<OaRegReportItemRow> listItemRowsByFormMainIds(List<Long> formMainIds) {
        if (formMainIds == null || formMainIds.isEmpty()) {
            return Collections.emptyList();
        }
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            return Collections.emptyList();
        }
        DbProduct product = OaDbDialectSupport.detect(jdbc);
        return queryItemRows(jdbc, product, formMainIds, 0, null, false);
    }

    /**
     * 按共享盘三级目录反查 OA 资料行（负责人动态加载 + 路径匹配）
     *
     * @param scanned           共享盘扫描结果
     * @param formMainId        可选主表 ID 过滤
     * @param hintFormMainIds   主表游标批次 ID，用于负责人精确名未命中时的补充反查
     * @return 匹配的资料行；无匹配或歧义时返回 null
     */
    public OaRegReportItemRow findItemRowByDirectory(ShareDriveScannedItem scanned,
                                                     Long formMainId,
                                                     List<Long> hintFormMainIds) {
        if (scanned == null) {
            return null;
        }
        if (formMainId != null && formMainId > 0) {
            List<OaRegReportItemRow> rows = listItemRowsByFormMainIds(List.of(formMainId));
            return OaRegReportItemMatcher.match(rows, scanned, formMainId);
        }
        List<OaRegReportItemRow> ownerRows = listItemRowsByOwnerName(scanned.ownerName(), null);
        OaRegReportItemRow matched = OaRegReportItemMatcher.match(ownerRows, scanned, null);
        if (matched != null) {
            return matched;
        }
        if (hintFormMainIds != null && !hintFormMainIds.isEmpty()) {
            List<OaRegReportItemRow> hintRows = listItemRowsByFormMainIds(hintFormMainIds);
            matched = OaRegReportItemMatcher.match(hintRows, scanned, null);
            if (matched != null) {
                return matched;
            }
        }
        return null;
    }

    private List<Long> listFormMainIds(JdbcTemplate jdbc,
                                       DbProduct product,
                                       int formBatchSize,
                                       String ownerNameFilter) {
        return listFormMainIds(jdbc, product, formBatchSize, ownerNameFilter, 0L);
    }

    private List<Long> listFormMainIds(JdbcTemplate jdbc,
                                       DbProduct product,
                                       int formBatchSize,
                                       String ownerNameFilter,
                                       long afterFormMainIdExclusive) {
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
        if (afterFormMainIdExclusive > 0) {
            sql += " WHERE m.id > ?";
            args.add(afterFormMainIdExclusive);
        }
        if (StringUtils.hasText(ownerNameFilter)) {
            sql += afterFormMainIdExclusive > 0 ? " AND om.name = ?" : " WHERE om.name = ?";
            args.add(ownerNameFilter.trim());
        }
        sql += " ORDER BY m.id";

        sql = OaDbDialectSupport.appendPagination(sql, args, formBatchSize, product);
        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);

        List<Long> ids = jdbc.query(sql, (rs, rowNum) -> rs.getLong("form_main_id"), args.toArray());
        return ids.stream().distinct().toList();
    }

    private List<OaRegReportItemRow> queryItemRowsByOwner(JdbcTemplate jdbc,
                                                          DbProduct product,
                                                          String ownerName,
                                                          Long formMainId) {
        String mainTable = regReportProperties.getFormMainTable();
        String subTable = regReportProperties.getFormSubTable();
        String fieldOwner = regReportProperties.getFieldOwner();
        String fieldIpdp = regReportProperties.getFieldIpdpName();
        String fieldItem = regReportProperties.getFieldItemName();
        String fieldAttachment = regReportProperties.getAttachmentField();
        String subFk = regReportProperties.getSubTableFk();
        String orgMemberTable = regReportProperties.getOrgMemberTable();
        String ownerJoin = OaDbDialectSupport.buildOwnerMemberJoin(orgMemberTable, "m", fieldOwner, product);

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
                WHERE om.name = ?
                """.formatted(fieldIpdp, fieldItem, fieldAttachment, mainTable, ownerJoin, subTable, subFk);

        List<Object> args = new ArrayList<>();
        args.add(ownerName);
        if (formMainId != null && formMainId > 0) {
            sql += " AND m.id = ?";
            args.add(formMainId);
        }
        sql += " ORDER BY m.id, s.id";

        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);
        log.info("【{}】按负责人反查资料子表, ownerName={}, formMainId={}", BIZ, ownerName, formMainId);
        try {
            return jdbc.query(sql, new ItemRowMapper(), args.toArray());
        } catch (Exception e) {
            log.info("【{}】按负责人反查失败, ownerName={}, reason={}", BIZ, ownerName, e.getMessage());
            log.error("【{}】按负责人反查异常, ownerName={}", BIZ, ownerName, e);
            throw e;
        }
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
