package com.cacch.integration.common.constant.moka;

import com.cacch.integration.common.constant.ihr.IhrStaffConstants;

import java.util.Map;

/**
 * Moka 学历文本 → iHR {@code diplomaType} 枚举映射
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MokaEntryAcademicDegreeMappingConstants {

    private MokaEntryAcademicDegreeMappingConstants() {
    }

    private static final Map<String, String> TEXT_TO_IHR = Map.ofEntries(
            Map.entry("博士", IhrStaffConstants.HIGHEST_EDUCATION_DOCTOR),
            Map.entry("博士研究生", IhrStaffConstants.HIGHEST_EDUCATION_DOCTOR),
            Map.entry("硕士", IhrStaffConstants.HIGHEST_EDUCATION_MASTER),
            Map.entry("硕士研究生", IhrStaffConstants.HIGHEST_EDUCATION_MASTER),
            Map.entry("MBA", IhrStaffConstants.HIGHEST_EDUCATION_MASTER),
            Map.entry("EMBA", IhrStaffConstants.HIGHEST_EDUCATION_MASTER),
            Map.entry("本科", IhrStaffConstants.HIGHEST_EDUCATION_UNDER_GRADUATE),
            Map.entry("大学本科", IhrStaffConstants.HIGHEST_EDUCATION_UNDER_GRADUATE),
            Map.entry("大专", IhrStaffConstants.HIGHEST_EDUCATION_JUNIOR),
            Map.entry("高职", IhrStaffConstants.HIGHEST_EDUCATION_JUNIOR),
            Map.entry("高中", IhrStaffConstants.HIGHEST_EDUCATION_SENIOR_MIDDLE),
            Map.entry("中专", IhrStaffConstants.HIGHEST_EDUCATION_SENIOR_MIDDLE),
            Map.entry("中技", IhrStaffConstants.HIGHEST_EDUCATION_SENIOR_MIDDLE),
            Map.entry("初中", IhrStaffConstants.HIGHEST_EDUCATION_JUNIOR_MIDDLE),
            Map.entry("初中及以下", IhrStaffConstants.HIGHEST_EDUCATION_JUNIOR_MIDDLE),
            Map.entry("小学", IhrStaffConstants.HIGHEST_EDUCATION_PRIMARY_SCHOOL),
            Map.entry("其他", IhrStaffConstants.HIGHEST_EDUCATION_OTHER)
    );

    /**
     * 将 Moka 学历文本映射为 iHR diplomaType
     *
     * @param mokaDegree Moka 原文（已 trim）
     * @return iHR 枚举值；未映射时返回 null
     */
    public static String mapToIhrDiplomaType(String mokaDegree) {
        if (mokaDegree == null || mokaDegree.isEmpty()) {
            return null;
        }
        String trimmed = mokaDegree.trim();
        String mapped = TEXT_TO_IHR.get(trimmed);
        if (mapped != null) {
            return mapped;
        }
        return TEXT_TO_IHR.get(trimmed.replace(" ", ""));
    }
}