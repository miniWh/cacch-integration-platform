package com.cacch.integration.service.mdm.convert;

import com.cacch.integration.common.utils.mdm.MdmEmpUtils;
import com.cacch.integration.entity.mdm.MdmEmpDO;
import com.cacch.integration.entity.mdm.MdmEmpOrgAssignDO;
import com.cacch.integration.entity.mdm.OdsIhrEmpDO;
import com.cacch.integration.integration.ihr.client.dto.IhrStaffBasicInfo;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDate;

/**
 * 人员主数据（MDM）域 MapStruct 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface MdmEmpConverter {

    // ==================== null 安全辅助方法（接口 default，生成代码直接调用） ====================

    /**
     * Long departmentId → String（iHR 返回 Long，ODS 与任职表存字符串形式）
     */
    default String toOrgCode(Long departmentId) {
        return MdmEmpUtils.longToStringSafe(departmentId);
    }

    /**
     * String → LocalDate（iHR yyyy-MM-dd 格式；兼容带时分秒）
     *
     * <p>解析失败按 null 处理并打 WARN（单字段脏数据不阻断整条落库）。</p>
     */
    default LocalDate toLocalDate(String dateStr, String staffId, String fieldName) {
        return MdmEmpUtils.parseDate(dateStr, staffId, fieldName);
    }

    // ==================== ODS 规范化 ====================

    /**
     * 构造 OdsIhrEmpDO（含额外业务参数：rowHash / verNo / dqFlag / id）
     *
     * @param basic   iHR 员工基本信息 DTO
     * @param odsId   雪花主键（由 Service 预生成）
     * @param rowHash 行变更检测哈希
     * @param verNo   数据版本号（首次 1，变更递增）
     * @param dqFlag  数据质量标记（0-正常 1-存在质量问题）
     * @return 已填充字段的规范化 DO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", source = "odsId")
    @Mapping(target = "ihrStaffId", source = "basic.staffId")
    @Mapping(target = "ihrStaffNo", source = "basic.staffNo")
    @Mapping(target = "ihrStaffName", source = "basic.staffName")
    @Mapping(target = "ihrMobile", source = "basic.mobileNo")
    @Mapping(target = "ihrEmail", source = "basic.email")
    @Mapping(target = "ihrWorkEmail", source = "basic.workEmail")
    @Mapping(target = "ihrIdCardType", source = "basic.idCardType")
    @Mapping(target = "ihrIdCard", source = "basic.idCardNo")
    @Mapping(target = "ihrDeptId", expression = "java(toOrgCode(basic.getDepartmentId()))")
    @Mapping(target = "ihrDeptName", source = "basic.departmentName")
    @Mapping(target = "ihrPositionId", source = "basic.positionId")
    @Mapping(target = "ihrPositionName", source = "basic.positionName")
    @Mapping(target = "ihrStatus", source = "basic.staffStatus")
    @Mapping(target = "ihrGender", source = "basic.sex")
    @Mapping(target = "ihrEducation", source = "basic.highestEducation")
    @Mapping(target = "ihrBirthday", expression = "java(toLocalDate(basic.getBirthday(), basic.getStaffId(), \"birthday\"))")
    @Mapping(target = "ihrHireDate", expression = "java(toLocalDate(basic.getEnrollInDate(), basic.getStaffId(), \"enrollInDate\"))")
    @Mapping(target = "ihrLeaveDate", expression = "java(toLocalDate(basic.getLeaveDate(), basic.getStaffId(), \"leaveDate\"))")
    @Mapping(target = "ihrProbationEndDate", expression = "java(toLocalDate(basic.getProbationEndDate(), basic.getStaffId(), \"probationEndDate\"))")
    @Mapping(target = "rowHash", source = "rowHash")
    @Mapping(target = "verNo", source = "verNo")
    @Mapping(target = "dqFlag", source = "dqFlag")
    OdsIhrEmpDO toOds(IhrStaffBasicInfo basic, Long odsId, String rowHash, int verNo, int dqFlag);

    // ==================== MDM_EMP 主档 ====================

    /**
     * 构造 MdmEmpDO（首次建档时调用）
     *
     * @param basic         iHR 员工基本信息 DTO
     * @param mdCode        人员主数据编码（雪花，由 Service 预生成）
     * @param srcSysCode    首次建档来源系统（如 IHR）
     * @param firstHireDate 首次入职日期（Service 解析好传入）
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "mdCode", source = "mdCode")
    @Mapping(target = "srcSysCode", source = "srcSysCode")
    @Mapping(target = "firstHireDate", source = "firstHireDate")
    @Mapping(target = "empNo", source = "basic.staffNo")
    @Mapping(target = "empName", source = "basic.staffName")
    @Mapping(target = "gender", source = "basic.sex")
    @Mapping(target = "birthday", expression = "java(toLocalDate(basic.getBirthday(), basic.getStaffId(), \"birthday\"))")
    @Mapping(target = "mobile", source = "basic.mobileNo")
    @Mapping(target = "personalEmail", source = "basic.email")
    @Mapping(target = "workEmail", source = "basic.workEmail")
    @Mapping(target = "education", source = "basic.highestEducation")
    @Mapping(target = "idCardType", source = "basic.idCardType")
    @Mapping(target = "idCard", source = "basic.idCardNo")
    @Mapping(target = "probationEndDate", expression = "java(toLocalDate(basic.getProbationEndDate(), basic.getStaffId(), \"probationEndDate\"))")
    @Mapping(target = "leaveDate", expression = "java(toLocalDate(basic.getLeaveDate(), basic.getStaffId(), \"leaveDate\"))")
    MdmEmpDO toNewMdEmp(IhrStaffBasicInfo basic, Long mdCode, String srcSysCode,
                        LocalDate firstHireDate);

    /**
     * 更新 MdmEmpDO 基础人属性（已有主档增量覆盖）
     *
     * @param basic  iHR 员工基本信息 DTO
     * @param target 已预填业务主键的 MdmEmpDO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "empNo", source = "basic.staffNo")
    @Mapping(target = "empName", source = "basic.staffName")
    @Mapping(target = "gender", source = "basic.sex")
    @Mapping(target = "birthday", expression = "java(toLocalDate(basic.getBirthday(), basic.getStaffId(), \"birthday\"))")
    @Mapping(target = "mobile", source = "basic.mobileNo")
    @Mapping(target = "personalEmail", source = "basic.email")
    @Mapping(target = "workEmail", source = "basic.workEmail")
    @Mapping(target = "education", source = "basic.highestEducation")
    @Mapping(target = "idCardType", source = "basic.idCardType")
    @Mapping(target = "idCard", source = "basic.idCardNo")
    @Mapping(target = "probationEndDate", expression = "java(toLocalDate(basic.getProbationEndDate(), basic.getStaffId(), \"probationEndDate\"))")
    @Mapping(target = "leaveDate", expression = "java(toLocalDate(basic.getLeaveDate(), basic.getStaffId(), \"leaveDate\"))")
    void updateBasicInfo(IhrStaffBasicInfo basic, @MappingTarget MdmEmpDO target);

    // ==================== MDM_EMP_ORG_ASSIGN 任职 ====================

    /**
     * 构造 MdmEmpOrgAssignDO（mdCode / orgCode / orgName / positionCode / positionName
     * / assignStatus / principalFlag / validFrom 由调用方显式传入）
     *
     * @param basic         iHR 员工基本信息 DTO（departmentId / departmentName / positionId / positionName 复用）
     * @param mdCode        人员主数据编码（雪花）
     * @param assignStatus  任职状态常量
     * @param principalFlag 主任职标记
     * @param validFrom     生效起始日
     * @return 已填充字段的任职 DO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "mdCode", source = "mdCode")
    @Mapping(target = "orgCode", expression = "java(toOrgCode(basic.getDepartmentId()))")
    @Mapping(target = "orgName", source = "basic.departmentName")
    @Mapping(target = "positionCode", source = "basic.positionId")
    @Mapping(target = "positionName", source = "basic.positionName")
    @Mapping(target = "assignStatus", source = "assignStatus")
    @Mapping(target = "principalFlag", source = "principalFlag")
    @Mapping(target = "validFrom", source = "validFrom")
    MdmEmpOrgAssignDO toAssign(IhrStaffBasicInfo basic, Long mdCode, String assignStatus, String principalFlag, LocalDate validFrom);
}
