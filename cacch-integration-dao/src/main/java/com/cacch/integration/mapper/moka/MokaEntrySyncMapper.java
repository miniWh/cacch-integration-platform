package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaEntrySyncDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

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
     * 推送完成后更新同步结果（状态 + iHR entryId + 消息 + 时间）
     *
     * @param applicationId Moka 申请 ID（业务唯一键）
     * @param status        新状态值（SUCCESS / FAILED / SKIPPED）
     * @param ihrEntryId    iHR 返回的 entryId（成功时填，失败/跳过为 null）
     * @param message       推送结果描述或失败原因
     * @param syncTime      同步完成时间
     * @return 受影响行数
     */
    @Update("UPDATE t_integration_moka_entry_sync " +
            "SET ihr_sync_status = #{status}, " +
            "    ihr_entry_id = #{ihrEntryId}, " +
            "    ihr_sync_message = #{message}, " +
            "    ihr_sync_time = #{syncTime}, " +
            "    updated_at = NOW() " +
            "WHERE application_id = #{applicationId} AND is_deleted = 0")
    int updateResult(@Param("applicationId") Integer applicationId,
                     @Param("status") String status,
                     @Param("ihrEntryId") String ihrEntryId,
                     @Param("message") String message,
                     @Param("syncTime") LocalDateTime syncTime);

    /**
     * 按状态查询同步记录（用于推送前筛选待同步/失败/跳过的记录）
     *
     * @param statusList 状态列表（PENDING / FAILED / SKIPPED 等）
     * @return 匹配的同步记录列表
     */
    @Select("<script>" +
            "SELECT * FROM t_integration_moka_entry_sync " +
            "WHERE is_deleted = 0 " +
            "AND ihr_sync_status IN " +
            "<foreach collection='statusList' item='s' open='(' separator=',' close=')'>" +
            "#{s}" +
            "</foreach>" +
            "</script>")
    List<MokaEntrySyncDO> listByStatus(@Param("statusList") List<String> statusList);
}
