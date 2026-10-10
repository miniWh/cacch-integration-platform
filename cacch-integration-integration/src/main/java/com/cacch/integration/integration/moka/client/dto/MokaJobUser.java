package com.cacch.integration.integration.moka.client.dto;

import lombok.Data;

/**
 * Moka ATS —— 职位查询响应中的职位相关人员信息
 *
 * <p>供职位负责人（{@code jobManager}，单对象）、职位协助人（{@code jobHrAssistant}）、
 * 用人经理（{@code jobHiringManager}）、面试官（{@code jobInterviewer}）三类列表复用；
 * 上述字段默认不返回，需联系 Moka CSM 在后台配置功能模块开启同步开关。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaJobUser {

    /**
     * 姓名
     */
    private String name;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * number 字段
     */
    private String number;
}
