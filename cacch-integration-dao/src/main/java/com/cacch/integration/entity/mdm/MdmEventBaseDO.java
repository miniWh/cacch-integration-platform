package com.cacch.integration.entity.mdm;

import com.cacch.integration.entity.base.BaseDO;
import lombok.Getter;
import lombok.Setter;

/**
 * 主数据变更事件 DO 基类 —— mdm_*_event 表共享字段
 *
 * <p>事件层公共结构：幂等键、事件类型、变更级别、明细 JSON、批次号与处理状态；
 * 各子域事件 DO 继承本类并补充关联主档 ID 与业务编码快照字段。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@Setter
public abstract class MdmEventBaseDO extends BaseDO {

    /**
     * 事件幂等键 SHA-256(主档标识:eventType:rowHash)（UNIQUE），
     * 重复产生时 DO NOTHING 吞掉
     */
    private String eventId;

    /**
     * 事件类型（人员：HIRE/LEAVE/REHIRE/TRANSFER/UPDATE；部门与岗位：CREATE/UPDATE/DISABLE），
     * 取值见 common.constant.mdm 事件常量
     */
    private String eventType;

    /**
     * 变更级别：HIGH-入离职/新建/停用 / NORMAL-调岗与属性更新
     */
    private String changeLevel;

    /**
     * 事件明细 JSON（业务键、编码、名称、批次号等；JSONB）
     */
    private String payload;

    /**
     * 产生本事件的 ETL 批次号
     */
    private String etlBatchNo;

    /**
     * 处理状态：PENDING-待消费
     */
    private String status;
}
