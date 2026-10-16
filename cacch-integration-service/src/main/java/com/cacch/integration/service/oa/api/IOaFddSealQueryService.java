package com.cacch.integration.service.oa.api;

import com.cacch.integration.integration.oa.client.dto.OaFddCompanyRow;
import com.cacch.integration.integration.oa.client.dto.OaFddSealRow;

import java.util.List;

/**
 * 法大大印章查询服务
 *
 * <p>从 OA 只读库查询法大大印章信息：印章完整列表与公司去重列表。
 * 纯只读 JDBC 查询，不涉及数据库写操作。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaFddSealQueryService {

    /**
     * 查询法大大印章完整列表（全量，无分页无筛选）
     *
     * @return 印章行列表；无数据或 OA 数据源未配置时返回空列表（非 null）
     */
    List<OaFddSealRow> listSeals();

    /**
     * 查询法大大公司去重列表（SQL DISTINCT，返回公司ID与公司名称）
     *
     * @return 公司去重行列表；无数据或 OA 数据源未配置时返回空列表（非 null）
     */
    List<OaFddCompanyRow> listDistinctCompanies();
}
