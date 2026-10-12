package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

import java.util.List;

/**
 * Moka offer 扩展信息 —— 自定义字段元素
 *
 * <p>对应 Moka 响应中 {@code data.customFields[]} 的元素结构，含字段值、
 * 字段元信息（id/名称/类型/必填/可见性/系统字段）、属性规则（数字范围、小数位等）、
 * 分组信息、多语言、选项编码等。</p>
 *
 * <p>字段类型 {@code type} 取值：
 * 1=单行文本，2=多行文本，...，11=附件（完整枚举见 Moka 文档）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaOfferCustomField {

    /**
     * 字段值
     */
    private String value;

    /**
     * 字段 ID
     */
    private Long id;

    /**
     * 字段分组 ID
     */
    private Long groupId;

    /**
     * 字段分组中文名称
     */
    private String groupNameZh;

    /**
     * 字段分组英文名称
     */
    private String groupNameEn;

    /**
     * 字段名称
     */
    private String name;

    /**
     * 字段类型：1=单行文本，2=多行文本，...，11=附件
     */
    private Integer type;

    /**
     * 字段选项内容（下拉/选择类字段使用）
     */
    private List<String> detail;

    /**
     * 是否必填
     */
    private Boolean isRequired;

    /**
     * 是否显示
     */
    private Boolean isVisible;

    /**
     * 是否系统字段
     */
    private Boolean isBuiltin;

    /**
     * 系统字段类型
     */
    private String builtinType;

    /**
     * 是否必审字段
     */
    private Boolean reapprovalRequired;

    /**
     * 招聘模式
     */
    private Integer hireMode;

    /**
     * 多语言信息
     */
    private Object supplementaryLocales;

    /**
     * 是否私密字段
     */
    private Boolean isSensitive;

    /**
     * 选项对应的编码列表
     */
    private List<String> codes;

    /**
     * 当前选择的编码
     */
    private String code;

    /**
     * 属性规则（数字型字段的校验规则）
     */
    private AttributeRule attributeRule;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 单位
     */
    private String unit;

    /**
     * 是否为不展示的联动字段
     */
    private Boolean isHideLinkedField;

    /**
     * 描述
     */
    private String desc;

    /**
     * 属性规则 —— 数字型字段的展示与校验规则
     *
     * <p>仅当字段类型为数字相关类型时返回；其他类型该字段为 null。</p>
     *
     * @author hongfu_zhou@cacch.com
     */
    @Data
    public static class AttributeRule {

        /**
         * 展示格式：0=默认，1=百分数，2=千分位
         */
        private Integer layout;

        /**
         * 小数位数
         */
        private Integer decimalLength;

        /**
         * 是否支持负数
         */
        private Boolean isSupportNegative;

        /**
         * 数字范围
         */
        private NumberRange numberRange;
    }

    /**
     * 数字范围 —— 数字型字段的取值约束
     *
     * @author hongfu_zhou@cacch.com
     */
    @Data
    public static class NumberRange {

        /**
         * 数字最大值
         */
        private Double max;

        /**
         * 数字最小值
         */
        private Double min;
    }
}
