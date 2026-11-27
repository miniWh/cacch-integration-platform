package com.cacch.integration.controller.heart;

import com.cacch.integration.common.constant.heart.HeartAiLogConstants;
import com.cacch.integration.common.result.Result;
import com.cacch.integration.integration.heart.adapter.HeartPoParseAdapter;
import com.cacch.integration.integration.heart.client.dto.HeartPoParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * PO 智能解析联调端点（URL + 类名双隔离，联调完成后整体删除）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test/heart/po")
public class TestHeartPoParseController {

    private static final String LOG_BIZ = HeartAiLogConstants.LOG_BIZ;

    private final HeartPoParseAdapter heartPoParseAdapter;

    /**
     * 上传 PO PDF 并返回结构化解析结果
     *
     * @param file PO PDF 文件
     * @return 解析结果（含置信度与告警）
     * @throws IOException 文件读取失败
     */
    @PostMapping("/parse")
    public Result<HeartPoParseResult> parse(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("【{}】收到 PO 解析测试请求, fileName={}, size={}",
                LOG_BIZ, file.getOriginalFilename(), file.getSize());
        return Result.success(heartPoParseAdapter.parse(file.getBytes(), file.getOriginalFilename()));
    }
}
