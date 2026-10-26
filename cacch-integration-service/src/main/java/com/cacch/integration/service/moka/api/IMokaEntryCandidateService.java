package com.cacch.integration.service.moka.api;

import com.cacch.integration.entity.moka.MokaEntryApplicationDO;

import java.util.List;

/**
 * Moka 待入职候选人服务接口
 *
 * <p>职责：Step 1a 分页拉取 + 4 表幂等 upsert；
 * Step 1b 职位详情补全 departmentCode；
 * Step 1c Offer 扩展补全 checkInDate + offerDepartmentCode。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IMokaEntryCandidateService {

    /**
     * Step 1a：分页拉取 Moka 待入职候选人并幂等入库
     *
     * <p>固定参数：pipelineIds=[163155,163156]、stageIds=[226137]、不传 archived。
     * 每次 limit=20，循环 {@code data.next} 翻页直到无更多数据。
     * 4 表 upsert：主表 + candidate_base + graduate_school + citizen + 状态表。
     * 补全字段（moka_department_code / check_in_date / offer_department_code）
     * 初始为 null，后续 Step 1b/1c 回写。</p>
     *
     * @param batch 同步批次号（格式 MOKA_ENTRY_yyyyMMddHHmmss）
     * @return 本批次写入的主表列表（供后续 Step 1b/1c 使用）
     */
    List<MokaEntryApplicationDO> syncAndUpsert(String batch);

    /**
     * Step 1b：按 jobId 去重查询职位详情，回写部门编码到主表
     *
     * <p>从主表中收集去重的 moka_job_id 列表，调用 MokaJobClient 批量查职位，
     * 取 department.code，回写到 moka_department_code 列。
     * jobId 为 null 或职位不存在的记录 department_code 保持 null。</p>
     *
     * @param applications 主表列表（包含 moka_job_id）
     * @return 成功回写的记录数
     */
    int enrichDepartmentCode(List<MokaEntryApplicationDO> applications);

    /**
     * 按 applicationId 批量查 Offer 扩展信息，回写入职日期、薪资与 Offer 部门
     *
     * <p>从主表收集所有 application_id，调用 MokaOfferClient 批量查 offer 扩展，
     * 取 checkInDate / salaryNumber / departmentCode / departmentName / approvalStatus，
     * 回写到主表对应列。Offer 不存在时保持 null。</p>
     *
     * @param applications 主表列表（包含 application_id）
     * @return 成功回写的记录数
     */
    int enrichOfferFields(List<MokaEntryApplicationDO> applications);

    /**
     * 历史薪资补数：回填 salary_number 为空的历史记录
     *
     * <p>补数范围：is_deleted=0 且 salary_number IS NULL 且
     * （check_in_date 或 offer_status 非空）—— 即曾查到过 Offer 扩展数据但未回填薪资的记录。
     * 逐批调用 Offer 扩展接口，复用 Step 1c 回写逻辑（5 个 Offer 字段整体刷新为最新值）。
     * 幂等：以 salary_number IS NULL 为收敛条件，重复调用安全。</p>
     *
     * @return int[2]：[0]=缺口记录数，[1]=成功回写记录数
     */
    int[] backfillOfferSalary();
}
