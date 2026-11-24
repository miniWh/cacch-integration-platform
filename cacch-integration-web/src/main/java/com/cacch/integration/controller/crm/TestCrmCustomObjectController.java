package com.cacch.integration.controller.crm;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectQueryResponse;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectSaveRequest;
import com.cacch.integration.integration.crm.client.dto.CrmCustomObjectSaveResponse;
import com.cacch.integration.service.crm.api.ICrmCustomObjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 勤策自定义对象联调测试接口
 *
 * <p>仅供联调使用，正式对接完成后可单独删除。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/crm/custom-objects")
@RequiredArgsConstructor
public class TestCrmCustomObjectController {

    private final ICrmCustomObjectService crmCustomObjectService;

    /**
     * 查询自定义对象数据
     *
     * @param request 查询条件，其中 object_key 必填
     * @return 勤策统一响应信封，response_data 为动态字段对象列表
     */
    @PostMapping("/query")
    public Result<CrmCustomObjectQueryResponse> query(
            @RequestBody(required = false) CrmCustomObjectQueryRequest request) {
        log.info("【CrmCustomObject】收到查询自定义对象数据测试请求");
        return Result.success(crmCustomObjectService.queryObjectData(request));
    }

    /**
     * 创建自定义对象数据
     *
     * @param request 创建请求，object_key 必填；is_trigger=true 时 trigger_employee 必填
     * @return 勤策统一响应信封，response_data 为新建数据标识节点（含 id）
     */
    @PostMapping("/save")
    public Result<CrmCustomObjectSaveResponse> save(@RequestBody CrmCustomObjectSaveRequest request) {
        log.info("【CrmCustomObject】收到创建自定义对象数据测试请求");
        return Result.success(crmCustomObjectService.saveObjectData(request));
    }
}
