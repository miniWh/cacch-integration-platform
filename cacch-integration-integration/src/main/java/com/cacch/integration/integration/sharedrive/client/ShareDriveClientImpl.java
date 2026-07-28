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
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 共享盘客户端：UNC 本地访问 + SMB 认证读取
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
        if (StringUtils.hasText(shareDriveProperties.getUsername())) {
            return pingSmbRoot(root);
        }
        Path path = Paths.get(root);
        boolean exists = Files.isDirectory(path);
        if (!exists) {
            log.info("【{}】共享盘不可用, reason=根目录不可访问, path={}", BIZ, root);
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
        List<CandidateFile> candidates = StringUtils.hasText(shareDriveProperties.getUsername())
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
        SMBClient client = new SMBClient();
        try (Connection connection = client.connect(root.host())) {
            AuthenticationContext auth = new AuthenticationContext(
                    shareDriveProperties.getUsername(),
                    shareDriveProperties.getPassword().toCharArray(),
                    null);
            Session session = connection.authenticate(auth);
            try (DiskShare share = (DiskShare) session.connectShare(root.shareName())) {
                if (!share.folderExists(smbDir)) {
                    log.info("【{}】SMB 目录不存在, relativeDir={}", BIZ, smbDir);
                    return List.of();
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
            }
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
            return false;
        }
        SMBClient client = new SMBClient();
        try (Connection connection = client.connect(root.host())) {
            AuthenticationContext auth = new AuthenticationContext(
                    shareDriveProperties.getUsername(),
                    shareDriveProperties.getPassword().toCharArray(),
                    null);
            Session session = connection.authenticate(auth);
            try (DiskShare share = (DiskShare) session.connectShare(root.shareName())) {
                share.list("");
                return true;
            }
        } catch (IOException e) {
            log.info("【{}】SMB 连通检查失败, root={}, reason={}", BIZ, rootPath, e.getMessage());
            return false;
        }
    }
}
