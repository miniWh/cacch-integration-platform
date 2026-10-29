package com.cacch.integration.integration.oa.client;

import com.cacch.integration.common.constant.oa.OaConstants;
import com.cacch.integration.common.constant.oa.OaContractAttachmentConstants;
import com.cacch.integration.integration.oa.client.dto.OaContractRow;
import com.cacch.integration.integration.oa.support.OaJdbcResultSetSupport;
import com.cacch.integration.integration.oa.support.ReadOnlyOaJdbcTemplate;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * OA 购销合同扫描客户端（只读扫描 formmain_5296，中间表 NOT IN 增量）
 *
 * <p>扫描条件（REQ-OA-003 / F2）：{@code FINISHEDFLAG=1}、{@code field0273}（NC 单号）非空、
 * {@code field0360}（附件引用）非空，且 {@code EXISTS formmain_2817}（field0329 = field0273 且
 * field0493 = 'crm订单'，即合同来源于 CRM 推送的销售订单）。</p>
 *
 * <p>增量策略：入参为中间表已入库 {@code oa_form_main_id} 排除列表，按分片（≤1000，规避
 * ORA-01795）逐片执行 {@code fm.ID NOT IN (分片)} 查询。<strong>注意</strong>：单个分片查询
 * 只排除了当前分片，其他分片已入库的 ID 仍会出现在本片结果中，因此各片结果必须按全集
 * 排除列表二次过滤并跨片去重后合并，否则会把已入库数据重复带回。</p>
 *
 * <p>排除列表为空（首次全量初始化）时不拼 NOT IN 条件，一次拉取全部符合条件数据。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
public class OaContractDbClient {

    private final ObjectProvider<JdbcTemplate> oaJdbcTemplateProvider;

    /**
     * @param oaJdbcTemplateProvider OA 只读库 JdbcTemplate（未配置时 getIfAvailable 返回 null）
     */
    public OaContractDbClient(@Qualifier("oaJdbcTemplate") ObjectProvider<JdbcTemplate> oaJdbcTemplateProvider) {
        this.oaJdbcTemplateProvider = oaJdbcTemplateProvider;
    }

    /**
     * 按 NOT IN 排除列表增量扫描符合条件的购销合同
     *
     * @param excludedFormMainIds 中间表已入库的 formmain_5296.ID 排除列表，可空
     * @param chunkSize           NOT IN 分片大小，自动收敛到 [1, 1000]
     * @return 未入库的合同行（已跨片去重）；OA 数据源未配置时返回空列表
     */
    public List<OaContractRow> scanContractsNotIn(List<Long> excludedFormMainIds, int chunkSize) {
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            log.info("【{}】扫描终止, reason=OA数据源未配置(oa.datasource.url)", OaContractAttachmentConstants.LOG_BIZ);
            return List.of();
        }
        List<Long> excluded = excludedFormMainIds == null
                ? List.of()
                : excludedFormMainIds.stream().filter(Objects::nonNull).distinct().toList();
        int effectiveChunk = Math.min(Math.max(chunkSize, 1), OaContractAttachmentConstants.MAX_NOT_IN_CHUNK);

        if (excluded.isEmpty()) {
            List<OaContractRow> rows = queryByNotInIds(jdbc, List.of());
            log.info("【{}】扫描完成, mode=排除列表为空(全量初始化), rows={}",
                    OaContractAttachmentConstants.LOG_BIZ, rows.size());
            return rows;
        }

