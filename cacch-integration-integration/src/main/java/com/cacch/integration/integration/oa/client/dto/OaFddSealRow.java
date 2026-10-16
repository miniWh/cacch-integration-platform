package com.cacch.integration.integration.oa.client.dto;

/**
 * 法大大印章行
 *
 * @param companyId    法大大公司ID（formmain_5474.FIELD0002）
 * @param companyName  公司名称（ORG_UNIT.NAME）
 * @param sealId       法大大印章ID（formson_5475.FIELD0009）
 * @param sealTypeName 印章类型名称（CTP_ENUM_ITEM.SHOWVALUE）
 * @param adminId      法大大印章管理员ID（formson_5475.FIELD0011）
 * @param adminName    印章管理员名称（ORG_MEMBER.NAME）
 * @param adminPhone   印章管理员手机号（formson_5475.FIELD0015）
 * @author hongfu_zhou@cacch.com
 */
public record OaFddSealRow(
        String companyId,
        String companyName,
        String sealId,
        String sealTypeName,
        String adminId,
        String adminName,
        String adminPhone) {
}
