package com.cacch.integration.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位主数据变更事件 DO —— 映射 PG 表 {@code mdm_position_event}
 *
 * <p>事件层：落库过程中识别的新建/更新/停用事件，
 * {@code eventId} 为幂等键（UNIQUE），重复产生时 DO NOTHING 吞掉；
 * 事件公共结构继承自 {@link MdmEventBaseDO}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_position_event")
public class MdmPositionEventDO extends MdmEventBaseDO {

    /**
     * 关联 mdm_position.id
     */
    private Long positionId;

    /**
     * 事件发生时职位编号（快照）
     */
    private String positionCode;
}
