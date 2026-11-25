package com.cacch.integration.controller.wecom;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListRequest;
import com.cacch.integration.integration.wecom.client.dto.mail.WeComMailListResponse;
import com.cacch.integration.manager.wecom.api.IWeComMailManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业微信测试接口（仅用于联调，可单独删除）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/wecom")
@RequiredArgsConstructor
public class TestWeComController {

    private final IWeComMailManager weComMailManager;

    /**
     * 获取应用收件箱邮件列表
     *
     * @param request 查询条件，begin_time / end_time 必填
     * @return 邮件 ID 列表与翻页游标
     */
    @PostMapping("/mails/list")
    public Result<WeComMailListResponse> listMails(@RequestBody(required = false) WeComMailListRequest request) {
        log.info("收到获取收件箱邮件列表测试请求");
        return Result.success(weComMailManager.getMailList(request));
    }
}
