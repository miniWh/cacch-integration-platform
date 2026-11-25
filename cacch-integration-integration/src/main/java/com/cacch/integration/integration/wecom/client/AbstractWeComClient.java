package com.cacch.integration.integration.wecom.client;

import com.cacch.integration.integration.support.RestTemplateHttpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Collections;

/**
 * 企业微信 API 客户端抽象基类
 *
 * <p>沉淀企微各接口共用的 JSON 请求头构造与 HTTP POST 调用；access_token 由调用方
 * 通过 URL query 参数传入（企微鉴权特性），基类不做鉴权与 URL 拼接。</p>
 *
 * <p>注意：出参/入参日志统一由 {@link RestTemplateHttpClient} 脱敏打印，
 * 子类与业务层不得重复输出完整报文。存量企微 Client（会议、文档、智能表格等）
 * 保持现状，新增接口一律继承本类。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@RequiredArgsConstructor
public abstract class AbstractWeComClient {

    protected final RestTemplateHttpClient httpClient;

    /**
     * 发起企微 JSON POST 调用
     *
     * @param url          完整请求地址（含 access_token query 参数）
     * @param body         请求体对象，未赋值字段不会被序列化
     * @param responseType 响应类型
     * @param logBiz       日志业务标识（如 WeComMail）
     * @param action       接口中文名称
     * @param <T>          响应类型
     * @return 已反序列化的响应体
     */
    protected <T> T postJson(String url, Object body, Class<T> responseType, String logBiz, String action) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return httpClient.doPostOrPut(HttpMethod.POST, url, body, headers, logBiz, action, responseType);
    }
}
