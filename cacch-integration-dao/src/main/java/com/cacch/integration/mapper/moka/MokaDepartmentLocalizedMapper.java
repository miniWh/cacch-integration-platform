package com.cacch.integration.mapper.moka;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cacch.integration.entity.moka.MokaDepartmentLocalizedDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Moka 部门多语言名称子表 Mapper
 *
 * <p>复合主键 (department_code, locale)，BaseMapper 单主键方法不适用，
 * 全部通过自定义方法 + Wrapper 条件查询。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper
public interface MokaDepartmentLocalizedMapper extends BaseMapper<MokaDepartmentLocalizedDO> {

    /**
     * 查询单个部门的所有多语言名称
     *
     * @param departmentCode 部门编码
     * @return 该部门的多语言条目列表；无记录时返回空列表
     */
    @Select("SELECT department_code, locale, prop_value FROM t_integration_moka_department_localized WHERE department_code = #{departmentCode} ORDER BY locale")
    List<MokaDepartmentLocalizedDO> selectByDepartmentCode(@Param("departmentCode") String departmentCode);

    /**
     * 按语言代码查询整个部门多语言映射
     *
     * @param locale 语言代码（如 zh_CN）
     * @return 指定语言的所有部门名称映射
     */
    @Select("SELECT department_code, locale, prop_value FROM t_integration_moka_department_localized WHERE locale = #{locale}")
    List<MokaDepartmentLocalizedDO> selectByLocale(@Param("locale") String locale);

    /**
     * 删除指定部门的所有多语言条目（用于重新覆盖）
     *
     * @param departmentCode 部门编码
     * @return 受影响行数
     */
    @Update("DELETE FROM t_integration_moka_department_localized WHERE department_code = #{departmentCode}")
    int deleteByDepartmentCode(@Param("departmentCode") String departmentCode);

    /**
     * UPSERT：INSERT ON CONFLICT (department_code, locale) DO UPDATE
     *
     * <p>复合主键的 ON CONFLICT，PostgreSQL 12 兼容。</p>
     *
     * @param departmentCode 部门编码
     * @param locale         语言代码
     * @param propValue      语言对应的部门名称
     * @return 受影响行数
     */
    @Update("INSERT INTO t_integration_moka_department_localized (department_code, locale, prop_value) " +
            "VALUES (#{departmentCode}, #{locale}, #{propValue}) " +
            "ON CONFLICT (department_code, locale) DO UPDATE SET " +
            "prop_value = EXCLUDED.prop_value")
    int upsert(@Param("departmentCode") String departmentCode,
               @Param("locale") String locale,
               @Param("propValue") String propValue);
}
