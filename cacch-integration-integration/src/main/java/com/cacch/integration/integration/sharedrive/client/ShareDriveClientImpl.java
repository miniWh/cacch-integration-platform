package com.cacch.integration.integration.sharedrive.client;

import com.cacch.integration.common.config.sharedrive.ShareDriveProperties;
import com.cacch.integration.common.constant.sharedrive.ShareDriveConstants;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;
import com.cacch.integration.integration.sharedrive.support.ShareDriveFileSupport;
import com.cacch.integration.integration.sharedrive.support.ShareDriveUncPathSupport;
import com.cacch.integration.integration.sharedrive.support.ShareDriveVersionSupport;
import com.cacch.integration.integration.sharedrive.support.ShareDriveVersionSupport.CandidateFile;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.share.DiskShare;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 共享盘客户端：UNC 远程路径走 SMB（smbj），本地已挂载路径走 NIO
 *
 * <p>Linux 服务器无法像 Windows 资源管理器那样直接访问 {@code \\host\share}，
 * 须通过 SMB 协议；未配置账号时尝试 Guest，多数环境需配置 {@code share-drive.username/password}。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${share-drive.root-path:}'.length() > 0")
public class ShareDriveClientImpl implements IShareDriveClient {

    private static final String BIZ = ShareDriveConstants.LOG_BIZ;

    private final ShareDriveProperties shareDriveProperties;

    @Override
    public boolean isAvailable() {
        if (!shareDriveProperties.isConfigured()) {
            log.info("【{}】共享盘不可用, reason=root-path未配置", BIZ);
            return false;
        }
        String root = shareDriveProperties.getRootPath().trim();
        if (shouldUseSmb(root)) {
            boolean ok = pingSmbRoot(root);
            if (!ok) {
                log.info("【{}】共享盘不可用, reason=SMB根目录不可访问, path={}, hint=Linux服务器须配置share-drive.username/password",
                        BIZ, root);
            }
            return ok;
        }
        Path path = Paths.get(root);
        boolean exists = Files.isDirectory(path);
        if (!exists) {
            log.info("【{}】共享盘不可用, reason=本地挂载目录不可访问, path={}", BIZ, root);
        }
        return exists;
    }

    @Override
    public Optional<ShareDriveFile> pickLatestVersion(String directoryPath) {
        if (!StringUtils.hasText(directoryPath)) {
            log.info("【{}】读取目录终止, reason=路径为空", BIZ);
            return Optional.empty();
        }
        Pattern versionPattern = Pattern.compile(shareDriveProperties.getVersionPattern());
        List<CandidateFile> candidates = shouldUseSmb(directoryPath)
                ? listViaSmb(directoryPath, versionPattern)
                : listViaNio(directoryPath, versionPattern);
        CandidateFile latest = ShareDriveVersionSupport.pickLatest(candidates, versionPattern);
        if (latest == null) {
            return Optional.empty();
        }
        return Optional.of(new ShareDriveFile(
                latest.fileName(),
                latest.fileVersion(),
                latest.fileSize(),
                ShareDriveFileSupport.sha256(latest.content()),
                latest.modifiedAt(),
                latest.content(),
                latest.contentType()));
    }

    /**
     * UNC 远程路径或未显式配置本地挂载时走 SMB
     *
     * @param path 根路径或目录路径
     * @return true 表示使用 smbj
     */
    private boolean shouldUseSmb(String path) {
        if (ShareDriveUncPathSupport.isUncPath(path)) {
            return true;
        }
        return StringUtils.hasText(shareDriveProperties.getUsername());
    }

    private AuthenticationContext buildAuthContext() {
        if (StringUtils.hasText(shareDriveProperties.getUsername())) {
            return new AuthenticationContext(
                    shareDriveProperties.getUsername(),
                    shareDriveProperties.getPassword().toCharArray(),
                    null);
        }
        log.info("【{}】未配置 SMB 账号，尝试 Guest 访问；若失败请配置 share-drive.username/password", BIZ);
        return AuthenticationContext.guest();
    }

