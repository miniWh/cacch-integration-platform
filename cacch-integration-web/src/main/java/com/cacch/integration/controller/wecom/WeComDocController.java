package com.cacch.integration.controller.wecom;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.wecom.WeComDocConverter;
import com.cacch.integration.dto.wecom.request.CreateSmartSheetDocRequest;
import com.cacch.integration.dto.wecom.vo.WeComDocVO;
import com.cacch.integration.manager.wecom.api.IWeComDocManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业微信文档接口
 *
 * <p>鉴权使用配置文件 {@code wecom.apps} 中自建应用的 corpid + secret，调用方无需传递密钥。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping("/api/v1/wecom/docs")
@RequiredArgsConstructor
public class WeComDocController {

    private final IWeComDocManager weComDocManager;
    private final WeComDocConverter weComDocConverter;

    /**
     * 新建智能表格文档
     *
     * @param request 文档名称与管理员列表
     * @return 新建文档的 docId 与访问链接
     */
    @PostMapping("/smartsheets")
    public Result<WeComDocVO> createSmartSheetDoc(@Valid @RequestBody CreateSmartSheetDocRequest request) {
        return Result.success(weComDocConverter.toVO(
                weComDocManager.createSmartSheetDoc(request.getDocName(), request.getAdminUsers())));
    }
}
