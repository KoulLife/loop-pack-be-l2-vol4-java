package com.loopers.infrastructure.metrics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetricsEntity, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, view_count, sales_count, like_count, updated_at)
        VALUES (:productId, :viewDelta, :salesDelta, :likeDelta, NOW())
        ON DUPLICATE KEY UPDATE
            view_count  = view_count  + :viewDelta,
            sales_count = sales_count + :salesDelta,
            like_count  = like_count  + :likeDelta,
            updated_at  = NOW()
        """, nativeQuery = true)
    void upsert(
        @Param("productId") Long productId,
        @Param("viewDelta") long viewDelta,
        @Param("salesDelta") long salesDelta,
        @Param("likeDelta") long likeDelta
    );
}
