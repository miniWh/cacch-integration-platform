package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

import java.util.List;

/**
 * Moka ATS —— 职位查询响应中的单条职位信息
 *
 * <p>字段与 Moka 官方文档全量对齐。注意：{@code jobManager} / {@code jobHrAssistant} /
 * {@code jobHiringManager} / {@code jobInterviewer} 默认不返回，
 * 需联系 Moka CSM 在后台配置功能模块开启官网职位接口负责人同步开关，未开启时为 null。</p>
 *
 * <p>口径差异说明：入参 hireMode 为 string（social/campus）、出参为 integer（1=社招，2=校招）；
 * 入参 commitment 为 number（1-4）、出参为 string（全职/兼职/实习/其它），均按文档原样映射。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobItem {

    /**
     * 职位 id
     */
    private String id;

    /**
     * 职位名称
     */
    private String title;

    /**
     * 职位状态：open=开启，closed=关闭，pause=暂停
     */
    private String status;

    /**
     * 职位描述
     */
    private String description;

    /**
     * 职位编号
     */
    private String mjCode;

    /**
     * 最低薪资
     */
    private Integer minSalary;

    /**
     * 最高薪资
     */
    private Integer maxSalary;

    /**
     * 薪资单位
     */
    private Integer salaryUnit;

    /**
     * 发薪月数
     */
    private Integer payPeriod;

    /**
     * 发薪方式
     */
    private Integer paymentMethod;

    /**
     * 最低工作经验
     */
    private Integer minExperience;

    /**
     * 最高工作经验
     */
    private Integer maxExperience;

    /**
     * 招聘人数
     */
    private Integer number;

    /**
     * 职位性质：全职 / 兼职 / 实习 / 其它
     */
    private String commitment;

    /**
     * 学历要求
     */
    private String education;

    /**
     * 职位关闭时间（ISO8601）
     */
    private String closedAt;

    /**
     * 开始招聘时间（ISO8601）
     */
    private String openedAt;

    /**
     * 职位更新时间（ISO8601）
     */
    private String updatedAt;

    /**
     * 是否优先招聘：1 为优先招聘
     */
    private Integer prior;

    /**
     * 招聘模式：1=社招，2=校招
     */
    private Integer hireMode;

    /**
     * 官网发布时间（职位在招聘官网或内推官网的最新发布时间）
     */
    private String publishedAt;

    /**
     * 职位目标完成时间
     */
    private String finishedAt;

    /**
     * 是否为内推职位
     */
    private Boolean isRecommendation;

    /**
     * 职位所在部门信息
     */
    private MokaJobDepartment department;

    /**
     * 职位所属流程 ID
     */
    private String pipelineId;

    /**
     * 职位所属流程名称
     */
    private String pipelineName;

    /**
     * 职位地点列表；无数据时可能为 null 或空列表
     */
    private List<MokaJobLocation> locations;

    /**
     * 职位所属门店列表；无数据时可能为 null 或空列表
     */
    private List<MokaJobStore> stores;

    /**
     * 职位职能信息
     */
    private MokaJobZhineng zhineng;

    /**
     * 职位自定义字段列表；无数据时可能为 null 或空列表
     */
    private List<MokaJobCustomField> customFields;

    /**
     * 职位负责人（默认不返回，需 Moka CSM 开启同步开关）
     */
    private MokaJobUser jobManager;

    /**
     * 职位协助人列表（默认不返回，需 Moka CSM 开启同步开关）
     */
    private List<MokaJobUser> jobHrAssistant;

    /**
     * 用人经理列表（默认不返回，需 Moka CSM 开启同步开关）
     */
    private List<MokaJobUser> jobHiringManager;

    /**
     * 面试官列表（默认不返回，需 Moka CSM 开启同步开关）
     */
    private List<MokaJobUser> jobInterviewer;

    /**
     * 自定义招聘查询 id
     */
    private Long jobTemplateId;

    /**
     * 是否虚拟职位：true 为虚拟职位
     */
    private Boolean isVirtual;

    /**
     * 申请表列表；无数据时可能为 null 或空列表
     */
    private List<MokaJobApplyForm> applyForms;

    /**
     * 面试评价表列表；无数据时可能为 null 或空列表
     */
    private List<MokaJobRoundFeedback> roundFeedbacks;

    /**
     * 职级信息列表；无数据时可能为 null 或空列表
     */
    private List<MokaJobRankInfo> jobRankInfo;

    /**
     * 职位创建日期
     */
    private String createAt;

    /**
     * 是否保密职位：1=是，0=否
     */
    private Integer confidential;

    /**
     * 职位发布到的官网（根据官网类型区分）
     */
    private MokaJobSitesByCategory sitesByCategory;
}
