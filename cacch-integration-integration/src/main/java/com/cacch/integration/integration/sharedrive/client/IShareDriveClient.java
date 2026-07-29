package com.cacch.integration.integration.sharedrive.client;

import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScanRequest;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

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
     * 读取目录内创建时间最新的「最终版本」文件元数据
     *
     * @param directoryPath 资料项目目录完整路径（UNC）
     * @return 最新最终版本文件元数据；目录不存在或无匹配文件时返回 empty
     */
    Optional<ShareDriveFile> pickLatestVersion(String directoryPath);

    /**
     * 流式扫描共享盘三级目录：每发现一个含「最终版本」文件的资料目录，立即回调处理（不批量缓存文件内容）
     *
     * @param request   扫描条件（负责人/IPDP 过滤与数量上限）
     * @param processor 逐条处理器；返回 false 时终止后续扫描
     * @return 实际处理（回调）的目录数
     */
    int scanAndProcessItemDirectories(ShareDriveScanRequest request, Consumer<ShareDriveScannedItem> processor);

    /**
     * 以流方式读取扫描项对应文件（扫描阶段仅加载元数据，上传前调用此方法）
     *
     * @param item     扫描结果（含目录路径与文件元数据）
     * @param consumer 流消费回调
     * @throws IOException 打开或读取文件失败时抛出
     */
    void readFileStream(ShareDriveScannedItem item, ShareDriveFileStreamConsumer consumer) throws IOException;
}
