package com.cacch.integration.integration.fileserver.client;

import com.cacch.integration.common.config.fileserver.FileServerProperties;
import com.cacch.integration.integration.oa.client.OaClient;
import com.cacch.integration.integration.oa.client.dto.OaFileDownloadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 公司内部文件服务器上传客户端（REQ-OA-003 / D12）
 *
 * <p>实现方式（NFS 挂载）：经 {@link OaClient#downloadAttachmentFile(String, String)}
 * 以 OA 统一鉴权下载附件文件流 → 保存至 NFS 挂载目录
 * （{@code file-server.mount-path}/{@code file-server.sub-dir}/{yyyyMMdd}/{fileId}/）→
 * 以「{@code file-server.url-prefix} + 落盘绝对路径」拼接可访问 URL 返回。</p>
 *
 * <p>落盘文件名以 OA 下载接口返回的文件名（Content-Disposition）为准，
 * 缺失时回退调用方传入的 OA 附件元数据文件名；每个附件独立 fileId 子目录，杜绝同名覆盖。
 * 文件写入为覆盖写，同一附件重试落盘幂等。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileServerUploadClient {

    private static final String BIZ = "文件服务器";

    private static final DateTimeFormatter DATE_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 文件名非法字符（Windows/Linux 通吃的路径分隔符与保留符）
     */
    private static final String FILE_NAME_ILLEGAL_CHARS = "[\\\\/:*?\"<>|\\r\\n]";

    private final FileServerProperties fileServerProperties;
    private final OaClient oaClient;

    /**
     * 下载 OA 附件并保存至挂载目录，返回可访问 URL
     *
     * <p>链路：OA 统一鉴权下载（token 由 Manager 编排注入）→ 落盘
     * {@code {mount-path}/{sub-dir}/{yyyyMMdd}/{fileId}/{fileName}} → 拼 URL 前缀返回。</p>
     *
     * @param token            OA Rest Token（{@code IOaTokenService.getToken} 获取，禁止写入日志）
     * @param fallbackFileName OA 附件元数据文件名（ctp_attachment.FILENAME），下载响应未携带
     *                         文件名时的回退值，可空
     * @param fileId           附件文件标识（OA ctp_attachment.FILE_URL，下载接口的 fileId），不可为空
     * @return 附件可访问 URL（url-prefix + 落盘绝对路径）
     * @throws RestClientException   OA 附件下载失败（网络、HTTP 非 2xx、响应为空或为 JSON 报文）时抛出
     * @throws IllegalStateException 挂载目录未配置或落盘失败（目录创建 / 文件写入 IO 异常）时抛出
     */
    public String upload(String token, String fallbackFileName, String fileId) {
        if (!StringUtils.hasText(fileId)) {
            log.info("【{}】上传终止, reason=附件文件标识为空", BIZ);
            throw new IllegalStateException("文件服务器上传失败：附件文件标识为空");
        }
        // 1. OA 统一鉴权下载附件文件流（文件名以 OA 下载文件名为准，缺失时回退元数据文件名）
        OaFileDownloadResult download = oaClient.downloadAttachmentFile(token, fileId.trim());
        String fileName = resolveFileName(download.fileName(), fallbackFileName, fileId);

        // 2. 落盘：{mount-path}/{sub-dir}/{yyyyMMdd}/{fileId}/{fileName}
        String absolutePath = saveToMountDir(fileId, fileName, download.content());

        // 3. 拼 URL：url-prefix + 绝对路径
        String url = fileServerProperties.getUrlPrefix() + absolutePath;
        log.info("【{}】上传完成, fileId={}, fileName={}, path={}, url={}",
                BIZ, fileId, fileName, absolutePath, url);
        return url;
    }

    /**
     * 解析落盘文件名：优先 OA 下载接口返回的文件名，缺失时回退附件元数据文件名
     *
     * @param downloadedName   下载响应 Content-Disposition 文件名，可空
     * @param fallbackFileName OA 附件元数据文件名，可空
     * @param fileId           附件文件标识，用于日志
     * @return 清洗非法字符后的落盘文件名，不为空
     * @throws IllegalStateException 下载文件名与元数据文件名均为空时抛出
     */
    private String resolveFileName(String downloadedName, String fallbackFileName, String fileId) {
        String raw = StringUtils.hasText(downloadedName) ? downloadedName.trim() : fallbackFileName;
        if (!StringUtils.hasText(raw)) {
            log.info("【{}】上传终止, reason=下载文件名与元数据文件名均为空, fileId={}", BIZ, fileId);
            throw new IllegalStateException("文件服务器上传失败：附件文件名为空, fileId=" + fileId);
        }
        String sanitized = raw.replaceAll(FILE_NAME_ILLEGAL_CHARS, "_");
        log.info("【{}】落盘文件名解析, fileId={}, downloadedName={}, resolved={}",
                BIZ, fileId, downloadedName, sanitized);
        return sanitized;
    }

    /**
     * 保存文件至 NFS 挂载目录
     *
     * <p>目录结构 {@code {mount-path}/{sub-dir}/{yyyyMMdd}/{fileId}/{fileName}}；
     * 文件为覆盖写，同一附件重试落盘幂等；失败时抛 IllegalStateException 走重试。</p>
     *
     * @param fileId   附件文件标识（唯一，独立子目录防同名覆盖）
     * @param fileName 落盘文件名（已清洗非法字符）
     * @param content  文件内容，不为 null
     * @return 落盘绝对路径（以 / 开头）
     * @throws IllegalStateException 挂载目录未配置或目录创建 / 文件写入失败时抛出
     */
    private String saveToMountDir(String fileId, String fileName, byte[] content) {
        if (!StringUtils.hasText(fileServerProperties.getMountPath())) {
            log.info("【{}】落盘终止, reason=未配置挂载目录(file-server.mount-path)", BIZ);
            throw new IllegalStateException("文件服务器上传失败：未配置挂载目录(file-server.mount-path)");
        }
        String dateDir = LocalDate.now().format(DATE_DIR_FORMAT);
        Path dir = Paths.get(fileServerProperties.getMountPath(),
                fileServerProperties.getSubDir(), dateDir, fileId.trim());
        Path target = dir.resolve(fileName);
        try {
            Files.createDirectories(dir);
            Files.write(target, content);
            return toAbsolutePathString(target);
        } catch (IOException e) {
            log.error("【{}】落盘失败, fileId={}, target={}", BIZ, fileId, target, e);
            throw new IllegalStateException("文件服务器落盘失败: " + target + ", reason=" + e.getMessage(), e);
        }
    }

    /**
     * 转为统一 / 分隔的绝对路径字符串（Windows 调试时亦输出合法形态）
     *
     * @param target 落盘目标路径
     * @return 以 / 开头的绝对路径字符串
     */
    private String toAbsolutePathString(Path target) {
        String absolute = target.toAbsolutePath().normalize().toString().replace('\\', '/');
        // Windows 调试环境可能产出盘符相对形态，兜底补前导 /
        return absolute.startsWith("/") ? absolute : "/" + absolute;
    }
}
