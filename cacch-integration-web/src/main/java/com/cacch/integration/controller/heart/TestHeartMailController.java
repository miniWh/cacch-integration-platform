package com.cacch.integration.controller.heart;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.heart.client.dto.HeartMailInfo;
import com.cacch.integration.integration.heart.client.dto.HeartMailQueryRequest;
import com.cacch.integration.service.heart.api.IHeartMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Heart 邮件拉取测试接口（仅用于联调，可单独删除）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/heart/mails")
@RequiredArgsConstructor
public class TestHeartMailController {

    private final IHeartMailService heartMailService;

    /**
     * 拉取邮件摘要
     *
     * @param request 拉取条件，since 为空时取当天 00:00
     * @return 邮件摘要列表
     */
    @PostMapping("/query")
    public Result<List<HeartMailInfo>> query(@RequestBody(required = false) HeartMailQueryRequest request) {
        log.info("【HeartMail】收到拉取邮件测试请求");
        return Result.success(heartMailService.queryMails(request == null ? null : request.getSince()));
    }
}
