package com.cacch.integration.config.mdm;

import com.cacch.integration.common.config.mdm.MdmEmpProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 人员主数据（MDM）配置注册 — 在 web 启动层完成 yml 绑定，供 service 模块注入
 *
 * @author hongfu_zhou@cacch.com
 */
@Configuration
@EnableConfigurationProperties(MdmEmpProperties.class)
public class MdmConfiguration {
}
