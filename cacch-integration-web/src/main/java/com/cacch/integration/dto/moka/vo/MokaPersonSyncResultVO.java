package com.cacch.integration.dto.moka.vo;

import com.cacch.integration.manager.moka.api.IMokaPersonSyncManager;
import lombok.Getter;

/**
 * Moka 人员同步结果 VO —— 供 Web 层 API 返回
 *
 * <p>对应 Manager 层 {@link IMokaPersonSyncManager.MokaPersonSyncResult}
 * record，通过静态工厂 {@link #from} 转换，避免 Manager 层 record
 * 直接暴露到 Controller。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
public class MokaPersonSyncResultVO {

    /**
     * persondetail 表返回的员工总数
     */
    private final int totalFetched;

    /**
     * Moka 人员中间表成功 upsert 的员工数
     */
    private final int personUpserted;

    /**
     * 因 departmentId 在 ihr_department 找不到 department_code 被跳过的条数
     */
    private final int deptCodeSkipped;

    /**
     * 因 userId 为空等校验失败被跳过的条数
     */
    private final int invalidSkipped;

    public MokaPersonSyncResultVO(int totalFetched, int personUpserted,
                                  int deptCodeSkipped, int invalidSkipped) {
        this.totalFetched = totalFetched;
        this.personUpserted = personUpserted;
        this.deptCodeSkipped = deptCodeSkipped;
        this.invalidSkipped = invalidSkipped;
    }

    /**
     * 将 Manager 层返回的 record 转换为 Web 层 VO
     *
     * @param result Manager 层同步执行结果
     * @return 对应的 VO 实例
     */
    public static MokaPersonSyncResultVO from(IMokaPersonSyncManager.MokaPersonSyncResult result) {
        return new MokaPersonSyncResultVO(result.totalFetched(), result.personUpserted(),
                result.deptCodeSkipped(), result.invalidSkipped());
    }
}
