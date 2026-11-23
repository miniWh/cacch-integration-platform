package com.cacch.integration.integration.moka.adapter;

import com.cacch.integration.common.constant.moka.MokaEntryCustomFieldConstants;
import com.cacch.integration.common.constant.moka.MokaEntryRelationMappingConstants;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Moka EHR v2 申请单解析（面试登记表 customFields + educationInfo）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class MokaEhrApplicationParseAdapter {

    private MokaEhrApplicationParseAdapter() {
    }

    /**
     * 解析单条 EHR v2 data 元素
     *
     * @param applicationItem 含 basicInfo 的 JsonNode
     * @return 解析结果；applicationId 缺失时返回 null
     */
    public static MokaEhrParsedApplication parse(JsonNode applicationItem) {
        if (applicationItem == null || applicationItem.isMissingNode()) {
            return null;
        }
        JsonNode basicInfo = applicationItem.get("basicInfo");
        if (basicInfo == null || basicInfo.isMissingNode()) {
            return null;
        }
        JsonNode appIdNode = basicInfo.get("applicationId");
        if (appIdNode == null || appIdNode.isNull()) {
            return null;
        }
        int applicationId = appIdNode.asInt();

        JsonNode customFields = basicInfo.get("customFields");
        String emergencyName = stringCustomField(customFields,
                MokaEntryCustomFieldConstants.CUSTOM_FIELD_ID_EMERGENCY_NAME,
                MokaEntryCustomFieldConstants.SECTION_BASIC_INFO, true);
        String emergencyRelation = stringCustomField(customFields,
                MokaEntryCustomFieldConstants.CUSTOM_FIELD_ID_EMERGENCY_RELATION,
                MokaEntryCustomFieldConstants.SECTION_BASIC_INFO, false);
        String emergencyRelationIhr = MokaEntryRelationMappingConstants.mapToIhr(emergencyRelation);
        String emergencyPhone = stringCustomField(customFields,
                MokaEntryCustomFieldConstants.CUSTOM_FIELD_ID_EMERGENCY_PHONE,
                MokaEntryCustomFieldConstants.SECTION_BASIC_INFO, false);

        List<MokaEhrParsedEducation> educations = parseEducations(basicInfo.get("educationInfo"), customFields);

        return new MokaEhrParsedApplication(
                applicationId,
                emergencyName,
                emergencyRelation,
                emergencyRelationIhr,
                emergencyPhone,
                educations
        );
    }

    private static List<MokaEhrParsedEducation> parseEducations(JsonNode educationInfo, JsonNode customFields) {
        List<MokaEhrParsedEducation> result = new ArrayList<>();
        if (educationInfo == null || !educationInfo.isArray() || educationInfo.isEmpty()) {
            return result;
        }
        for (int i = 0; i < educationInfo.size(); i++) {
            JsonNode edu = educationInfo.get(i);
            if (edu == null || edu.isMissingNode()) {
                continue;
            }
            String mokaEducationId = textOrNull(edu.get("id"));
            String isFullTime = resolveIsFullTime(customFields, mokaEducationId, i);
            result.add(new MokaEhrParsedEducation(
                    mokaEducationId,
                    i,
                    textOrNull(edu.get("school")),
                    textOrNull(edu.get("speciality")),
                    textOrNull(edu.get("academicDegree")),
                    textOrNull(edu.get("startDate")),
                    textOrNull(edu.get("endDate")),
                    isFullTime
            ));
        }
        return result;
    }

    private static String resolveIsFullTime(JsonNode customFields, String educationId, int index) {
        if (customFields == null || !customFields.isArray()) {
            return null;
        }
        JsonNode matched = null;
        for (JsonNode cf : customFields) {
            if (matchesCustomField(cf, MokaEntryCustomFieldConstants.CUSTOM_FIELD_ID_IS_FULL_TIME,
                    MokaEntryCustomFieldConstants.SECTION_EDUCATION_INFO)) {
                continue;
            }
            JsonNode builtinInfoId = cf.get("builtinInfoId");
            if (educationId != null && builtinInfoId != null && !builtinInfoId.isNull()) {
                String builtinStr = builtinInfoId.isNumber()
                        ? String.valueOf(builtinInfoId.asLong())
                        : builtinInfoId.asText();
                if (educationId.equals(builtinStr)) {
                    matched = cf;
                    break;
                }
            }
        }
        if (matched == null) {
            for (JsonNode cf : customFields) {
                if (matchesCustomField(cf, MokaEntryCustomFieldConstants.CUSTOM_FIELD_ID_IS_FULL_TIME,
                        MokaEntryCustomFieldConstants.SECTION_EDUCATION_INFO)) {
                    continue;
                }
                if (cf.path("index").asInt(-1) == index) {
                    matched = cf;
                    break;
                }
            }
        }
        if (matched == null) {
            return null;
        }
        JsonNode value = matched.get("value");
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.booleanValue()
                    ? MokaEntryCustomFieldConstants.FULL_TIME_YES
                    : MokaEntryCustomFieldConstants.FULL_TIME_NO;
        }
        if (value.isTextual()) {
            String text = value.asText().trim();
            if ("true".equalsIgnoreCase(text) || "1".equals(text) || MokaEntryCustomFieldConstants.FULL_TIME_YES.equals(text)) {
                return MokaEntryCustomFieldConstants.FULL_TIME_YES;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text) || MokaEntryCustomFieldConstants.FULL_TIME_NO.equals(text)) {
                return MokaEntryCustomFieldConstants.FULL_TIME_NO;
            }
        }
        return null;
    }

    private static String stringCustomField(JsonNode customFields, int fieldId, String section, boolean trim) {
        if (customFields == null || !customFields.isArray()) {
            return null;
        }
        for (JsonNode cf : customFields) {
            if (matchesCustomField(cf, fieldId, section)) {
                continue;
            }
            JsonNode value = cf.get("value");
            if (value == null || value.isNull()) {
                return null;
            }
            if (!value.isValueNode()) {
                return null;
            }
            String text = value.asText();
            if (text == null) {
                return null;
            }
            return trim ? text.trim() : text;
        }
        return null;
    }

    private static boolean matchesCustomField(JsonNode cf, int fieldId, String section) {
        if (cf == null || cf.isMissingNode()) {
            return true;
        }
        return cf.path("customFieldId").asInt(-1) != fieldId
                || !section.equals(cf.path("section").asText(null));
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isEmpty() ? null : text;
    }

    /**
     * EHR v2 解析结果
     *
     * @param applicationId        申请 id
     * @param emergencyName        紧急联系人姓名
     * @param emergencyRelation    关系原文
     * @param emergencyRelationIhr iHR 映射关系
     * @param emergencyPhone       紧急联系人电话
     * @param educations           教育经历行
     */
    public record MokaEhrParsedApplication(
            int applicationId,
            String emergencyName,
            String emergencyRelation,
            String emergencyRelationIhr,
            String emergencyPhone,
            List<MokaEhrParsedEducation> educations
    ) {
    }

    /**
     * 单条教育经历解析结果
     */
    public record MokaEhrParsedEducation(
            String mokaEducationId,
            int educationIndex,
            String school,
            String speciality,
            String academicDegree,
            String startDate,
            String endDate,
            String isFullTime
    ) {
    }
}