package com.cacch.integration.dto.moka.vo;

import com.cacch.integration.manager.moka.api.IMokaDepartmentPushManager;
import lombok.Data;

/**
 * Moka 部门手动删除 —— 执行结果 VO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaManualDeleteResultVO {

    /**
     * 请求传入的部门编码条数（去重后）
     */
    private Integer requestCount;

    /**
     * 本地表中不存在的编码条数
     */
    private Integer notFoundCount;

    /**
     * 状态已是 4（已删除）而跳过标记的条数
     */
    private Integer alreadyDeletedCount;

    /**
     * 本次新标记为 3（待删除）的条数
     */
    private Integer markedCount;

    /**
     * 删除编排命中的待删除（状态 3）部门总数（含此前差集标记的历史待删部门）
     */
    private Integer deletePendingCount;

    /**
     * 因存在未删除子部门（孤儿校验）而跳过删除的条数
     */
    private Integer deleteSkippedCount;

    /**
     * Moka DELETE API 整体是否成功；
     * 无待删除部门、未配置 moka.delete-email 或全部被孤儿校验跳过时为 null（未调用）
     */
    private Boolean deleteSuccess;

    /**
     * Moka 删除成功后本地置 4（已删除，记录 moka_delete_time）的条数
     */
    private Integer deletedCount;

    /**
     * 构造 VO
     *
     * @param result Manager 层手动删除执行结果
     * @return VO 实例
     */
    public static MokaManualDeleteResultVO from(IMokaDepartmentPushManager.MokaManualDeleteResult result) {
        MokaManualDeleteResultVO vo = new MokaManualDeleteResultVO();
        vo.setRequestCount(result.requestCount());
        vo.setNotFoundCount(result.notFoundCount());
        vo.setAlreadyDeletedCount(result.alreadyDeletedCount());
        vo.setMarkedCount(result.markedCount());
        vo.setDeletePendingCount(result.deletePendingCount());
        vo.setDeleteSkippedCount(result.deleteSkippedCount());
        vo.setDeleteSuccess(result.deleteSuccess());
        vo.setDeletedCount(result.deletedCount());
        return vo;
    }
}
