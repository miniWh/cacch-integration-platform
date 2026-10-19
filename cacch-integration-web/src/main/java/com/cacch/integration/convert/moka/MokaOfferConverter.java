package com.cacch.integration.convert.moka;

import com.cacch.integration.dto.moka.vo.MokaOfferExtendInfoVO;
import com.cacch.integration.integration.moka.client.dto.MokaOfferExtendInfoResponse;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Moka offer 扩展信息 MapStruct 转换器
 *
 * <p>负责 integration 层 DTO → web 层 VO 的单向转换：
 * {@link MokaOfferExtendInfoResponse} → {@link MokaOfferExtendInfoVO}。
 * 请求体由 Service 层组装（orgId 由配置注入），无需 MapStruct 转换。</p>
 *
 * <p>响应为 Moka 通用单层结构（{@code data} 直接为 offer 扩展信息列表），
 * 与 VO 结构一致，MapStruct 自动映射。嵌套类型（{@code OfferExtendInfo} /
 * {@code MokaOfferCustomField} / {@code MokaOfferAttachment} / {@code MokaOfferHistory}）
 * 字段同名同义，MapStruct 自动映射；{@code offerDetermins} 字段名按 Moka 原样保留。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface MokaOfferConverter {

    /**
     * integration 层响应 → web 层结果 VO
     *
     * <p>success 字段由本服务根据 code 计算（Moka 原始 success 字段已废弃）；
     * code / msg 原样透传便于调用方排查；data 列表逐元素映射。</p>
     *
     * @param response integration 层 offer 扩展信息响应
     * @return web 层结果 VO；response 为 null 时返回 null
     */
    MokaOfferExtendInfoVO toVO(MokaOfferExtendInfoResponse response);

    /**
     * 批量映射 offer 扩展信息列表
     *
     * @param list integration 层列表
     * @return web 层列表；为 null 时返回 null
     */
    List<MokaOfferExtendInfoVO.OfferExtendInfo> toVOList(
            List<MokaOfferExtendInfoResponse.OfferExtendInfo> list);

    /**
     * 单元素映射 offer 扩展信息
     *
     * @param item integration 层元素
     * @return web 层元素；为 null 时返回 null
     */
    MokaOfferExtendInfoVO.OfferExtendInfo toVOItem(
            MokaOfferExtendInfoResponse.OfferExtendInfo item);
}
