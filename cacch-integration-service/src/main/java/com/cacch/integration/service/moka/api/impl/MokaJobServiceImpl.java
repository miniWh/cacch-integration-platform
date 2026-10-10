package com.cacch.integration.service.moka.api.impl;

import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.moka.client.MokaJobClient;
import com.cacch.integration.integration.moka.client.dto.MokaJobListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaJobListResponse;
import com.cacch.integration.service.moka.api.IMokaJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Moka 职位服务实现
 *
 * <p>封装 Moka ATS 职位域（Jobs）的业务逻辑，异常路径统一包装为 BizException 上抛。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MokaJobServiceImpl implements IMokaJobService {

    private static final String BIZ = "Moka 职位服务";

    private final MokaJobClient mokaJobClient;

    @Override
    public MokaJobListResponse getJobs(MokaJobListRequest request) {
        log.info("【{}】开始查询职位信息, hireMode={}, status={}, limit={}, offset={}",
                BIZ, request == null ? null : request.getHireMode(),
                request == null ? null : request.getStatus(),
                request == null ? null : request.getLimit(),
                request == null ? null : request.getOffset());

        try {
            MokaJobListResponse response = mokaJobClient.getJobs(request);
            if (!response.isSuccess()) {
                log.info("【{}】查询职位信息终止, code={}, msg={}",
                        BIZ, response.getCode(), response.getMsg());
                throw new BizException(ResultCode.INTEGRATION_ERROR,
                        String.format("Moka 查询职位信息业务失败: code=%d, msg=%s",
                                response.getCode(), response.getMsg()));
            }
            int jobSize = response.getData() == null || response.getData().getJobs() == null
                    ? 0 : response.getData().getJobs().size();
            log.info("【{}】查询职位信息成功, total={}, jobSize={}",
                    BIZ,
                    response.getData() == null ? null : response.getData().getTotal(),
                    jobSize);
            return response;

        } catch (BizException e) {
            throw e;
        } catch (RestClientException e) {
            log.info("【{}】查询职位信息终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】Moka HTTP 调用失败", BIZ, e);
            throw new BizException(ResultCode.INTEGRATION_ERROR,
                    "Moka 查询职位信息失败: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            log.info("【{}】查询职位信息终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】Moka 配置缺失", BIZ, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, e.getMessage(), e);
        } catch (Exception e) {
            log.info("【{}】查询职位信息终止, reason={}", BIZ, e.getMessage());
            log.error("【{}】未知异常", BIZ, e);
            throw new BizException(ResultCode.SYSTEM_ERROR, "Moka 查询职位信息系统异常", e);
        }
    }
}
