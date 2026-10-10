package com.cacch.integration.dto.moka.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Moka 职位查询请求 VO
 *
 * <p>对应 Moka 职位查询接口 {@code POST /api-platform/v1/jobs/getJobs} 的入参；
 * 仅 hireMode 必填，其余字段均可选；limit / offset 文档声明为 string 类型，按原样传递。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobListRequestVO {

    /**
     * 招聘模式（必填）：social=社招，campus=校招
     */
    @NotBlank(message = "hireMode 不能为空，可选值：social、campus")
    private String hireMode;

    /**
     * 分页用的每页条数，默认 30；文档类型为 string，按原样传递
     */
    private String limit;

    /**
     * 分页用的偏移量，所索引从 0 开始；文档类型为 string，按原样传递
     */
    private String offset;

    /**
     * 工作地点 id 列表（通过地址列表接口返回的地址 id，如 [1,2,3]）
     */
    private List<Long> locationIds;

    /**
     * 职位职能 id（通过职能列表接口返回）
     */
    private Long zhinengId;

    /**
     * 官网 id
     */
    private Long siteId;

    /**
     * 职位性质：1=全职，2=兼职，3=实习，4=其它
     */
    private Integer commitment;

    /**
     * 职位按更新时间筛选的起始时间，如 2020-06-01
     */
    private String updatedFromAt;

    /**
     * 职位按更新时间筛选的截止时间，如 2020-07-01
     */
    private String updatedEndAt;

    /**
     * 职位状态：open=开启，closed=关闭，pause=暂停
     */
    private String status;

    /**
     * 职位 id
     */
    private String jobId;

    /**
     * 职位所属部门编号列表
     */
    private List<String> departmentCodes;

    /**
     * 职位所属门店编号列表
     */
    private List<String> storeCodes;

    /**
     * 按职位自定义字段筛选的列表；无数据时可能为 null 或空列表
     */
    private List<CustomFieldFilter> customFields;

    /**
     * 自定义字段过滤项
     */
    @Data
    public static class CustomFieldFilter {

        /**
         * 自定义字段 id
         */
        private String id;

        /**
         * 自定义字段值
         */
        private String value;
    }
}
