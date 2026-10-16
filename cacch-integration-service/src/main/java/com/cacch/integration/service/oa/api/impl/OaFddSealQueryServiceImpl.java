package com.cacch.integration.service.oa.api.impl;

import com.cacch.integration.integration.oa.client.OaFddSealDbClient;
import com.cacch.integration.integration.oa.client.dto.OaFddCompanyRow;
import com.cacch.integration.integration.oa.client.dto.OaFddSealRow;
import com.cacch.integration.service.oa.api.IOaFddSealQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 法大大印章查询服务实现
 *
 * <p>纯只读 JDBC 查询，委托 {@link OaFddSealDbClient} 从 OA 只读库查询印章与公司信息。
 * 不涉及数据库写操作，不使用数据库事务。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OaFddSealQueryServiceImpl implements IOaFddSealQueryService {

    private final OaFddSealDbClient oaFddSealDbClient;

    @Override
    public List<OaFddSealRow> listSeals() {
        return oaFddSealDbClient.listSeals();
    }

    @Override
    public List<OaFddCompanyRow> listDistinctCompanies() {
        return oaFddSealDbClient.listDistinctCompanies();
    }
}
