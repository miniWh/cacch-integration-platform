package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

import java.util.List;

/**
 * Moka ATS —— 根据条件查询申请信息（分页）响应
 *
 * <p>外层响应：{ code, msg, data }。
 * 注意成功码为 **200**（与部门接口的 code=0 不同）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaApplicationListResponse {

    /**
     * 返回码：200 成功，其他错误
     */
    private Integer code;

    /**
     * 成功返回 "success"，失败返回对应错误信息
     */
    private String msg;

    /**
     * 数据对象
     */
    private ApplicationListData data;

    /**
     * 判断请求是否成功（code=200）
     *
     * @return code == 200
     */
    public boolean isSuccess() {
        return code != null && code == 200;
    }

    /**
     * data 嵌套对象
     */
    @Data
    public static class ApplicationListData {

        /**
         * 下次请求带上此值自动分页；无更多数据时不返回
         */
        private String next;

        /**
         * 申请信息列表
         */
        private List<ApplicationItem> applicationList;
    }

    /**
     * 单条申请信息
     */
    @Data
    public static class ApplicationItem {

        /**
         * 申请 id
         */
        private Integer applicationId;

        /**
         * 候选人基础信息
         */
        private CandidateBaseInfo candidateBaseInfo;

        /**
         * 职位信息
         */
        private Job job;

        /**
         * 流程信息
         */
        private Pipeline pipeline;

        /**
         * 阶段信息
         */
        private Stage stage;

        /**
         * 是否归档
         */
        private Boolean archived;

        /**
         * 归档原因信息
         */
        private ArchiveReasons archiveReasons;

        /**
         * 招聘模式：1=社招，2=校招
         */
        private Integer hireMode;

        /**
         * 申请时间（时间戳毫秒）
         */
        private Long appliedAt;

        /**
         * 创建阶段时间（时间戳毫秒）
         */
        private Long createdAt;

        /**
         * 更新时间（时间戳毫秒）
         */
        private Long updatedAt;

        /**
         * 移动阶段时间（时间戳毫秒）
         */
        private Long movedAt;

        /**
         * 申请所有者
         */
        private OwnerInfo ownerInfo;

        /**
         * 校园大使信息
         */
        private Ambassadors ambassadors;
    }

    /**
     * 候选人基础信息
     */
    @Data
    public static class CandidateBaseInfo {

        /**
         * 候选人 id
         */
        private Integer candidateId;

        /**
         * 姓名
         */
        private String name;

        /**
         * 性别：1=男，2=女
         */
        private Integer gender;

        /**
         * 年龄
         */
        private Integer age;

        /**
         * 出生日期（时间戳毫秒）
         */
        private Long birthDate;

        /**
         * 国籍
         */
        private String nationality;

        /**
         * 工作经验
         */
        private Integer experience;

        /**
         * 创建时间（时间戳毫秒）
         */
        private Long createdAt;

        /**
         * 是否入库（有任一申请归档过）
         */
        private Boolean archived;

        /**
         * 归档日期（时间戳毫秒）
         */
        private Long archivedAt;

        /**
         * 最后激活时间（时间戳毫秒）
         */
        private Integer activatedAt;

        /**
         * 是否在黑名单人才库
         */
        private Boolean isBlacklist;

        /**
         * 联系方式
         */
        private ContactInfo contactInfo;

        /**
         * 毕业院校信息
         */
        private GraduateSchool graduateSchool;

        /**
         * 身份证信息
         */
        private Citizen citizen;
    }

    /**
     * 联系方式
     */
    @Data
    public static class ContactInfo {
        /**
         * 邮箱
         */
        private String email;
        /**
         * 手机号
         */
        private String phone;
        /**
         * 国家呼叫代码
         */
        private String countryCallingCode;
    }

    /**
     * 毕业院校信息
     */
    @Data
    public static class GraduateSchool {
        /**
         * 毕业专业
         */
        private String lastSpeciality;
        /**
         * 毕业学校
         */
        private String lastSchool;
        /**
         * 毕业时间（年份）
         */
        private Integer graduateDate;
        /**
         * 学历/学位：1=其他，2=初中及以下，3=高中，4=中专，5=大专，6=本科，7=硕士，8=MBA，9=博士
         */
        private Integer academicDegree;
    }

    /**
     * 身份证信息
     */
    @Data
    public static class Citizen {
        /**
         * 证件号码
         */
        private String citizenId;
        /**
         * 证件类型：1=身份证，2=香港身份证，3=澳门身份证，4=台湾身份证，5=护照，6=其他证件
         */
        private Integer certificateType;
        /**
         * 签发机关
         */
        private String idCardSignOrganization;
        /**
         * 证件有效期，格式 ["2025-03-05","2025-03-21"]
         */
        private String idCardValidity;
    }

    /**
     * 职位信息
     */
    @Data
    public static class Job {
        /**
         * 职位 id
         */
        private String jobId;
        /**
         * 职位名称
         */
        private String jobTitle;
    }

    /**
     * 流程信息
     */
    @Data
    public static class Pipeline {
        /**
         * 流程 id
         */
        private Integer pipelineId;
        /**
         * 流程名称
         */
        private String name;
    }

    /**
     * 阶段信息
     */
    @Data
    public static class Stage {
        /**
         * 阶段 id
         */
        private Integer stageId;
        /**
         * 阶段名称
         */
        private String name;
        /**
         * 阶段类型：100=初筛型，101=offer型，102=待入职，200=筛选型，201=面试型，202=测试型，205=无类型，206=试工类型
         */
        private Integer stageType;
    }

    /**
     * 归档原因信息
     */
    @Data
    public static class ArchiveReasons {
        /**
         * 归档原因 Id
         */
        private Integer id;
        /**
         * 归档类型：1=被候选人拒绝，2=被我们拒绝，3=录用，4=系统原因，5=离职
         */
        private Integer type;
        /**
         * 归档原因类型枚举：REJECTED_BY_CANDIDATES / REJECTED_BY_US / HIRED / SYSTEM / CHECKOUT
         */
        private String archiveReasonType;
        /**
         * 归档原因名称
         */
        private String name;
        /**
         * 归档时间（时间戳毫秒）
         */
        private Long archivedAt;
    }

    /**
     * 申请所有者
     */
    @Data
    public static class OwnerInfo {
        /**
         * 申请所有者 Id
         */
        private Integer ownerId;
        /**
         * 申请所有者名称
         */
        private String ownerName;
    }

    /**
     * 校园大使信息
     */
    @Data
    public static class Ambassadors {
        /**
         * 校园大使姓名
         */
        private String name;
        /**
         * 校园大使手机号
         */
        private String phone;
        /**
         * 校园大使邮箱
         */
        private String email;
    }
}
