package com.cacch.integration.dto.moka.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Moka 组织架构全量同步 —— web 层请求体
 *
 * <p>调用方通过此接口透传部门列表，Service/Client 层完成校验与转发。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaOrgSyncRequest {

    /**
     * 部门数据列表（必填，至少一个）
     */
    @NotNull(message = "部门列表不可为 null")
    @NotEmpty(message = "部门列表至少包含一条记录")
    @Valid
    private List<MokaDepartmentItem> departments;

    /**
     * 单条部门数据
     *
     * @author hongfu_zhou@cacch.com
     */
    @Data
    public static class MokaDepartmentItem {

        /**
         * 部门名称（必填）
         */
        @NotBlank(message = "部门名称不可为空")
        private String name;

        /**
         * 客户系统的部门 id —— Moka 侧主键（必填）
         */
        @NotBlank(message = "departmentCode 不可为空")
        private String departmentCode;

        /**
         * 上级部门唯一 id；一级部门固定传 "0"（必填）
         */
        @NotBlank(message = "parentCode 不可为空；一级部门传 \"0\"")
        private String parentCode;

        /**
         * 部门类型：1 普通部门 / 2 门店部门（可选；不传时 Moka 默认为 1）
         */
        @Min(value = 1, message = "type 取值 1 或 2")
        @Max(value = 2, message = "type 取值 1 或 2")
        private Integer type;

        /**
         * 部门排序，范围 0~10000；为空默认排在最后（可选）
         */
        @Min(value = 0, message = "sequence 范围 0~10000")
        @Max(value = 10000, message = "sequence 范围 0~10000")
        private Integer sequence;

        /**
         * 部门多语言名称（可选；locale 如 zh-CN、en-US；propValue 为对应语言名称）
         */
        private List<MokaLocalizedNameItem> localizedNames;
    }

    /**
     * 部门多语言名称条目
     *
     * @author hongfu_zhou@cacch.com
     */
    @Data
    public static class MokaLocalizedNameItem {

        /**
         * 语言标识，如 zh-CN、en-US
         */
        private String locale;

        /**
         * 该语言对应的部门名称
         */
        private String propValue;
    }
}
