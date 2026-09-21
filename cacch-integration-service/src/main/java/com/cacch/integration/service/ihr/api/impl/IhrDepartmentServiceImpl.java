package com.cacch.integration.service.ihr.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.entity.ihr.IhrDepartmentDO;
import com.cacch.integration.mapper.ihr.IhrDepartmentMapper;
import com.cacch.integration.service.ihr.api.IIhrDepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import org.springframework.util.CollectionUtils;

/**
 * IHR 部门快照 Service 实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IhrDepartmentServiceImpl implements IIhrDepartmentService {

    private static final String BIZ = "IHR 部门快照 Service";

    /**
     * 部门启用状态枚举值（对齐 iHR 接口返回）
     */
    private static final String DEPARTMENT_STATUS_ENABLE = "ENABLE";

    private final IhrDepartmentMapper mapper;

    /**
     * 批量 upsert — 单批次内开启事务，中途失败回滚全部
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchUpsert(List<IhrDepartmentDO> deptList, String syncBatch) {
        if (deptList == null || deptList.isEmpty()) {
            log.info("【{}】batchUpsert 跳过, deptList 为空", BIZ);
            return 0;
        }
        int upserted = 0;
        for (IhrDepartmentDO d : deptList) {
            // @TableId(ASSIGN_ID) 只对 BaseMapper.insert() 自动生效，
            // 手写 @Update 注解绕过了 IdentifierGenerator，需手动填雪花 ID
            if (d.getId() == null) {
                d.setId(IdWorker.getId());
            }
            try {
                upserted += mapper.upsert(d, syncBatch);
            } catch (Exception e) {
                log.info("【{}】upsert 终止, uuid={}, reason={}", BIZ, d.getUuid(), e.getMessage());
                log.error("【{}】upsert 失败, uuid={}, name={}", BIZ, d.getUuid(), d.getName(), e);
                throw new BizException(ResultCode.INTEGRATION_ERROR, "IHR 部门 upsert 失败: uuid=" + d.getUuid() + ", reason=" + e.getMessage(), e);
            }
        }
        return upserted;
    }

    @Override
    public List<IhrDepartmentDO> listEnabled() {
        LambdaQueryWrapper<IhrDepartmentDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IhrDepartmentDO::getDepartmentStatus, DEPARTMENT_STATUS_ENABLE)
                .orderByAsc(IhrDepartmentDO::getSequence)
                .orderByAsc(IhrDepartmentDO::getId);
        List<IhrDepartmentDO> list = mapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            log.info("【{}】listEnabled 返回空, 无 ENABLE 状态部门", BIZ);
            return Collections.emptyList();
        }
        log.info("【{}】listEnabled 命中, count={}", BIZ, list.size());
        return list;
    }

    @Override
    public List<IhrDepartmentDO> listByIhrDeptIds(List<String> ihrDeptIds) {
        if (CollectionUtils.isEmpty(ihrDeptIds)) {
            log.info("【{}】listByIhrDeptIds 跳过, ihrDeptIds 为空", BIZ);
            return Collections.emptyList();
        }
        List<IhrDepartmentDO> list = mapper.selectList(new LambdaQueryWrapper<IhrDepartmentDO>()
                .in(IhrDepartmentDO::getIhrDeptId, ihrDeptIds));
        if (list == null || list.isEmpty()) {
            log.info("【{}】listByIhrDeptIds 返回空, 传入 ihrDeptIds={}", BIZ, ihrDeptIds.size());
            return Collections.emptyList();
        }
        log.info("【{}】listByIhrDeptIds 命中, 传入={}, 返回={}", BIZ, ihrDeptIds.size(), list.size());
        return list;
    }
}
