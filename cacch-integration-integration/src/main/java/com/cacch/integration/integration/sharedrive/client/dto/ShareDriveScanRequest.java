package com.cacch.integration.integration.sharedrive.client.dto;

/**
 * 共享盘三级目录扫描条件
 *
 * @param ownerNameFilter 登记负责人过滤；空表示扫描全部负责人目录
 * @param ipdpNameFilter  IPDP 目录过滤；空表示扫描负责人下全部 IPDP
 * @param maxItems        最多返回含有效文件的资料项目目录数
 * @author hongfu_zhou@cacch.com
 */
public record ShareDriveScanRequest(
        String ownerNameFilter,
        String ipdpNameFilter,
        int maxItems
) {
}
