package com.cacch.integration.integration.oa.client;

import com.cacch.integration.common.config.oa.OaRegReportProperties;
import com.cacch.integration.integration.oa.client.dto.OaRegReportItemRow;
import com.cacch.integration.integration.oa.support.OaDbDialectSupport;
import com.cacch.integration.integration.oa.support.OaDbDialectSupport.DbProduct;
import com.cacch.integration.integration.oa.support.OaJdbcResultSetSupport;
import com.cacch.integration.integration.oa.support.OaRegReportItemMatcher;
import com.cacch.integration.integration.oa.support.ReadOnlyOaJdbcTemplate;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;
import com.cacch.integration.integration.sharedrive.support.ShareDriveIpdpDirectorySupport;
import com.cacch.integration.integration.sharedrive.support.ShareDrivePathNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
            return queryItemRows(jdbc, product, List.of(String.valueOf(formMainId)), rowLimit, ownerNameFilter, true);
        }

        List<String> formMainIds = listFormMainIds(jdbc, product, formBatchSize, ownerNameFilter);
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
            return queryItemRows(jdbc, product, List.of(String.valueOf(formMainId)), 0, ownerNameFilter, false);
        }
        int batch = formBatchSize > 0 ? formBatchSize : 20;
        List<String> formMainIds = listFormMainIds(jdbc, product, batch, ownerNameFilter);
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
    public List<String> listFormMainIdsAfterCursor(String afterFormMainIdExclusive,
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
        return listFormMainIds(jdbc, product, limit, ownerNameFilter, afterFormMainIdExclusive);
    }

    /**
     * 按登记负责人姓名拉取资料子表行（OA org_member.name 精确匹配）
     *
     * @param ownerName  登记负责人姓名（共享盘 L1 目录名）
     * @param formMainId 可选主表 ID；非空时仅查该主表
     * @return 资料行列表
     */
    public List<OaRegReportItemRow> listItemRowsByOwnerName(String ownerName, String formMainId) {
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
    public List<OaRegReportItemRow> listItemRowsByFormMainIds(List<String> formMainIds) {
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
                                                     List<String> hintFormMainIds) {
        if (scanned == null) {
            return null;
        }
        if (formMainId != null && formMainId > 0) {
            List<OaRegReportItemRow> rows = listItemRowsByFormMainIds(List.of(String.valueOf(formMainId)));
            return OaRegReportItemMatcher.match(rows, scanned, String.valueOf(formMainId));
        }
        List<OaRegReportItemRow> ownerRows = listItemRowsByOwnerName(scanned.ownerName(), null);
        OaRegReportItemRow matched = OaRegReportItemMatcher.match(ownerRows, scanned, null, hintFormMainIds);
        if (matched != null) {
            return matched;
        }
        if (hintFormMainIds != null && !hintFormMainIds.isEmpty()) {
            List<OaRegReportItemRow> hintRows = listItemRowsByFormMainIds(hintFormMainIds);
            matched = OaRegReportItemMatcher.match(hintRows, scanned, null, hintFormMainIds);
            if (matched != null) {
                return matched;
            }
        }
        List<OaRegReportItemRow> ipdpItemRows = listItemRowsByIpdpProjectAndItem(
                scanned.ipdpName(), scanned.ipdpProjectNo(), scanned.itemName());
        matched = OaRegReportItemMatcher.match(ipdpItemRows, scanned,
                formMainId != null && formMainId > 0 ? String.valueOf(formMainId) : null,
                hintFormMainIds);
        if (matched != null) {
            return matched;
        }
        if (!ipdpItemRows.isEmpty()) {
            log.info("【{}】IPDP+项目编号+资料项目已命中但未能消歧, diskOwner={}, ipdp={}, projectNo={}, item={}, rowCount={}, owners={}",
                    BIZ, scanned.ownerName(), scanned.ipdpName(), scanned.ipdpProjectNo(), scanned.itemName(),
                    ipdpItemRows.size(),
                    ipdpItemRows.stream()
                            .map(OaRegReportItemRow::ownerName)
                            .filter(StringUtils::hasText)
                            .distinct()
                            .limit(10)
                            .toList());
        }
        return null;
    }

    /**
     * 按 IPDP 名称 + 项目编号 + 资料项目拉取候选行（负责人由后续路径匹配过滤）
     *
     * @param ipdpName      IPDP 名称（共享盘 L2 解析出的 field0160）
     * @param ipdpProjectNo IPDP 项目编号（共享盘 L2 解析出的 field0164）
     * @param itemName      资料项目名称（共享盘 L3 目录名）
     * @return 候选资料行，无数据时返回空列表
     */
    public List<OaRegReportItemRow> listItemRowsByIpdpProjectAndItem(String ipdpName,
                                                                     String ipdpProjectNo,
                                                                     String itemName) {
        if (!StringUtils.hasText(ipdpName) || !StringUtils.hasText(ipdpProjectNo) || !StringUtils.hasText(itemName)) {
            return Collections.emptyList();
        }
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            return Collections.emptyList();
        }
        DbProduct product = OaDbDialectSupport.detect(jdbc);
        List<OaRegReportItemRow> rows = queryItemRowsByItemName(jdbc, product, itemName.trim());
        if (rows.isEmpty()) {
            log.info("【{}】按资料项目精确反查无结果, item={}, 尝试子串匹配", BIZ, itemName);
            rows = queryItemRowsByItemNameContains(jdbc, product, itemName.trim());
        }
        List<OaRegReportItemRow> matched = rows.stream()
                .filter(row -> ShareDrivePathNormalizer.matchesIpdpNameLoosely(row.ipdpName(), ipdpName))
                .filter(row -> ShareDriveIpdpDirectorySupport.matchesProjectNo(row.ipdpProjectNo(), ipdpProjectNo))
                .toList();
        if (matched.isEmpty()) {
            matched = rows.stream()
                    .filter(row -> ShareDrivePathNormalizer.matchesIpdpNameLoosely(row.ipdpName(), ipdpName))
                    .toList();
            log.info("【{}】按 field0164 未命中，降级为 IPDP+资料项目反查, ipdp={}, diskProjectNo={}, item={}, matchedCount={}",
                    BIZ, ipdpName, ipdpProjectNo, itemName, matched.size());
        } else {
            log.info("【{}】按 IPDP+field0164+资料项目反查, ipdp={}, projectNo={}, item={}, sqlRowCount={}, matchedCount={}",
                    BIZ, ipdpName, ipdpProjectNo, itemName, rows.size(), matched.size());
        }
        return matched;
    }

    private List<OaRegReportItemRow> queryItemRowsByItemName(JdbcTemplate jdbc,
                                                             DbProduct product,
                                                             String itemName) {
        return queryItemRowsByItemPredicate(jdbc, product, "TRIM(%s) = ?", List.of(itemName));
    }

    private List<OaRegReportItemRow> queryItemRowsByItemNameContains(JdbcTemplate jdbc,
                                                                     DbProduct product,
                                                                     String itemName) {
        String predicate = switch (product) {
            case ORACLE -> "INSTR(TRIM(%s), ?) > 0";
            case SQL_SERVER -> "CHARINDEX(?, TRIM(%s)) > 0";
            case LIMIT -> "TRIM(%s) LIKE CONCAT('%%', ?, '%%')";
        };
        return queryItemRowsByItemPredicate(jdbc, product, predicate, List.of(itemName));
    }

    private List<OaRegReportItemRow> queryItemRowsByItemPredicate(JdbcTemplate jdbc,
                                                                  DbProduct product,
                                                                  String itemPredicateTemplate,
                                                                  List<Object> args) {
        String mainTable = regReportProperties.getFormMainTable();
        String subTable = regReportProperties.getFormSubTable();
        String fieldOwner = regReportProperties.getFieldOwner();
        String fieldIpdp = regReportProperties.getFieldIpdpName();
        String fieldIpdpProjectNo = regReportProperties.getFieldIpdpProjectNo();
        String fieldItem = regReportProperties.getFieldItemName();
        String fieldAttachment = regReportProperties.getAttachmentField();
        String subFk = regReportProperties.getSubTableFk();
        String orgMemberTable = regReportProperties.getOrgMemberTable();
        String ownerJoin = OaDbDialectSupport.buildOwnerMemberJoin(orgMemberTable, "m", fieldOwner, product);
        String ownerNameSelect = OaDbDialectSupport.selectOwnerNameColumn("m", fieldOwner, product);
        String itemRowSelect = buildItemRowSelectClause(
                product, ownerNameSelect, fieldIpdp, fieldIpdpProjectNo, fieldItem, fieldAttachment);
        String itemText = OaDbDialectSupport.castColumnAsText("s." + fieldItem, product);
        String itemPredicate = itemPredicateTemplate.formatted(itemText);

        String sql = """
                SELECT %s
                FROM %s m
                %s
                INNER JOIN %s s ON s.%s = m.id
                WHERE %s
                """.formatted(itemRowSelect, mainTable, ownerJoin, subTable, subFk, itemPredicate);

        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);
        try {
            return jdbc.query(sql, new ItemRowMapper(), args.toArray());
        } catch (Exception e) {
            log.info("【{}】按资料项目反查失败, itemPredicate={}, reason={}", BIZ, itemPredicate, e.getMessage());
            throw e;
        }
    }

    private List<String> listFormMainIds(JdbcTemplate jdbc,
                                       DbProduct product,
                                       int formBatchSize,
                                       String ownerNameFilter) {
        return listFormMainIds(jdbc, product, formBatchSize, ownerNameFilter, null);
    }

    private List<String> listFormMainIds(JdbcTemplate jdbc,
                                       DbProduct product,
                                       int formBatchSize,
                                       String ownerNameFilter,
                                       String afterFormMainIdExclusive) {
        String mainTable = regReportProperties.getFormMainTable();
        String subTable = regReportProperties.getFormSubTable();
        String fieldOwner = regReportProperties.getFieldOwner();
        String subFk = regReportProperties.getSubTableFk();
        String orgMemberTable = regReportProperties.getOrgMemberTable();
        String ownerJoin = OaDbDialectSupport.buildOwnerMemberJoin(orgMemberTable, "m", fieldOwner, product);
        String ownerFilterClause = OaDbDialectSupport.ownerNameEqualsClause("m", fieldOwner, product);
        String formMainIdSelect = OaDbDialectSupport.selectFormMainIdColumn("m", product);

        String sql = """
                SELECT DISTINCT %s
                FROM %s m
                %s
                INNER JOIN %s s ON s.%s = m.id
                """.formatted(formMainIdSelect, mainTable, ownerJoin, subTable, subFk);

        List<Object> args = new ArrayList<>();
        boolean hasWhere = false;
        if (StringUtils.hasText(afterFormMainIdExclusive) && !"0".equals(afterFormMainIdExclusive.trim())) {
            sql += " WHERE m.id > ?";
            args.add(new BigDecimal(afterFormMainIdExclusive.trim()));
            hasWhere = true;
        }
        if (StringUtils.hasText(ownerNameFilter)) {
            sql += hasWhere ? " AND " : " WHERE ";
            sql += ownerFilterClause;
            String trimmed = ownerNameFilter.trim();
            args.add(trimmed);
            args.add(trimmed);
            hasWhere = true;
        }
        sql += " ORDER BY m.id";

        sql = OaDbDialectSupport.appendPagination(sql, args, formBatchSize, product);
        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);

        List<String> ids = jdbc.query(sql,
                (rs, rowNum) -> OaJdbcResultSetSupport.readIdAsString(rs, "form_main_id"),
                args.toArray());
        return ids.stream().filter(StringUtils::hasText).distinct().toList();
    }

    private List<OaRegReportItemRow> queryItemRowsByOwner(JdbcTemplate jdbc,
                                                          DbProduct product,
                                                          String ownerName,
                                                          String formMainId) {
        String mainTable = regReportProperties.getFormMainTable();
        String subTable = regReportProperties.getFormSubTable();
        String fieldOwner = regReportProperties.getFieldOwner();
        String fieldIpdp = regReportProperties.getFieldIpdpName();
        String fieldIpdpProjectNo = regReportProperties.getFieldIpdpProjectNo();
        String fieldItem = regReportProperties.getFieldItemName();
        String fieldAttachment = regReportProperties.getAttachmentField();
        String subFk = regReportProperties.getSubTableFk();
        String orgMemberTable = regReportProperties.getOrgMemberTable();
        String ownerJoin = OaDbDialectSupport.buildOwnerMemberJoin(orgMemberTable, "m", fieldOwner, product);
        String ownerNameSelect = OaDbDialectSupport.selectOwnerNameColumn("m", fieldOwner, product);
        String ownerFilterClause = OaDbDialectSupport.ownerNameEqualsClause("m", fieldOwner, product);
        String itemRowSelect = buildItemRowSelectClause(
                product, ownerNameSelect, fieldIpdp, fieldIpdpProjectNo, fieldItem, fieldAttachment);

        String sql = """
                SELECT %s
                FROM %s m
                %s
                INNER JOIN %s s ON s.%s = m.id
                WHERE %s
                """.formatted(itemRowSelect, mainTable, ownerJoin, subTable, subFk, ownerFilterClause);

        List<Object> args = new ArrayList<>();
        args.add(ownerName);
        args.add(ownerName);
        if (StringUtils.hasText(formMainId)) {
            sql += " AND " + OaDbDialectSupport.selectFormMainIdColumn("m", product) + " = ?";
            args.add(formMainId.trim());
        }
        sql += " ORDER BY m.id, s.id";

        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);
        log.info("【{}】按负责人反查资料子表, ownerName={}, formMainId={}", BIZ, ownerName, formMainId);
        try {
            List<OaRegReportItemRow> rows = jdbc.query(sql, new ItemRowMapper(), args.toArray());
            if (rows.isEmpty()) {
                log.info("【{}】按负责人反查无结果, ownerName={}, hint=请核对 field0223 存 id/姓名及 org_member.name 是否与共享盘目录一致",
                        BIZ, ownerName);
            } else {
                List<String> formMainIds = rows.stream()
                        .map(OaRegReportItemRow::formMainId)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList();
                log.info("【{}】按负责人反查命中, ownerName={}, rowCount={}, formMainIds={}, sample={}",
                        BIZ, ownerName, rows.size(), formMainIds, summarizeRow(rows.getFirst()));
                if (formMainIds.isEmpty()) {
                    log.info("【{}】按负责人反查 formMainId 均为空, ownerName={}, hint=请检查 JDBC 列别名或 m.id 类型",
                            BIZ, ownerName);
                }
            }
            return rows;
        } catch (Exception e) {
            log.info("【{}】按负责人反查失败, ownerName={}, reason={}", BIZ, ownerName, e.getMessage());
            log.error("【{}】按负责人反查异常, ownerName={}", BIZ, ownerName, e);
            throw e;
        }
    }

    private List<OaRegReportItemRow> queryItemRows(JdbcTemplate jdbc,
                                                   DbProduct product,
                                                   List<String> formMainIds,
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
        String fieldIpdpProjectNo = regReportProperties.getFieldIpdpProjectNo();
        String fieldItem = regReportProperties.getFieldItemName();
        String fieldAttachment = regReportProperties.getAttachmentField();
        String subFk = regReportProperties.getSubTableFk();
        String orgMemberTable = regReportProperties.getOrgMemberTable();
        String ownerJoin = OaDbDialectSupport.buildOwnerMemberJoin(orgMemberTable, "m", fieldOwner, product);
        String ownerNameSelect = OaDbDialectSupport.selectOwnerNameColumn("m", fieldOwner, product);
        String ownerFilterClause = OaDbDialectSupport.ownerNameEqualsClause("m", fieldOwner, product);
        String itemRowSelect = buildItemRowSelectClause(
                product, ownerNameSelect, fieldIpdp, fieldIpdpProjectNo, fieldItem, fieldAttachment);

        String inPlaceholders = formMainIds.stream().map(id -> "?").collect(Collectors.joining(", "));

        String sql = """
                SELECT %s
                FROM %s m
                %s
                INNER JOIN %s s ON s.%s = m.id
                WHERE m.id IN (%s)
                """.formatted(itemRowSelect, mainTable, ownerJoin, subTable, subFk, inPlaceholders);

        List<Object> args = formMainIds.stream()
                .filter(StringUtils::hasText)
                .map(id -> new BigDecimal(id.trim()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (StringUtils.hasText(ownerNameFilter)) {
            sql += " AND " + ownerFilterClause;
            String trimmed = ownerNameFilter.trim();
            args.add(trimmed);
            args.add(trimmed);
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
                projects.add(row.formMainId() + ":" + row.ipdpName() + "(" + row.ipdpProjectNo() + ")");
            }
        }
        log.info("【{}】本批次覆盖 IPDP 项目数={}, 项目={}", BIZ, projects.size(), projects);
    }

    private static String buildItemRowSelectClause(DbProduct product,
                                                   String ownerNameSelect,
                                                   String fieldIpdp,
                                                   String fieldIpdpProjectNo,
                                                   String fieldItem,
                                                   String fieldAttachment) {
        return String.join(", ",
                OaDbDialectSupport.selectFormMainIdColumn("m", product),
                ownerNameSelect,
                OaDbDialectSupport.selectTextColumn("m", fieldIpdp, "ipdp_name", product),
                OaDbDialectSupport.selectTextColumn("m", fieldIpdpProjectNo, "ipdp_project_no", product),
                OaDbDialectSupport.selectSubRowIdColumn("s", product),
                OaDbDialectSupport.selectTextColumn("s", fieldItem, "item_name", product),
                OaDbDialectSupport.selectTextColumn("s", fieldAttachment, "current_attachment_ref", product));
    }

    private static String summarizeRow(OaRegReportItemRow row) {
        if (row == null) {
            return "null";
        }
        return "formMainId=" + row.formMainId()
                + ", subRowId=" + row.subRowId()
                + ", ipdp=" + row.ipdpName()
                + ", projectNo=" + row.ipdpProjectNo()
                + ", item=" + row.itemName();
    }

    private static final class ItemRowMapper implements RowMapper<OaRegReportItemRow> {

        @Override
        public OaRegReportItemRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            String formMainId = OaJdbcResultSetSupport.readIdAsString(rs, "form_main_id");
            String ownerName = OaJdbcResultSetSupport.readString(rs, "owner_name");
            String ipdpName = OaJdbcResultSetSupport.readString(rs, "ipdp_name");
            String ipdpProjectNo = OaJdbcResultSetSupport.readString(rs, "ipdp_project_no");
            String subRowId = OaJdbcResultSetSupport.readIdAsString(rs, "sub_row_id");
            String itemName = OaJdbcResultSetSupport.readString(rs, "item_name");
            String attachmentRef = OaJdbcResultSetSupport.readString(rs, "current_attachment_ref");
            return new OaRegReportItemRow(
                    formMainId,
                    ownerName,
                    ipdpName,
                    ipdpProjectNo,
                    subRowId,
                    itemName,
                    attachmentRef);
        }
    }
}
