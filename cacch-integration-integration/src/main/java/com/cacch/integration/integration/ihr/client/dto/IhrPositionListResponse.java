package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * IHR 获取公司职位清单 — 响应体包装
 *
 * <p>通用字段（code / message / errorResult / isSuccess）继承自 {@link IhrBaseResponse}。
 * 注意：
 * <ul>
 *     <li>{@code data} 直接是 JSONArray，不是嵌套对象（不同于部门清单 v3 的 data.content 结构）</li>
 *     <li>无分页，一次返回全部职位</li>
 * </ul>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IhrPositionListResponse extends IhrBaseResponse {

    /**
     * 职位列表（直接是 JSONArray，非分页包装）
     */
    private List<IhrPosition> data;
}
