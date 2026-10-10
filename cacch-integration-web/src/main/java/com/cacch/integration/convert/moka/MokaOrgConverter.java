package com.cacch.integration.convert.moka;

import com.cacch.integration.dto.moka.request.MokaOrgSyncRequest;
import com.cacch.integration.dto.moka.request.MokaOrgSyncRequest.MokaDepartmentItem;
import com.cacch.integration.dto.moka.request.MokaOrgSyncRequest.MokaLocalizedNameItem;
import com.cacch.integration.dto.moka.vo.MokaDeptSyncResultVO;
import com.cacch.integration.dto.moka.vo.MokaDeptVO;
import com.cacch.integration.integration.moka.client.dto.MokaDeptItem;
import com.cacch.integration.integration.moka.client.dto.MokaDeptListResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncRequest;
import com.cacch.integration.integration.moka.client.dto.MokaDeptSyncResponse;
import com.cacch.integration.integration.moka.client.dto.MokaDepartment;
import com.cacch.integration.integration.moka.client.dto.MokaDepartment.MokaLocalizedName;
import org.mapstruct.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * Moka 组织架构 MapStruct 转换器
 *
 * <p>负责 web 层 DTO ↔ integration 层 DTO 的双向转换：
 * <ul>
 *     <li>web {@link MokaOrgSyncRequest} → integration {@link MokaDeptSyncRequest}</li>
 *     <li>integration {@link MokaDeptSyncResponse} → web {@link MokaDeptSyncResultVO}</li>
 * </ul>
 *
 * <p>嵌套列表元素映射（departments / localizedNames）使用 default 方法显式实现，
 * 避免依赖 MapStruct 自动映射可能产生的隐式错误。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface MokaOrgConverter {

    /**
     * 将 web 层全量同步请求转为 integration 层上游请求体
     */
    default MokaDeptSyncRequest toUpstreamRequest(MokaOrgSyncRequest source) {
        if (source == null) {
            return null;
        }
        MokaDeptSyncRequest target = new MokaDeptSyncRequest();
        target.setDepartments(toDepartmentList(source.getDepartments()));
        return target;
    }

    /**
     * web 层部门列表 → integration 层部门列表
     */
    default List<MokaDepartment> toDepartmentList(List<MokaDepartmentItem> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return source.stream().map(this::toDepartment).toList();
    }

    /**
     * web 层部门条目 → integration 层部门
     */
    default MokaDepartment toDepartment(MokaDepartmentItem source) {
        if (source == null) {
            return null;
        }
        MokaDepartment target = new MokaDepartment();
        target.setName(source.getName());
        target.setDepartmentCode(source.getDepartmentCode());
        target.setParentCode(source.getParentCode());
        target.setType(source.getType());
        target.setSequence(source.getSequence());
        target.setLocalizedNames(toLocalizedNameList(source.getLocalizedNames()));
        return target;
    }

    /**
     * web 层多语言条目列表 → integration 层多语言条目列表
     */
    default List<MokaLocalizedName> toLocalizedNameList(List<MokaLocalizedNameItem> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        return source.stream().map(item -> {
            MokaLocalizedName target = new MokaLocalizedName();
            target.setLocale(item.getLocale());
            target.setPropValue(item.getPropValue());
            return target;
        }).toList();
    }

    /**
     * integration 层同步响应 → web 层同步结果 VO
     */
    default MokaDeptSyncResultVO toSyncResultVO(MokaDeptSyncResponse response) {
        if (response == null) {
            return null;
        }
        MokaDeptSyncResultVO vo = new MokaDeptSyncResultVO();
        vo.setNewCount(response.getNewCount());
        vo.setUpdateCount(response.getUpdateCount());
        vo.setDeleteCount(response.getDeleteCount());
        return vo;
    }

    /**
     * integration 层全量组织架构响应 → web 层部门 VO 列表
     *
     * @param response Moka 全量组织架构响应
     * @return 部门 VO 列表；data 为空时返回空列表
     */
    default List<MokaDeptVO> toDeptVOList(MokaDeptListResponse response) {
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return Collections.emptyList();
        }
        return response.getData().stream().map(this::toDeptVO).toList();
    }

    /**
     * integration 层部门项 → web 层部门 VO
     */
    default MokaDeptVO toDeptVO(MokaDeptItem source) {
        if (source == null) {
            return null;
        }
        MokaDeptVO vo = new MokaDeptVO();
        vo.setId(source.getId());
        vo.setName(source.getName());
        vo.setDepartmentCode(source.getDepartmentCode());
        vo.setParentCode(source.getParentCode());
        vo.setParentId(source.getParentId());
        vo.setType(source.getType());
        vo.setSequence(source.getSequence());
        vo.setStatus(source.getStatus());
        vo.setIsDeleted(source.getIsDeleted());
        vo.setCreatedTime(source.getCreatedTime());
        vo.setLastModifiedTime(source.getLastModifiedTime());
        vo.setLocalizedNames(toLocalizedVOList(source.getLocalizedNames()));
        return vo;
    }

    /**
     * integration 层多语言条目列表 → web 层多语言 VO 列表
     */
    default List<MokaDeptVO.MokaLocalizedVO> toLocalizedVOList(List<MokaLocalizedName> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        return source.stream().map(item -> {
            MokaDeptVO.MokaLocalizedVO vo = new MokaDeptVO.MokaLocalizedVO();
            vo.setLocale(item.getLocale());
            vo.setPropValue(item.getPropValue());
            return vo;
        }).toList();
    }
}
