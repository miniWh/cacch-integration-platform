package com.cacch.integration.common.config.moka;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Moka 待入职推送 iHR 后的扩展字段配置（花名册自定义 key）
 *
 * <p>紧急联系人关系、教育经历「是否全日制」在 iHR 侧多为 flex/自定义字段，
 * key 须与「查询花名册字段信息」一致；未配置时仅写入标准字段（姓名/电话），跳过 flex。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "moka.entry-ihr")
public class MokaEntryIhrExtensionProperties {

    /**
     * 员工基础信息 — 紧急联系人关系自定义字段 key；空白时不传
     */
    private final String emergencyRelationFieldKey;

    /**
     * 教育经历 v2 — 是否全日制 flexAttributes key；空白时不传
     */
    private final String educationFullTimeFlexKey;

    public MokaEntryIhrExtensionProperties(String emergencyRelationFieldKey, String educationFullTimeFlexKey) {
        this.emergencyRelationFieldKey = blankToEmpty(emergencyRelationFieldKey);
        this.educationFullTimeFlexKey = blankToEmpty(educationFullTimeFlexKey);
    }

    private static String blankToEmpty(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    public boolean hasEmergencyRelationFieldKey() {
        return !emergencyRelationFieldKey.isEmpty();
    }

    public boolean hasEducationFullTimeFlexKey() {
        return !educationFullTimeFlexKey.isEmpty();
    }
}