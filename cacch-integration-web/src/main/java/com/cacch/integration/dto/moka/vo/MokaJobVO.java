package com.cacch.integration.dto.moka.vo;

import lombok.Data;

import java.util.List;

/**
 * Moka 职位视图对象 —— 单条职位明细（全量透传）
 *
 * <p>与 integration 层 {@code MokaJobItem} 字段一一对应；出参全量透传给调用方，
 * Moka 未返回的字段（如 jobManager 等需 CSM 开启同步开关的字段）自然为 null。</p>
 *
 * <p>口径差异说明：入参 hireMode 为 string（social/campus）、出参为 integer（1=社招，2=校招）；
 * 入参 commitment 为 number（1-4）、出参为 string（全职/兼职/实习/其它）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobVO {

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
    private Department department;

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
    private List<Location> locations;

    /**
     * 职位所属门店列表；无数据时可能为 null 或空列表
     */
    private List<Store> stores;

    /**
     * 职位职能信息
     */
    private Zhineng zhineng;

    /**
     * 职位自定义字段列表；无数据时可能为 null 或空列表
     */
    private List<CustomField> customFields;

    /**
     * 职位负责人（默认不返回，需 Moka CSM 开启同步开关）
     */
    private User jobManager;

    /**
     * 职位协助人列表（默认不返回，需 Moka CSM 开启同步开关）
     */
    private List<User> jobHrAssistant;

    /**
     * 用人经理列表（默认不返回，需 Moka CSM 开启同步开关）
     */
    private List<User> jobHiringManager;

    /**
     * 面试官列表（默认不返回，需 Moka CSM 开启同步开关）
     */
    private List<User> jobInterviewer;

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
    private List<ApplyForm> applyForms;

    /**
     * 面试评价表列表；无数据时可能为 null 或空列表
     */
    private List<RoundFeedback> roundFeedbacks;

    /**
     * 职级信息列表；无数据时可能为 null 或空列表
     */
    private List<RankInfo> jobRankInfo;

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
    private SitesByCategory sitesByCategory;

    /**
     * 职位所在部门信息
     */
    @Data
    public static class Department {

        /**
         * 部门 id
         */
        private Integer id;

        /**
         * 部门编码
         */
        private Integer code;

        /**
         * 部门名称
         */
        private String name;
    }

    /**
     * 职位地点信息
     */
    @Data
    public static class Location {

        /**
         * 地址 id
         */
        private Integer id;

        /**
         * 省份
         */
        private String province;

        /**
         * 城市
         */
        private String city;

        /**
         * 地区
         */
        private String area;

        /**
         * 地点所在国家
         */
        private String country;

        /**
         * 地点详细信息
         */
        private String address;
    }

    /**
     * 职位所属门店信息
     */
    @Data
    public static class Store {

        /**
         * 门店 id
         */
        private Integer id;

        /**
         * 门店编码
         */
        private String apiCode;

        /**
         * 门店名称
         */
        private String name;
    }

    /**
     * 职位职能信息
     */
    @Data
    public static class Zhineng {

        /**
         * 职能 id
         */
        private Integer id;

        /**
         * 职能名称
         */
        private String name;
    }

    /**
     * 职位自定义字段
     */
    @Data
    public static class CustomField {

        /**
         * 自定义字段 id
         */
        private Integer id;

        /**
         * 自定义字段名称
         */
        private String name;

        /**
         * 自定义字段类型：string_info / bool_info
         */
        private String type;

        /**
         * 自定义字段的值
         */
        private String value;
    }

    /**
     * 职位相关人员信息（负责人 / 协助人 / 用人经理 / 面试官通用）
     */
    @Data
    public static class User {

        /**
         * 姓名
         */
        private String name;

        /**
         * 邮箱
         */
        private String email;

        /**
         * 手机号
         */
        private String phone;

        /**
         * number 字段
         */
        private String number;
    }

    /**
     * 申请表信息
     */
    @Data
    public static class ApplyForm {

        /**
         * 申请表 id
         */
        private Long applyFormId;

        /**
         * 申请表类型：NORMAL_PORTAL=普通官网，RECOMMENDATION_PORTAL=内推官网
         */
        private String type;

        /**
         * 申请表适用浏览器类型：pc 或 mobile
         */
        private String device;
    }

    /**
     * 面试评价表信息
     */
    @Data
    public static class RoundFeedback {

        /**
         * 面试轮次 id
         */
        private Long roundId;

        /**
         * 面试评价表 id
         */
        private String feedbackId;

        /**
         * 面试轮次关联的用户组 id 列表；无关联时可能为 null 或空列表
         */
        private List<Long> userGroupsIds;
    }

    /**
     * 职级信息
     */
    @Data
    public static class RankInfo {

        /**
         * 职级 id
         */
        private Integer id;

        /**
         * 职级名称
         */
        private String name;

        /**
         * 职级别
         */
        private String type;

        /**
         * 职级顺序
         */
        private Integer level;

        /**
         * 职级备注
         */
        private String comment;
    }

    /**
     * 职位发布到的官网分布（根据官网类型区分）
     */
    @Data
    public static class SitesByCategory {

        /**
         * 职位关联的社招官网 id 列表
         */
        private List<Long> social;

        /**
         * 职位关联的校招官网 id 列表
         */
        private List<Long> camp;

        /**
         * 职位关联的全局内推官网 id 列表
         */
        private List<Long> recommend;

        /**
         * 职位关联的校招内推官网 id 列表（Moka 原始字段名 recommend_campus）
         */
        private List<Long> recommendCampus;

        /**
         * 职位关联的社招内推官网 id 列表（Moka 原始字段名 recommend_social）
         */
        private List<Long> recommendSocial;
    }
}
