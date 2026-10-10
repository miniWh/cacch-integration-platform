package com.cacch.integration.convert.ihr;

import com.cacch.integration.dto.ihr.request.IhrStaffEntryFormCreateApiRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffEntryFormCreateRequest;
import org.mapstruct.Mapper;

/**
 * IHR 新增待入职 — MapStruct Converter（API Request → integration Request）
 *
 * <p>字段名完全一致，按类型直接映射即可。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface IhrStaffEntryFormConverter {

    /**
     * web 层带校验注解的 API Request → integration 层纯净请求 DTO
     *
     * @param source API 请求体（已通过 @Valid 校验）
     * @return 纯净请求 DTO，直接下发 Client
     */
    IhrStaffEntryFormCreateRequest toIntegrationRequest(IhrStaffEntryFormCreateApiRequest source);
}
