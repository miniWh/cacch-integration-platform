package com.cacch.integration.service.moka.support;

import com.cacch.integration.common.config.moka.MokaEntryIhrExtensionProperties;
import com.cacch.integration.common.constant.moka.MokaEntryAcademicDegreeMappingConstants;
import com.cacch.integration.common.constant.moka.MokaEntryCustomFieldConstants;
import com.cacch.integration.entity.moka.MokaEntryCandidateBaseDO;
import com.cacch.integration.entity.moka.MokaEntryEducationDO;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffEducationCreateV2Request;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffUpdateBasicInfoRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Moka 待入职 PG 快照 → iHR 扩展写接口请求体组装
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
public class MokaEntryIhrPushExtensionAssembler {

    /**
     * 组装修改员工基础信息请求（仅含紧急联系人相关字段，不传 mobileNo）
     *
     * @param base     候选人基础表行
     * @param ihrProps 花名册 flex key 配置
     * @return 无可写字段时返回 null
     */
    public IhrStaffUpdateBasicInfoRequest buildEmergencyContactUpdate(
            MokaEntryCandidateBaseDO base,
            MokaEntryIhrExtensionProperties ihrProps) {
        if (base == null) {
            return null;
        }
        boolean hasName = StringUtils.isNotBlank(base.getEmergencyContactName());
        boolean hasPhone = StringUtils.isNotBlank(base.getEmergencyContactPhone());
        boolean hasRelation = StringUtils.isNotBlank(base.getEmergencyContactRelationIhr());
        if (!hasName && !hasPhone && !hasRelation) {
            return null;
        }

        IhrStaffUpdateBasicInfoRequest req = new IhrStaffUpdateBasicInfoRequest();
        if (hasName) {
            req.setEmergencyContactName(base.getEmergencyContactName().trim());
        }
        if (hasPhone) {
            req.setEmergencyContactMobile(base.getEmergencyContactPhone().trim());
        }
        if (hasRelation && ihrProps != null && ihrProps.hasEmergencyRelationFieldKey()) {
            req.setCustomField(ihrProps.getEmergencyRelationFieldKey(), base.getEmergencyContactRelationIhr());
        }
        return req;
    }

    /**
     * 组装创建教育经历 v2 请求
     *
     * @param row      教育经历行
     * @param staffId  iHR 员工 ID
     * @param ihrProps flex key 配置
     * @return 必填字段不全时返回 null
     */
    public IhrStaffEducationCreateV2Request buildEducationCreate(
            MokaEntryEducationDO row,
            String staffId,
            MokaEntryIhrExtensionProperties ihrProps) {
        if (row == null || StringUtils.isBlank(staffId)) {
            return null;
        }
        String school = StringUtils.trimToNull(row.getSchool());
        String major = StringUtils.trimToNull(row.getSpeciality());
        String entryDate = toIhrDate(row.getStartDate());
        String graduateDate = toIhrDate(row.getEndDate());
        if (school == null || major == null || entryDate == null || graduateDate == null) {
            return null;
        }

        IhrStaffEducationCreateV2Request req = new IhrStaffEducationCreateV2Request();
        req.setStaffId(staffId);
        req.setSchoolName(school);
        req.setMajor(major);
        req.setEntryDate(entryDate);
        req.setGraduateDate(graduateDate);

        String diploma = MokaEntryAcademicDegreeMappingConstants.mapToIhrDiplomaType(row.getAcademicDegree());
        if (diploma != null) {
            req.setDiplomaType(diploma);
        }

        String fullTime = row.getIsFullTime();
        if (ihrProps != null && ihrProps.hasEducationFullTimeFlexKey() && StringUtils.isNotBlank(fullTime)) {
            Map<String, Object> flex = new HashMap<>();
            flex.put(ihrProps.getEducationFullTimeFlexKey(), mapFullTimeFlexValue(fullTime.trim()));
            req.setFlexAttributes(flex);
        }
        return req;
    }

    /**
     * 将 yyyy-MM 或 yyyy-MM-dd 转为 iHR 要求的 yyyy-MM-dd
     */
    public String toIhrDate(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String t = raw.trim();
        if (t.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return t;
        }
        if (t.matches("\\d{4}-\\d{2}")) {
            return t + "-01";
        }
        return null;
    }

    private static Object mapFullTimeFlexValue(String fullTimeText) {
        if (MokaEntryCustomFieldConstants.FULL_TIME_YES.equals(fullTimeText)) {
            return Boolean.TRUE;
        }
        if (MokaEntryCustomFieldConstants.FULL_TIME_NO.equals(fullTimeText)) {
            return Boolean.FALSE;
        }
        return fullTimeText;
    }
}