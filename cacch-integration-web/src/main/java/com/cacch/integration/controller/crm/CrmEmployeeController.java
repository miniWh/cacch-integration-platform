package com.cacch.integration.controller.crm;

import com.cacch.integration.common.dto.crm.CrmEmployeeDTO;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.crm.CrmEmployeeConverter;
import com.cacch.integration.dto.crm.request.CrmEmployeeByIdRequest;
import com.cacch.integration.dto.crm.request.CrmEmployeeQueryApiRequest;
import com.cacch.integration.dto.crm.vo.CrmEmployeeVO;
import com.cacch.integration.service.crm.api.ICrmEmployeeQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 勤策 CRM 员工帐号查询（对外产品化接口）
 *
 * <p>勤策 openId / appKey 仅保留在中台；其他项目通过本接口查询，勿直连勤策。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping("/api/v1/crm/employees")
@RequiredArgsConstructor
public class CrmEmployeeController {

    private final ICrmEmployeeQueryService crmEmployeeQueryService;
    private final CrmEmployeeConverter crmEmployeeConverter;

    /**
     * 按勤策员工 ID 查询员工帐号
     *
     * @param request 含 employeeId，不可为空
     * @return 员工视图；勤策无对应员工时 data 为 null
     */
    @PostMapping("/query-by-id")
    public Result<CrmEmployeeVO> queryById(@Valid @RequestBody CrmEmployeeByIdRequest request) {
        CrmEmployeeDTO dto = crmEmployeeQueryService.queryById(request.getEmployeeId());
        return Result.success(crmEmployeeConverter.toVO(dto));
    }

    /**
     * 按 id / empId / empCode 查询员工帐号
     *
     * @param request 组合条件，至少一项非空
     * @return 员工视图；勤策无对应员工时 data 为 null
     */
    @PostMapping("/query")
    public Result<CrmEmployeeVO> query(@RequestBody CrmEmployeeQueryApiRequest request) {
        if (request == null) {
            request = new CrmEmployeeQueryApiRequest();
        }
        CrmEmployeeDTO dto = crmEmployeeQueryService.query(
                request.getId(), request.getEmpId(), request.getEmpCode());
        return Result.success(crmEmployeeConverter.toVO(dto));
    }
}