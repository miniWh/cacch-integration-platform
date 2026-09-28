package com.cacch.integration.convert.ihr;

import com.cacch.integration.dto.ihr.request.SearchDepartmentRequest;
import com.cacch.integration.dto.ihr.vo.DepartmentPageVO;
import com.cacch.integration.dto.ihr.vo.DepartmentVO;
import com.cacch.integration.entity.ihr.IhrDepartmentDO;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchRequest;
import com.cacch.integration.integration.ihr.client.dto.IhrOrgSearchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;

/**
 * IHR 组织架构 MapStruct 转换器
 *
 * <p>负责 web 层 DTO ↔ integration 层 DTO 的双向转换：
 * <ul>
 *     <li>web {@link SearchDepartmentRequest} → integration {@link IhrOrgSearchRequest}</li>
 *     <li>integration {@link IhrDepartment} → web {@link DepartmentVO}</li>
 *     <li>integration {@link IhrOrgSearchResponse} → web {@link DepartmentPageVO}</li>
 * </ul>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface IhrOrgConverter {

    /**
     * 将 web 层入参转为 integration 层的上游请求体
     */
    default IhrOrgSearchRequest toUpstreamRequest(SearchDepartmentRequest source) {
        if (source == null) {
            return null;
        }
        IhrOrgSearchRequest target = new IhrOrgSearchRequest();
        target.setPage(source.getPage() == null ? 0 : source.getPage());
        target.setSize(source.getSize() == null ? 10 : source.getSize());
        if (source.getConditions() == null || source.getConditions().isEmpty()) {
            target.setSearchArgsList(Collections.emptyList());
            return target;
        }
        List<IhrOrgSearchRequest.SearchArg> args = source.getConditions().stream()
                .map(c -> {
                    IhrOrgSearchRequest.SearchArg arg = new IhrOrgSearchRequest.SearchArg();
                    arg.setSearchKey(c.getSearchKey());
                    arg.setSearchParam(c.getSearchParam());
                    arg.setSearchType(c.getSearchType());
                    return arg;
                })
                .toList();
        target.setSearchArgsList(args);
        return target;
    }

    /**
     * integration 部门实体 → web VO
     *
     * <p>字段名不一致的映射（IHR 实测字段名 → VO 字段名）：
     * storeNumber → shortNumber、createdDate → createDate</p>
     */
    @Mapping(source = "storeNumber", target = "shortNumber")
    @Mapping(source = "createdDate", target = "createDate")
    DepartmentVO toVO(IhrDepartment source);

    /**
     * integration 部门列表 → web VO 列表（null/空 入参返回空列表）
     */
    default List<DepartmentVO> toVOList(List<IhrDepartment> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return source.stream().map(this::toVO).toList();
    }

    /**
     * integration 分页响应 → web 分页 VO
     */
    default DepartmentPageVO toPageVO(IhrOrgSearchResponse response) {
        if (response == null) {
            return null;
        }
        DepartmentPageVO pageVO = new DepartmentPageVO();
        pageVO.setDepartments(toVOList(response.getData()));
        pageVO.setTotalElements(response.getTotalElements());
        pageVO.setTotalPages(response.getTotalPages());
        pageVO.setEnd(Boolean.TRUE.equals(response.getEnd()));
        return pageVO;
    }

    /**
     * IHR 部门快照 DO → web DepartmentVO
     *
     * <p>字段映射差异：DO.storeNumber → VO.shortNumber；
     * DO.id(Long 雪花主键) 不映射到 VO.id（VO.id 复用 uuid/ihr_dept_id 语义）；
     * DO.createdDate(LocalDateTime) 与 VO.createDate(Long 毫秒) 类型不匹配，暂忽略。</p>
     *
     * @param source 部门快照 DO
     * @return 部门视图对象；source 为 null 时返回 null
     */
    @Mapping(target = "id", source = "ihrDeptId")
    @Mapping(source = "storeNumber", target = "shortNumber")
    @Mapping(target = "createDate", ignore = true)
    DepartmentVO toVOFromDO(IhrDepartmentDO source);

    /**
     * IHR 部门快照 DO 列表 → web DepartmentVO 列表
     *
     * @param source 部门快照 DO 列表
     * @return VO 列表，null/空 入参返回空列表
     */
    default List<DepartmentVO> toVOListFromDO(List<IhrDepartmentDO> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return source.stream().map(this::toVOFromDO).toList();
    }
}
