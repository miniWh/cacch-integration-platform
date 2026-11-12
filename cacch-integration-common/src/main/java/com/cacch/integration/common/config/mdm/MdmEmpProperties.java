package com.cacch.integration.common.config.mdm;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 人员主数据（MDM）员工域配置
 *
 * <p>构造器绑定（不可变），由 web 启动层
 * {@code com.cacch.integration.config.mdm.MdmConfiguration} 注册。
 * 人员同步由 ESB 通过 HTTP 接口定时调度触发，无应用内定时配置。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "mdm.emp")
public class MdmEmpProperties {

    /**
     * 身份哈希盐 —— mdm_emp.id_hash = SHA-256(盐 + 证件号)；
     */
    private final String idHashSalt;

    public MdmEmpProperties(String idHashSalt) {
        this.idHashSalt = idHashSalt != null ? idHashSalt : "";
    }
}
