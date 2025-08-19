package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    // createdAt 기준 내림차순 (최신순)
    List<Store> findAllByOrderByCreatedAtDesc();

    Optional<Store> findByMerchantId(Long merchantId);   // 추가
    boolean existsByMerchantId(Long merchantId);         // 추가
}
