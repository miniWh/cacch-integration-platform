package com.cacch.integration.entity.mdm;

import com.cacch.integration.entity.base.BaseDO;
import lombok.Getter;
import lombok.Setter;

/**
 * 主数据多源映射 DO 基类 —— mdm_*_src_ref 表共享字段
 *
 * <p>多源映射公共结构：来源系统编码、源实体类型与源实体 ID；
 * 主档本体不存来源标识，多源追溯与融合经本表进行，各子域继承本类并补充
 * 关联主档 ID 字段。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@Setter
public abstract class MdmSrcRefBaseDO extends BaseDO {

    /**
     * 来源系统编码：IHR / WECOM / OA ...
     */
    private String srcSysCode;

    /**
     * 源系统实体类型：STAFF / DEPT / POSITION / USER / ACCOUNT
     */
    private String srcEntityType;

    /**
     * 源系统实体 ID（源系统内唯一 handle，如 iHR staffId UUID / iHR 部门 uuid）
     */
    private String srcEntityId;
}
