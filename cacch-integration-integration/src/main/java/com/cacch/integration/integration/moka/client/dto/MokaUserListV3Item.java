package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 获取人事信息 V3 —— userList 单条
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MokaUserListV3Item {

    private Long userId;
    private String phone;
    private Integer roleId;
    private Integer role;
    private Integer deactivated;
    private String number;
}