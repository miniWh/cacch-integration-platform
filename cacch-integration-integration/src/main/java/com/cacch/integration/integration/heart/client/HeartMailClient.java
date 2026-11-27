package com.cacch.integration.integration.heart.client;

import com.cacch.integration.common.config.heart.HeartProperties;
import com.cacch.integration.common.constant.heart.HeartMailConstants;
import com.cacch.integration.common.constant.heart.HeartMailLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.heart.client.dto.HeartMailAttachmentInfo;
import com.cacch.integration.integration.heart.client.dto.HeartMailInfo;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;

/**
 * Heart 邮件 IMAP 收信客户端
 *
 * <p>负责连接企业邮 IMAP、按时间检索邮件，并按需映射摘要或下载正文与 PDF 附件；
 * 邮件以只读方式打开，不改变已读状态。附件以流式方式写入临时文件（SHA-256 边写边算），
 * 内存占用与附件大小无关。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeartMailClient {

    private static final String LOG_BIZ = HeartMailLogConstants.LOG_BIZ;

    private final HeartProperties heartProperties;

    /**
     * 拉取指定时间区间内的邮件摘要
     *
     * @param startTime 开始时间（含）
     * @param endTime   结束时间（含）
     * @return 邮件摘要列表，最多 {@code heart.mail.max-per-round} 封
     */
    public List<HeartMailInfo> queryMails(LocalDateTime startTime, LocalDateTime endTime) {
        return fetch(startTime, endTime, false);
    }

    /**
     * 拉取指定时间区间内的邮件，并下载正文与 PDF 附件
     *
     * <p>单封邮件内容拉取失败不影响其余邮件，失败原因写入 {@code fetchError}。</p>
     *
     * @param startTime 开始时间（含）
     * @param endTime   结束时间（含）
     * @return 带正文与附件的邮件列表
     */
    public List<HeartMailInfo> fetchMailsWithContent(LocalDateTime startTime, LocalDateTime endTime) {
        return fetch(startTime, endTime, true);
    }

    /**
     * 按 IMAP UID 拉取单封邮件（含正文与附件），用于失败重试
     *
     * @param uid IMAP 邮件 UID
     * @return 邮件信息，邮件已不存在时返回 {@code null}
     */
    public HeartMailInfo fetchMailByUid(Long uid) {
        if (uid == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "邮件 UID 不能为空");
        }
        MailSession session = connect(heartProperties.getMail());
        try {
            Message message = ((UIDFolder) session.folder()).getMessageByUID(uid);
            if (message == null) {
                return null;
            }
            return toMailInfo(message, (UIDFolder) session.folder(), true);
        } catch (Exception e) {
            log.error("【{}】按 UID 拉取邮件失败, uid={}, error={}", LOG_BIZ, uid, e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "按 UID 拉取邮件失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(session.folder(), session.store());
        }
    }

    /**
     * 拉取邮件（摘要或带内容）
     *
     * <p>仅检索未读邮件，并与收件时间区间取交集。</p>
     *
     * @param startTime   开始时间（含）
     * @param endTime     结束时间（含）
     * @param withContent 是否下载正文与附件
     * @return 邮件列表
     */
    private List<HeartMailInfo> fetch(LocalDateTime startTime, LocalDateTime endTime, boolean withContent) {
        HeartProperties.Mail mail = heartProperties.getMail();
        String host = mail.getHost();
        int port = mail.getPort();
        int maxPerRound = mail.getMaxPerRound() == null
                ? HeartMailConstants.DEFAULT_MAX_PER_ROUND : mail.getMaxPerRound();

        MailSession session = connect(mail);
        try {
            // 腾讯企业邮对 JavaMail 的 LE 条件（生成 OR BEFORE ... ON ...）解析异常，改用 LT 次日 00:00 表达上界
            LocalDateTime endExclusive = endTime.toLocalDate().plusDays(1).atStartOfDay();
            // 仅拉取未读邮件：UNSEEN 与时间区间取交集
            SearchTerm searchTerm = new AndTerm(new SearchTerm[]{
                    new ReceivedDateTerm(ComparisonTerm.GE,
                            Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant())),
                    new ReceivedDateTerm(ComparisonTerm.LT,
                            Date.from(endExclusive.atZone(ZoneId.systemDefault()).toInstant())),
                    new FlagTerm(new Flags(Flags.Flag.SEEN), false)});
            Message[] messages = session.folder().search(searchTerm);
            int count = Math.min(messages.length, maxPerRound);
            log.info("【{}】{}", LOG_BIZ, HeartMailLogConstants.buildAction(
                    "IMAP 连接成功, host=" + host + ", port=" + port
                            + ", startTime=" + startTime + ", endTime=" + endTime
                            + ", 命中邮件=" + messages.length + ", 本轮处理=" + count
                            + ", 下载内容=" + withContent));

            List<HeartMailInfo> result = new ArrayList<>(count);
            UIDFolder uidFolder = (UIDFolder) session.folder();
            for (int i = 0; i < count; i++) {
                try {
                    result.add(toMailInfo(messages[i], uidFolder, withContent));
                } catch (Exception e) {
                    if (!withContent) {
                        throw e;
                    }
                    log.error("【{}】邮件解析失败, error={}", LOG_BIZ, e.getMessage());
                    result.add(buildFailedInfo(messages[i], uidFolder, e));
                }
            }
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("【{}】IMAP 拉取邮件失败, host={}, port={}, error={}",
                    LOG_BIZ, host, port, e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "IMAP 拉取邮件失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(session.folder(), session.store());
        }
    }

    /**
     * 建立 IMAP 连接并只读打开收件箱
     *
     * @param mail 邮件配置
     * @return 已打开的会话
     */
    private MailSession connect(HeartProperties.Mail mail) {
        String host = mail.getHost();
        int port = mail.getPort();
        String folderName = StringUtils.hasText(mail.getFolder())
                ? mail.getFolder() : HeartMailConstants.DEFAULT_FOLDER;

        Properties properties = new Properties();
        properties.put("mail.store.protocol", HeartMailConstants.IMAP_PROTOCOL);
        properties.put("mail.imaps.ssl.enable", String.valueOf(Boolean.TRUE.equals(mail.getSsl())));
        properties.put("mail.imaps.connectiontimeout", String.valueOf(HeartMailConstants.CONNECTION_TIMEOUT_MS));
        properties.put("mail.imaps.timeout", String.valueOf(HeartMailConstants.READ_TIMEOUT_MS));

        Store store = null;
        Folder folder = null;
        try {
            Session session = Session.getInstance(properties);
            store = session.getStore(HeartMailConstants.IMAP_PROTOCOL);
            store.connect(host, port, mail.getUsername(), mail.getPassword());
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);
            return new MailSession(store, folder);
        } catch (Exception e) {
            closeQuietly(folder, store);
            log.error("【{}】IMAP 连接失败, host={}, port={}, error={}",
                    LOG_BIZ, host, port, e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "IMAP 连接失败: " + e.getMessage(), e);
        }
    }

    /**
     * 邮件 → 摘要信息（可按需下载正文与附件）
     *
     * @param message     IMAP 邮件
     * @param uidFolder   UID 文件夹
     * @param withContent 是否下载正文与附件
     * @return 邮件信息
     * @throws Exception 邮件基础信息读取异常
     */
    private HeartMailInfo toMailInfo(Message message, UIDFolder uidFolder, boolean withContent) throws Exception {
        HeartMailInfo info = new HeartMailInfo();
        info.setUid(uidFolder.getUID(message));
        info.setMessageId(firstHeader(message, "Message-ID"));
        info.setSubject(decodeText(message.getSubject()));
        info.setFrom(resolveFrom(message));

        Date receivedDate = message.getReceivedDate() != null
                ? message.getReceivedDate() : message.getSentDate();
        if (receivedDate != null) {
            info.setReceivedAt(LocalDateTime.ofInstant(receivedDate.toInstant(), ZoneId.systemDefault()));
        }

        List<String> attachmentNames = new ArrayList<>();
        info.setAttachmentNames(attachmentNames);
        if (!withContent) {
            collectAttachmentNames(message, attachmentNames);
            return info;
        }

        List<Path> tempFiles = new ArrayList<>();
        try {
            StringBuilder plain = new StringBuilder();
            StringBuilder html = new StringBuilder();
            info.setAttachments(new ArrayList<>());
            collectContent(message, info, plain, html, tempFiles);
            info.setBodyText(plain.isEmpty() ? null : plain.toString());
            info.setBodyHtml(html.isEmpty() ? null : html.toString());
        } catch (Exception e) {
            // 该封邮件内容不完整：清理已下载临时文件，交由重试
            tempFiles.forEach(this::deleteQuietly);
            info.setAttachments(new ArrayList<>());
            info.setFetchError("正文/附件拉取失败: " + e.getMessage());
            log.warn("【{}】正文/附件拉取失败, uid={}, error={}", LOG_BIZ, info.getUid(), e.getMessage());
        }
        return info;
    }

    /**
     * 构造解析失败邮件信息（仅保留可用基础字段与失败原因）
     *
     * @param message   邮件
     * @param uidFolder UID 文件夹
     * @param e         解析异常
     * @return 带失败原因的邮件信息
     */
    private HeartMailInfo buildFailedInfo(Message message, UIDFolder uidFolder, Exception e) {
        HeartMailInfo info = new HeartMailInfo();
        try {
            info.setUid(uidFolder.getUID(message));
        } catch (Exception ignore) {
            // UID 读取失败时保持为空
        }
        try {
            info.setMessageId(firstHeader(message, "Message-ID"));
        } catch (Exception ignore) {
            // Message-ID 读取失败时保持为空
        }
        info.setFetchError("邮件解析失败: " + e.getMessage());
        return info;
    }

    /**
     * 读取首个指定名称的邮件头
     *
     * @param message 邮件
     * @param name    头名称
     * @return 头内容，不存在返回 null
     * @throws Exception 读取异常
     */
    private String firstHeader(Message message, String name) throws Exception {
        String[] headers = message.getHeader(name);
        return headers == null || headers.length == 0 ? null : headers[0];
    }

    /**
     * 解析发件人地址
     *
     * @param message 邮件
     * @return 发件人地址，缺失返回 null
     * @throws Exception 读取异常
     */
    private String resolveFrom(Message message) throws Exception {
        if (message.getFrom() == null || message.getFrom().length == 0) {
            return null;
        }
        if (message.getFrom()[0] instanceof InternetAddress address) {
            return address.getAddress();
        }
        return decodeText(message.getFrom()[0].toString());
    }

    /**
     * 递归收集附件文件名（不下载内容）
     *
     * @param part  邮件/邮件体
     * @param names 附件名容器
     * @throws Exception 读取异常
     */
    private void collectAttachmentNames(Part part, List<String> names) throws Exception {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                collectAttachmentNames(multipart.getBodyPart(i), names);
            }
            return;
        }
        String fileName = part.getFileName();
        if (StringUtils.hasText(fileName)) {
            names.add(decodeText(fileName));
        }
    }

    /**
     * 递归提取正文与 PDF 附件
     *
     * <p>正文按 {@code text/plain}、{@code text/html} 分别收集并限长截断；
     * PDF 附件流式写入临时文件，非 PDF 附件仅记录文件名。</p>
     *
     * @param part  邮件/邮件体
     * @param info  邮件信息容器
     * @param plain 纯文本正文容器
     * @param html  HTML 正文容器
     * @param temps 临时文件容器
     * @throws Exception 读取/下载异常
     */
    private void collectContent(Part part, HeartMailInfo info, StringBuilder plain, StringBuilder html,
                                List<Path> temps) throws Exception {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                collectContent(multipart.getBodyPart(i), info, plain, html, temps);
            }
            return;
        }
        String fileName = part.getFileName();
        if (StringUtils.hasText(fileName)) {
            String decodedName = decodeText(fileName);
            info.getAttachmentNames().add(decodedName);
            if (isPdf(part, decodedName)) {
                HeartMailAttachmentInfo attachment = downloadPdf(part, decodedName);
                info.getAttachments().add(attachment);
                temps.add(Path.of(attachment.getTempFilePath()));
            }
            return;
        }
        if (part.isMimeType("text/plain")) {
            appendCapped(plain, String.valueOf(part.getContent()));
        } else if (part.isMimeType("text/html")) {
            appendCapped(html, String.valueOf(part.getContent()));
        }
    }

    /**
     * 流式下载 PDF 附件至临时文件，同时计算 SHA-256
     *
     * @param part     附件体
     * @param fileName 附件文件名
     * @return 附件信息（含临时文件路径）
     * @throws Exception 下载异常
     */
    private HeartMailAttachmentInfo downloadPdf(Part part, String fileName) throws Exception {
        Path temp = Files.createTempFile("heart-mail-", ".pdf");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new DigestInputStream(part.getInputStream(), digest)) {
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        HeartMailAttachmentInfo attachment = new HeartMailAttachmentInfo();
        attachment.setName(fileName);
        attachment.setSize(Files.size(temp));
        attachment.setSha256(HexFormat.of().formatHex(digest.digest()));
        attachment.setTempFilePath(temp.toAbsolutePath().toString());
        return attachment;
    }

    /**
     * 判断是否为 PDF 附件
     *
     * @param part     附件体
     * @param fileName 解码后文件名
     * @return 是否 PDF
     * @throws Exception 类型读取异常
     */
    private boolean isPdf(Part part, String fileName) throws Exception {
        return part.isMimeType("application/pdf") || fileName.toLowerCase().endsWith(".pdf");
    }

    /**
     * 追加正文并按上限截断
     *
     * @param builder 正文容器
     * @param text    待追加文本
     */
    private void appendCapped(StringBuilder builder, String text) {
        if (!StringUtils.hasText(text) || builder.length() >= HeartMailConstants.MAX_BODY_CHARS) {
            return;
        }
        int remain = HeartMailConstants.MAX_BODY_CHARS - builder.length();
        builder.append(text.length() <= remain ? text : text.substring(0, remain));
    }

    /**
     * 删除临时文件（忽略异常）
     *
     * @param path 临时文件路径
     */
    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("【{}】删除临时文件失败, path={}, error={}", LOG_BIZ, path, e.getMessage());
        }
    }

    /**
     * 解码邮件头编码（如 =?UTF-8?B?...?=）
     *
     * @param text 原始文本
     * @return 解码后文本
     * @throws Exception 解码异常
     */
    private String decodeText(String text) throws Exception {
        return text == null ? null : MimeUtility.decodeText(text);
    }

    /**
     * 静默关闭文件夹与连接
     *
     * @param folder 邮件文件夹
     * @param store  邮件连接
     */
    private void closeQuietly(Folder folder, Store store) {
        if (folder != null && folder.isOpen()) {
            try {
                // 只读打开，expunge 传 false 避免任何写操作
                folder.close(false);
            } catch (Exception e) {
                log.warn("【{}】关闭邮件文件夹失败, error={}", LOG_BIZ, e.getMessage());
            }
        }
        if (store != null && store.isConnected()) {
            try {
                store.close();
            } catch (Exception e) {
                log.warn("【{}】关闭邮件连接失败, error={}", LOG_BIZ, e.getMessage());
            }
        }
    }

    /**
     * 已打开的邮件会话
     *
     * @param store  IMAP 连接
     * @param folder 邮件文件夹
     */
    private record MailSession(Store store, Folder folder) {
    }
}
