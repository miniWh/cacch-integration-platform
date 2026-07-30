package com.cacch.integration.integration.sharedrive.client.dto;

/**
 * 共享盘扫描到的资料项目目录（含最新版文件）
 *
 * @param ownerName         登记负责人目录名（磁盘 L1 实际名称）
 * @param ipdpDirectoryName IPDP 目录名（磁盘 L2 实际名称，含项目编号括号）
 * @param ipdpName          解析后的 IPDP 名称（对应 OA field0160）
 * @param ipdpProjectNo     解析后的项目编号（对应 OA field0164）
 * @param itemName          资料项目目录名（磁盘 L3 实际名称）
 * @param directoryPath     资料项目目录 UNC 完整路径
 * @param latestFile        目录内最新版文件
 * @author hongfu_zhou@cacch.com
 */
public record ShareDriveScannedItem(
        String ownerName,
        String ipdpDirectoryName,
        String ipdpName,
        String ipdpProjectNo,
        String itemName,
        String directoryPath,
        ShareDriveFile latestFile
) {
}
