package com.cacch.integration.convert.wecom;

import com.cacch.integration.dto.wecom.vo.WeComDocVO;
import com.cacch.integration.integration.wecom.client.dto.doc.WeComCreateDocResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 企微文档 MapStruct 转换器
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface WeComDocConverter {

    /**
     * 企微创建文档响应转 VO
     *
     * @param response 企微原始响应
     * @return 文档 VO
     */
    @Mapping(source = "docid", target = "docId")
    WeComDocVO toVO(WeComCreateDocResponse response);
}
