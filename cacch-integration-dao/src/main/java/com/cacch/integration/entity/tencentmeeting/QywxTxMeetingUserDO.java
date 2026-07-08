package com.cacch.integration.entity.tencentmeeting;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企微 userid 与腾讯会议 userid 映射 DO
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@TableName("qywx_tx_meeting_users")
public class QywxTxMeetingUserDO {

    /**
     * 企微 userid
     */
    @TableId("user_id")
    private String userId;

    /**
     * 腾讯会议 userid
     */
    @TableField("tx_meeting_user_id")
    private String txMeetingUserId;

    @TableField("create_time")
    private LocalDateTime createTime;
}
