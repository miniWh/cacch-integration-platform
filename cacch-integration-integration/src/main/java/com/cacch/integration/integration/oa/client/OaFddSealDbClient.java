package com.cacch.integration.integration.oa.client;

import com.cacch.integration.common.constant.oa.OaConstants;
import com.cacch.integration.integration.oa.client.dto.OaFddCompanyRow;
import com.cacch.integration.integration.oa.client.dto.OaFddSealRow;
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 法大大印章 OA 库查询客户端（只读 JOIN 印章子表、公司主表、组织成员、枚举项、组织单元）
 *
 * <p>印章子表 {@code formson_5475} 通过 {@code FORMMAIN_ID} 关联公司主表 {@code formmain_5474}，
 * 通过 {@code FIELD0012} 关联组织成员 {@code ORG_MEMBER}（印章管理员），通过 {@code FIELD0010}
 * 关联枚举项 {@code CTP_ENUM_ITEM}（印章类型），公司主表通过 {@code FIELD0003} 关联组织单元
 * {@code ORG_UNIT}（公司名称）。<strong>禁止</strong>对 OA 库执行写操作。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
public class OaFddSealDbClient {

    private static final String BIZ = "法大大印章";

    private final ObjectProvider<JdbcTemplate> oaJdbcTemplateProvider;

    /**
     * @param oaJdbcTemplateProvider OA 只读库 JdbcTemplate（未配置时 getIfAvailable 返回 null）
     */
    public OaFddSealDbClient(@Qualifier("oaJdbcTemplate") ObjectProvider<JdbcTemplate> oaJdbcTemplateProvider) {
        this.oaJdbcTemplateProvider = oaJdbcTemplateProvider;
    }

    /**
     * 查询法大大印章完整列表（全量，无分页无筛选）
     *
     * <p>联合查询印章子表、公司主表、组织成员、枚举项、组织单元，返回全部印章行。
     * LEFT JOIN 保证印章子表记录即使关联信息缺失也能返回。</p>
     *
     * @return 印章行列表；OA 数据源未配置时返回空列表（非 null）
     */
    public List<OaFddSealRow> listSeals() {
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            log.info("【{}】查询印章列表终止, reason=OA数据源未配置(oa.datasource.url)", BIZ);
            return List.of();
        }
        DbProduct product = OaDbDialectSupport.detect(jdbc);

