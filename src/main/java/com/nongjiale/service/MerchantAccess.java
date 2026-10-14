package com.nongjiale.service;

import com.nongjiale.domain.Merchant;
import com.nongjiale.domain.MerchantUser;
import com.nongjiale.repository.MerchantRepository;
import com.nongjiale.repository.MerchantUserRepository;
import com.nongjiale.security.AuthPrincipal;
import com.nongjiale.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 商家接口只信 token 上的 merchantId。查询串里的 merchantId 与 token 不一致 → 403。
 * mustChangePassword 时，除 me / 改密外一律 403。
 */
@Service
public class MerchantAccess {
    private final MerchantUserRepository users;
    private final MerchantRepository merchants;

    public MerchantAccess(MerchantUserRepository users, MerchantRepository merchants) {
        this.users = users;
        this.merchants = merchants;
    }

    public record Context(AuthPrincipal principal, MerchantUser user, Merchant merchant) {}

    public Context require(Authentication auth, HttpServletRequest request, boolean allowWhileMustChangePassword) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p) || !p.isMerchant()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权限");
        }
        rejectForeignMerchantId(request, p.merchantId());
        MerchantUser user = users.findById(p.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, 401, "未登录"));
        if (!user.getMerchantId().equals(p.merchantId())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "未登录");
        }
        Merchant merchant = merchants.findById(user.getMerchantId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, 401, "未登录"));
        if (!user.isEnabled() || !merchant.isEnabled()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "账号已停用，请联系运营");
        }
        if (!allowWhileMustChangePassword && user.isMustChangePassword()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "请先修改初始密码");
        }
        return new Context(p, user, merchant);
    }

    public static void rejectForeignMerchantId(HttpServletRequest request, Long tokenMerchantId) {
        if (request == null) return;
        String raw = request.getParameter("merchantId");
        if (raw == null || raw.isBlank()) return;
        long claimed;
        try {
            claimed = Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "merchantId 不合法");
        }
        if (!Long.valueOf(claimed).equals(tokenMerchantId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权限");
        }
    }
}
