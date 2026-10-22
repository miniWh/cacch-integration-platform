package com.cacch.integration.common.dto.batch;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量处理结果基类（通用泛型封装）
 *
 * <p>统一封装批量处理的成功、失败、跳过三类结果：调用方逐条调用
 * {@link #recordSuccess(Object)} / {@link #recordFailure(Object, String)} /
 * {@link #recordSkip(Object, String)}，计数与明细由本类自动维护，
 * 业务代码无需手工累加。</p>
 *
 * <p>明细超过上限时仅停止追加明细（{@link #detailsTruncated} 置为 true），
 * 计数值仍按实际处理条数统计，保证统计准确。</p>
 *
 * @param <T> 明细主体类型，由各业务域定义
 * @author hongfu_zhou@cacch.com
 */
@Getter
public class BatchProcessResult<T> {

    /**
     * 默认明细上限
     */
    private static final int DEFAULT_MAX_DETAILS = 1000;

    /**
     * 失败/跳过原因最大长度
     */
    private static final int MAX_REASON_LENGTH = 500;

    /**
     * 处理明细，按处理顺序排列；超过上限时截断
     */
    private final List<BatchProcessDetail<T>> details = new ArrayList<>();

    /**
     * 明细上限，子类可通过构造器调整
     */
    private final int maxDetails;

    /**
     * 处理总条数
     */
    private int totalCount;

    /**
     * 成功条数
     */
    private int successCount;

    /**
     * 失败条数
     */
    private int failureCount;

    /**
     * 跳过条数
     */
    private int skipCount;

    /**
     * 明细是否因超过上限被截断
     */
    private boolean detailsTruncated;

    /**
     * 使用默认明细上限构造
     */
    public BatchProcessResult() {
        this(DEFAULT_MAX_DETAILS);
    }

    /**
     * 指定明细上限构造
     *
     * @param maxDetails 明细上限，小于 1 时按默认值处理
     */
    public BatchProcessResult(int maxDetails) {
        this.maxDetails = maxDetails > 0 ? maxDetails : DEFAULT_MAX_DETAILS;
    }

    /**
     * 记录一条成功
     *
     * @param item 明细主体
     */
    public void recordSuccess(T item) {
        totalCount++;
        successCount++;
        onSuccess(item);
        appendDetail(item, BatchProcessStatus.SUCCESS, null);
    }

    /**
     * 记录一条失败
     *
     * @param item   明细主体
     * @param reason 失败原因
     */
    public void recordFailure(T item, String reason) {
        totalCount++;
        failureCount++;
        String trimmed = trimReason(reason);
        onFailure(item, trimmed);
        appendDetail(item, BatchProcessStatus.FAILURE, trimmed);
    }

    /**
     * 记录一条跳过
     *
     * @param item   明细主体
     * @param reason 跳过原因
     */
    public void recordSkip(T item, String reason) {
        totalCount++;
        skipCount++;
        String trimmed = trimReason(reason);
        onSkip(item, trimmed);
        appendDetail(item, BatchProcessStatus.SKIPPED, trimmed);
    }

    /**
     * 是否存在失败明细
     *
     * @return true 表示存在失败
     */
    public boolean hasFailure() {
        return failureCount > 0;
    }

    /**
     * 成功回调钩子，子类可覆写以维护域内专有统计
     *
     * @param item 明细主体
     */
    protected void onSuccess(T item) {
        // 默认无扩展逻辑
    }

    /**
     * 失败回调钩子，子类可覆写以维护域内专有统计
     *
     * @param item   明细主体
     * @param reason 失败原因
     */
    protected void onFailure(T item, String reason) {
        // 默认无扩展逻辑
    }

    /**
     * 跳过回调钩子，子类可覆写以维护域内专有统计
     *
     * @param item   明细主体
     * @param reason 跳过原因
     */
    protected void onSkip(T item, String reason) {
        // 默认无扩展逻辑
    }

    /**
     * 追加明细，超过上限时仅标记截断
     */
    private void appendDetail(T item, BatchProcessStatus status, String reason) {
        if (details.size() >= maxDetails) {
            detailsTruncated = true;
            return;
        }
        details.add(new BatchProcessDetail<>(item, status, reason));
    }

    /**
     * 截断原因，避免异常信息过长
     */
    private String trimReason(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() > MAX_REASON_LENGTH ? reason.substring(0, MAX_REASON_LENGTH) : reason;
    }
}
