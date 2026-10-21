package com.cacch.integration.service.keruyun.api;

/**
 * 客如云门店 token 服务接口
 *
 * <p>负责 token 的 Redis 缓存读写，缓存未命中时回源客如云获取。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IKeruyunTokenService {

    /**
     * 获取客如云门店 token（优先读 Redis 缓存，未命中时调用客如云 token 接口并回写缓存）
     *
     * @return 门店 token
     * @throws com.cacch.integration.common.exception.BizException 鉴权配置缺失或客如云返回非成功码时抛出
     */
    String getAccessToken();
}
