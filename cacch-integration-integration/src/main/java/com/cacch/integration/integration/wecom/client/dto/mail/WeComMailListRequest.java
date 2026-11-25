package com.cacch.integration.integration.wecom.client.dto.mail;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 企业微信获取收件箱邮件列表请求参数
 *
 * <p>begin_time / end_time 为接口必填项；未赋值字段不会出现在请求体中。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeComMailListRequest {

    /**
     * 开始时间，unix 时间戳（秒）；uint32 上限 4294967295 超出 Integer 范围，故用 Long
     */
    @JsonProperty("begin_time")
    @JsonAlias("beginTime")
    private Long beginTime;

    /**
     * 结束时间，unix 时间戳（秒）
     */
    @JsonProperty("end_time")
    @JsonAlias("endTime")
    private Long endTime;

    /**
     * 翻页游标，取上一次返回的 next_cursor；首次拉取可不填
     */
    @JsonProperty("cursor")
    private String cursor;

    /**
     * 期望请求的数据量，默认 100，最大 1000
     */
    @JsonProperty("limit")
    private Integer limit;
}
