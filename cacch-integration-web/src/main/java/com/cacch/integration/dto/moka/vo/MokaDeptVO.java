package com.cacch.integration.dto.moka.vo;

import lombok.Data;

import java.util.List;

/**
 * Moka 组织架构 —— 部门视图对象（获取全量组织架构响应项）
 *
 * <p>由 integration 层 {@link com.cacch.integration.integration.moka.client.dto.MokaDeptItem}
 * 经 {@link com.cacch.integration.convert.moka.MokaOrgConverter#toDeptVO} 转换而来。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaDeptVO {

    /**
     * Moka 系统内部门自增 ID
     */
    private Long id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 客户系统的部门 id（主键）
     */
    private String departmentCode;

    /**
     * 上级部门的 departmentCode；一级部门为 "0"
     */
    private String parentCode;

    /**
     * Moka 系统内上级部门自增 ID；一级部门为 0
     */
    private Long parentId;

    /**
     * 部门类型：1 普通部门 / 2 门店部门
     */
    private Integer type;

    /**
     * 部门排序，0~10000
     */
    private Integer sequence;

    /**
     * 部门状态：1 正常 / 0 停用
     */
    private Integer status;

    /**
     * 逻辑删除标记：0 正常 / 1 已删除
     */
    private Integer isDeleted;

    /**
     * 创建时间，格式 yyyy-MM-dd HH:mm:ss
     */
    private String createdTime;

    /**
     * 最后修改时间，格式 yyyy-MM-dd HH:mm:ss
     */
    private String lastModifiedTime;

    /**
     * 部门名称的多语言信息
     */
    private List<MokaLocalizedVO> localizedNames;

    /**
     * 部门多语言名称条目 VO
     *
     * @author hongfu_zhou@cacch.com
     */
    @Data
    public static class MokaLocalizedVO {

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
