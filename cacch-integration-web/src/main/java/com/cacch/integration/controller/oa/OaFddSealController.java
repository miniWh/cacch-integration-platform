package com.cacch.integration.controller.oa;

import com.cacch.integration.common.result.Result;
import com.cacch.integration.convert.oa.OaFddSealConverter;
import com.cacch.integration.dto.oa.vo.OaFddCompanyVO;
import com.cacch.integration.dto.oa.vo.OaFddSealVO;
import com.cacch.integration.service.oa.api.IOaFddSealQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 法大大印章查询 REST 接口
 *
 * <p>对外提供法大大印章完整列表查询与公司去重列表查询能力，
 * 数据来源于 OA 只读库（formson_5475 印章子表等联合查询）。
 * 所有接口统一返回 {@link Result} 格式。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oa/fdd-seals")
public class OaFddSealController {

    private final IOaFddSealQueryService oaFddSealQueryService;
    private final OaFddSealConverter oaFddSealConverter;

    /**
     * 查询法大大印章完整列表（全量，无分页无筛选）
     *
     * @return 统一返回体，data 为印章 VO 列表；无数据或 OA 数据源未配置时返回空列表
     */
    @GetMapping
    public Result<List<OaFddSealVO>> listSeals() {
        return Result.success(oaFddSealConverter.toVOList(oaFddSealQueryService.listSeals()));
    }

    /**
     * 查询法大大公司去重列表（返回公司ID与公司名称）
     *
     * @return 统一返回体，data 为公司 VO 列表；无数据或 OA 数据源未配置时返回空列表
     */
    @GetMapping("/companies")
    public Result<List<OaFddCompanyVO>> listCompanies() {
        return Result.success(oaFddSealConverter.toCompanyVOList(oaFddSealQueryService.listDistinctCompanies()));
    }
}
