package com.suzhong.njl.service.merchant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.MerchantUser;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.MerchantUserMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.web.ApiException;

/**
 * 商家端租户隔离闸门。
 *
 * <p>职责：把「当前登录商家能操作哪个主体」的判断收敛在一处，商家端的门店、上下架申请、意向
 * 等业务先调用本类拿到 {@code merchantOrgId}，再以该 ID 作为 SQL 过滤条件，避免各服务各写一套鉴权。
 *
 * <p>隔离方式：令牌里只带登录时快照的 {@code merchant_user.id} 与 {@code merchant_org.id}，每次请求
 * 都回查账号与主体，校验账号启用、主体启用、令牌快照未被改绑，任一不满足即拒绝。因此运营停用账号
 * 或主体后，旧令牌会立即失效，不必等令牌自然过期。
 *
 * <p>事务边界：只做只读校验，不开启事务，也不写库；由调用方的事务包裹业务写入。
 */
@Component
public class MerchantAccess {

    private final MerchantUserMapper merchantUserMapper;
    private final MerchantOrgMapper merchantOrgMapper;

    public MerchantAccess(MerchantUserMapper merchantUserMapper, MerchantOrgMapper merchantOrgMapper) {
        this.merchantUserMapper = merchantUserMapper;
        this.merchantOrgMapper = merchantOrgMapper;
    }

    /**
     * 取当前登录商家可操作的主体 ID，并完成账号与主体双重校验。
     *
     * <p>流程：按令牌中的 {@code userId} 回查商家账号 → 校验账号启用 → 校验令牌快照中的
     * {@code merchantOrgId} 与库中一致 → 校验主体存在且为 {@code ACTIVE} → 返回主体 ID。
     *
     * <p>返回的 ID 必须作为后续查询/更新的过滤条件（如 {@code eq(Store::getMerchantOrgId, orgId)}），
     * 否则会越权访问其他商家的数据。
     *
     * @param principal 当前登录主体：令牌过滤器解析 Bearer 后写入 {@code SecurityContext}，控制器读取后传入
     * @return 当前商家主体 ID，即 {@code merchant_org.id}
     * @throws ApiException 账号不存在时 401「未登录」；账号停用、令牌主体与账号归属不一致、
     *                      主体不存在或非启用时 403
     */
    public long requireOrgId(AuthenticatedUser principal) {
        // 令牌只存 merchant_user.id，账号被物理删除后旧令牌仍可能通过解析，必须回查确认账号存在
        MerchantUser user = merchantUserMapper.selectById(principal.userId());
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        // 运营停用账号后立即生效：不依赖令牌过期，每次请求都看最新 enabled
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账号已停用");
        }
        // 主体 ID 取自令牌快照，必须与账号当前归属一致；防止令牌签发后被改绑主体造成越权
        if (principal.merchantOrgId() == null || !principal.merchantOrgId().equals(user.getMerchantOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权限");
        }
        // 主体被运营停用或记录缺失时拒绝；主体停用不清理门店数据，但旧令牌立即失效
        MerchantOrg org = merchantOrgMapper.selectById(user.getMerchantOrgId());
        if (org == null || org.getStatus() != MerchantOrgStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "商家已停用");
        }
        return user.getMerchantOrgId();
    }
}
