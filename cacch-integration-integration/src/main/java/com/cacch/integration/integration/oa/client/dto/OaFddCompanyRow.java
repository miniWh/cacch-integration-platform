package com.cacch.integration.integration.oa.client.dto;

/**
 * 法大大公司去重行（OA formmain_5474 DISTINCT 查询结果）
 *
 * @param companyId   法大大公司ID（formmain_5474.FIELD0002）
 * @param companyName 公司名称（ORG_UNIT.NAME）
 * @author hongfu_zhou@cacch.com
 */
public record OaFddCompanyRow(String companyId, String companyName) {
}
