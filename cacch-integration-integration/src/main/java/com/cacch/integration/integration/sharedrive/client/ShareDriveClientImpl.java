package com.cacch.integration.integration.sharedrive.client;

import com.cacch.integration.common.config.sharedrive.ShareDriveProperties;
import com.cacch.integration.common.constant.sharedrive.ShareDriveConstants;
import com.cacch.integration.integration.oa.support.OaRegReportPathSupport;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveFile;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScanRequest;
import com.cacch.integration.integration.sharedrive.client.dto.ShareDriveScannedItem;
import com.cacch.integration.integration.sharedrive.support.ShareDriveDirectorySupport;
import com.cacch.integration.integration.sharedrive.support.ShareDriveFinalVersionSupport;
import com.cacch.integration.integration.sharedrive.support.ShareDriveFinalVersionSupport.CandidateFile;
import com.cacch.integration.integration.sharedrive.support.ShareDriveFileSupport;
import com.cacch.integration.integration.sharedrive.support.ShareDriveIpdpDirectorySupport;
import com.cacch.integration.integration.sharedrive.support.ShareDrivePathNormalizer;
import com.cacch.integration.integration.sharedrive.support.ShareDriveUncPathSupport;
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 共享盘客户端：UNC 远程路径走 SMB（smbj），本地已挂载路径走 NIO
 *
 * <p>扫描阶段仅读取文件元数据；上传前通过 {@link #readFileStream} 流式读取内容。</p>
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
    public int scanAndProcessItemDirectories(ShareDriveScanRequest request, Consumer<ShareDriveScannedItem> processor) {
        if (request == null || request.maxItems() <= 0 || processor == null) {
            log.info("【{}】扫描终止, reason=扫描参数无效", BIZ);
            return 0;
        }
        if (!isAvailable()) {
            log.info("【{}】扫描终止, reason=共享盘不可用", BIZ);
            return 0;
        }
        String root = shareDriveProperties.getRootPath().trim();
        int processed = shouldUseSmb(root) ? scanViaSmb(request, processor) : scanViaNio(request, processor);
        log.info("【{}】共享盘扫描完成, ownerFilter={}, ipdpFilter={}, finalSuffix={}, processed={}",
                BIZ, request.ownerNameFilter(), request.ipdpNameFilter(),
                shareDriveProperties.getFinalVersionSuffix(), processed);
        return processed;
    }

    @Override
    public void readFileStream(ShareDriveScannedItem item, ShareDriveFileStreamConsumer consumer) throws IOException {
        if (item == null || consumer == null) {
            throw new IOException("扫描项或流消费者为空");
        }
        ShareDriveFile file = item.latestFile();
        if (file == null || !StringUtils.hasText(file.fileName())) {
            throw new IOException("扫描项无有效文件元数据, path=" + item.directoryPath());
        }
        String directoryPath = item.directoryPath();
        if (shouldUseSmb(directoryPath)) {
            readSmbFileStream(directoryPath, file.fileName(), consumer);
        } else {
            readNioFileStream(directoryPath, file.fileName(), consumer);
        }
    }

    private int scanViaSmb(ShareDriveScanRequest request, Consumer<ShareDriveScannedItem> processor) {
        ShareDriveUncPathSupport.UncRoot root = ShareDriveUncPathSupport.parseRoot(shareDriveProperties.getRootPath());
        if (root == null) {
            log.info("【{}】扫描终止, reason=UNC根路径解析失败", BIZ);
            return 0;
        }
        int[] processed = {0};
        try {
            withDiskShare(root, share -> {
                walkOwnerLevel(share, request, processor, processed);
                return null;
            });
        } catch (Exception e) {
            log.info("【{}】共享盘扫描失败, reason={}", BIZ, e.getMessage());
            log.error("【{}】共享盘扫描异常", BIZ, e);
        }
        return processed[0];
    }

    private void walkOwnerLevel(DiskShare share,
                                ShareDriveScanRequest request,
                                Consumer<ShareDriveScannedItem> processor,
                                int[] processed) {
        for (String ownerDir : listChildDirectories(share, "")) {
            if (processed[0] >= request.maxItems()) {
                return;
            }
            if (StringUtils.hasText(request.ownerNameFilter())
                    && !ShareDrivePathNormalizer.matchesDirectoryNameLoosely(ownerDir, request.ownerNameFilter())) {
                continue;
            }
            walkIpdpLevel(share, ownerDir, request, processor, processed);
        }
    }

    private void walkIpdpLevel(DiskShare share,
                               String ownerDir,
                               ShareDriveScanRequest request,
                               Consumer<ShareDriveScannedItem> processor,
                               int[] processed) {
        for (String ipdpDir : listChildDirectories(share, ownerDir)) {
            if (processed[0] >= request.maxItems()) {
                return;
            }
            if (!shouldScanIpdpDirectory(ownerDir, ipdpDir, request)) {
                continue;
            }
            String ipdpPath = ownerDir + "\\" + ipdpDir;
            walkItemLevel(share, ownerDir, ipdpDir, ipdpPath, request, processor, processed);
        }
    }

    private boolean shouldScanIpdpDirectory(String ownerDir, String ipdpDir, ShareDriveScanRequest request) {
        ShareDriveIpdpDirectorySupport.ParsedIpdpDirectory parsed = ShareDriveIpdpDirectorySupport.parse(ipdpDir);
        if (parsed == null) {
            log.info("【{}】跳过L2, reason=未解析IPDP名称与项目编号, owner={}, ipdpDir={}", BIZ, ownerDir, ipdpDir);
            return false;
        }
        if (StringUtils.hasText(request.ipdpNameFilter())
                && !matchesIpdpFilter(ipdpDir, request.ipdpNameFilter())) {
            return false;
        }
        if (request.ownerAllowedProjectNos().isEmpty()) {
            return true;
        }
        Set<String> allowed = ShareDriveIpdpDirectorySupport.resolveAllowedProjectNos(
                request.ownerAllowedProjectNos(), ownerDir);
        if (allowed.isEmpty()) {
            log.info("【{}】跳过L2, reason=负责人不在OA批次, owner={}, ipdpDir={}, projectNo={}",
                    BIZ, ownerDir, ipdpDir, parsed.ipdpProjectNo());
            return false;
        }
        if (!ShareDriveIpdpDirectorySupport.matchesAllowedProjectNo(allowed, parsed.ipdpProjectNo())) {
            log.info("【{}】跳过L2, reason=项目编号与OA field0164不匹配, owner={}, ipdpDir={}, diskProjectNo={}, allowed={}",
                    BIZ, ownerDir, ipdpDir, parsed.ipdpProjectNo(), allowed);
            return false;
        }
        log.info("【{}】L2项目编号匹配通过, owner={}, ipdpDir={}, projectNo={}",
                BIZ, ownerDir, ipdpDir, parsed.ipdpProjectNo());
        return true;
    }

    private void walkItemLevel(DiskShare share,
                               String ownerDir,
                               String ipdpDir,
                               String ipdpPath,
                               ShareDriveScanRequest request,
                               Consumer<ShareDriveScannedItem> processor,
                               int[] processed) {
        for (String itemDir : listChildDirectories(share, ipdpPath)) {
            if (processed[0] >= request.maxItems()) {
                return;
            }
            String itemRelative = ipdpPath + "\\" + itemDir;
            CandidateFile latest = pickLatestFinalVersionInSmbDirectory(share, itemRelative);
            if (latest == null) {
                continue;
            }
            Optional<ShareDriveScannedItem> scannedOpt = toScannedItem(ownerDir, ipdpDir, itemDir, toShareDriveFile(latest));
            if (scannedOpt.isEmpty()) {
                continue;
            }
            ShareDriveScannedItem scanned = scannedOpt.get();
            log.info("【{}】扫描到含最终版本文件, directoryPath={}, file={}, createdAt={}",
                    BIZ, scanned.directoryPath(), latest.fileName(), latest.createdAt());
            processor.accept(scanned);
            processed[0]++;
        }
    }

    private Optional<ShareDriveScannedItem> toScannedItem(String ownerDir,
                                                          String ipdpDir,
                                                          String itemDir,
                                                          ShareDriveFile file) {
        ShareDriveIpdpDirectorySupport.ParsedIpdpDirectory parsed = ShareDriveIpdpDirectorySupport.parse(ipdpDir);
        if (parsed == null) {
            log.info("【{}】跳过目录, reason=L2未解析IPDP名称与项目编号段, ipdpDir={}", BIZ, ipdpDir);
            return Optional.empty();
        }
        String directoryPath = OaRegReportPathSupport.buildItemDirectory(
                shareDriveProperties.getRootPath(), ownerDir, ipdpDir, itemDir);
        return Optional.of(new ShareDriveScannedItem(
                ownerDir,
                ipdpDir,
                parsed.ipdpName(),
                parsed.ipdpProjectNo(),
                itemDir,
                directoryPath,
                file));
    }

    private boolean matchesIpdpFilter(String ipdpDir, String filter) {
        if (!StringUtils.hasText(filter)) {
            return true;
        }
        if (ShareDrivePathNormalizer.matchesDirectoryNameLoosely(ipdpDir, filter)) {
            return true;
        }
        ShareDriveIpdpDirectorySupport.ParsedIpdpDirectory parsed = ShareDriveIpdpDirectorySupport.parse(ipdpDir);
        return parsed != null
                && ShareDrivePathNormalizer.matchesDirectoryNameLoosely(parsed.ipdpName(), filter);
    }

    private int scanViaNio(ShareDriveScanRequest request, Consumer<ShareDriveScannedItem> processor) {
        String root = shareDriveProperties.getRootPath().trim();
        Path rootPath = Paths.get(root);
        if (!Files.isDirectory(rootPath)) {
            return 0;
        }
        int[] processed = {0};
        try (Stream<Path> owners = Files.list(rootPath)) {
            owners.filter(Files::isDirectory).forEach(ownerPath -> {
                if (processed[0] >= request.maxItems()) {
                    return;
                }
                String ownerDir = ownerPath.getFileName().toString();
                if (StringUtils.hasText(request.ownerNameFilter())
                        && !ShareDrivePathNormalizer.matchesDirectoryNameLoosely(ownerDir, request.ownerNameFilter())) {
                    return;
                }
                scanNioIpdpLevel(ownerPath, ownerDir, request, processor, processed);
            });
        } catch (IOException e) {
            log.info("【{}】NIO 扫描失败, reason={}", BIZ, e.getMessage());
        }
        return processed[0];
    }

    private void scanNioIpdpLevel(Path ownerPath,
                                  String ownerDir,
                                  ShareDriveScanRequest request,
                                  Consumer<ShareDriveScannedItem> processor,
                                  int[] processed) {
        try (Stream<Path> ipdps = Files.list(ownerPath)) {
            ipdps.filter(Files::isDirectory).forEach(ipdpPath -> {
                if (processed[0] >= request.maxItems()) {
                    return;
                }
                String ipdpDir = ipdpPath.getFileName().toString();
                if (!shouldScanIpdpDirectory(ownerDir, ipdpDir, request)) {
                    return;
                }
                scanNioItemLevel(ipdpPath, ownerDir, ipdpDir, request, processor, processed);
            });
        } catch (IOException e) {
            log.info("【{}】NIO 列举 IPDP 失败, owner={}, reason={}", BIZ, ownerDir, e.getMessage());
        }
    }

    private void scanNioItemLevel(Path ipdpPath,
                                  String ownerDir,
                                  String ipdpDir,
                                  ShareDriveScanRequest request,
                                  Consumer<ShareDriveScannedItem> processor,
                                  int[] processed) {
        try (Stream<Path> items = Files.list(ipdpPath)) {
            items.filter(Files::isDirectory).forEach(itemPath -> {
                if (processed[0] >= request.maxItems()) {
                    return;
                }
                CandidateFile latest = pickLatestFinalVersionInNioDirectory(itemPath);
                if (latest == null) {
                    return;
                }
                String itemDir = itemPath.getFileName().toString();
                Optional<ShareDriveScannedItem> scannedOpt = toScannedItem(
                        ownerDir, ipdpDir, itemDir, toShareDriveFile(latest));
                if (scannedOpt.isEmpty()) {
                    return;
                }
                ShareDriveScannedItem scanned = scannedOpt.get();
                log.info("【{}】扫描到含最终版本文件, directoryPath={}, file={}, createdAt={}",
                        BIZ, scanned.directoryPath(), latest.fileName(), latest.createdAt());
                processor.accept(scanned);
                processed[0]++;
            });
        } catch (IOException e) {
            log.info("【{}】NIO 列举资料项目失败, ipdp={}, reason={}", BIZ, ipdpDir, e.getMessage());
        }
    }

    private CandidateFile pickLatestFinalVersionInNioDirectory(Path itemPath) {
        try (Stream<Path> files = Files.list(itemPath)) {
            List<CandidateFile> candidates = new ArrayList<>();
            files.filter(Files::isRegularFile).forEach(filePath -> {
                CandidateFile candidate = toCandidateMetaViaNio(filePath);
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
                CandidateFile candidate = toSmbCandidateMeta(relativeDir, fileName, entry);
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
                latest.createdAt(),
                latest.modifiedAt(),
                latest.contentType());
    }

    private void readNioFileStream(String directoryPath, String fileName, ShareDriveFileStreamConsumer consumer)
            throws IOException {
        Path filePath = Paths.get(directoryPath, fileName);
        if (!Files.isRegularFile(filePath)) {
            throw new IOException("文件不存在或不可读: " + filePath);
        }
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            consumer.accept(inputStream);
        }
    }

    private void readSmbFileStream(String directoryPath, String fileName, ShareDriveFileStreamConsumer consumer)
            throws IOException {
        if (!shareDriveProperties.hasCredentials()) {
            throw new IOException("未配置 SMB 账号");
        }
        ShareDriveUncPathSupport.UncRoot root = ShareDriveUncPathSupport.parseRoot(shareDriveProperties.getRootPath());
        if (root == null) {
            throw new IOException("UNC 根路径解析失败");
        }
        String relativeDir = ShareDriveUncPathSupport.toRelativeDirectory(directoryPath, shareDriveProperties.getRootPath());
        if (relativeDir == null) {
            throw new IOException("相对路径解析失败: " + directoryPath);
        }
        String smbDir = relativeDir.replace('/', '\\');
        try {
            withDiskShare(root, share -> {
                String resolvedDir = ShareDriveDirectorySupport.resolveRelativeDirectory(share, smbDir);
                if (resolvedDir == null) {
                    throw new RuntimeException(new IOException("SMB 目录不存在: " + smbDir));
                }
                String relativeFile = resolvedDir.isEmpty() ? fileName : resolvedDir + "\\" + fileName;
                try (InputStream inputStream = share.openFile(
                        relativeFile,
                        EnumSet.of(AccessMask.GENERIC_READ),
                        null,
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OPEN,
                        null).getInputStream()) {
                    consumer.accept(inputStream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException io) {
                throw io;
            }
            throw new IOException("SMB 读取文件失败: " + fileName, e);
        } catch (Exception e) {
            throw new IOException("SMB 读取文件失败: " + fileName, e);
        }
    }

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
                CandidateFile candidate = toCandidateMetaViaNio(filePath);
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

    private CandidateFile toCandidateMetaViaNio(Path filePath) {
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
            return new CandidateFile(
                    fileName,
                    size,
                    toLocalDateTime(attrs.creationTime().toMillis()),
                    toLocalDateTime(attrs.lastModifiedTime().toMillis()),
                    ShareDriveFileSupport.guessContentType(fileName));
        } catch (IOException e) {
            log.info("【{}】读取文件元数据失败, path={}, reason={}", BIZ, filePath, e.getMessage());
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
                    CandidateFile candidate = toSmbCandidateMeta(resolvedDir, fileName, entry);
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

    private CandidateFile toSmbCandidateMeta(String smbDir,
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
        return new CandidateFile(
                fileName,
                size,
                toLocalDateTime(entry.getCreationTime().toEpochMillis()),
                toLocalDateTime(entry.getLastWriteTime().toEpochMillis()),
                ShareDriveFileSupport.guessContentType(fileName));
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
