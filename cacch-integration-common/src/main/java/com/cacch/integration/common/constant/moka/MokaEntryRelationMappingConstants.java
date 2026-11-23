package com.cacch.integration.common.constant.moka;

import java.util.Map;

/**
 * Moka 紧急联系人关系（Q-02）→ iHR 员工基础信息下拉值映射
 *
 * <p>落库时写入 {@code emergency_contact_relation_ihr}；映射不到时返回 null。
 * 完整枚举待业务补齐后可扩展 {@link #MOKA_TO_IHR}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MokaEntryRelationMappingConstants {

    private MokaEntryRelationMappingConstants() {
    }

    private static final Map<String, String> MOKA_TO_IHR = Map.ofEntries(
            Map.entry("父亲", "FATHER"),
            Map.entry("母亲", "MOTHER"),
            Map.entry("配偶", "SPOUSE"),
            Map.entry("子女", "CHILD"),
            Map.entry("父子", "FATHER"),
            Map.entry("父女", "FATHER"),
            Map.entry("母子", "MOTHER"),
            Map.entry("母女", "MOTHER"),
            Map.entry("兄弟", "BROTHER"),
            Map.entry("姐妹", "SISTER"),
            Map.entry("其他", "OTHER")
    );

    /**
     * 将 Moka 关系文本映射为 iHR 选项值
     *
     * @param mokaRelation Moka 原文（已 trim）
     * @return iHR 枚举值；未配置映射时返回 null
     */
    public static String mapToIhr(String mokaRelation) {
        if (mokaRelation == null || mokaRelation.isEmpty()) {
            return null;
        }
        return MOKA_TO_IHR.get(mokaRelation);
    }
}