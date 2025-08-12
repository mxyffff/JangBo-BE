package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.Category;
import me.swudam.jangbo.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    // 필요 시 나중에 추가. 예시일 뿐 수정/삭제 가능
    List<Store> findByStoreName(String name); // 상점명으로 검색
    List<Store> findByCategory(Category category); // 카테고리로 검색
}
