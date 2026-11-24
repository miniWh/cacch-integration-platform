package com.cacch.integration.controller.crm;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.crm.client.dto.CrmContractAddRequest;
import com.cacch.integration.integration.crm.client.dto.CrmContractAddResponse;
import com.cacch.integration.service.crm.api.ICrmContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 勤策合同联调测试接口
 *
 * <p>仅供联调使用，正式对接完成后可单独删除。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/crm/contracts")
@RequiredArgsConstructor
public class TestCrmContractController {

    private final ICrmContractService crmContractService;

    /**
     * 新增销售合同
     *
     * @param request 新增销售合同请求体（camelCase），客户标识与明细为必填
     * @return 勤策统一响应信封，response_data 为 JSON 节点（含新增合同 ID）
     */
    @PostMapping
    public Result<CrmContractAddResponse> addContract(@RequestBody CrmContractAddRequest request) {
        log.info("【CrmContract】收到新增销售合同测试请求");
        return Result.success(crmContractService.addContract(request));
    }
}
