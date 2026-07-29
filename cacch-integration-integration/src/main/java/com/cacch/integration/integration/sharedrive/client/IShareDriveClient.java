package com.cacch.integration.integration.sharedrive.client;

import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScanRequest;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;

import java.util.List;
import java.util.Optional;

/**
 * 共享盘客户端（SMB 读文件）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IShareDriveClient {

    /**
     * 检查共享盘是否可用（挂载/连通）
     *
     * @return true 表示可读
     */
    boolean isAvailable();

    /**
     * 读取目录内最新版本文件
     *
     * @param directoryPath 资料项目目录完整路径（UNC）
     * @return 最新版文件；目录不存在或无有效文件时返回 empty
     */
    Optional<ShareDriveFile> pickLatestVersion(String directoryPath);

    /**
     * 扫描共享盘三级目录，仅返回目录内存在有效文件的资料项目
     *
     * @param request 扫描条件（负责人/IPDP 过滤与数量上限）
     * @return 扫描结果；共享盘不可用或根路径未配置时返回空列表
     */
    List<ShareDriveScannedItem> scanItemDirectories(ShareDriveScanRequest request);
}
