package com.cacch.integration.integration.ihr.client.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * IHR 根据职位 id 获取职位清单 — 响应体包装
 *
 * <p>通用字段（code / message / errorResult / isSuccess）继承自 {@link IhrBaseResponse}。
 * 注意：
 * <ul>
 *     <li>{@code data} 直接是 JSONArray，不是嵌套对象（与全量职位清单同构）</li>
 *     <li>无分页，一次返回请求 ID 集合内全部存在的职位</li>
 * </ul>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IhrPositionIdsResponse extends IhrBaseResponse {

    /**
     * 职位列表（直接是 JSONArray，非分页包装；iHR 侧不存在的 ID 无对应元素）
     */
    private List<IhrPosition> data;
}
