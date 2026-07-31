package com.cacch.integration.integration.sharedrive.client;

import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScanRequest;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 共享盘客户端（SMB 读写）
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

    // ── REQ-OA-002 目录治理写操作 ──

    /**
     * 判断 UNC 目录是否存在
     *
     * @param path 完整 UNC 路径
     * @return true 表示路径存在且为目录
     */
    boolean existsDirectory(String path);

    /**
     * 递归创建目录（mkdir -p 语义：L1 → L2 → L3 逐级创建，父级已存在则跳过）
     *
     * @param path 完整 UNC 路径
     * @throws com.cacch.integration.common.exception.BizException 无写权限、路径非法或磁盘满时抛出
     */
    void mkdirs(String path);

    /**
     * 判断目录是否为空（无文件、无子目录）
     *
     * <p>按 {@code ignoreSystemFiles} 配置决定是否忽略 desktop.ini / Thumbs.db 等系统文件；
     * 默认不忽略（含系统文件视为非空，更安全）。
     *
     * @param path              完整 UNC 路径
     * @param ignoreSystemFiles 需要忽略的文件名集合；空集合表示不忽略
     * @return true 表示目录存在且内容为空（忽略列表中的文件不计）
     */
    boolean isEmptyDirectory(String path, java.util.Set<String> ignoreSystemFiles);

    /**
     * 删除空目录（仅删单层 L3，禁止递归删 L2/L1）
     *
     * <p>调用前须二次 {@link #isEmptyDirectory} 确认为空，降低并发误删风险。
     *
     * @param path 完整 UNC 路径
     * @throws com.cacch.integration.common.exception.BizException 目录非空或 SMB 异常时抛出
     */
    void deleteEmptyDirectory(String path);
}