    private List<CandidateFile> listViaNio(String directoryPath, Pattern versionPattern) {
        Path dir = Paths.get(directoryPath);
        if (!Files.isDirectory(dir)) {
            log.info("【{}】目录不存在或不可读, path={}", BIZ, directoryPath);
            return List.of();
        }
        List<CandidateFile> candidates = new ArrayList<>();
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(Files::isRegularFile).forEach(filePath -> {
                CandidateFile candidate = toCandidateViaNio(filePath, versionPattern);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            });
        } catch (IOException e) {
            log.info("【{}】列举目录失败, path={}, reason={}", BIZ, directoryPath, e.getMessage());
            log.error("【{}】列举目录异常, path={}", BIZ, directoryPath, e);
        }
        return candidates;
    }

    private CandidateFile toCandidateViaNio(Path filePath, Pattern versionPattern) {
        try {
            String fileName = filePath.getFileName().toString();
            if (!ShareDriveFileSupport.isAllowedExtension(fileName, shareDriveProperties.getAllowedExtensions())) {
                return null;
            }
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            long size = attrs.size();
            if (size <= 0 || size > shareDriveProperties.getMaxFileSizeBytes()) {
                log.info("【{}】跳过文件, reason=大小无效或超限, fileName={}, size={}", BIZ, fileName, size);
                return null;
            }
            byte[] content = Files.readAllBytes(filePath);
            return new CandidateFile(
                    fileName,
                    ShareDriveVersionSupport.parseVersion(fileName, versionPattern),
                    size,
                    ShareDriveVersionSupport.toLocalDateTime(attrs.lastModifiedTime().toMillis()),
                    content,
                    ShareDriveFileSupport.guessContentType(fileName));
        } catch (IOException e) {
            log.info("【{}】读取文件失败, path={}, reason={}", BIZ, filePath, e.getMessage());
            return null;
        }
    }

    private List<CandidateFile> listViaSmb(String directoryPath, Pattern versionPattern) {
        ShareDriveUncPathSupport.UncRoot root = ShareDriveUncPathSupport.parseRoot(shareDriveProperties.getRootPath());
        if (root == null) {
            log.info("【{}】SMB 读取终止, reason=根路径解析失败, root={}", BIZ, shareDriveProperties.getRootPath());
            return List.of();
        }
        String relativeDir = ShareDriveUncPathSupport.toRelativeDirectory(directoryPath, shareDriveProperties.getRootPath());
        if (relativeDir == null) {
            log.info("【{}】SMB 读取终止, reason=相对路径解析失败, path={}", BIZ, directoryPath);
            return List.of();
        }
        String smbDir = relativeDir.replace('/', '\\');
        List<CandidateFile> candidates = new ArrayList<>();
        try {
            withDiskShare(root, share -> {
                if (!share.folderExists(smbDir)) {
                    log.info("【{}】SMB 目录不存在, relativeDir={}", BIZ, smbDir);
                    return null;
                }
                for (FileIdBothDirectoryInformation entry : share.list(smbDir)) {
                    String fileName = entry.getFileName();
                    if (".".equals(fileName) || "..".equals(fileName)) {
                        continue;
                    }
                    if ((entry.getFileAttributes() & 0x00000010L) != 0) {
                        continue;
                    }
                    CandidateFile candidate = readSmbFile(share, smbDir, fileName, entry, versionPattern);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
                return null;
            });
        } catch (IOException e) {
            log.info("【{}】SMB 列举目录失败, path={}, reason={}", BIZ, directoryPath, e.getMessage());
            log.error("【{}】SMB 列举目录异常, path={}", BIZ, directoryPath, e);
        }
        return candidates;
    }

    private CandidateFile readSmbFile(DiskShare share,
                                      String smbDir,
                                      String fileName,
                                      FileIdBothDirectoryInformation entry,
                                      Pattern versionPattern) {
        if (!ShareDriveFileSupport.isAllowedExtension(fileName, shareDriveProperties.getAllowedExtensions())) {
            return null;
        }
        long size = entry.getEndOfFile();
        if (size <= 0 || size > shareDriveProperties.getMaxFileSizeBytes()) {
            log.info("【{}】跳过 SMB 文件, reason=大小无效或超限, fileName={}, size={}", BIZ, fileName, size);
            return null;
        }
        String relativeFile = smbDir.isEmpty() ? fileName : smbDir + "\\" + fileName;
        try (InputStream inputStream = share.openFile(
                relativeFile,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null).getInputStream()) {
            byte[] content = inputStream.readAllBytes();
            return new CandidateFile(
                    fileName,
                    ShareDriveVersionSupport.parseVersion(fileName, versionPattern),
                    size,
                    ShareDriveVersionSupport.toLocalDateTime(entry.getLastWriteTime().toEpochMillis()),
                    content,
                    ShareDriveFileSupport.guessContentType(fileName));
        } catch (IOException e) {
            log.info("【{}】SMB 读取文件失败, file={}, reason={}", BIZ, relativeFile, e.getMessage());
            return null;
        }
    }

    private boolean pingSmbRoot(String rootPath) {
        ShareDriveUncPathSupport.UncRoot root = ShareDriveUncPathSupport.parseRoot(rootPath);
        if (root == null) {
            log.info("【{}】SMB 连通检查终止, reason=UNC根路径解析失败, root={}", BIZ, rootPath);
            return false;
        }
        try {
            withDiskShare(root, share -> {
                share.list("");
                return true;
            });
            log.info("【{}】SMB 根目录连通成功, host={}, share={}", BIZ, root.host(), root.shareName());
            return true;
        } catch (IOException e) {
            log.info("【{}】SMB 连通检查失败, root={}, reason={}", BIZ, rootPath, e.getMessage());
            return false;
        }
    }

    private <T> T withDiskShare(ShareDriveUncPathSupport.UncRoot root, Function<DiskShare, T> action)
            throws IOException {
        SMBClient client = new SMBClient();
        try (Connection connection = client.connect(root.host())) {
            AuthenticationContext auth = buildAuthContext();
            try (var session = connection.authenticate(auth)) {
                try (DiskShare share = (DiskShare) session.connectShare(root.shareName())) {
                    return action.apply(share);
                }
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
