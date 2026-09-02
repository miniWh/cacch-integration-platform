package com.cacch.integration.common.config.ihr;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IHR 开放平台接入配置属性 — 由 yml 的 {@code ihr} 节点注入
 *
 * <p>配置 POJO 定义在 common 模块，由 web 模块的 {@code IhrConfiguration} 通过
 * {@code @EnableConfigurationProperties} 注册为 Bean。</p>
 *
 * <pre>
 * 绑定说明（重要）：
 * 构造器参数必须是「扁平标量」而非嵌套对象。Spring 构造器绑定会按参数名拼接前缀，
 * 即 {@code ihr.app-key} / {@code ihr.app-secret} 直接绑定到本类的 appKey / appSecret；
 * 若写成 {@code IhrProperties(IhrAppConfig credential)}，Spring 会去找 {@code ihr.credential.*}，
 * 导致 appKey / appSecret 静默为 null（凭证失效且无报错，排查成本极高）。
 * {@link IhrAppConfig} 仅作为值对象在本类内部组装，不参与 Spring 绑定。
 * </pre>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "ihr")
public class IhrProperties {

    /**
     * IHR 接入凭证
     * -- GETTER --
     * 获取 IHR 接入凭证对象
     * IHR 凭证；yml 未配置时返回空对象（appKey / appSecret 均为 null）
     */
    private final IhrAppConfig credential;

    public IhrProperties(String appKey, String appSecret) {
        this.credential = new IhrAppConfig(appKey, appSecret);
    }

}
