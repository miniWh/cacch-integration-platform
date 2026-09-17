package com.cacch.integration.integration.oa.client;

import com.cacch.integration.common.constant.oa.OaConstants;
import com.cacch.integration.integration.oa.client.dto.OaAttachmentRow;
import com.cacch.integration.integration.oa.support.OaDbDialectSupport;
import com.cacch.integration.integration.oa.support.OaDbDialectSupport.DbProduct;
import com.cacch.integration.integration.oa.support.OaJdbcResultSetSupport;
import com.cacch.integration.integration.oa.support.ReadOnlyOaJdbcTemplate;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * NC 单号附件 OA 库查询客户端（只读 JOIN 内部购销合同表与 ctp_attachment）
 *
 * <p>内部购销合同表 {@code formmain_5296} 的 NC 单号字段为 {@code field0273}，
 * {@code field0360} 为 NC 单号对应的附件引用字段，值对应致远 OA 附件表 {@code ctp_attachment}
 * 的 {@code sub_reference}（OA 含义：附件归属的子引用）；同一 NC 单号命中多附件时按附件 ID
 * 升序取第一条。<strong>禁止</strong>对 OA 库执行写操作。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
public class OaAttachmentDbClient {

    private static final String BIZ = "NC附件";

    private final ObjectProvider<JdbcTemplate> oaJdbcTemplateProvider;

    /**
     * @param oaJdbcTemplateProvider OA 只读库 JdbcTemplate（未配置时 getIfAvailable 返回 null）
     */
    public OaAttachmentDbClient(@Qualifier("oaJdbcTemplate") ObjectProvider<JdbcTemplate> oaJdbcTemplateProvider) {
        this.oaJdbcTemplateProvider = oaJdbcTemplateProvider;
    }

    /**
     * 按 NC 单号查询第一条关联附件
     *
     * <p>联合查询内部购销合同表与附件表，比较双方均转文本避免隐式数值转换触发脏数据异常；
     * 命中多行时按附件 ID 升序仅取第一条。</p>
     *
     * @param ncNo NC 单号（对应合同表 field0273），不可为空
     * @return 第一条附件行；NC 单号无匹配、未关联附件或 OA 数据源未配置时返回 empty
     */
    public Optional<OaAttachmentRow> findFirstAttachmentByNcNo(String ncNo) {
        if (!StringUtils.hasText(ncNo)) {
            log.info("【{}】查询终止, reason=ncNo为空", BIZ);
            return Optional.empty();
        }
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            log.info("【{}】查询终止, reason=OA数据源未配置(oa.datasource.url)", BIZ);
            return Optional.empty();
        }

        DbProduct product = OaDbDialectSupport.detect(jdbc);
        String ncNoExpr = OaDbDialectSupport.castColumnAsText(
                "m." + OaConstants.CONTRACT_FIELD_NC_NO, product);
        String refExpr = OaDbDialectSupport.castColumnAsText(
                "m." + OaConstants.CONTRACT_FIELD_ATTACHMENT_REF, product);
        String subRefExpr = OaDbDialectSupport.castColumnAsText("a.sub_reference", product);

        String sql = """
                SELECT %s AS attachment_id,
                       %s AS file_url,
                       %s AS file_name
                FROM %s m
                INNER JOIN %s a ON %s = TRIM(%s)
                WHERE TRIM(%s) = ?
                ORDER BY a.id
                """.formatted(
                OaDbDialectSupport.castColumnAsText("a.id", product),
                OaDbDialectSupport.castColumnAsText("a.file_url", product),
                OaDbDialectSupport.castColumnAsText("a.filename", product),
                OaConstants.CONTRACT_FORM_MAIN,
                OaConstants.CTP_ATTACHMENT_TABLE,
                subRefExpr,
                refExpr,
                ncNoExpr);
        List<Object> args = new ArrayList<>();
        args.add(ncNo.trim());
        sql = OaDbDialectSupport.appendPagination(sql, args, 1, product);
        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);

        String trimmed = ncNo.trim();
        log.info("【{}】按NC单号查询附件, ncNo={}", BIZ, trimmed);
        try {
            List<OaAttachmentRow> rows = jdbc.query(sql, new AttachmentRowMapper(), args.toArray());
            if (rows.isEmpty()) {
                log.info("【{}】按NC单号查询无结果, ncNo={}", BIZ, trimmed);
                return Optional.empty();
            }
            OaAttachmentRow row = rows.getFirst();
            log.info("【{}】按NC单号查询命中, ncNo={}, attachmentId={}, fileName={}, fileUrl={}",
                    BIZ, trimmed, row.id(), row.fileName(), row.fileUrl());
            return Optional.of(row);
        } catch (Exception e) {
            log.info("【{}】按NC单号查询失败, ncNo={}, reason={}", BIZ, trimmed, e.getMessage());
            log.error("【{}】按NC单号查询异常, ncNo={}", BIZ, trimmed, e);
            throw e;
        }
    }

    /**
     * 附件行结果映射（列名对齐查询别名）
     */
    private static final class AttachmentRowMapper implements RowMapper<OaAttachmentRow> {

        @Override
        public OaAttachmentRow mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            return new OaAttachmentRow(
                    OaJdbcResultSetSupport.readIdAsString(rs, "attachment_id"),
                    OaJdbcResultSetSupport.readString(rs, "file_url"),
                    OaJdbcResultSetSupport.readString(rs, "file_name"));
        }
    }
}
