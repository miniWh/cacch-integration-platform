package com.cacch.integration.integration.moka.client.dto;

import com.cacch.integration.common.constant.moka.MokaConstants;
import lombok.Data;

import java.util.List;

/**
 * Moka 用户信息同步接口 —— syncInfo 请求体 DTO
 *
 * <p>对接 Moka 开放平台 {@code POST /api-platform/v1/users/syncInfo}
 * （{@linkplain <a href="https://www.mokahr.com/docs/api/#-72">API 文档 #-72</a>}）。
 * 以 {@code uniqueType="phone"} 指定的手机号作为用户唯一性匹配键，
 * Moka 侧存在则更新、不存在则创建。</p>
 *
 * <p>请求体结构：
 * <pre>
 * {
 *   "usersInfo": [
 *     {
 *       "phone": "13800138000",
 *       "name": "张三",
 *       "nickname": "花名",
 *       "email": "zhangsan@cacch.com",
 *       "number": "EMP001",
 *       "roleId": 223379,
 *       "departmentCode": ["D001"],
 *       "deactivated": 0,
 *       "uniqueType": "phone",
 *       "locale": "zh-CN",
 *       "timezone": "Asia/Shanghai",
 *       "updateDepartment": false,
 *       "updateSuperiorEmail": false,
 *       "autoActivated": 0,
 *       "thirdPartyId": ""
 *     }
 *   ]
 * }
 * </pre>
 * </p>
 *
 * <p>固定参数集中在 {@link MokaConstants}，构造 {@link MokaUserInfo}
 * 时由 Manager 层显式填充，避免遗漏。Moka API 单次推送上限 100 条。</p>
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MokaUserSyncRequest {

    /**
     * 待同步用户信息列表（单批 ≤ 100 条）
     */
    private List<MokaUserInfo> usersInfo;

    /**
     * Moka 用户信息项 —— 单个员工的同步字段集合
     *
     * <p>字段映射（中间表 → Moka API）：
     * <ul>
     *     <li>{@code phone}        ← contact_phone（唯一标识）</li>
     *     <li>{@code name}         ← user_name</li>
     *     <li>{@code nickname}     ← nickname</li>
     *     <li>{@code email}        ← company_email（允许为空）</li>
     *     <li>{@code number}       ← employee_no</li>
     *     <li>{@code roleId}       ← role_id（默认 223379）</li>
     *     <li>{@code departmentCode} ← department_code（数组包装）</li>
     *     <li>{@code deactivated}  ← deactivated（0/1）</li>
     * </ul>
     * 固定参数（{@link MokaConstants}）：
     * {@code uniqueType / locale / timezone / updateDepartment /
     * updateSuperiorEmail / autoActivated / thirdPartyId}</p>
     *
     * @author hongfu_zhou@cacch.com
     */
    @Data
    public static class MokaUserInfo {

        /**
         * 用户手机号（唯一标识，对应 uniqueType=phone）
         */
        private String phone;

        /**
         * 用户姓名
         */
        private String name;

        /**
         * 用户昵称 / 花名
         */
        private String nickname;

        /**
         * 工作邮箱（允许为空）
         */
        private String email;

        /**
         * 工号
         */
        private String number;

        /**
         * Moka 自定义角色 ID
         */
        private Integer roleId;

        /**
         * 部门编号列表 —— Moka API 要求数组形式，单部门场景传单元素数组
         */
        private String[] departmentCode;

        /**
         * 是否禁用：0-不禁用 1-禁用（首次创建传 1 会被 Moka 拒绝）
         */
        private Integer deactivated;

        /**
         * 用户唯一标识类型，固定 "phone"
         *
         * @see MokaConstants#USER_UNIQUE_TYPE
         */
        private String uniqueType;

        /**
         * 用户语言，固定 "zh-CN"
         *
         * @see MokaConstants#USER_LOCALE
         */
        private String locale;

        /**
         * 用户时区，固定 "Asia/Shanghai"
         *
         * @see MokaConstants#USER_TIMEZONE
         */
        private String timezone;

        /**
         * 是否更新部门，固定 false
         *
         * @see MokaConstants#USER_UPDATE_DEPARTMENT
         */
        private Boolean updateDepartment;

        /**
         * 是否更新直属领导邮箱，固定 false
         *
         * @see MokaConstants#USER_UPDATE_SUPERIOR_EMAIL
         */
        private Boolean updateSuperiorEmail;

        /**
         * 是否自动激活，固定 0
         *
         * @see MokaConstants#USER_AUTO_ACTIVATED
         */
        private Integer autoActivated;

        /**
         * 第三方 ID，固定空串
         *
         * @see MokaConstants#USER_THIRD_PARTY_ID
         */
        private String thirdPartyId;
    }
}
