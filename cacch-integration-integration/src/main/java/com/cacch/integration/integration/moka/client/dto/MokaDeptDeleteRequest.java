package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Moka 删除及合并组织架构 —— 请求体
 *
 * <p>字段严格匹配 Moka API {@code DELETE /api-platform/v1/departments} 的请求结构。
 * 本项目仅标记删除（不传 {@code target_department_code}，不把人员合并到其他部门）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaDeptDeleteRequest {

    /**
     * 待删除部门列表（必填，至少一条）
     */
    private List<MokaDeptDeleteItem> departments;

    /**
     * 操作人邮箱（必填，须为 Moka 注册邮箱）
     */
    private String email;

    /**
     * 待删除部门条目
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MokaDeptDeleteItem {

        /**
         * 待删除部门编码（必填，对应本地表 department_code）
         */
        @JsonProperty("department_code")
        private String departmentCode;

        /**
         * 删除后部门人员合并入的目标部门编码（可选；本项目仅标记删除，不传该字段）
         */
        @JsonProperty("target_department_code")
        private String targetDepartmentCode;
    }
}
