package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaEntryCandidateBaseDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Moka 待入职候选人 —— 基础信息 Mapper（CandidateBaseInfo + ContactInfo）
 *
 * <p>幂等写入通过 {@code INSERT ON CONFLICT (application_id) DO UPDATE} 实现。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaEntryCandidateBaseMapper extends BaseMapper<MokaEntryCandidateBaseDO> {

    @Update("INSERT INTO t_integration_moka_entry_candidate_base " +
            "(id, application_id, moka_name, moka_gender, moka_age, moka_birth_date, moka_nationality, " +
            "moka_experience, moka_candidate_created_at, moka_candidate_archived, moka_candidate_archived_at, " +
            "moka_is_blacklist, moka_contact_phone, moka_contact_email, moka_country_calling_code) " +
            "VALUES " +
            "(#{id}, #{applicationId}, #{mokaName}, #{mokaGender}, #{mokaAge}, #{mokaBirthDate}, #{mokaNationality}, " +
            "#{mokaExperience}, #{mokaCandidateCreatedAt}, #{mokaCandidateArchived}, #{mokaCandidateArchivedAt}, " +
            "#{mokaIsBlacklist}, #{mokaContactPhone}, #{mokaContactEmail}, #{mokaCountryCallingCode}) " +
            "ON CONFLICT (application_id) DO UPDATE SET " +
            "moka_name = EXCLUDED.moka_name, " +
            "moka_gender = EXCLUDED.moka_gender, " +
            "moka_age = EXCLUDED.moka_age, " +
            "moka_birth_date = EXCLUDED.moka_birth_date, " +
            "moka_nationality = EXCLUDED.moka_nationality, " +
            "moka_experience = EXCLUDED.moka_experience, " +
            "moka_candidate_created_at = EXCLUDED.moka_candidate_created_at, " +
            "moka_candidate_archived = EXCLUDED.moka_candidate_archived, " +
            "moka_candidate_archived_at = EXCLUDED.moka_candidate_archived_at, " +
            "moka_is_blacklist = EXCLUDED.moka_is_blacklist, " +
            "moka_contact_phone = EXCLUDED.moka_contact_phone, " +
            "moka_contact_email = EXCLUDED.moka_contact_email, " +
            "moka_country_calling_code = EXCLUDED.moka_country_calling_code, " +
            "updated_at = NOW()")
    int upsert(@Param("id") Long id,
               @Param("applicationId") Integer applicationId,
               @Param("mokaName") String mokaName,
               @Param("mokaGender") Integer mokaGender,
               @Param("mokaAge") Integer mokaAge,
               @Param("mokaBirthDate") java.time.LocalDateTime mokaBirthDate,
               @Param("mokaNationality") String mokaNationality,
               @Param("mokaExperience") Integer mokaExperience,
               @Param("mokaCandidateCreatedAt") java.time.LocalDateTime mokaCandidateCreatedAt,
               @Param("mokaCandidateArchived") Boolean mokaCandidateArchived,
               @Param("mokaCandidateArchivedAt") java.time.LocalDateTime mokaCandidateArchivedAt,
               @Param("mokaIsBlacklist") Boolean mokaIsBlacklist,
               @Param("mokaContactPhone") String mokaContactPhone,
               @Param("mokaContactEmail") String mokaContactEmail,
               @Param("mokaCountryCallingCode") String mokaCountryCallingCode);
}
