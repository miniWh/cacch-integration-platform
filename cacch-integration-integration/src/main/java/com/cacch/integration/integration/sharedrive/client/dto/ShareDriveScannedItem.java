package com.cacch.integration.integration.sharedrive.client.dto;

/**
 * 共享盘扫描到的资料项目目录（含最新版文件）
 *
 * @param ownerName     登记负责人目录名（磁盘实际名称）
 * @param ipdpName      IPDP 目录名（磁盘实际名称）
 * @param itemName      资料项目目录名（磁盘实际名称）
 * @param directoryPath 资料项目目录 UNC 完整路径
 * @param latestFile    目录内最新版文件
 * @author hongfu_zhou@cacch.com
 */
public record ShareDriveScannedItem(
        String ownerName,
        String ipdpName,
        String itemName,
        String directoryPath,
        ShareDriveFile latestFile
) {
}
