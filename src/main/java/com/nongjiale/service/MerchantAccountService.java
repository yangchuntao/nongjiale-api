package com.nongjiale.service;

import com.nongjiale.domain.Merchant;
import com.nongjiale.domain.MerchantUser;
import com.nongjiale.repository.HomestayRepository;
import com.nongjiale.repository.MerchantRepository;
import com.nongjiale.repository.MerchantUserRepository;
import com.nongjiale.security.AuthPrincipal;
import com.nongjiale.security.SessionTokens;
import com.nongjiale.web.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MerchantAccountService {
    private final MerchantUserRepository users;
    private final MerchantRepository merchants;
    private final HomestayRepository homestays;
    private final PasswordEncoder encoder;
    private final SessionTokens tokens;

    public MerchantAccountService(MerchantUserRepository users, MerchantRepository merchants,
                                  HomestayRepository homestays, PasswordEncoder encoder, SessionTokens tokens) {
        this.users = users;
        this.merchants = merchants;
        this.homestays = homestays;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    public Map<String, Object> login(String phone, String password) {
        // 入驻未通过时没有 merchant_user，与密码错误一样 401，不暴露申请是否存在。
        MerchantUser user = users.findByPhone(phone).orElse(null);
        if (user == null || !encoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "手机号或密码错误");
        }
        Merchant merchant = merchants.findById(user.getMerchantId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, 401, "手机号或密码错误"));
        if (!user.isEnabled() || !merchant.isEnabled()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "账号已停用，请联系运营");
        }
        String token = tokens.issue(new AuthPrincipal(
                user.getId(), user.getPhone(), "MERCHANT", "MERCHANT", merchant.getId()));
        Map<String, Object> data = profile(user, merchant);
        data.put("token", token);
        return data;
    }

    public Map<String, Object> profile(MerchantUser user, Merchant merchant) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("merchantUserId", user.getId());
        data.put("merchantId", merchant.getId());
        data.put("subjectName", merchant.getSubjectName());
        data.put("phone", user.getPhone());
        data.put("role", "MERCHANT");
        data.put("mustChangePassword", user.isMustChangePassword());
        return data;
    }

    @Transactional
    public Map<String, Object> changePassword(MerchantUser user, Merchant merchant, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "新密码至少 6 位");
        }
        if (!encoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "原密码不正确");
        }
        user.setPasswordHash(encoder.encode(newPassword));
        user.setMustChangePassword(false);
        users.save(user);
        return profile(user, merchant);
    }

    public Page<Map<String, Object>> adminList(int page, int pageSize) {
        return merchants.findAll(PageRequests.of(page, pageSize)).map(m -> {
            MerchantUser user = users.findFirstByMerchantIdOrderByIdAsc(m.getId()).orElse(null);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("merchantId", m.getId());
            row.put("subjectName", m.getSubjectName());
            row.put("loginPhone", user == null ? "" : user.getPhone());
            row.put("enabled", m.isEnabled());
            row.put("storeCount", homestays.countByMerchantId(m.getId()));
            return row;
        });
    }

    @Transactional
    public Map<String, Object> setEnabled(Long merchantId, boolean enabled) {
        Merchant merchant = merchants.findById(merchantId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "商家不存在"));
        merchant.setEnabled(enabled);
        for (MerchantUser user : users.findByMerchantId(merchantId)) {
            user.setEnabled(enabled);
        }
        MerchantUser login = users.findFirstByMerchantIdOrderByIdAsc(merchantId).orElse(null);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("merchantId", merchant.getId());
        row.put("subjectName", merchant.getSubjectName());
        row.put("loginPhone", login == null ? "" : login.getPhone());
        row.put("enabled", merchant.isEnabled());
        row.put("storeCount", homestays.countByMerchantId(merchant.getId()));
        return row;
    }

    @Transactional
    public Map<String, Object> resetPassword(Long merchantId) {
        Merchant merchant = merchants.findById(merchantId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "商家不存在"));
        MerchantUser user = users.findFirstByMerchantIdOrderByIdAsc(merchantId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, 404, "商家账号不存在"));
        String initial = Passwords.initial();
        user.setPasswordHash(encoder.encode(initial));
        user.setMustChangePassword(true);
        users.save(user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("merchantId", merchant.getId());
        data.put("loginPhone", user.getPhone());
        data.put("initialPassword", initial);
        data.put("mustChangePassword", true);
        return data;
    }
}
