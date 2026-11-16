package com.cacch.integration.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位主数据多源映射表 DO —— 映射 PG 表 {@code mdm_position_src_ref}
 *
 * <p>iHR 等来源系统的岗位标识（如 iHR 职位 uuid）与 mdm_position.id 的映射，
 * 主档本体不存来源标识，多源追溯与融合经本表进行；
 * 多源映射公共结构继承自 {@link MdmSrcRefBaseDO}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_position_src_ref")
public class MdmPositionSrcRefDO extends MdmSrcRefBaseDO {

    /**
     * 关联 mdm_position.id
     */
    private Long positionId;
}
