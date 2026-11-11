package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * IHR「获取员工基本信息」接口完整响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IhrStaffBasicResponse extends IhrBaseResponse {

    /**
     * 员工基本信息（32 字段单对象）
     */
    private IhrStaffBasicInfo data;

    /**
     * 原始响应报文（反序列化前的 HTTP 响应原文）
     *
     * <p>非 iHR 返回字段，由 {@code IhrStaffClient} 在反序列化后回填，
     * 供数据治理链路贴源落库（stg_ihr_emp.raw_json）。加 {@code @JsonIgnore}
     * 避免本对象被再次序列化时把原文嵌进外层报文。</p>
     */
    @JsonIgnore
    private String rawJson;
}
