package com.cacch.integration.integration.sharedrive.client;

import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;

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
}
