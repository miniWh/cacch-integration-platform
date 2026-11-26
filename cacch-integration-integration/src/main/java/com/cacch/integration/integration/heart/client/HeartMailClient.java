package com.cacch.integration.integration.heart.client;

import com.cacch.integration.common.config.heart.HeartProperties;
import com.cacch.integration.common.constant.heart.HeartMailConstants;
import com.cacch.integration.common.constant.heart.HeartMailLogConstants;
import com.cacch.integration.common.exception.BizException;
import com.cacch.integration.common.result.ResultCode;
import com.cacch.integration.integration.heart.client.dto.HeartMailInfo;
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
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Heart 邮件 IMAP 收信客户端
 *
 * <p>仅负责连接企业邮 IMAP、按时间检索邮件并映射摘要信息；
 * 邮件以只读方式打开，不改变已读状态，也不下载附件内容。</p>
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
        HeartProperties.Mail mail = heartProperties.getMail();
        String host = mail.getHost();
        int port = mail.getPort();
        String folderName = StringUtils.hasText(mail.getFolder())
                ? mail.getFolder() : HeartMailConstants.DEFAULT_FOLDER;
        int maxPerRound = mail.getMaxPerRound() == null
                ? HeartMailConstants.DEFAULT_MAX_PER_ROUND : mail.getMaxPerRound();

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

            // 腾讯企业邮对 JavaMail 的 LE 条件（生成 OR BEFORE ... ON ...）解析异常，改用 LT 次日 00:00 表达上界
            LocalDateTime endExclusive = endTime.toLocalDate().plusDays(1).atStartOfDay();
            SearchTerm searchTerm = new AndTerm(
                    new ReceivedDateTerm(ComparisonTerm.GE,
                            Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant())),
                    new ReceivedDateTerm(ComparisonTerm.LT,
                            Date.from(endExclusive.atZone(ZoneId.systemDefault()).toInstant())));
            Message[] messages = folder.search(searchTerm);
            int count = Math.min(messages.length, maxPerRound);
            log.info("【{}】{}", LOG_BIZ, HeartMailLogConstants.buildAction(
                    "IMAP 连接成功, host=" + host + ", port=" + port
                            + ", startTime=" + startTime + ", endTime=" + endTime
                            + ", 命中邮件=" + messages.length + ", 本轮处理=" + count));

            List<HeartMailInfo> result = new ArrayList<>(count);
            UIDFolder uidFolder = (UIDFolder) folder;
            for (int i = 0; i < count; i++) {
                result.add(toMailInfo(messages[i], uidFolder));
            }
            return result;
        } catch (Exception e) {
            log.error("【{}】IMAP 拉取邮件失败, host={}, port={}, error={}",
                    LOG_BIZ, host, port, e.getMessage());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "IMAP 拉取邮件失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(folder, store);
        }
    }

    /**
     * 邮件 → 摘要信息
     *
     * @param message   IMAP 邮件
     * @param uidFolder UID 文件夹
     * @return 邮件摘要
     * @throws Exception 邮件读取异常
     */
    private HeartMailInfo toMailInfo(Message message, UIDFolder uidFolder) throws Exception {
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
        collectAttachmentNames(message, attachmentNames);
        info.setAttachmentNames(attachmentNames);
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
}
