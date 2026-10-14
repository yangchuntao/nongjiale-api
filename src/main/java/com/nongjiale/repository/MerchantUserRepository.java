package com.nongjiale.repository;

import com.nongjiale.domain.MerchantUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantUserRepository extends JpaRepository<MerchantUser, Long> {
    Optional<MerchantUser> findByPhone(String phone);
    Optional<MerchantUser> findFirstByMerchantIdOrderByIdAsc(Long merchantId);
    List<MerchantUser> findByMerchantId(Long merchantId);
}
