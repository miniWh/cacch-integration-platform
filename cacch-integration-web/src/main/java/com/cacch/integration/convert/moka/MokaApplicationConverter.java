package com.cacch.integration.convert.moka;

import com.cacch.integration.dto.moka.vo.MokaApplicationHiredResultVO;
import com.cacch.integration.dto.moka.vo.MokaApplicationResignResultVO;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationHiredResponse;
import com.cacch.integration.integration.moka.client.dto.MokaApplicationResignResponse;
import org.mapstruct.Mapper;

/**
 * Moka 入离职 MapStruct 转换器
 *
 * <p>负责 web 层 DTO ↔ integration 层 DTO 的双向转换：
 * <ul>
 *     <li>web {@link com.cacch.integration.dto.moka.request.MokaApplicationResignRequest}
 *         → integration {@link com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest}</li>
 *     <li>integration {@link MokaApplicationResignResponse} → web {@link MokaApplicationResignResultVO}</li>
 * </ul></p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface MokaApplicationConverter {

    /**
     * web 层标记离职请求 → integration 层上游请求体
     *
     * <p>两层 DTO 字段同名同义，逐字段复制；talentPoolId 列表引用直接透传
     * （请求体每次独立反序列化，无共享可变状态风险）。</p>
     *
     * @param source web 层标记离职请求（已通过 @Valid 校验）
     * @return integration 层离职请求体；source 为 null 时返回 null
     */
    default com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest toUpstreamResignRequest(
            com.cacch.integration.dto.moka.request.MokaApplicationResignRequest source) {
        if (source == null) {
            return null;
        }
        com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest target =
                new com.cacch.integration.integration.moka.client.dto.MokaApplicationResignRequest();
        target.setReasonType(source.getReasonType());
        target.setDetail(source.getDetail());
        target.setTime(source.getTime());
        target.setEmail(source.getEmail());
        target.setTalentPoolId(source.getTalentPoolId());
        target.setIsResignWriteJob(source.getIsResignWriteJob());
        target.setResignJobName(source.getResignJobName());
        target.setResignDepartment(source.getResignDepartment());
        target.setReason(source.getReason());
        target.setResignJobType(source.getResignJobType());
        return target;
    }

    /**
     * integration 层离职响应 → web 层结果 VO
     *
     * <p>success 字段由本服务根据 code 计算（Moka 原始 success 字段已废弃），
     * code/errorMessage 原样透传便于调用方排查。</p>
     *
     * @param response integration 层离职操作响应
     * @return 离职结果 VO；response 为 null 时返回 null
     */
    default MokaApplicationResignResultVO toResignResultVO(MokaApplicationResignResponse response) {
        if (response == null) {
            return null;
        }
        MokaApplicationResignResultVO vo = new MokaApplicationResignResultVO();
        vo.setSuccess(response.isSuccess());
        vo.setCode(response.getCode());
        vo.setErrorMessage(response.getErrorMessage());
        return vo;
    }

    /**
     * web 层标记入职请求 → integration 层上游请求体
     *
     * <p>两层 DTO 字段同名同义，逐字段复制；{@code talentPoolIds} 列表引用直接透传
     * （请求体每次独立反序列化，无共享可变状态风险）。{@code hcId} 为 null 时，
     * 上游 integration DTO 的 {@code NON_NULL} 序列化配置会将其从 JSON 中省略，不传给 Moka。</p>
     *
     * @param source web 层标记入职请求（已通过 @Valid 校验）
     * @return integration 层入职请求体；source 为 null 时返回 null
     */
    default com.cacch.integration.integration.moka.client.dto.MokaApplicationHiredRequest toUpstreamHiredRequest(
            com.cacch.integration.dto.moka.request.MokaApplicationHiredRequest source) {
        if (source == null) {
            return null;
        }
        com.cacch.integration.integration.moka.client.dto.MokaApplicationHiredRequest target =
                new com.cacch.integration.integration.moka.client.dto.MokaApplicationHiredRequest();
        target.setHcId(source.getHcId());
        target.setHiredAt(source.getHiredAt());
        target.setProbation(source.getProbation());
        target.setIsUseOfferHcId(source.getIsUseOfferHcId());
        target.setTalentPoolIds(source.getTalentPoolIds());
        return target;
    }

    /**
     * integration 层入职响应 → web 层结果 VO
     *
     * <p>success / errorMessage 原样透传。</p>
     *
     * @param response integration 层入职操作响应
     * @return 入职结果 VO；response 为 null 时返回 null
     */
    default MokaApplicationHiredResultVO toHiredResultVO(MokaApplicationHiredResponse response) {
        if (response == null) {
            return null;
        }
        MokaApplicationHiredResultVO vo = new MokaApplicationHiredResultVO();
        vo.setSuccess(response.getSuccess());
        vo.setErrorMessage(response.getErrorMessage());
        return vo;
    }
}
