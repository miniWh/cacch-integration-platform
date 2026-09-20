package com.cacch.integration.service.moka.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.integration.entity.moka.MokaDepartmentDO;
import com.cacch.integration.entity.moka.MokaDepartmentLocalizedDO;
import com.cacch.integration.mapper.moka.MokaDepartmentLocalizedMapper;
import com.cacch.integration.mapper.moka.MokaDepartmentMapper;
import com.cacch.integration.service.moka.api.IMokaDepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Moka 部门持久化服务实现
 *
 * <p>主表与多语言子表共用同一 PG schema，
 * upsert 走 PostgreSQL {@code INSERT ... ON CONFLICT DO UPDATE} 原子语义。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MokaDepartmentServiceImpl implements IMokaDepartmentService {

    private final MokaDepartmentMapper deptMapper;
    private final MokaDepartmentLocalizedMapper localizedMapper;

    // —— 主表 ——

    @Override
    public MokaDepartmentDO getByCode(String departmentCode) {
        return deptMapper.selectById(departmentCode);
    }

    @Override
    public List<MokaDepartmentDO> listAll() {
        return deptMapper.selectList(new LambdaQueryWrapper<MokaDepartmentDO>()
                .orderByAsc(MokaDepartmentDO::getSequence)
                .orderByAsc(MokaDepartmentDO::getCreateTime));
    }

    @Override
    public List<MokaDepartmentDO> listByParentCode(String parentCode) {
        return deptMapper.selectByParentCode(parentCode);
    }

    @Override
    public List<MokaDepartmentDO> listByType(Integer type) {
        return deptMapper.selectByType(type);
    }

    @Override
    public List<MokaDepartmentDO> listByMokaSyncStatus(Integer mokaSyncStatus) {
        return deptMapper.selectByMokaSyncStatus(mokaSyncStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public MokaDepartmentDO upsert(MokaDepartmentDO dept) {
        int rows = deptMapper.upsert(
                dept.getDepartmentCode(),
                dept.getName(),
                dept.getParentCode(),
                dept.getType(),
                dept.getSequence(),
                dept.getMokaSyncStatus()
        );
        if (rows == 0) {
            log.warn("【MokaDept】upsert 主表影响 0 行, departmentCode={}", dept.getDepartmentCode());
        }
        return deptMapper.selectById(dept.getDepartmentCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 60)
    public int batchUpsert(List<MokaDepartmentDO> depts) {
        if (CollectionUtils.isEmpty(depts)) {
            log.info("【MokaDept】批量 upsert 空列表, 跳过");
            return 0;
        }
        int success = 0;
        int failed = 0;
        for (MokaDepartmentDO dept : depts) {
            try {
                deptMapper.upsert(
                        dept.getDepartmentCode(),
                        dept.getName(),
                        dept.getParentCode(),
                        dept.getType(),
                        dept.getSequence(),
                        dept.getMokaSyncStatus()
                );
                success++;
            } catch (Exception e) {
                failed++;
                log.error("【MokaDept】批量 upsert 单条失败, departmentCode={}, 已失败={}",
                        dept.getDepartmentCode(), failed, e);
            }
        }
        log.info("【MokaDept】批量 upsert 完成, 总计={}, 成功={}, 失败={}",
                depts.size(), success, failed);
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public int deleteByCode(String departmentCode) {
        // 子表通过外键 ON DELETE CASCADE 自动清除，主表直接删
        return deptMapper.deleteById(departmentCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public int updateMokaSyncStatus(String departmentCode, Integer mokaSyncStatus) {
        return deptMapper.updateMokaSyncStatus(departmentCode, mokaSyncStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public int deleteAll() {
        // 先删子表（无 cascade 时显式控制顺序），再删主表
        int localRows = localizedMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>());
        int deptRows = deptMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>());
        log.info("【MokaDept】deleteAll 完成, 主表={}行, 子表={}行", deptRows, localRows);
        return deptRows;
    }

    // —— 子表 ——

    @Override
    public List<MokaDepartmentLocalizedDO> listLocalizedByDeptCode(String departmentCode) {
        return localizedMapper.selectByDepartmentCode(departmentCode);
    }

    @Override
    public List<MokaDepartmentLocalizedDO> listLocalizedByLocale(String locale) {
        return localizedMapper.selectByLocale(locale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public MokaDepartmentLocalizedDO upsertLocalized(MokaDepartmentLocalizedDO localized) {
        int rows = localizedMapper.upsert(
                localized.getDepartmentCode(),
                localized.getLocale(),
                localized.getPropValue()
        );
        if (rows == 0) {
            log.warn("【MokaDeptLocalized】upsert 子表影响 0 行, departmentCode={}, locale={}",
                    localized.getDepartmentCode(), localized.getLocale());
        }
        // 复合主键无法 selectById，用条件查询
        List<MokaDepartmentLocalizedDO> list = localizedMapper.selectByDepartmentCode(localized.getDepartmentCode());
        return list.stream()
                .filter(l -> localized.getLocale().equals(l.getLocale()))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 60)
    public int batchUpsertLocalized(List<MokaDepartmentLocalizedDO> localizes) {
        if (CollectionUtils.isEmpty(localizes)) {
            log.info("【MokaDeptLocalized】批量 upsert 空列表, 跳过");
            return 0;
        }
        int success = 0;
        int failed = 0;
        for (MokaDepartmentLocalizedDO localized : localizes) {
            try {
                localizedMapper.upsert(
                        localized.getDepartmentCode(),
                        localized.getLocale(),
                        localized.getPropValue()
                );
                success++;
            } catch (Exception e) {
                failed++;
                log.error("【MokaDeptLocalized】批量 upsert 单条失败, departmentCode={}, locale={}, 已失败={}",
                        localized.getDepartmentCode(), localized.getLocale(), failed, e);
            }
        }
        log.info("【MokaDeptLocalized】批量 upsert 完成, 总计={}, 成功={}, 失败={}",
                localizes.size(), success, failed);
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 10)
    public int deleteLocalizedByDeptCode(String departmentCode) {
        return localizedMapper.deleteByDepartmentCode(departmentCode);
    }
}
