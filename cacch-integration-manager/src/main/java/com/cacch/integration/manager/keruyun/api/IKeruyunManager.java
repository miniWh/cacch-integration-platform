package com.cacch.integration.manager.keruyun.api;

import java.util.Map;

/**
 * 客如云接口编排 Manager
 *
 * <p>统一承担「取门店 token → 调用客如云业务接口」的编排，业务域 Service 后续接入时
 * 只需调用本接口，不直接依赖 token 服务与 Client。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IKeruyunManager {

    /**
     * 获取客如云门店 token（优先 Redis 缓存）
     *
     * @return 门店 token
     * @throws com.cacch.integration.common.exception.BizException 鉴权配置缺失或获取失败时抛出
     */
    String getAccessToken();

    /**
     * 调用客如云 POST 接口
     *
     * @param path         接口路径，如 {@code /open/v1/order/create}
     * @param body         业务请求体对象，可为 null
     * @param responseType 响应 DTO 类型
     * @param <T>          响应 DTO 类型
     * @return 客如云业务响应
     * @throws com.cacch.integration.common.exception.BizException token 获取失败或接口调用异常时抛出
     */
    <T> T post(String path, Object body, Class<T> responseType);

    /**
     * 调用客如云 GET 接口
     *
     * @param path         接口路径
     * @param query        业务查询参数，可为 null
     * @param responseType 响应 DTO 类型
     * @param <T>          响应 DTO 类型
     * @return 客如云业务响应
     * @throws com.cacch.integration.common.exception.BizException token 获取失败或接口调用异常时抛出
     */
    <T> T get(String path, Map<String, String> query, Class<T> responseType);
}
