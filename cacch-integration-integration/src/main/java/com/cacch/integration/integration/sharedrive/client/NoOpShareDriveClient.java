package com.cacch.integration.integration.sharedrive.client;

import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScanRequest;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
    public int scanAndProcessItemDirectories(ShareDriveScanRequest request, Consumer<ShareDriveScannedItem> processor) {
        return 0;
    }

    @Override
    public void readFileStream(ShareDriveScannedItem item, ShareDriveFileStreamConsumer consumer) throws IOException {
        throw new IOException("共享盘未配置");
    }
}
