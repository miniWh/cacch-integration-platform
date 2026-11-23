package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaEntryEducationDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Moka 待入职 —— 教育经历 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaEntryEducationMapper extends BaseMapper<MokaEntryEducationDO> {

    /**
     * 按申请 id 物理删除教育经历（Step 1d 全量替换前调用）
     *
     * @param applicationId Moka 申请 id
     * @return 删除行数
     */
    @Delete("DELETE FROM t_integration_moka_entry_education WHERE application_id = #{applicationId}")
    int deleteByApplicationId(@Param("applicationId") Integer applicationId);

    @Insert("INSERT INTO t_integration_moka_entry_education " +
            "(id, application_id, moka_education_id, education_index, school, speciality, academic_degree, " +
            "start_date, end_date, is_full_time) " +
            "VALUES (#{id}, #{applicationId}, #{mokaEducationId}, #{educationIndex}, #{school}, #{speciality}, " +
            "#{academicDegree}, #{startDate}, #{endDate}, #{isFullTime})")
    int insertRow(@Param("id") Long id,
                  @Param("applicationId") Integer applicationId,
                  @Param("mokaEducationId") String mokaEducationId,
                  @Param("educationIndex") Integer educationIndex,
                  @Param("school") String school,
                  @Param("speciality") String speciality,
                  @Param("academicDegree") String academicDegree,
                  @Param("startDate") String startDate,
                  @Param("endDate") String endDate,
                  @Param("isFullTime") String isFullTime);
}