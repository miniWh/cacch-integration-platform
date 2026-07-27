package com.cacch.integration.service.oa.api;

import com.cacch.integration.integration.oa.client.dto.OaFileUploadResult;
import com.cacch.integration.integration.oa.client.dto.OaOrgMember;
import com.cacch.integration.integration.oa.client.dto.OaProcessStartRequest;
import tools.jackson.databind.JsonNode;

/**
 * 致远 OA OpenAPI 服务（Token / 人员 / 发起流程 / 流程状态 / CAP4）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IOaOpenApiService {

    /**
     * 获取 Token（走缓存）
     *
     * @param loginName 绑定登录名，可空
     * @return Token 字符串
     */
    String getToken(String loginName);

    /**
     * 清除 Token 缓存
     *
     * @param loginName 绑定登录名，可空
     */
    void evictToken(String loginName);

    /**
     * 按人员编码查询原始响应
     *
     * @param code      人员编号（CRM emp_code），不可为空
     * @param pageNo    页号，可空（默认 0）
     * @param pageSize  每页条数，可空（默认 20）
     * @param loginName 获取 Token 时绑定的登录名，可空
     * @return 原始 JSON
     */
    JsonNode getOrgMembersByCode(String code, Integer pageNo, Integer pageSize, String loginName);

    /**
     * 按人员编码解析首个人员对象
     *
     * @param code      人员编号，不可为空
     * @param loginName Token 绑定登录名，可空
     * @return 人员；查无时返回 null
     */
    OaOrgMember getOrgMemberByCode(String code, String loginName);

    /**
     * 发起表单流程
     *
     * @param request 发起请求，不可为空
     * @return 原始响应 JSON
     */
    JsonNode startProcess(OaProcessStartRequest request);

    /**
     * 查询流程状态
     *
     * @param flowId    流程实例 ID，不可为空
     * @param loginName Token 绑定登录名，可空
     * @return 原始响应 JSON
     */
    JsonNode getFlowState(String flowId, String loginName);

    /**
     * 上传附件至致远 OA
     *
     * @param fileBytes   文件内容，不可为空
     * @param fileName    文件名，不可为空
     * @param contentType MIME 类型，可空
     * @param loginName   Token 绑定登录名，可空
     * @return 上传结果，含 fileUrl（文件 ID）
     */
    OaFileUploadResult uploadAttachment(byte[] fileBytes, String fileName, String contentType, String loginName);

    /**
     * 获取 CAP4 表单元数据（通过 export 接口，响应 definition 节点）
     *
     * @param formCode      无流程表单模板编码，可空（默认 {@code oa.reg-report.form-code}）
     * @param templateCode  表单模板编号，可空；未传时与 formCode 相同
     * @param rightId       CAP4 操作权限 ID，可空（默认 {@code oa.reg-report.right-id}）
     * @param loginName     OA 登录名，可空（默认 {@code oa.default-login-name}）
     * @param beginDateTime 导出起始日期 yyyy-MM-dd，可空（默认 10 年前）
     * @param endDateTime   导出截止日期 yyyy-MM-dd，可空（默认明天）
     * @param dataId        指定主表数据 ID，可空
     * @param page          页码，可空（默认 1）
     * @param pageSize      每页条数，可空（默认 1，仅取元数据时减小数据量）
     * @return 致远原始响应 JSON（元数据见 {@code data.data.definition}）
     */
    JsonNode getCap4FormMetadata(String formCode,
                                 String templateCode,
                                 String rightId,
                                 String loginName,
                                 String beginDateTime,
                                 String endDateTime,
                                 Long dataId,
                                 Integer page,
                                 Integer pageSize);
}
