package me.swudam.jangbo.repository;

import me.swudam.jangbo.entity.Category;
import me.swudam.jangbo.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
