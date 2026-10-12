package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaEntryCitizenDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Moka 待入职候选人 —— 证件信息 Mapper（Citizen）
 *
 * <p>幂等写入通过 {@code INSERT ON CONFLICT (application_id) DO UPDATE} 实现。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaEntryCitizenMapper extends BaseMapper<MokaEntryCitizenDO> {

    @Update("INSERT INTO t_integration_moka_entry_citizen " +
            "(id, application_id, citizen_id, certificate_type, id_card_sign_organization, id_card_validity) " +
            "VALUES (#{id}, #{applicationId}, #{citizenId}, #{certificateType}, #{idCardSignOrganization}, #{idCardValidity}) " +
            "ON CONFLICT (application_id) DO UPDATE SET " +
            "citizen_id = EXCLUDED.citizen_id, " +
            "certificate_type = EXCLUDED.certificate_type, " +
            "id_card_sign_organization = EXCLUDED.id_card_sign_organization, " +
            "id_card_validity = EXCLUDED.id_card_validity, " +
            "updated_at = NOW()")
    int upsert(@Param("id") Long id,
               @Param("applicationId") Integer applicationId,
               @Param("citizenId") String citizenId,
               @Param("certificateType") Integer certificateType,
               @Param("idCardSignOrganization") String idCardSignOrganization,
               @Param("idCardValidity") String idCardValidity);
}
