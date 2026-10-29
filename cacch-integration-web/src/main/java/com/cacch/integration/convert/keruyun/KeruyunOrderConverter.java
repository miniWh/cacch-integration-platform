package com.cacch.integration.convert.keruyun;

import com.cacch.integration.dto.keruyun.vo.KeruyunOrderVO;
import com.cacch.integration.entity.keruyun.KeruyunOrderDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 客如云订单 DO → VO 转换器
 *
 * <p>金额字段在 DO 中以分为单位（Long），VO 中以元为单位（BigDecimal，保留 2 位小数）。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public abstract class KeruyunOrderConverter {

    /**
     * DO → VO
     *
     * @param source 订单 DO，内部主键 id 需大于 0
     * @return 订单视图对象；source 为 null 时返回 null
     */
    @Mapping(target = "orderAmtYuan", expression = "java(fenToYuan(source.getOrderAmt()))")
    @Mapping(target = "promoAmtYuan", expression = "java(fenToYuan(source.getPromoAmt()))")
    @Mapping(target = "orderReceivedAmtYuan", expression = "java(fenToYuan(source.getOrderReceivedAmt()))")
    public abstract KeruyunOrderVO toVO(KeruyunOrderDO source);

    /**
     * 分（Long）→ 元（BigDecimal，2 位小数，HALF_UP）
     *
     * @param fen 金额，单位分；null 时返回 null
     * @return 金额，单位元，保留 2 位小数
     */
    protected BigDecimal fenToYuan(Long fen) {
        if (fen == null) {
            return null;
        }
        return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
