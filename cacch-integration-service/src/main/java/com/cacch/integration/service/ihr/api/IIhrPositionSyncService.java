package com.cacch.integration.service.ihr.api;

import com.cacch.integration.integration.ihr.client.dto.IhrPosition;

import java.util.List;

/**
 * IHR 岗位主数据（MDM）同步链路拉取 Service 接口 —— 岗位域新链路
 *
 * <p>封装岗位 MDM 链路的两个 iHR 接口（全量职位清单 / 按职位 ID 批量查询），
 * 含 token 获取、鉴权失败强刷重试与业务码校验。
 * 旧链路 {@link IIhrPositionService} 保持不动，新旧并行。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IIhrPositionSyncService {

    /**
     * 全量拉取 iHR 职位清单（获取公司职位清单，无分页一次返回全部）
     *
     * <p>校验业务 code（非 0 抛 BizException），鉴权失败自动强刷 token 重试一次。
     * 首页即空时返回空列表。</p>
     *
     * @return iHR 全量职位列表（含 id/positionName/positionCode/departmentId 等）；无数据时返回空列表（非 null）
     * @throws com.cacch.integration.common.exception.BizException HTTP 调用失败（含重试后）或业务 code 非 0 时抛出（整轮终止）
     */
    List<IhrPosition> fetchAllPositions();

    /**
     * 根据职位 ID 集合批量获取职位清单（自动分批，每批 100）
     *
     * <p>内部按 100 个 ID 一批调用 iHR「根据职位 id 获取职位清单」接口
     * （POST positionIds，Body 为职位 uuid 数组），汇总全部批次结果返回；
     * 每批校验业务 code（非 0 抛 BizException），鉴权失败自动强刷 token 重试。
     * 供员工变更职位保障阶段回源新旧岗位。</p>
     *
     * @param positionIds 职位 ID 集合（iHR 职位 uuid）；null 或空时返回空列表
     * @return 职位列表（iHR 侧不存在的 ID 无对应元素）；无数据时返回空列表（非 null）
     * @throws com.cacch.integration.common.exception.BizException 任一批次 HTTP 调用失败（含重试后）或业务 code 非 0 时抛出
     */
    List<IhrPosition> listPositionsByIds(List<String> positionIds);
}
