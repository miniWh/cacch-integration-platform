package com.cacch.integration.service.moka.api;

import com.cacch.integration.entity.moka.MokaDepartmentDO;
import com.cacch.integration.entity.moka.MokaDepartmentLocalizedDO;

import java.util.List;

/**
 * Moka 部门持久化服务 —— 主表 + 多语言子表的 CRUD 与批量操作
 *
 * <p>主表主键为业务字段 department_code（非雪花 BIGINT），
 * upsert 语义通过 PostgreSQL 的 {@code INSERT ... ON CONFLICT (department_code) DO UPDATE} 实现。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaDepartmentService {

    // —— 主表 MokaDepartmentDO ——

    /**
     * 按部门编码查询
     *
     * @param departmentCode 部门编码，不可为空
     * @return 主表记录；不存在时返回 null
     */
    MokaDepartmentDO getByCode(String departmentCode);

    /**
     * 查询全量部门列表
     *
     * @return 所有部门；无记录时返回空列表
     */
    List<MokaDepartmentDO> listAll();

    /**
     * 按父部门编码查询直接子部门
     *
     * @param parentCode 父部门编码，一级部门传 "0"
     * @return 子部门列表；无记录时返回空列表
     */
    List<MokaDepartmentDO> listByParentCode(String parentCode);

    /**
     * 按部门类型筛选
     *
     * @param type 部门类型：1-普通部门，2-门店部门
     * @return 匹配类型的部门列表
     */
    List<MokaDepartmentDO> listByType(Integer type);

    /**
     * 单条 upsert（INSERT ... ON CONFLICT DO UPDATE）
     *
     * @param dept 部门实体，departmentCode 不可为空
     * @return 落库后的记录
     */
    MokaDepartmentDO upsert(MokaDepartmentDO dept);

    /**
     * 批量 upsert（事务内逐条执行，单条失败不阻断批次）
     *
     * @param depts 部门列表，不可为 null；空列表直接返回
     * @return 成功 upsert 条数
     */
    int batchUpsert(List<MokaDepartmentDO> depts);

    /**
     * 按部门编码删除主表记录（子表通过外键 ON DELETE CASCADE 自动清除）
     *
     * @param departmentCode 部门编码，不可为空
     * @return 删除条数
     */
    int deleteByCode(String departmentCode);

    /**
     * 清空所有部门数据（谨慎使用）
     *
     * @return 删除条数（主表行数）
     */
    int deleteAll();

    // —— 子表 MokaDepartmentLocalizedDO ——

    /**
     * 查询指定部门的所有多语言名称
     *
     * @param departmentCode 部门编码
     * @return 多语言条目列表；无记录时返回空列表
     */
    List<MokaDepartmentLocalizedDO> listLocalizedByDeptCode(String departmentCode);

    /**
     * 查询指定语言的所有部门名称映射
     *
     * @param locale 语言代码（如 zh_CN）
     * @return 该语言下的全部映射条目
     */
    List<MokaDepartmentLocalizedDO> listLocalizedByLocale(String locale);

    /**
     * 单条 upsert 多语言记录（INSERT ... ON CONFLICT (department_code, locale) DO UPDATE）
     *
     * @param localized 多语言实体，departmentCode + locale 为复合主键
     * @return 落库后的记录
     */
    MokaDepartmentLocalizedDO upsertLocalized(MokaDepartmentLocalizedDO localized);

    /**
     * 批量 upsert 多语言记录
     *
     * @param localizes 多语言列表
     * @return 成功 upsert 条数
     */
    int batchUpsertLocalized(List<MokaDepartmentLocalizedDO> localizes);

    /**
     * 删除指定部门的全部多语言条目
     *
     * @param departmentCode 部门编码
     * @return 删除条数
     */
    int deleteLocalizedByDeptCode(String departmentCode);
}
