package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaEntryGraduateSchoolDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Moka 待入职候选人 —— 毕业院校 Mapper（GraduateSchool）
 *
 * <p>幂等写入通过 {@code INSERT ON CONFLICT (application_id) DO UPDATE} 实现。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaEntryGraduateSchoolMapper extends BaseMapper<MokaEntryGraduateSchoolDO> {

    @Update("INSERT INTO t_integration_moka_entry_graduate_school " +
            "(id, application_id, last_speciality, last_school, graduate_date, academic_degree, school_city_id) " +
            "VALUES (#{id}, #{applicationId}, #{lastSpeciality}, #{lastSchool}, #{graduateDate}, #{academicDegree}, #{schoolCityId}) " +
            "ON CONFLICT (application_id) DO UPDATE SET " +
            "last_speciality = EXCLUDED.last_speciality, " +
            "last_school = EXCLUDED.last_school, " +
            "graduate_date = EXCLUDED.graduate_date, " +
            "academic_degree = EXCLUDED.academic_degree, " +
            "school_city_id = EXCLUDED.school_city_id, " +
            "updated_at = NOW()")
    int upsert(@Param("id") Long id,
               @Param("applicationId") Integer applicationId,
               @Param("lastSpeciality") String lastSpeciality,
               @Param("lastSchool") String lastSchool,
               @Param("graduateDate") Long graduateDate,
               @Param("academicDegree") Integer academicDegree,
               @Param("schoolCityId") Integer schoolCityId);
}
