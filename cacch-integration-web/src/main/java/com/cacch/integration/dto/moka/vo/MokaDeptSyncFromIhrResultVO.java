package com.cacch.integration.dto.moka.vo;

import com.cacch.integration.manager.moka.api.IMokaDepartmentSyncManager.MokaDeptSyncResult;
import lombok.Data;

/**
 * Moka 部门从 IHR 同步的执行结果 VO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaDeptSyncFromIhrResultVO {

    /**
     * 从 IHR 拉取的部门总数（含被跳过的）
     */
    private Integer totalFetched;

    /**
     * 主表 upsert 成功条数
     */
    private Integer deptUpserted;

    /**
     * 多语言子表 upsert 成功条数
     */
    private Integer localizedUpserted;

    /**
     * 因缺失 departmentCode 等校验失败而跳过的条数
     */
    private Integer deptSkipped;

    /**
     * 构造 VO
     *
     * @param result Manager 层同步执行结果
     * @return VO 实例
     */
    public static MokaDeptSyncFromIhrResultVO from(MokaDeptSyncResult result) {
        MokaDeptSyncFromIhrResultVO vo = new MokaDeptSyncFromIhrResultVO();
        vo.setTotalFetched(result.totalFetched());
        vo.setDeptUpserted(result.deptUpserted());
        vo.setLocalizedUpserted(result.localizedUpserted());
        vo.setDeptSkipped(result.deptSkipped());
        return vo;
    }
}
