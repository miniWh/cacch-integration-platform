package com.cacch.integration.service.moka.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.moka.client.MokaCandidatesClient;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationListResponse;
import com.cacch.integration.service.moka.api.IMokaCandidatesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Moka 候选人服务实现
 *
 * <p>封装 Moka ATS 候选人域（Candidates）的业务逻辑，异常路径统一包装为 BizException 上抛。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MokaCandidatesServiceImpl implements IMokaCandidatesService {

    private static final String BIZ = "Moka 候选人服务";

    private final MokaCandidatesClient mokaCandidatesClient;

    @Override
    public MokaApplicationListResponse listApplications(MokaApplicationListRequest request) {
        // request 为 null 时兜底空对象（表示无条件查询全部）
        if (request == null) {
            request = new MokaApplicationListRequest();
        }

        log.info("【{}】开始查询申请信息, limit={}, jobId={}, hireMode={}",
                BIZ, request.getLimit(), request.getJobId(), request.getHireMode());

        try {
            MokaApplicationListResponse response = mokaCandidatesClient.listApplications(request);
            if (!response.isSuccess()) {
                log.info("【{}】查询申请信息终止, code={}, msg={}",
                        BIZ, response.getCode(), response.getMsg());
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        String.format("Moka 查询申请信息业务失败: code=%d, msg=%s",
                                response.getCode(), response.getMsg()));
            }
            int listSize = response.getData() == null || response.getData().getApplicationList() == null
                    ? 0 : response.getData().getApplicationList().size();
            String next = response.getData() == null ? null : response.getData().getNext();
            log.info("【{}】查询申请信息成功, listSize={}, next={}", BIZ, listSize, next);
            return response;

        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】查询申请信息终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】Moka HTTP 调用失败", BIZ, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "Moka 查询申请信息失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.info("【{}】查询申请信息终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】未知异常", BIZ, e);
            throw new BizException(ResultCode.SYSTEM_ERROR,
                    "Moka 查询申请信息系统异常", e);
        }
    }
}
