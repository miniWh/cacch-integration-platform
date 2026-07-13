package com.cacch.integration.manager.crm.api.impl;

import com.cacch.integration.common.dto.crm.CrmOaUserMappingResult;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.entity.crm.CrmOaUserMappingDO;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.crm.support.CrmEmployeeSupport;
import com.cacch.integration.integration.oa.client.dto.OaOrgMember;
import com.cacch.integration.integration.oa.support.OaResponseSupport;
import com.cacch.integration.manager.crm.api.ICrmOaUserMappingManager;
import com.cacch.integration.service.crm.api.ICrmOaUserMappingService;
import com.cacch.integration.service.crm.api.ICrmOpenApiService;
import com.cacch.integration.service.oa.api.IOaOpenApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * CRM↔OA 人员映射编排实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmOaUserMappingManagerImpl implements ICrmOaUserMappingManager {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final ICrmOaUserMappingService crmOaUserMappingService;
    private final ICrmOpenApiService crmOpenApiService;
    private final IOaOpenApiService oaOpenApiService;

    @Override
    public CrmOaUserMappingResult resolve(String crmEmployeeId) {
        return resolve(crmEmployeeId, false);
    }

    @Override
    public CrmOaUserMappingResult resolve(String crmEmployeeId, boolean forceRefresh) {
        if (!StringUtils.hasText(crmEmployeeId)) {
            log.info("【CrmOaUserMapping】映射终止, reason=CRM员工ID为空");
            return CrmOaUserMappingResult.fail(crmEmployeeId, "CRM员工ID为空");
        }
        String employeeId = crmEmployeeId.trim();
        log.info("【CrmOaUserMapping】开始解析人员映射, crmEmployeeId={}, forceRefresh={}",
                employeeId, forceRefresh);

        if (!forceRefresh) {
            CrmOaUserMappingDO cached = crmOaUserMappingService.getByCrmEmployeeId(employeeId);
            if (cached != null && StringUtils.hasText(cached.getOaUserId())) {
                log.info("【CrmOaUserMapping】命中库表缓存, crmEmployeeId={}, empCode={}, oaUserId={}",
                        employeeId, cached.getEmpCode(), cached.getOaUserId());
                return CrmOaUserMappingResult.ok(
                        employeeId,
                        cached.getEmpCode(),
                        cached.getOaUserId(),
                        cached.getOaLoginName(),
                        cached.getCrmEmployeeName(),
                        true);
            }
            if (cached != null) {
                log.info("【CrmOaUserMapping】库表映射无效(缺oaUserId)，将重新解析, crmEmployeeId={}", employeeId);
            }
        }

        try {
            return resolveRemoteAndPersist(employeeId);
        } catch (BizException e) {
            log.info("【CrmOaUserMapping】映射业务失败, crmEmployeeId={}, reason={}",
                    employeeId, e.getMessage());
            return CrmOaUserMappingResult.fail(employeeId, e.getMessage());
        } catch (Exception e) {
            log.info("【CrmOaUserMapping】映射异常终止, crmEmployeeId={}, reason={}",
                    employeeId, e.getMessage());
            log.error("【CrmOaUserMapping】映射失败, crmEmployeeId={}", employeeId, e);
            return CrmOaUserMappingResult.fail(employeeId, "人员映射异常: " + e.getMessage());
        }
    }

    @Override
    public CrmOaUserMappingDO getCached(String crmEmployeeId) {
        return crmOaUserMappingService.getByCrmEmployeeId(crmEmployeeId);
    }

    private CrmOaUserMappingResult resolveRemoteAndPersist(String employeeId) {
        // ① CRM 查询员工帐号 → emp_code
        CrmOpenApiResponse crmResponse = crmOpenApiService.queryEmployeeById(employeeId);
        JsonNode responseData = crmResponse.getResponseData();
        JsonNode employeeNode = CrmEmployeeSupport.firstEmployeeNode(responseData);
        if (employeeNode == null) {
            log.info("【CrmOaUserMapping】映射终止, crmEmployeeId={}, reason=CRM员工响应无数据", employeeId);
            return CrmOaUserMappingResult.fail(employeeId, "CRM无员工数据");
        }
        String empCode = CrmEmployeeSupport.text(employeeNode, "emp_code");
        String empName = firstNonBlank(
                CrmEmployeeSupport.text(employeeNode, "emp_name"),
                CrmEmployeeSupport.text(employeeNode, "name"));
        if (!StringUtils.hasText(empCode)) {
            log.info("【CrmOaUserMapping】映射终止, crmEmployeeId={}, reason=CRM无emp_code", employeeId);
            return CrmOaUserMappingResult.fail(employeeId, "CRM无 emp_code");
        }
        log.info("【CrmOaUserMapping】CRM员工解析成功, crmEmployeeId={}, empCode={}, empName={}",
                employeeId, empCode, empName);

        // ② OA 按编码取人员 → id / loginName
        JsonNode oaRaw = oaOpenApiService.getOrgMembersByCode(empCode, null, null, null);
        OaOrgMember oaMember = OaResponseSupport.firstOrgMember(oaRaw);
        if (oaMember == null || !StringUtils.hasText(oaMember.getId())) {
            log.info("【CrmOaUserMapping】映射终止, crmEmployeeId={}, empCode={}, reason=OA按code查无",
                    employeeId, empCode);
            return CrmOaUserMappingResult.fail(employeeId, "OA按 code 查无人员, emp_code=" + empCode);
        }
        log.info("【CrmOaUserMapping】OA人员解析成功, crmEmployeeId={}, empCode={}, oaUserId={}, oaLoginName={}",
                employeeId, empCode, oaMember.getId(), oaMember.getLoginName());

        // ③ UPSERT 落库
        CrmOaUserMappingDO mapping = new CrmOaUserMappingDO();
        mapping.setCrmEmployeeId(employeeId);
        mapping.setEmpCode(empCode);
        mapping.setOaUserId(oaMember.getId().trim());
        mapping.setOaLoginName(StringUtils.hasText(oaMember.getLoginName())
                ? oaMember.getLoginName().trim() : null);
        mapping.setCrmEmployeeName(empName);
        mapping.setCrmRawPayload(toJsonObject(responseData != null ? responseData : employeeNode));
        mapping.setOaRawPayload(toJsonObject(oaRaw));
        CrmOaUserMappingDO saved = crmOaUserMappingService.saveOrUpdateByCrmEmployeeId(mapping);

        return CrmOaUserMappingResult.ok(
                saved.getCrmEmployeeId(),
                saved.getEmpCode(),
                saved.getOaUserId(),
                saved.getOaLoginName(),
                saved.getCrmEmployeeName(),
                false);
    }

    private static Object toJsonObject(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(node, Object.class);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
