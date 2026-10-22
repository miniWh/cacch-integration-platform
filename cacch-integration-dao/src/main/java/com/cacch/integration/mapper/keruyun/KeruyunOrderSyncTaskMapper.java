package com.cacch.integration.mapper.keruyun;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.keruyun.KeruyunOrderSyncTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 客如云订单同步任务控制表 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface KeruyunOrderSyncTaskMapper extends BaseMapper<KeruyunOrderSyncTaskDO> {

    /**
     * CAS 状态流转：仅当当前状态等于 expectedStatus 且版本号匹配时更新
     *
     * @param id              任务主键
     * @param expectedStatus  期望的当前状态（失败返回 0 表示被其他执行器抢先）
     * @param newStatus       目标状态
     * @param expectedVersion 期望的版本号
     * @return 受影响行数，1 表示成功，0 表示 CAS 失败
     */
    @Update("UPDATE stg_keruyun_order_sync_task SET status = #{newStatus}, version = version + 1, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND status = #{expectedStatus} AND version = #{expectedVersion} AND is_deleted = 0")
    int updateStatusWithCas(@Param("id") Long id,
                            @Param("expectedStatus") String expectedStatus,
                            @Param("newStatus") String newStatus,
                            @Param("expectedVersion") Integer expectedVersion);
}
