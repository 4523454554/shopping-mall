package com.jay.shoppingmall.domain.seller;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findByUserIdAndIsActivatedTrue(Long userId);

    boolean existsByCompanyName(String companyName);

//    Optional<Seller> findByItemId(Long itemId);
}
