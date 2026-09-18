package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Moka 组织架构全量同步 —— 单条部门数据
 *
 * <p>字段严格匹配 Moka API {@code PUT /api-platform/v2/departments} 的 departments 数组元素。
 * 同步以 {@code departmentCode} 为主键与系统内部门对比：
 * 系统没有则新增；两边都有则更新（系统内已标记删除的会恢复为正常）；
 * 系统有但本次未传则标记为已删除（需手动在 Moka 后台合并删除）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaDepartment {

    /**
     * 部门名称（必填）
     */
    private String name;

    /**
     * 客户系统的部门 id —— Moka 侧主键（必填）
     */
    private String departmentCode;

    /**
     * 上级部门的唯一 id；一级部门固定传 "0"（必填）
     */
    private String parentCode;

    /**
     * 部门类型：1 普通部门（默认）/ 2 门店部门（可选）
     */
    private Integer type;

    /**
     * 部门排序，范围 0~10000 支持两位小数；为空默认排在最后，按排序从小到大、创建时间从先到后（可选）
     */
    private BigDecimal sequence;

    /**
     * 部门名称的多语言信息（可选；只能传入 Moka 已开通的语言；name 字段默认作为 zh-CN）
     */
    private List<MokaLocalizedName> localizedNames;

    /**
     * 部门多语言名称条目
     *
     * @author hongfu_zhou@cacch.com
     */
    @Data
    public static class MokaLocalizedName {

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
