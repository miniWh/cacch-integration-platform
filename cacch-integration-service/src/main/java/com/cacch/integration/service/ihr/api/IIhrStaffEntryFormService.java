package com.cacch.integration.service.ihr.api;

import com.cacch.integration.integration.ihr.client.dto.IhrStaffEntryFormCreateRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffEntryFormCreateResponse;

/**
 * IHR 新增待入职 Service
 *
 * <p>职责：获取 access_token → 调用 IHR「新增待入职-简略」接口 → 返回响应。
 * HTTP 401/403 时自动强制刷新 token 后重试一次。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrStaffEntryFormService {

    /**
     * 调用 IHR 新增待入职-简略接口
     *
     * <p>接口无数据库写入，纯透传代理。HTTP 401/403 会强制刷新 token 后重试一次。</p>
     *
     * @param requestBody 待入职请求体（不可为 null，字段校验由 web 层 @Valid 保证）
     * @return iHR 原始响应（含 entryId、data、errorResult）
     * @throws com.cacch.integration.common.exception.BizException HTTP 非 2xx 或业务 code != 0
     */
    IhrStaffEntryFormCreateResponse createEntryForm(IhrStaffEntryFormCreateRequest requestBody);
}
