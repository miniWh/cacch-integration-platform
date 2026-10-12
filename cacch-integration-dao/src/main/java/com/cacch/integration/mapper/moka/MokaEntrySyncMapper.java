package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaEntrySyncDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * Moka 待入职 —— iHR 同步状态 Mapper
 *
 * <p>幂等写入通过 {@code INSERT ON CONFLICT (application_id) DO UPDATE} 实现。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaEntrySyncMapper extends BaseMapper<MokaEntrySyncDO> {

    /**
     * UPSERT：按 application_id 初始化状态（默认 PENDING）
     *
     * <p>DO UPDATE 时不动 ihr_sync_status（保持已有状态），
     * 只更新 updated_at。</p>
     */
    @Update("INSERT INTO t_integration_moka_entry_sync " +
            "(id, application_id, ihr_sync_status) " +
            "VALUES (#{id}, #{applicationId}, #{ihrSyncStatus}) " +
            "ON CONFLICT (application_id) DO UPDATE SET " +
            "updated_at = NOW()")
    int upsert(@Param("id") Long id,
               @Param("applicationId") Integer applicationId,
               @Param("ihrSyncStatus") String ihrSyncStatus);

    /**
     * 按 application_id 更新同步状态 + 结果
     *
     * @param applicationId Moka 申请 ID
     * @param status 同步状态
     * @param entryId iHR 返回的 entryId（成功时回填，允许 null）
     * @param message 同步结果/失败原因
     * @param syncTime 最近一次同步时间
     */
    @Update("UPDATE t_integration_moka_entry_sync " +
            "SET ihr_sync_status = #{status}, " +
            "    ihr_entry_id = #{entryId}, " +
            "    ihr_sync_message = #{message}, " +
            "    ihr_sync_time = #{syncTime}, " +
            "    updated_at = NOW() " +
            "WHERE application_id = #{applicationId} AND is_deleted = 0")
    int updateStatus(@Param("applicationId") Integer applicationId,
                     @Param("status") String status,
                     @Param("entryId") String entryId,
                     @Param("message") String message,
                     @Param("syncTime") LocalDateTime syncTime);
}
