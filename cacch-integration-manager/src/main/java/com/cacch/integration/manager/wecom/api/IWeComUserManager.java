package com.cacch.integration.manager.wecom.api;

/**
 * 企业微信通讯录编排接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IWeComUserManager {

    /**
     * 按 userid 查询成员姓名（内部自动获取 access_token）
     *
     * @param userid 成员 UserID，不可为空
     * @return 成员姓名；查询失败或姓名为空时返回 null
     */
    String getUserName(String userid);
}
