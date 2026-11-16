package com.cacch.integration.service.mdm.convert;

import com.cacch.integration.common.utils.mdm.MdmUtils;
import com.cacch.integration.entity.mdm.MdmPositionDO;
import com.cacch.integration.entity.mdm.OdsIhrPositionDO;
import com.cacch.integration.integration.ihr.client.dto.IhrPosition;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 岗位主数据（MDM）域 MapStruct 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface MdmPositionConverter {

    // ==================== null / 空串 安全辅助方法（接口 default，生成代码直接调用） ====================

    /**
     * String → null 安全（空串视为 NULL，防止数据库存储 '' 污染）
     */
    default String nullIfBlank(String value) {
        return MdmUtils.nullIfBlank(value);
    }

    /**
     * Long（iHR departmentId）→ String（ODS departmentId 字段；null 透传）
     */
    default String toStr(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Boolean isPositionGroup → Integer 0/1（ODS is_position_group 字段；null 透传）
     */
    default Integer toGroupFlag(Boolean isPositionGroup) {
        return isPositionGroup == null ? null : (isPositionGroup ? 1 : 0);
    }

    // ==================== ODS 规范化 ====================

    /**
     * 构造 OdsIhrPositionDO（含额外业务参数：rowHash / verNo / id / positionScopeJson；dqFlag 当前恒 0）
     *
     * <p>ODS 职责：完整源系统快照。departmentId 存 iHR 原始部门 ID（Long 转 String），
     * 主档翻译由 Service 层独立完成，Converter 不持有翻译逻辑。</p>
     *
     * @param pos               iHR 职位实体 DTO
     * @param odsId             雪花主键（由 Service 预生成）
     * @param rowHash           行变更检测哈希（基于 ODS 侧字段构造）
     * @param verNo             数据版本号（首次 1，变更递增）
     * @param positionScopeJson 应用范围部门 ID 列表 JSON 字符串（由 Service 预序列化；null 透传）
     * @return 已填充字段的规范化 DO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", source = "odsId")
    @Mapping(target = "ihrPositionUuid", source = "pos.id")
    @Mapping(target = "companyId", expression = "java(nullIfBlank(pos.getCompanyId()))")
    @Mapping(target = "positionName", expression = "java(nullIfBlank(pos.getPositionName()))")
    @Mapping(target = "abbreviation", expression = "java(nullIfBlank(pos.getAbbreviation()))")
    @Mapping(target = "positionCode", expression = "java(nullIfBlank(pos.getPositionCode()))")
    @Mapping(target = "appliedRange", expression = "java(nullIfBlank(pos.getAppliedRange()))")
    @Mapping(target = "capacity", source = "pos.capacity")
    @Mapping(target = "effectiveDate", expression = "java(nullIfBlank(pos.getEffectiveDate()))")
    @Mapping(target = "description", expression = "java(nullIfBlank(pos.getDescription()))")
    @Mapping(target = "departmentId", expression = "java(toStr(pos.getDepartmentId()))")
    @Mapping(target = "departmentName", expression = "java(nullIfBlank(pos.getDepartmentName()))")
    @Mapping(target = "jobTitleId", expression = "java(nullIfBlank(pos.getJobTitleId()))")
    @Mapping(target = "jobTitleName", expression = "java(nullIfBlank(pos.getJobTitleName()))")
    @Mapping(target = "positionGradeId", expression = "java(nullIfBlank(pos.getPositionGradeId()))")
    @Mapping(target = "positionGradeName", expression = "java(nullIfBlank(pos.getPositionGradeName()))")
    @Mapping(target = "qualifications", expression = "java(nullIfBlank(pos.getQualifications()))")
    @Mapping(target = "parentId", expression = "java(nullIfBlank(pos.getParentId()))")
    @Mapping(target = "isPositionGroup", expression = "java(toGroupFlag(pos.getIsPositionGroup()))")
    @Mapping(target = "positionState", expression = "java(nullIfBlank(pos.getPositionState()))")
    @Mapping(target = "positionStateString", expression = "java(nullIfBlank(pos.getPositionStateString()))")
    @Mapping(target = "updatedDate", expression = "java(nullIfBlank(pos.getUpdatedDate()))")
    @Mapping(target = "createdDate", expression = "java(nullIfBlank(pos.getCreatedDate()))")
    @Mapping(target = "positionScope", source = "positionScopeJson")
    @Mapping(target = "rowHash", source = "rowHash")
    @Mapping(target = "verNo", source = "verNo")
    @Mapping(target = "dqFlag", constant = "0")
    OdsIhrPositionDO toOds(IhrPosition pos, Long odsId, String rowHash, int verNo, String positionScopeJson);

    // ==================== MDM_POSITION 主档 ====================

    /**
     * 构造 MdmPositionDO（首次建档时调用）
     *
     * <p>主档职责：纯主数据语义，departmentId 存翻译后的 mdm_dept.id(BIGINT)。
     * id 显式忽略（防止 MapStruct 同名隐式映射把 iHR 职位 ID 填入主键），
     * insert 时保持 null 由 MyBatis-Plus ASSIGN_ID 生成雪花主键。</p>
     *
     * @param pos        iHR 职位实体 DTO
     * @param srcSysCode 首次建档来源系统（如 IHR）
     * @param mdmDeptId  翻译后的 mdm_dept.id（未命中时为 null；由 Service 层完成翻译后传入）
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "srcSysCode", source = "srcSysCode")
    @Mapping(target = "positionCode", expression = "java(nullIfBlank(pos.getPositionCode()))")
    @Mapping(target = "positionName", expression = "java(nullIfBlank(pos.getPositionName()))")
    @Mapping(target = "parentPositionId", expression = "java(nullIfBlank(pos.getParentId()))")
    @Mapping(target = "departmentId", source = "mdmDeptId")
    @Mapping(target = "departmentName", expression = "java(nullIfBlank(pos.getDepartmentName()))")
    @Mapping(target = "jobTitleId", expression = "java(nullIfBlank(pos.getJobTitleId()))")
    @Mapping(target = "jobTitleName", expression = "java(nullIfBlank(pos.getJobTitleName()))")
    @Mapping(target = "positionGradeId", expression = "java(nullIfBlank(pos.getPositionGradeId()))")
    @Mapping(target = "positionGradeName", expression = "java(nullIfBlank(pos.getPositionGradeName()))")
    @Mapping(target = "appliedRange", expression = "java(nullIfBlank(pos.getAppliedRange()))")
    @Mapping(target = "capacity", source = "pos.capacity")
    @Mapping(target = "effectiveDate", expression = "java(nullIfBlank(pos.getEffectiveDate()))")
    @Mapping(target = "positionState", expression = "java(nullIfBlank(pos.getPositionState()))")
    MdmPositionDO toNewMdPosition(IhrPosition pos, String srcSysCode, Long mdmDeptId);

    /**
     * 更新 MdmPositionDO 可变属性（已有主档增量覆盖；srcSysCode / id 均保留不映射）
     *
     * <p>id 显式忽略：防止 MapStruct 同名隐式映射用 iHR 职位 ID 覆写已有主键，
     * 导致 updateById 指向错误行。</p>
     *
     * @param pos       iHR 职位实体 DTO
     * @param mdmDeptId 翻译后的 mdm_dept.id（未命中时为 null）
     * @param target    已按 src_ref 定位的 MdmPositionDO（含真实主键）
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "positionCode", expression = "java(nullIfBlank(pos.getPositionCode()))")
    @Mapping(target = "positionName", expression = "java(nullIfBlank(pos.getPositionName()))")
    @Mapping(target = "parentPositionId", expression = "java(nullIfBlank(pos.getParentId()))")
    @Mapping(target = "departmentId", source = "mdmDeptId")
    @Mapping(target = "departmentName", expression = "java(nullIfBlank(pos.getDepartmentName()))")
    @Mapping(target = "jobTitleId", expression = "java(nullIfBlank(pos.getJobTitleId()))")
    @Mapping(target = "jobTitleName", expression = "java(nullIfBlank(pos.getJobTitleName()))")
    @Mapping(target = "positionGradeId", expression = "java(nullIfBlank(pos.getPositionGradeId()))")
    @Mapping(target = "positionGradeName", expression = "java(nullIfBlank(pos.getPositionGradeName()))")
    @Mapping(target = "appliedRange", expression = "java(nullIfBlank(pos.getAppliedRange()))")
    @Mapping(target = "capacity", source = "pos.capacity")
    @Mapping(target = "effectiveDate", expression = "java(nullIfBlank(pos.getEffectiveDate()))")
    @Mapping(target = "positionState", expression = "java(nullIfBlank(pos.getPositionState()))")
    void updateBasicInfo(IhrPosition pos, Long mdmDeptId, @MappingTarget MdmPositionDO target);
}
