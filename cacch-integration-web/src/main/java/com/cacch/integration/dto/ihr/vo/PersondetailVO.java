package com.cacch.integration.dto.ihr.vo;

import lombok.Data;

/**
 * iHR persondetail 人员详情视图
 *
 * <p>字段与 {@code PersondetailDO} 当前已映射字段保持一致；未命中时各字段均为 null。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class PersondetailVO {

    /**
     * 员工 ID（persondetail.id，业务主键）
     */
    private String userId;

    /**
     * 员工编号（persondetail."staffNo"）
     */
    private String employeeNo;

    /**
     * 员工姓名（persondetail."staffName"）
     */
    private String userName;

    /**
     * 昵称（persondetail."nickName"）
     */
    private String nickname;

    /**
     * 邮箱（persondetail."workEmail"）
     */
    private String companyEmail;

    /**
     * 手机号（persondetail."mobileNo"）
     */
    private String contactPhone;

    /**
     * 部门 ID（persondetail."departmentId"）
     */
    private String departmentId;

    /**
     * 在职状态（persondetail."staffStatus"），取值 IN_SERVICE=在职 / QUIT=离职
     */
    private String employeeStatus;

    /**
     * 合同开始日期（persondetail."contractBeginDate"，text，格式 yyyy-MM-dd）
     */
    private String contractBeginDate;

    /**
     * 试用期结束日期（persondetail."probationEndDate"，text，格式 yyyy-MM-dd）
     */
    private String probationEndDate;

    /**
     * 离职日期（persondetail."leaveDate"，text，格式 yyyy-MM-dd；在职时为 null）
     */
    private String leaveDate;
}
