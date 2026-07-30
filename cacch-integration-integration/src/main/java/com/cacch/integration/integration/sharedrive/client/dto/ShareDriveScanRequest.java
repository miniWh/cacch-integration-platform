package com.cacch.integration.integration.sharedrive.client.dto;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 共享盘三级目录扫描条件
 *
 * @param ownerNameFilter         登记负责人过滤；空表示扫描全部负责人目录
 * @param ipdpNameFilter          IPDP L2 目录过滤；空表示不按名称过滤
 * @param maxItems                最多返回含有效文件的资料项目目录数
 * @param ownerAllowedProjectNos  负责人 → OA field0164 项目编号集合；扫描 L2 时按编号匹配，空表示不过滤
 * @author hongfu_zhou@cacch.com
 */
public record ShareDriveScanRequest(
        String ownerNameFilter,
        String ipdpNameFilter,
        int maxItems,
        Map<String, Set<String>> ownerAllowedProjectNos
) {

    public ShareDriveScanRequest(String ownerNameFilter, String ipdpNameFilter, int maxItems) {
        this(ownerNameFilter, ipdpNameFilter, maxItems, Map.of());
    }

    public ShareDriveScanRequest {
        if (ownerAllowedProjectNos == null || ownerAllowedProjectNos.isEmpty()) {
            ownerAllowedProjectNos = Map.of();
        } else {
            ownerAllowedProjectNos = ownerAllowedProjectNos.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> Set.copyOf(entry.getValue())));
        }
    }
}
