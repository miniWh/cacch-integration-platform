package com.cacch.integration.service.mdm.convert;

import com.cacch.integration.common.utils.mdm.MdmDeptUtils;
import com.cacch.integration.entity.mdm.MdmDeptDO;
import com.cacch.integration.entity.mdm.OdsIhrDeptDO;
import com.cacch.integration.integration.ihr.client.dto.IhrDepartment;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 部门主数据（MDM）域 MapStruct 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface MdmDeptConverter {

    // ==================== null / 空串 安全辅助方法（接口 default，生成代码直接调用） ====================

    /**
     * String → null 安全（空串视为 NULL，防止数据库存储 '' 污染）
     */
    default String nullIfBlank(String value) {
        return MdmDeptUtils.nullIfBlank(value);
    }

    /**
     * Boolean virtual → Integer 0/1（ODS is_virtual 字段；null 透传）
     */
    default Integer toVirtualFlag(Boolean virtual) {
        return virtual == null ? null : (virtual ? 1 : 0);
    }

    // ==================== ODS 规范化 ====================

    /**
     * 构造 OdsIhrDeptDO（含额外业务参数：rowHash / verNo / id；dqFlag 当前恒 0）
     *
     * @param dept    iHR 部门实体 DTO
     * @param odsId   雪花主键（由 Service 预生成）
     * @param rowHash 行变更检测哈希
     * @param verNo   数据版本号（首次 1，变更递增）
     * @return 已填充字段的规范化 DO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", source = "odsId")
    @Mapping(target = "ihrDeptUuid", source = "dept.uuid")
    @Mapping(target = "ihrDeptId", expression = "java(nullIfBlank(dept.getId()))")
    @Mapping(target = "departmentCode", expression = "java(nullIfBlank(dept.getDepartmentCode()))")
    @Mapping(target = "deptName", expression = "java(nullIfBlank(dept.getName()))")
    @Mapping(target = "parentId", expression = "java(nullIfBlank(dept.getParentId()))")
    @Mapping(target = "parentDepartmentCode", expression = "java(nullIfBlank(dept.getParentDepartmentCode()))")
    @Mapping(target = "deptType", expression = "java(nullIfBlank(dept.getType()))")
    @Mapping(target = "storeNumber", expression = "java(nullIfBlank(dept.getStoreNumber()))")
    @Mapping(target = "principalStaffId", expression = "java(nullIfBlank(dept.getPrincipalStaffId()))")
    @Mapping(target = "deptStatus", expression = "java(nullIfBlank(dept.getDepartmentStatus()))")
    @Mapping(target = "isVirtual", expression = "java(toVirtualFlag(dept.getVirtual()))")
    @Mapping(target = "costName", expression = "java(nullIfBlank(dept.getCostName()))")
    @Mapping(target = "costCode", expression = "java(nullIfBlank(dept.getCostCode()))")
    @Mapping(target = "companySiteId", expression = "java(nullIfBlank(dept.getCompanySiteId()))")
    @Mapping(target = "companySiteName", expression = "java(nullIfBlank(dept.getCompanySiteName()))")
    @Mapping(target = "sequence", source = "dept.sequence")
    @Mapping(target = "rowHash", source = "rowHash")
    @Mapping(target = "verNo", source = "verNo")
    @Mapping(target = "dqFlag", constant = "0")
    OdsIhrDeptDO toOds(IhrDepartment dept, Long odsId, String rowHash, int verNo);

    // ==================== MDM_DEPT 主档 ====================

    /**
     * 构造 MdmDeptDO（首次建档时调用；id 由 MyBatis-Plus ASSIGN_ID 自动填充）
     *
     * @param dept       iHR 部门实体 DTO
     * @param srcSysCode 首次建档来源系统（如 IHR）
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "deptUuid", source = "dept.uuid")
    @Mapping(target = "srcSysCode", source = "srcSysCode")
    @Mapping(target = "deptCode", expression = "java(nullIfBlank(dept.getDepartmentCode()))")
    @Mapping(target = "deptName", expression = "java(nullIfBlank(dept.getName()))")
    @Mapping(target = "parentDeptCode", expression = "java(nullIfBlank(dept.getParentDepartmentCode()))")
    @Mapping(target = "deptType", expression = "java(nullIfBlank(dept.getType()))")
    @Mapping(target = "deptStatus", expression = "java(nullIfBlank(dept.getDepartmentStatus()))")
    @Mapping(target = "principalStaffId", expression = "java(nullIfBlank(dept.getPrincipalStaffId()))")
    @Mapping(target = "sequence", source = "dept.sequence")
    MdmDeptDO toNewMdDept(IhrDepartment dept, String srcSysCode);

    /**
     * 更新 MdmDeptDO 可变属性（已有主档增量覆盖；srcSysCode 保留不映射）
     *
     * @param dept   iHR 部门实体 DTO
     * @param target 已预填业务主键的 MdmDeptDO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "deptCode", expression = "java(nullIfBlank(dept.getDepartmentCode()))")
    @Mapping(target = "deptName", expression = "java(nullIfBlank(dept.getName()))")
    @Mapping(target = "parentDeptCode", expression = "java(nullIfBlank(dept.getParentDepartmentCode()))")
    @Mapping(target = "deptType", expression = "java(nullIfBlank(dept.getType()))")
    @Mapping(target = "deptStatus", expression = "java(nullIfBlank(dept.getDepartmentStatus()))")
    @Mapping(target = "principalStaffId", expression = "java(nullIfBlank(dept.getPrincipalStaffId()))")
    @Mapping(target = "sequence", source = "dept.sequence")
    void updateBasicInfo(IhrDepartment dept, @MappingTarget MdmDeptDO target);
}
