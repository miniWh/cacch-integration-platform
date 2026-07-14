package com.cacch.integration.controller.crm;

import com.cacch.integration.common.dto.crm.CrmOrderCollectResult;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.dto.crm.request.CrmOrderCollectRequest;
import com.cacch.integration.manager.crm.api.ICrmOrderCollectManager;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM 订单采集联调接口
 *
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping("/api/v1/crm/collect")
@RequiredArgsConstructor
public class CrmOrderCollectController {

    private final ICrmOrderCollectManager crmOrderCollectManager;

    /**
     * 手动触发订单采集入库
     *
     * <p>不传 beginTime 时按 Asia/Shanghai 当天时间窗采集；传入则按指定 modify_time 区间。</p>
     *
     * @param request 可选时间窗
     * @return 采集统计
     */
    @PostMapping("/orders")
    public Result<CrmOrderCollectResult> collectOrders(
            @RequestBody(required = false) CrmOrderCollectRequest request) {
        if (request != null && StringUtils.hasText(request.getBeginTime())) {
            return Result.success(crmOrderCollectManager.collectByModifyTime(
                    request.getBeginTime(), request.getEndTime()));
        }
        return Result.success(crmOrderCollectManager.collectToday());
    }
}