        List<List<Long>> chunks = partition(excluded, effectiveChunk);
        Set<Long> excludedSet = new HashSet<>(excluded);
        Set<Long> seen = new HashSet<>();
        List<OaContractRow> merged = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            List<OaContractRow> chunkRows = queryByNotInIds(jdbc, chunks.get(i));
            int added = 0;
            for (OaContractRow row : chunkRows) {
                if (row.oaFormMainId() != null
                        && !excludedSet.contains(row.oaFormMainId())
                        && seen.add(row.oaFormMainId())) {
                    merged.add(row);
                    added++;
                }
            }
            log.info("【{}】分片扫描完成, chunk={}/{}, 命中={}, 有效={}",
                    OaContractAttachmentConstants.LOG_BIZ, i + 1, chunks.size(), chunkRows.size(), added);
        }
        log.info("【{}】扫描完成, 已入库排除={}, 分片={}, 有效新增={}",
                OaContractAttachmentConstants.LOG_BIZ, excluded.size(), chunks.size(), merged.size());
        return merged;
    }

    /**
     * 执行单次 NOT IN 扫描查询
     *
     * @param jdbc     OA 只读 JdbcTemplate
     * @param notInIds NOT IN 排除 ID；为空时不拼 NOT IN 条件
     * @return 合同行列表；无数据时返回空列表（非 null）
     */
    private List<OaContractRow> queryByNotInIds(JdbcTemplate jdbc, List<Long> notInIds) {
        // 参数顺序：① 标量子查询 ?（CRM 合同 ID）→ ② NOT IN 分片 ID → ③ EXISTS ?（订单来源）
        List<Object> args = new ArrayList<>();
        args.add(OaContractAttachmentConstants.ORDER_SOURCE_CRM);
        String notInClause = "";
        if (!notInIds.isEmpty()) {
            String placeholders = String.join(", ", java.util.Collections.nCopies(notInIds.size(), "?"));
            notInClause = " AND fm.ID NOT IN (" + placeholders + ")";
            args.addAll(notInIds);
        }
        args.add(OaContractAttachmentConstants.ORDER_SOURCE_CRM);

        String sql = """
                SELECT fm.ID AS oa_form_main_id,
                       fm.START_DATE AS oa_start_date,
                       fm.FINISHEDFLAG AS finished_flag,
                       fm.FIELD0273 AS nc_order_no,
                       fm.FIELD0104 AS oa_contract_no,
                       fm.FIELD0360 AS oa_attachment_ref,
                       (SELECT f2817.%s
                          FROM %s f2817
                         WHERE f2817.%s = fm.FIELD0273
                           AND f2817.%s = ?
                           AND ROWNUM = 1) AS crm_contract_id
                FROM %s fm
                WHERE fm.FINISHEDFLAG = 1
                  AND fm.FIELD0273 IS NOT NULL
                  AND fm.FIELD0360 IS NOT NULL%s
                  AND EXISTS (
                      SELECT 1
                      FROM %s f2817
                      WHERE f2817.%s = fm.FIELD0273
                        AND f2817.%s = ?
                  )
                ORDER BY fm.ID
                """.formatted(
                OaContractAttachmentConstants.SALES_ORDER_FIELD_CONTRACT_ID,
                OaConstants.FORM_MAIN,
                OaContractAttachmentConstants.SALES_ORDER_FIELD_NO,
                OaContractAttachmentConstants.SALES_ORDER_FIELD_SOURCE,
                OaConstants.CONTRACT_FORM_MAIN,
                notInClause,
                OaConstants.FORM_MAIN,
                OaContractAttachmentConstants.SALES_ORDER_FIELD_NO,
                OaContractAttachmentConstants.SALES_ORDER_FIELD_SOURCE);
        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);
        try {
            return jdbc.query(sql, new ContractRowMapper(), args.toArray());
        } catch (Exception e) {
            log.error("【{}】扫描查询异常, notInSize={}", OaContractAttachmentConstants.LOG_BIZ, notInIds.size(), e);
            throw e;
        }
    }

    /**
     * 按 NC 销售订单号反查 CRM 合同数据 ID（formmain_2817.field0495）
     *
     * <p>用于存量数据补偿：field0495 标量子查询上线前入库的记录 {@code crm_contract_id} 为空，
     * 且 NOT IN 增量扫描不会重刷已入库记录，需在处理链路按 {@code field0329}（NC 单号）
     * 反查补偿；取 {@code ROWNUM = 1} 防止多行放大。</p>
     *
     * @param ncOrderNo NC 销售订单号（formmain_5296.field0273）
     * @return CRM 合同数据 ID；未命中或 OA 数据源未配置时返回 Optional.empty()
     */
    public Optional<String> findCrmContractIdByNcNo(String ncOrderNo) {
        if (ncOrderNo == null || ncOrderNo.isBlank()) {
            log.info("【{}】CRM合同ID反查终止, reason=NC单号为空", OaContractAttachmentConstants.LOG_BIZ);
            return Optional.empty();
        }
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            log.info("【{}】CRM合同ID反查终止, reason=OA数据源未配置(oa.datasource.url)",
                    OaContractAttachmentConstants.LOG_BIZ);
            return Optional.empty();
        }
        String sql = """
                SELECT f2817.%s AS crm_contract_id
                FROM %s f2817
                WHERE f2817.%s = ?
                  AND f2817.%s = ?
                  AND ROWNUM = 1
                """.formatted(
                OaContractAttachmentConstants.SALES_ORDER_FIELD_CONTRACT_ID,
                OaConstants.FORM_MAIN,
                OaContractAttachmentConstants.SALES_ORDER_FIELD_NO,
                OaContractAttachmentConstants.SALES_ORDER_FIELD_SOURCE);
        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);
        try {
            List<String> ids = jdbc.query(sql, (rs, rowNum) ->
                            OaJdbcResultSetSupport.readString(rs, "crm_contract_id"),
                    ncOrderNo.trim(), OaContractAttachmentConstants.ORDER_SOURCE_CRM);
            return ids.isEmpty() ? Optional.empty() : Optional.ofNullable(ids.getFirst());
        } catch (Exception e) {
            log.error("【{}】CRM合同ID反查异常, ncOrderNo={}", OaContractAttachmentConstants.LOG_BIZ, ncOrderNo, e);
            throw e;
        }
    }

    /**
     * 列表按固定大小分片
     *
     * @param source 源列表，非空
     * @param size   片大小，≥1
     * @return 分片结果，按原顺序排列
     */
    private static List<List<Long>> partition(List<Long> source, int size) {
        List<List<Long>> chunks = new ArrayList<>();
        for (int from = 0; from < source.size(); from += size) {
            chunks.add(source.subList(from, Math.min(from + size, source.size())));
        }
        return chunks;
    }

    /**
     * 合同行结果映射（列名对齐查询别名）
     */
    private static final class ContractRowMapper implements RowMapper<OaContractRow> {

        @Override
        public OaContractRow mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            Timestamp startDate = rs.getObject("oa_start_date") == null
                    ? null
                    : rs.getTimestamp(OaJdbcResultSetSupport.resolveColumnIndex(rs, "oa_start_date"));
            Long finishedFlag = OaJdbcResultSetSupport.readLong(rs, "finished_flag");
            return new OaContractRow(
                    OaJdbcResultSetSupport.readLong(rs, "oa_form_main_id"),
                    startDate == null ? null : startDate.toLocalDateTime(),
                    OaContractAttachmentConstants.flowStatusOf(
                            finishedFlag == null ? null : finishedFlag.intValue()),
                    OaJdbcResultSetSupport.readString(rs, "nc_order_no"),
                    OaJdbcResultSetSupport.readString(rs, "oa_contract_no"),
                    OaJdbcResultSetSupport.readString(rs, "oa_attachment_ref"),
                    OaJdbcResultSetSupport.readString(rs, "crm_contract_id"));
        }
    }
}
