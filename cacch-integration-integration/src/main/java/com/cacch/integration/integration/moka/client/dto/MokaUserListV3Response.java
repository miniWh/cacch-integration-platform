package com.cacch.integration.integration.moka.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 获取人事信息 V3 响应
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MokaUserListV3Response extends MokaBaseResponse<MokaUserListV3Response.UserListData> {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserListData {
        private Long nextUserId;
        private List<MokaUserListV3Item> userList;
    }
}