package com.cacch.integration.manager.keruyun.api.impl;

import com.cacch.integration.integration.keruyun.client.KeruyunClient;
import com.cacch.integration.manager.keruyun.api.IKeruyunManager;
import com.cacch.integration.service.keruyun.api.IKeruyunTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 客如云接口编排 Manager 实现
 *
 * <p>编排顺序：从 Token Service 获取门店 token（Redis 缓存）→ 交由 Client 完成签名与调用。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeruyunManagerImpl implements IKeruyunManager {

    private final IKeruyunTokenService keruyunTokenService;
    private final KeruyunClient keruyunClient;

    @Override
    public String getAccessToken() {
        return keruyunTokenService.getAccessToken();
    }

    /**
     * 调用客如云 POST 接口（token 由 Token Service 统一提供）
     *
     * @param path         接口路径
     * @param body         业务请求体对象，可为 null
     * @param responseType 响应 DTO 类型
     * @param <T>          响应 DTO 类型
     * @return 客如云业务响应
     */
    @Override
    public <T> T post(String path, Object body, Class<T> responseType) {
        String token = keruyunTokenService.getAccessToken();
        return keruyunClient.post(path, body, token, responseType);
    }

    /**
     * 调用客如云 GET 接口（token 由 Token Service 统一提供）
     *
     * @param path         接口路径
     * @param query        业务查询参数，可为 null
     * @param responseType 响应 DTO 类型
     * @param <T>          响应 DTO 类型
     * @return 客如云业务响应
     */
    @Override
    public <T> T get(String path, Map<String, String> query, Class<T> responseType) {
        String token = keruyunTokenService.getAccessToken();
        return keruyunClient.get(path, query, token, responseType);
    }
}
