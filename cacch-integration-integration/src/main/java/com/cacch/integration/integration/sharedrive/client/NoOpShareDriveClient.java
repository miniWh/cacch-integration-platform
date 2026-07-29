package com.cacch.integration.integration.sharedrive.client;

import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScanRequest;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 共享盘客户端占位实现（未接入 SMB，始终返回 empty）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@ConditionalOnMissingBean(IShareDriveClient.class)
public class NoOpShareDriveClient implements IShareDriveClient {

    private static final String BIZ = "ShareDrive";

    private final AtomicBoolean availabilityLogged = new AtomicBoolean();

    @Override
    public boolean isAvailable() {
        if (availabilityLogged.compareAndSet(false, true)) {
            log.info("【{}】共享盘未实现 SMB 读取，isAvailable=false", BIZ);
        }
        return false;
    }

    @Override
    public Optional<ShareDriveFile> pickLatestVersion(String directoryPath) {
        return Optional.empty();
    }

    @Override
    public List<ShareDriveScannedItem> scanItemDirectories(ShareDriveScanRequest request) {
        return Collections.emptyList();
    }
}
