package com.cacch.integration.convert.moka;

import com.cacch.integration.dto.moka.request.MokaJobListRequestVO;
import com.cacch.integration.dto.moka.vo.MokaJobListVO;
import com.cacch.integration.dto.moka.vo.MokaJobVO;
import com.cacch.integration.integration.moka.client.dto.MokaJobData;
import com.cacch.integration.integration.moka.client.dto.MokaJobItem;
import com.cacch.integration.integration.moka.client.dto.MokaJobListRequest;
import org.mapstruct.Mapper;

/**
 * Moka 职位 MapStruct 转换器
 *
 * <p>负责 web 层 DTO ↔ integration 层 DTO 的转换：
 * <ul>
 *     <li>web {@link MokaJobListRequestVO} → integration {@link MokaJobListRequest}</li>
 *     <li>integration {@link MokaJobItem} → web {@link MokaJobVO}（单条职位，全量透传）</li>
 *     <li>integration {@link MokaJobData} → web {@link MokaJobListVO}（total + jobs 包装）</li>
 * </ul></p>
 *
 * <p>两层嵌套结构（department / locations / stores / zhineng / customFields /
 * sitesByCategory 等）字段名完全一致，由 MapStruct 自动生成嵌套映射。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface MokaJobConverter {

    /**
     * web 层职位查询请求 → integration 层上游请求体
     *
     * <p>两层 DTO 字段同名同义逐字段复制；customFields 元素
     * （{@code MokaJobListRequestVO.CustomFieldFilter} → {@code MokaJobCustomFieldFilter}）
     * 由 MapStruct 自动映射。</p>
     *
     * @param source web 层职位查询请求（已通过 @Valid 校验）
     * @return integration 层查询请求体；source 为 null 时返回 null
     */
    MokaJobListRequest toClientRequest(MokaJobListRequestVO source);

    /**
     * integration 层单条职位 → web 层职位 VO（全量透传）
     *
     * @param item integration 层单条职位信息
     * @return 职位 VO；item 为 null 时返回 null
     */
    MokaJobVO toVO(MokaJobItem item);

    /**
     * integration 层职位 data 节点 → web 层职位列表 VO（全量透传）
     *
     * @param data integration 层 data 节点（total + jobs）
     * @return 职位列表 VO；data 为 null 时返回 null
     */
    MokaJobListVO toListVO(MokaJobData data);
}
