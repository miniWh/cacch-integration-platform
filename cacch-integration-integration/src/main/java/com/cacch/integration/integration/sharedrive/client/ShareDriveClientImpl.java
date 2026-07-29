package com.cacch.integration.integration.sharedrive.client;

import com.cacch.integration.common.config.sharedrive.ShareDriveProperties;
import com.cacch.integration.common.constant.sharedrive.ShareDriveConstants;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;
import com.cacch.integration.integration.oa.support.OaRegReportPathSupport;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScanRequest;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;
import com.cacch.integration.integration.sharedrive.support.ShareDrivePathNormalizer;
import com.cacch.integration.integration.sharedrive.support.ShareDriveDirectorySupport;
import com.cacch.integration.integration.sharedrive.support.ShareDriveFileSupport;
import com.cacch.integration.integration.sharedrive.support.ShareDriveUncPathSupport;
import com.cacch.integration.integration.sharedrive.support.ShareDriveFinalVersionSupport;
import com.cacch.integration.integration.sharedrive.support.ShareDriveFinalVersionSupport.CandidateFile;
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
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 共享盘客户端：UNC 远程路径走 SMB（smbj），本地已挂载路径走 NIO
 *
 * <p>Linux 服务器须配置 {@code share-drive.username/password}；Guest 账号在多数文件服务器已禁用。</p>
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
            if (!shareDriveProperties.hasCredentials()) {
                log.info("【{}】共享盘不可用, reason=UNC路径未配置SMB账号(Guest已禁用), path={}, "
                                + "hint=请配置 share-drive.username/password",
                        BIZ, root);
                return false;
            }
            boolean ok = pingSmbRoot(root);
            if (!ok) {
                log.info("【{}】共享盘不可用, reason=SMB根目录不可访问, path={}", BIZ, root);
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
        String finalSuffix = shareDriveProperties.getFinalVersionSuffix();
        List<CandidateFile> candidates = shouldUseSmb(directoryPath)
                ? listViaSmb(directoryPath)
                : listViaNio(directoryPath);
        CandidateFile latest = ShareDriveFinalVersionSupport.pickLatestFinalVersion(candidates, finalSuffix);
        if (latest == null) {
            log.info("【{}】目录无最终版本文件, path={}, suffix={}", BIZ, directoryPath, finalSuffix);
            return Optional.empty();
        }
        return Optional.of(toShareDriveFile(latest));
    }

    @Override
    public List<ShareDriveScannedItem> scanItemDirectories(ShareDriveScanRequest request) {
        if (request == null || request.maxItems() <= 0) {
            log.info("【{}】扫描终止, reason=扫描参数无效", BIZ);
            return List.of();
        }
        if (!isAvailable()) {
            log.info("【{}】扫描终止, reason=共享盘不可用", BIZ);
            return List.of();
        }
        String root = shareDriveProperties.getRootPath().trim();
        if (shouldUseSmb(root)) {
            return scanViaSmb(request);
        }
        return scanViaNioScan(request);
    }

    private List<ShareDriveScannedItem> scanViaSmb(ShareDriveScanRequest request) {
        ShareDriveUncPathSupport.UncRoot root = ShareDriveUncPathSupport.parseRoot(shareDriveProperties.getRootPath());
        if (root == null) {
            log.info("【{}】扫描终止, reason=UNC根路径解析失败", BIZ);
            return List.of();
        }
        List<ShareDriveScannedItem> results = new ArrayList<>();
        try {
            withDiskShare(root, share -> {
                walkOwnerLevel(share, request, results);
                return null;
            });
        } catch (Exception e) {
            log.info("【{}】共享盘扫描失败, reason={}", BIZ, e.getMessage());
            log.error("【{}】共享盘扫描异常", BIZ, e);
        }
        log.info("【{}】共享盘扫描完成, ownerFilter={}, ipdpFilter={}, finalSuffix={}, found={}",
                BIZ, request.ownerNameFilter(), request.ipdpNameFilter(),
                shareDriveProperties.getFinalVersionSuffix(), results.size());
        return results;
    }

    private void walkOwnerLevel(DiskShare share,
                                ShareDriveScanRequest request,
                                List<ShareDriveScannedItem> results) {
        for (String ownerDir : listChildDirectories(share, "")) {
            if (StringUtils.hasText(request.ownerNameFilter())
                    && !ShareDrivePathNormalizer.matchesDirectoryNameLoosely(ownerDir, request.ownerNameFilter())) {
                continue;
            }
            walkIpdpLevel(share, ownerDir, request, results);
            if (results.size() >= request.maxItems()) {
                return;
            }
        }
    }

    private void walkIpdpLevel(DiskShare share,
                               String ownerDir,
                               ShareDriveScanRequest request,
                               List<ShareDriveScannedItem> results) {
        for (String ipdpDir : listChildDirectories(share, ownerDir)) {
            if (StringUtils.hasText(request.ipdpNameFilter())
                    && !ShareDrivePathNormalizer.matchesDirectoryNameLoosely(ipdpDir, request.ipdpNameFilter())) {
                continue;
            }
            String ipdpPath = ownerDir + "\\" + ipdpDir;
            walkItemLevel(share, ownerDir, ipdpDir, ipdpPath, request, results);
            if (results.size() >= request.maxItems()) {
                return;
            }
        }
    }

    private void walkItemLevel(DiskShare share,
                               String ownerDir,
                               String ipdpDir,
                               String ipdpPath,
                               ShareDriveScanRequest request,
                               List<ShareDriveScannedItem> results) {
        for (String itemDir : listChildDirectories(share, ipdpPath)) {
            String itemRelative = ipdpPath + "\\" + itemDir;
            CandidateFile latest = pickLatestFinalVersionInSmbDirectory(share, itemRelative);
            if (latest == null) {
                continue;
            }
            String directoryPath = OaRegReportPathSupport.buildItemDirectory(
                    shareDriveProperties.getRootPath(), ownerDir, ipdpDir, itemDir);
            results.add(new ShareDriveScannedItem(
                    ownerDir,
                    ipdpDir,
                    itemDir,
                    directoryPath,
                    toShareDriveFile(latest)));
            log.info("【{}】扫描到含最终版本文件, directoryPath={}, file={}, createdAt={}",
                    BIZ, directoryPath, latest.fileName(), latest.createdAt());
            if (results.size() >= request.maxItems()) {
                return;
            }
        }
    }

    private List<ShareDriveScannedItem> scanViaNioScan(ShareDriveScanRequest request) {
        String root = shareDriveProperties.getRootPath().trim();
        Path rootPath = Paths.get(root);
        if (!Files.isDirectory(rootPath)) {
            return List.of();
        }
        List<ShareDriveScannedItem> results = new ArrayList<>();
        try (Stream<Path> owners = Files.list(rootPath)) {
            owners.filter(Files::isDirectory).forEach(ownerPath -> {
                if (results.size() >= request.maxItems()) {
                    return;
                }
                String ownerDir = ownerPath.getFileName().toString();
                if (StringUtils.hasText(request.ownerNameFilter())
                        && !ShareDrivePathNormalizer.matchesDirectoryNameLoosely(ownerDir, request.ownerNameFilter())) {
                    return;
                }
                scanNioIpdpLevel(ownerPath, ownerDir, request, results);
            });
        } catch (IOException e) {
            log.info("【{}】NIO 扫描失败, reason={}", BIZ, e.getMessage());
        }
        return results;
    }

    private void scanNioIpdpLevel(Path ownerPath,
                                  String ownerDir,
                                  ShareDriveScanRequest request,
                                  List<ShareDriveScannedItem> results) {
        try (Stream<Path> ipdps = Files.list(ownerPath)) {
            ipdps.filter(Files::isDirectory).forEach(ipdpPath -> {
                if (results.size() >= request.maxItems()) {
                    return;
                }
                String ipdpDir = ipdpPath.getFileName().toString();
                if (StringUtils.hasText(request.ipdpNameFilter())
                        && !ShareDrivePathNormalizer.matchesDirectoryNameLoosely(ipdpDir, request.ipdpNameFilter())) {
                    return;
                }
                scanNioItemLevel(ipdpPath, ownerDir, ipdpDir, request, results);
            });
        } catch (IOException e) {
            log.info("【{}】NIO 列举 IPDP 失败, owner={}, reason={}", BIZ, ownerDir, e.getMessage());
        }
    }

    private void scanNioItemLevel(Path ipdpPath,
                                  String ownerDir,
                                  String ipdpDir,
                                  ShareDriveScanRequest request,
                                  List<ShareDriveScannedItem> results) {
        try (Stream<Path> items = Files.list(ipdpPath)) {
            items.filter(Files::isDirectory).forEach(itemPath -> {
                if (results.size() >= request.maxItems()) {
                    return;
                }
                CandidateFile latest = pickLatestFinalVersionInNioDirectory(itemPath);
                if (latest == null) {
                    return;
                }
                String itemDir = itemPath.getFileName().toString();
                String directoryPath = OaRegReportPathSupport.buildItemDirectory(
                        shareDriveProperties.getRootPath(), ownerDir, ipdpDir, itemDir);
                results.add(new ShareDriveScannedItem(
                        ownerDir, ipdpDir, itemDir, directoryPath, toShareDriveFile(latest)));
                log.info("【{}】扫描到含最终版本文件, directoryPath={}, file={}, createdAt={}",
                        BIZ, directoryPath, latest.fileName(), latest.createdAt());
            });
        } catch (IOException e) {
            log.info("【{}】NIO 列举资料项目失败, ipdp={}, reason={}", BIZ, ipdpDir, e.getMessage());
        }
    }

    private CandidateFile pickLatestFinalVersionInNioDirectory(Path itemPath) {
        try (Stream<Path> files = Files.list(itemPath)) {
            List<CandidateFile> candidates = new ArrayList<>();
            files.filter(Files::isRegularFile).forEach(filePath -> {
                CandidateFile candidate = toCandidateViaNio(filePath);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            });
            return ShareDriveFinalVersionSupport.pickLatestFinalVersion(
                    candidates, shareDriveProperties.getFinalVersionSuffix());
        } catch (IOException e) {
            return null;
        }
    }

    private CandidateFile pickLatestFinalVersionInSmbDirectory(DiskShare share, String relativeDir) {
        List<CandidateFile> candidates = new ArrayList<>();
        try {
            for (FileIdBothDirectoryInformation entry : share.list(relativeDir)) {
                String fileName = entry.getFileName();
                if (".".equals(fileName) || "..".equals(fileName)) {
                    continue;
                }
                if (ShareDriveDirectorySupport.isDirectoryEntry(entry)) {
                    continue;
                }
                CandidateFile candidate = readSmbFile(share, relativeDir, fileName, entry);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
        } catch (Exception e) {
            log.info("【{}】SMB 列举资料目录失败, dir={}, reason={}", BIZ, relativeDir, e.getMessage());
            return null;
        }
        return ShareDriveFinalVersionSupport.pickLatestFinalVersion(
                candidates, shareDriveProperties.getFinalVersionSuffix());
    }

    private List<String> listChildDirectories(DiskShare share, String parentDir) {
        List<String> names = new ArrayList<>();
        try {
            for (FileIdBothDirectoryInformation entry : share.list(StringUtils.hasText(parentDir) ? parentDir : "")) {
                String name = entry.getFileName();
                if (".".equals(name) || "..".equals(name)) {
                    continue;
                }
                if (ShareDriveDirectorySupport.isDirectoryEntry(entry)) {
                    names.add(name);
                }
            }
        } catch (Exception e) {
            log.info("【{}】SMB 列举子目录失败, parent={}, reason={}", BIZ, parentDir, e.getMessage());
        }
        return names;
    }

    private ShareDriveFile toShareDriveFile(CandidateFile latest) {
        return new ShareDriveFile(
                latest.fileName(),
                latest.fileSize(),
                ShareDriveFileSupport.sha256(latest.content()),
                latest.createdAt(),
                latest.modifiedAt(),
                latest.content(),
                latest.contentType());
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
        return shareDriveProperties.hasCredentials();
    }

    private AuthenticationContext buildAuthContext() {
        String domain = StringUtils.hasText(shareDriveProperties.getDomain())
                ? shareDriveProperties.getDomain()
                : null;
        return new AuthenticationContext(
                shareDriveProperties.getUsername(),
                shareDriveProperties.getPassword().toCharArray(),
                domain);
    }

    private List<CandidateFile> listViaNio(String directoryPath) {
        Path dir = Paths.get(directoryPath);
        if (!Files.isDirectory(dir)) {
            log.info("【{}】目录不存在或不可读, path={}", BIZ, directoryPath);
            return List.of();
        }
        List<CandidateFile> candidates = new ArrayList<>();
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(Files::isRegularFile).forEach(filePath -> {
                CandidateFile candidate = toCandidateViaNio(filePath);
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

    private CandidateFile toCandidateViaNio(Path filePath) {
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
                    size,
                    toLocalDateTime(attrs.creationTime().toMillis()),
                    toLocalDateTime(attrs.lastModifiedTime().toMillis()),
                    content,
                    ShareDriveFileSupport.guessContentType(fileName));
        } catch (IOException e) {
            log.info("【{}】读取文件失败, path={}, reason={}", BIZ, filePath, e.getMessage());
            return null;
        }
    }

    private List<CandidateFile> listViaSmb(String directoryPath) {
        if (!shareDriveProperties.hasCredentials()) {
            log.info("【{}】SMB 读取终止, reason=未配置SMB账号", BIZ);
            return List.of();
        }
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
                String resolvedDir = ShareDriveDirectorySupport.resolveRelativeDirectory(share, smbDir);
                if (resolvedDir == null) {
                    log.info("【{}】SMB 目录不存在, relativeDir={}, path={}", BIZ, smbDir, directoryPath);
                    return null;
                }
                if (!resolvedDir.equals(smbDir)) {
                    log.info("【{}】SMB 目录名已解析匹配, expected={}, resolved={}", BIZ, smbDir, resolvedDir);
                }
                int listed = 0;
                int accepted = 0;
                for (FileIdBothDirectoryInformation entry : share.list(resolvedDir)) {
                    listed++;
                    String fileName = entry.getFileName();
                    if (".".equals(fileName) || "..".equals(fileName)) {
                        continue;
                    }
                    if (ShareDriveDirectorySupport.isDirectoryEntry(entry)) {
                        continue;
                    }
                    CandidateFile candidate = readSmbFile(share, resolvedDir, fileName, entry);
                    if (candidate != null) {
                        accepted++;
                        candidates.add(candidate);
                    }
                }
                log.info("【{}】SMB 目录列举完成, relativeDir={}, listed={}, accepted={}",
                        BIZ, resolvedDir, listed, accepted);
                return null;
            });
        } catch (Exception e) {
            log.info("【{}】SMB 列举目录失败, path={}, reason={}", BIZ, directoryPath, e.getMessage());
            log.error("【{}】SMB 列举目录异常, path={}", BIZ, directoryPath, e);
        }
        return candidates;
    }

    private CandidateFile readSmbFile(DiskShare share,
                                      String smbDir,
                                      String fileName,
                                      FileIdBothDirectoryInformation entry) {
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
                    size,
                    toLocalDateTime(entry.getCreationTime().toEpochMillis()),
                    toLocalDateTime(entry.getLastWriteTime().toEpochMillis()),
                    content,
                    ShareDriveFileSupport.guessContentType(fileName));
        } catch (IOException e) {
            log.info("【{}】SMB 读取文件失败, file={}, reason={}", BIZ, relativeFile, e.getMessage());
            return null;
        }
    }

    private static java.time.LocalDateTime toLocalDateTime(long epochMillis) {
        return com.cacch.integration.integration.sharedrive.support.ShareDriveVersionSupport
                .toLocalDateTime(epochMillis);
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
        } catch (Exception e) {
            log.info("【{}】SMB 连通检查失败, root={}, reason={}", BIZ, rootPath, e.getMessage());
            log.error("【{}】SMB 连通检查异常, root={}", BIZ, rootPath, e);
            return false;
        }
    }

    private <T> T withDiskShare(ShareDriveUncPathSupport.UncRoot root, Function<DiskShare, T> action) throws Exception {
        SMBClient client = new SMBClient();
        try (Connection connection = client.connect(root.host())) {
            AuthenticationContext auth = buildAuthContext();
            Session session = connection.authenticate(auth);
            try (DiskShare share = (DiskShare) session.connectShare(root.shareName())) {
                return action.apply(share);
            } finally {
                session.close();
            }
        }
    }
}
