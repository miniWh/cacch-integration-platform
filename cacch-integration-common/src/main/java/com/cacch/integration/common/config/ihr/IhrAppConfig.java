package com.cacch.integration.common.config.ihr;

import lombok.Getter;

/**
 * IHR 开放平台接入凭证 — 值对象，由 {@link IhrProperties} 组装
 *
 * <p>IHR 一个企业账户只下发一组 (appKey, appSecret)，不区分子应用，故采用标量配置而非 List。
 * 实际值由环境变量 {@code IHR_APP_KEY} / {@code IHR_APP_SECRET} 注入，yml 仅占位引用。</p>
 *
 * <pre>
 * 安全提示：IHR appSecret 等同于密码，禁止写入代码 / Git / 日志；
 * yml 通过 {@code ${IHR_APP_SECRET:}} 占位符注入；空值代表未配置（test 环境允许空值，prod 建议 fail-fast）。
 * </pre>
 *
 * <p>本类为不可变值对象，不参与 Spring 配置绑定（绑定发生在 {@link IhrProperties} 的扁平构造器上），
 * 因此无需 {@code @ConstructorBinding}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public class IhrAppConfig {

    /**
     * IHR 开放平台分配的 appKey（用户名）
     * -- GETTER --
     * 获取 appKey（IHR 用户名）
     */
    private final String appKey;

    /**
     * IHR 开放平台分配的 appSecret（密码），与 appKey 配对使用 Basic 认证
     * -- GETTER --
     * 获取 appSecret（IHR 密码）
     */
    private final String appSecret;

    public IhrAppConfig(String appKey, String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    /**
     * 是否已配置完整凭证（appKey 与 appSecret 均非空）
     *
     * @return true 表示凭证齐全，false 表示未配置或配置不全
     */
    public boolean isConfigured() {
        return appKey != null && !appKey.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }
}
