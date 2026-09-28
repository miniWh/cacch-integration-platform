package com.cacch.integration.service.ihr.api.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.ihr.IhrPositionDO;
import com.cacch.integration.integration.ihr.client.IhrPositionClient;
import com.cacch.integration.integration.ihr.client.dto.IhrPosition;
import com.cacch.integration.integration.ihr.client.dto.IhrPositionListResponse;
import com.cacch.integration.mapper.ihr.IhrPositionMapper;
import com.cacch.integration.service.ihr.api.IIhrPositionService;
import com.cacch.integration.service.ihr.api.IIhrTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

/**
 * IHR 职位快照 Service 实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IhrPositionServiceImpl implements IIhrPositionService {

    private static final String BIZ = "IHR职位快照Service";

    private final IIhrTokenService ihrTokenService;
    private final IhrPositionClient ihrPositionClient;
    private final IhrPositionMapper mapper;

    @Override
    public List<IhrPosition> fetchAll() {
        String accessToken = ihrTokenService.getAccessToken();
        IhrPositionListResponse response;
        try {
            response = ihrPositionClient.listAll(accessToken);
        } catch (HttpStatusCodeException e) {
            // 401/403：强制刷新 token 后重试一次
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED
                    || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.info("【{}】HTTP {}，强制刷新 token 后重试", BIZ, e.getStatusCode().value());
                accessToken = ihrTokenService.forceRefresh();
                response = ihrPositionClient.listAll(accessToken);
            } else {
                log.info("【{}】HTTP 调用终止, status={}", BIZ, e.getStatusCode().value());
                throw e;
            }
        } catch (RestClientException e) {
            log.info("【{}】HTTP 调用终止, reason={}", BIZ, e.getMessage());
            throw e;
        }

        if (response == null || !response.isSuccess()) {
            log.info("【{}】接口终止, code={}, message={}",
                    BIZ, response == null ? "null" : response.getCode(),
                    response == null ? "空响应" : response.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "IHR 职位清单拉取失败: code=" + (response == null ? "null" : response.getCode())
                            + ", message=" + (response == null ? "空响应" : response.getMessage()));
        }

        List<IhrPosition> data = response.getData();
        if (data == null || data.isEmpty()) {
            log.info("【{}】fetchAll 返回空, 无职位数据", BIZ);
            return Collections.emptyList();
        }
        log.info("【{}】fetchAll 命中, count={}", BIZ, data.size());
        return data;
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED,
            readOnly = false, timeout = 30)
    @Override
    public int batchUpsert(List<IhrPositionDO> positionList, String syncBatch) {
        if (positionList == null || positionList.isEmpty()) {
            log.info("【{}】batchUpsert 跳过, positionList 为空", BIZ);
            return 0;
        }
        int upserted = 0;
        for (IhrPositionDO d : positionList) {
            // @TableId(ASSIGN_ID) 只对 BaseMapper.insert() 自动生效，
            // 手写 @Update 注解绕过了 IdentifierGenerator，需手动填雪花 ID
            if (d.getId() == null) {
                d.setId(IdWorker.getId());
            }
            try {
                upserted += mapper.upsert(d, syncBatch);
            } catch (Exception e) {
                log.info("【{}】upsert 终止, uuid={}, reason={}", BIZ, d.getUuid(), e.getMessage());
                log.error("【{}】upsert 失败, uuid={}, positionName={}", BIZ, d.getUuid(), d.getPositionName(), e);
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        "IHR 职位 upsert 失败: uuid=" + d.getUuid() + ", reason=" + e.getMessage(), e);
            }
        }
        return upserted;
    }

    @Override
    public List<IhrPositionDO> listByDeptTree(String deptId) {
        if (deptId == null || deptId.isBlank()) {
            log.info("【{}】listByDeptTree 跳过, deptId 为空", BIZ);
            return Collections.emptyList();
        }
        List<IhrPositionDO> list = mapper.listByDeptTree(deptId);
        if (list == null || list.isEmpty()) {
            log.info("【{}】listByDeptTree 返回空, deptId={}", BIZ, deptId);
            return Collections.emptyList();
        }
        log.info("【{}】listByDeptTree 命中, deptId={}, count={}", BIZ, deptId, list.size());
        return list;
    }
}
