package com.cacch.integration.common.config.meeting;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 会议同步相关配置属性 — 由 yml 的 meeting.sync 绑定
 *
 * <p>采用构造器绑定：{@code private final} 字段 + 显式构造器，未配置项回退默认值。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@ConfigurationProperties(prefix = "meeting.sync")
public class MeetingSyncProperties {

    /**
     * 会议开始后延迟多少分钟才开始查询录制/纪要（弱门槛，避免会前空跑），默认 0
     */
    private final int minutesStartGraceMinutes;

    /**
     * 纪要最大等待小时数（自会议开始时间起算），超时后标记为未获取，默认 48
     */
    private final int minutesMaxWaitHours;

    /**
     * 总控表单次最多处理行数；{@code <=0} 表示不限制（仍受单页拉取上限约束）
     */
    private final int masterRecordBatchSize;

    /**
     * 员工会议表单次最多扫描张数（建会扫描 / 待办回写）；{@code <=0} 表示不限制
     */
    private final int meetingTableBatchSize;

    /**
     * 会议记录单次最多处理条数（反向同步 / 纪要拉取）；{@code <=0} 表示不限制
     */
    private final int meetingRecordBatchSize;

    /**
     * 待办回写单次最多写入条数（跨表累计）；{@code <=0} 表示不限制
     */
    private final int todoBatchSize;

    /**
     * 单次同步最长运行秒数，超时后优雅退出、下次继续；{@code <=0} 表示不限制
     */
    private final int maxRunSeconds;

    public MeetingSyncProperties(Integer minutesStartGraceMinutes,
                                 Integer minutesMaxWaitHours,
                                 Integer masterRecordBatchSize,
                                 Integer meetingTableBatchSize,
                                 Integer meetingRecordBatchSize,
                                 Integer todoBatchSize,
                                 Integer maxRunSeconds) {
        this.minutesStartGraceMinutes = minutesStartGraceMinutes != null ? minutesStartGraceMinutes : 0;
        this.minutesMaxWaitHours = minutesMaxWaitHours != null ? minutesMaxWaitHours : 48;
        this.masterRecordBatchSize = masterRecordBatchSize != null ? masterRecordBatchSize : 50;
        this.meetingTableBatchSize = meetingTableBatchSize != null ? meetingTableBatchSize : 50;
        this.meetingRecordBatchSize = meetingRecordBatchSize != null ? meetingRecordBatchSize : 100;
        this.todoBatchSize = todoBatchSize != null ? todoBatchSize : 100;
        this.maxRunSeconds = maxRunSeconds != null ? maxRunSeconds : 120;
    }
}
