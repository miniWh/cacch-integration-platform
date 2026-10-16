package com.cacch.integration.convert.oa;

import com.cacch.integration.dto.oa.vo.OaFddCompanyVO;
import com.cacch.integration.dto.oa.vo.OaFddSealVO;
import com.cacch.integration.integration.oa.client.dto.OaFddCompanyRow;
import com.cacch.integration.integration.oa.client.dto.OaFddSealRow;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 法大大印章 MapStruct 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface OaFddSealConverter {

    /**
     * 印章 Row 转 VO
     *
     * @param row 印章行
     * @return 印章 VO
     */
    OaFddSealVO toVO(OaFddSealRow row);

    /**
     * 印章 Row 列表转 VO 列表
     *
     * @param rows 印章行列表
     * @return 印章 VO 列表
     */
    List<OaFddSealVO> toVOList(List<OaFddSealRow> rows);

    /**
     * 公司 Row 转 VO
     *
     * @param row 公司行
     * @return 公司 VO
     */
    OaFddCompanyVO toCompanyVO(OaFddCompanyRow row);

    /**
     * 公司 Row 列表转 VO 列表
     *
     * @param rows 公司行列表
     * @return 公司 VO 列表
     */
    List<OaFddCompanyVO> toCompanyVOList(List<OaFddCompanyRow> rows);
}
