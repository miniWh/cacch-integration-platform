package com.cacch.integration.integration.ihr.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * iHR 开放平台通用响应基类
 *
 * <p>抽取 iHR 各接口响应中重复的通用字段（code / message / errorResult）与成功判定。
 * 业务数据 data 结构各接口差异较大（部门清单为分页对象、职位清单为裸数组），
 * 由各子类自行声明。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class IhrBaseResponse {

    /**
     * 业务状态码：0 表示成功，其它表示失败
     */
    private int code;

    /**
     * 状态描述
     */
    private String message;

    /**
     * 是否有错误明细（实测字段名为 errorResult，非 errorResultList）
     */
    @JsonProperty("errorResult")
    private Boolean errorResult;

    /**
     * 是否成功响应
     *
     * @return code == 0 时返回 true
     */
    public boolean isSuccess() {
        return code == 0;
    }
}
