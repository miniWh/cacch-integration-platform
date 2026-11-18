package com.cacch.integration.service.crm.api.impl;

import com.cacch.integration.common.dto.crm.CrmEmployeeDTO;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.crm.client.dto.CrmEmployeeQueryRequest;
import com.cacch.integration.integration.crm.client.dto.CrmOpenApiResponse;
import com.cacch.integration.integration.crm.support.CrmEmployeeSupport;
import com.cacch.integration.service.crm.api.ICrmEmployeeQueryService;
import com.cacch.integration.service.crm.api.ICrmOpenApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

/**
 * 勤策员工帐号查询服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmEmployeeQueryServiceImpl implements ICrmEmployeeQueryService {

    private final ICrmOpenApiService crmOpenApiService;

    @Override
    public CrmEmployeeDTO queryById(String employeeId) {
        if (!StringUtils.hasText(employeeId)) {
            log.info("【CrmEmployee】按ID查询终止, reason=employeeId为空");
            throw new BizException(ResultCode.PARAM_MISSING, "employeeId 不能为空");
        }
        String id = employeeId.trim();
        log.info("【CrmEmployee】按ID查询员工, employeeId={}", id);
        CrmOpenApiResponse response = crmOpenApiService.queryEmployeeById(id);
        return toDto(id, response);
    }

    @Override
    public CrmEmployeeDTO query(String id, String empId, String empCode) {
        String normalizedId = StringUtils.hasText(id) ? id.trim() : null;
        String normalizedEmpId = StringUtils.hasText(empId) ? empId.trim() : null;
        String normalizedEmpCode = StringUtils.hasText(empCode) ? empCode.trim() : null;
        if (normalizedId == null && normalizedEmpId == null && normalizedEmpCode == null) {
            log.info("【CrmEmployee】组合查询终止, reason=id/empId/empCode均为空");
            throw new BizException(ResultCode.PARAM_MISSING, "id、empId、empCode 至少填写一项");
        }
        log.info("【CrmEmployee】组合查询员工, id={}, empId={}, empCode={}",
                normalizedId, normalizedEmpId, normalizedEmpCode);
        CrmEmployeeQueryRequest request = CrmEmployeeQueryRequest.builder()
                .id(normalizedId)
                .empId(normalizedEmpId)
                .empCode(normalizedEmpCode)
                .build();
        CrmOpenApiResponse response = crmOpenApiService.queryEmployee(request);
        return toDto(normalizedId != null ? normalizedId : "combo", response);
    }

    private CrmEmployeeDTO toDto(String logKey, CrmOpenApiResponse response) {
        JsonNode employeeNode = CrmEmployeeSupport.firstEmployeeNode(response.getResponseData());
        if (employeeNode == null) {
            log.info("【CrmEmployee】查询无员工数据, key={}", logKey);
            return null;
        }
        String empName = firstNonBlank(
                CrmEmployeeSupport.text(employeeNode, "emp_name"),
                CrmEmployeeSupport.text(employeeNode, "name"));
        CrmEmployeeDTO dto = CrmEmployeeDTO.builder()
                .id(CrmEmployeeSupport.text(employeeNode, "id"))
                .empId(CrmEmployeeSupport.text(employeeNode, "emp_id"))
                .empCode(CrmEmployeeSupport.text(employeeNode, "emp_code"))
                .empName(empName)
                .mobile(CrmEmployeeSupport.text(employeeNode, "mobile"))
                .status(firstNonBlank(
                        CrmEmployeeSupport.text(employeeNode, "status"),
                        CrmEmployeeSupport.text(employeeNode, "emp_status")))
                .build();
        log.info("【CrmEmployee】查询成功, key={}, empCode={}, empName={}",
                logKey, dto.getEmpCode(), dto.getEmpName());
        return dto;
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a.trim();
        }
        if (StringUtils.hasText(b)) {
            return b.trim();
        }
        return null;
    }
}