        String sql = buildListSealsSql(product);
        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);

        log.info("【{}】查询印章完整列表", BIZ);
        try {
            List<OaFddSealRow> rows = jdbc.query(sql, new SealRowMapper());
            log.info("【{}】查询印章列表完成, count={}", BIZ, rows.size());
            return rows;
        } catch (Exception e) {
            log.info("【{}】查询印章列表终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】查询印章列表异常", BIZ, e);
            throw e;
        }
    }

    /**
     * 查询法大大公司去重列表（SQL DISTINCT，返回公司ID与公司名称）
     *
     * <p>联合查询印章子表、公司主表、组织单元，按公司ID与公司名称 DISTINCT 去重。</p>
     *
     * @return 公司去重行列表；OA 数据源未配置时返回空列表（非 null）
     */
    public List<OaFddCompanyRow> listDistinctCompanies() {
        JdbcTemplate jdbc = oaJdbcTemplateProvider.getIfAvailable();
        if (jdbc == null) {
            log.info("【{}】查询公司列表终止, reason=OA数据源未配置(oa.datasource.url)", BIZ);
            return List.of();
        }
        DbProduct product = OaDbDialectSupport.detect(jdbc);

        String sql = buildListCompaniesSql(product);
        ReadOnlyOaJdbcTemplate.assertSelectOnly(sql);

        log.info("【{}】查询公司去重列表", BIZ);
        try {
            List<OaFddCompanyRow> rows = jdbc.query(sql, new CompanyRowMapper());
            log.info("【{}】查询公司列表完成, count={}", BIZ, rows.size());
            return rows;
        } catch (Exception e) {
            log.info("【{}】查询公司列表终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】查询公司列表异常", BIZ, e);
            throw e;
        }
    }

    /**
     * 拼装印章完整列表 SQL（列经方言 CAST，别名对齐 RowMapper）
     */
    private String buildListSealsSql(DbProduct product) {
        return """
                SELECT %s,
                       %s,
                       %s,
                       %s,
                       %s,
                       %s,
                       %s
                FROM %s f5475
                  LEFT JOIN ORG_MEMBER om ON om.ID = f5475.%s
                  LEFT JOIN %s f5474 ON f5474.ID = f5475.FORMMAIN_ID
                  LEFT JOIN CTP_ENUM_ITEM cei ON cei.ID = f5475.%s
                  LEFT JOIN ORG_UNIT ou ON ou.ID = f5474.%s
                """.formatted(
                OaDbDialectSupport.selectTextColumn("f5474", OaConstants.FDD_FIELD_COMPANY_ID, "company_id", product),
                OaDbDialectSupport.selectTextColumn("ou", "NAME", "company_name", product),
                OaDbDialectSupport.selectTextColumn("f5475", OaConstants.FDD_FIELD_SEAL_ID, "seal_id", product),
                OaDbDialectSupport.selectTextColumn("cei", "SHOWVALUE", "seal_type_name", product),
                OaDbDialectSupport.selectTextColumn("f5475", OaConstants.FDD_FIELD_ADMIN_ID, "admin_id", product),
                OaDbDialectSupport.selectTextColumn("om", "NAME", "admin_name", product),
                OaDbDialectSupport.selectTextColumn("f5475", OaConstants.FDD_FIELD_ADMIN_PHONE, "admin_phone", product),
                OaConstants.FDD_SEAL_FORM_SON,
                OaConstants.FDD_FIELD_ADMIN_MEMBER_ID,
                OaConstants.FDD_COMPANY_FORM_MAIN,
                OaConstants.FDD_FIELD_SEAL_TYPE,
                OaConstants.FDD_FIELD_ORG_UNIT_ID);
    }

    /**
     * 拼装公司去重列表 SQL（DISTINCT，仅公司ID与公司名称）
     */
    private String buildListCompaniesSql(DbProduct product) {
        return """
                SELECT DISTINCT
                       %s,
                       %s
                FROM %s f5475
                  LEFT JOIN %s f5474 ON f5474.ID = f5475.FORMMAIN_ID
                  LEFT JOIN ORG_UNIT ou ON ou.ID = f5474.%s
                """.formatted(
                OaDbDialectSupport.selectTextColumn("f5474", OaConstants.FDD_FIELD_COMPANY_ID, "company_id", product),
                OaDbDialectSupport.selectTextColumn("ou", "NAME", "company_name", product),
                OaConstants.FDD_SEAL_FORM_SON,
                OaConstants.FDD_COMPANY_FORM_MAIN,
                OaConstants.FDD_FIELD_ORG_UNIT_ID);
    }

    /**
     * 印章行结果映射（列名对齐查询别名）
     */
    private static final class SealRowMapper implements RowMapper<OaFddSealRow> {

        @Override
        public OaFddSealRow mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            return new OaFddSealRow(
                    OaJdbcResultSetSupport.readIdAsString(rs, "company_id"),
                    OaJdbcResultSetSupport.readString(rs, "company_name"),
                    OaJdbcResultSetSupport.readIdAsString(rs, "seal_id"),
                    OaJdbcResultSetSupport.readString(rs, "seal_type_name"),
                    OaJdbcResultSetSupport.readIdAsString(rs, "admin_id"),
                    OaJdbcResultSetSupport.readString(rs, "admin_name"),
                    OaJdbcResultSetSupport.readString(rs, "admin_phone"));
        }
    }

    /**
     * 公司去重行结果映射（列名对齐查询别名）
     */
    private static final class CompanyRowMapper implements RowMapper<OaFddCompanyRow> {

        @Override
        public OaFddCompanyRow mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            return new OaFddCompanyRow(
                    OaJdbcResultSetSupport.readIdAsString(rs, "company_id"),
                    OaJdbcResultSetSupport.readString(rs, "company_name"));
        }
    }
}
