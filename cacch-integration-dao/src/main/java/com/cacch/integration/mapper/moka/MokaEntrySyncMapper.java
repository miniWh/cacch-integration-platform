package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaEntrySyncDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
}
