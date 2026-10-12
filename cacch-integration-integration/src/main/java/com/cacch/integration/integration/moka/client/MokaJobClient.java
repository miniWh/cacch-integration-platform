package com.cacch.integration.integration.moka.client;

import com.cacch.integration.common.config.moka.MokaProperties;
import com.cacch.integration.common.constant.moka.MokaConstants;
import com.cacch.integration.integration.moka.client.dto.MokaJobItem;
import com.cacch.integration.integration.moka.client.dto.MokaJobListRequest;
import com.cacch.integration.integration.moka.client.dto.MokaJobListResponse;
import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Moka ATS —— 职位查询 HTTP 客户端
 *
 * <p>调用 POST {@code /api-platform/v1/jobs/getJobs}。
 * 本 Client 提供两个便捷方法：
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MokaJobClient {

    private static final String BIZ = MokaConstants.LOG_BIZ;
    private static final String ACTION_GET_JOBS = "查询职位信息";
    private static final String ACTION_GET_DEPT_BY_JOB_IDS = "按jobId批量查职位部门编码";

    private final RestTemplateHttpClient httpClient;
    private final MokaProperties mokaProperties;

    /**
     * 查询职位信息 —— 透传 Moka {@code POST /api-platform/v1/jobs/getJobs}
     *
     * <p>通用透传接口：调用方自行组装 MokaJobListRequest（hireMode / status / limit / offset 等），
     * 本方法仅负责 HTTP 执行与鉴权头构造。</p>
     *
     * <p>与 Step 1b 专用的 {@link #getDepartmentCodeByJobIds(List)} 区别：
     * 后者内部固定 hireMode="social" 并翻页过滤 jobId→部门编码，用于批量补全；
     * 本方法直接透传请求参数，适合 Service 层按需查询。</p>
     *
     * @param request Moka 职位查询请求体，可为 null（内部会校验 API Key 配置）
     * @return Moka 原始响应体，调用方通过 response.isSuccess() 判定成功
     * @throws IllegalStateException Moka API Key 未配置时抛出
     */
    public MokaJobListResponse getJobs(MokaJobListRequest request) {
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_GET_JOBS);
            throw new IllegalStateException("Moka API Key 未配置");
        }

        String url = mokaProperties.getBaseUrl() + MokaConstants.JOB_LIST_PATH;
        HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

        MokaJobListResponse body = httpClient.doPostOrPut(
                HttpMethod.POST, url, request, headers, BIZ, ACTION_GET_JOBS, MokaJobListResponse.class);

        if (body.isSuccess()) {
            int jobSize = body.getData() == null || body.getData().getJobs() == null
                    ? 0 : body.getData().getJobs().size();
            Integer total = body.getData() == null ? null : body.getData().getTotal();
            log.info("【{}】{}成功, total={}, jobSize={}", BIZ, ACTION_GET_JOBS, total, jobSize);
        } else {
            log.info("【{}】{}终止, code={}, msg={}", BIZ, ACTION_GET_JOBS, body.getCode(), body.getMsg());
        }
        return body;
    }

    /**
     * 按 jobId + hireMode 批量查职位并提取部门编码
     *
     * <p>Step 1b 补全：由调用方根据候选人 hireMode（1→social, 2→campus）
     * 分组后传入对应的 hireMode 值，返回 Map<jobId, departmentCode>。</p>
     *
     * @param jobIds   待查询的 jobId 列表（去重后）
     * @param hireMode 招聘模式：social=社招，campus=校招
     * @return jobId → 部门编码 的映射；查不到的 jobId 不出现在 map 中
     */
    public Map<String, String> getDepartmentCodeByJobIds(List<String> jobIds, String hireMode) {
        if (jobIds == null || jobIds.isEmpty()) {
            log.info("【{}】{}终止, reason=jobIds 为空", BIZ, ACTION_GET_DEPT_BY_JOB_IDS);
            return Collections.emptyMap();
        }
        if (!mokaProperties.isApiKeyConfigured()) {
            log.info("【{}】{}终止, reason=Moka API Key 未配置", BIZ, ACTION_GET_DEPT_BY_JOB_IDS);
            throw new IllegalStateException("Moka API Key 未配置");
        }

        Map<String, String> result = new java.util.HashMap<>();
        fetchJobsByHireMode(hireMode, jobIds, result);

        log.info("【{}】{}完成, hireMode={}, 请求jobId数={}, 成功匹配={}",
                BIZ, ACTION_GET_DEPT_BY_JOB_IDS, hireMode, jobIds.size(), result.size());
        return result;
    }

    /**
     * 按指定 hireMode 翻页查询职位，匹配到目标 jobId 时提取部门编码
     */
    private void fetchJobsByHireMode(String hireMode, List<String> targetJobIds, Map<String, String> result) {
        int limit = 50;
        String offset = "0";
        int totalProcessed = 0;

        while (true) {
            MokaJobListRequest request = new MokaJobListRequest();
            request.setHireMode(hireMode);
            request.setLimit(String.valueOf(limit));
            request.setOffset(offset);

            String url = mokaProperties.getBaseUrl() + MokaConstants.JOB_LIST_PATH;
            HttpHeaders headers = MokaAuthSupport.buildBasicAuthHeaders(mokaProperties, true);

            MokaJobListResponse response;
            try {
                response = httpClient.doPostOrPut(
                        HttpMethod.POST, url, request, headers,
                        BIZ, ACTION_GET_DEPT_BY_JOB_IDS, MokaJobListResponse.class);
            } catch (Exception e) {
                log.info("【{}】{}[hireMode={}]HTTP异常终止, reason={}",
                        BIZ, ACTION_GET_DEPT_BY_JOB_IDS, hireMode, e.getMessage());
                break;
            }

            if (!response.isSuccess()) {
                log.info("【{}】{}[hireMode={}]终止, code={}, msg={}",
                        BIZ, ACTION_GET_DEPT_BY_JOB_IDS, hireMode, response.getCode(), response.getMsg());
                break;
            }

            if (response.getData() == null || response.getData().getJobs() == null) {
                log.info("【{}】{}[hireMode={}]成功但返回空数据, 已处理={}",
                        BIZ, ACTION_GET_DEPT_BY_JOB_IDS, hireMode, totalProcessed);
                break;
            }

            List<MokaJobItem> jobs = response.getData().getJobs();
            for (MokaJobItem job : jobs) {
                if (targetJobIds.contains(job.getId())
                        && job.getDepartment() != null
                        && job.getDepartment().getCode() != null) {
                    result.put(job.getId(), job.getDepartment().getCode());
                }
            }

            totalProcessed += jobs.size();

            // 翻页终止条件：本页不足 limit 或已处理数 >= total
            Integer total = response.getData().getTotal();
            if (jobs.size() < limit || (total != null && totalProcessed >= total)) {
                break;
            }
            offset = String.valueOf(totalProcessed);
        }

        log.info("【{}】{}[hireMode={}]完成, 已处理职位数={}, 本hireMode匹配={}",
                BIZ, ACTION_GET_DEPT_BY_JOB_IDS, hireMode, totalProcessed, result.size());
    }
}
