package com.cacch.integration.controller.crm;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.crm.client.dto.CrmProductQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmProductQueryResponse;
import com.cacch.integration.service.crm.api.ICrmProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 勤策商品域测试接口（仅用于联调，可单独删除）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/crm/products")
@RequiredArgsConstructor
public class TestCrmProductController {

    private final ICrmProductService crmProductService;

    /**
     * 查询商品
     *
     * @param request 商品查询条件，全部字段可选；不传请求体时按空条件查询
     * @return 商品查询结果，response_data 为商品列表
     */
    @PostMapping("/query")
    public Result<CrmProductQueryResponse> query(@RequestBody(required = false) CrmProductQueryRequest request) {
        log.info("【CrmProduct】收到查询商品测试请求");
        return Result.success(crmProductService.queryProducts(request));
    }
}
