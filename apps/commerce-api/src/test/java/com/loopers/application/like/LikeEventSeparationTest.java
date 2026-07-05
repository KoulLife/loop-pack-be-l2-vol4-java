package com.loopers.application.like;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.product.ProductModel;
import com.loopers.infrastructure.brand.BrandEntity;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.like.LikeJpaRepository;
import com.loopers.infrastructure.product.ProductEntity;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.product.ProductRepositoryImpl;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Tag("integration")
@SpringBootTest
class LikeEventSeparationTest {

    @Autowired private LikeApplicationService likeApplicationService;
    @Autowired private BrandJpaRepository brandJpaRepository;
    @Autowired private ProductJpaRepository productJpaRepository;
    @Autowired private LikeJpaRepository likeJpaRepository;
    @SpyBean private ProductRepositoryImpl productRepository;
    @Autowired private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private BrandEntity savedBrand() {
        return brandJpaRepository.save(BrandEntity.from(new BrandModel("나이키", "스포츠 브랜드")));
    }

    private ProductEntity savedProduct(BrandEntity brand) {
        return productJpaRepository.save(ProductEntity.from(
            new ProductModel(brand.getId(), "에어맥스", "운동화", 100_000L, 10),
            brand
        ));
    }

    @DisplayName("AFTER_COMMIT 리스너 실행 검증")
    @Nested
    class AfterCommitListener {

        @DisplayName("좋아요 성공 시 AFTER_COMMIT 리스너가 실행된다.")
        @Test
        void incrementLikeCount_calledOnce_afterSuccessfulLike() {
            // Arrange
            BrandEntity brand = savedBrand();
            ProductEntity product = savedProduct(brand);

            // Act
            likeApplicationService.like(1L, product.getId());

            // Assert
            verify(productRepository, times(1)).incrementLikeCount(product.getId());
            assertThat(productJpaRepository.findById(product.getId()).orElseThrow()
                .toDomain().getLikeCount()).isEqualTo(1L);
        }

        @DisplayName("좋아요 취소 성공 시 AFTER_COMMIT 리스너가 실행된다.")
        @Test
        void decrementLikeCount_calledOnce_afterSuccessfulUnlike() {
            // Arrange
            BrandEntity brand = savedBrand();
            ProductEntity product = savedProduct(brand);
            likeApplicationService.like(1L, product.getId());
            reset(productRepository);

            // Act
            likeApplicationService.unlike(1L, product.getId());

            // Assert
            verify(productRepository, times(1)).decrementLikeCount(product.getId());
            assertThat(productJpaRepository.findById(product.getId()).orElseThrow()
                .toDomain().getLikeCount()).isEqualTo(0L);
        }

        @DisplayName("트랜잭션 롤백 시 AFTER_COMMIT 리스너가 실행되지 않는다.")
        @Test
        void incrementLikeCount_neverCalled_whenTransactionRollsBack() {
            // Arrange: 존재하지 않는 상품 → CoreException → 롤백
            // Act
            assertThatThrownBy(() -> likeApplicationService.like(1L, 999L))
                .isInstanceOf(CoreException.class);

            // Assert: 리스너 미실행
            verify(productRepository, never()).incrementLikeCount(anyLong());
        }
    }

    @DisplayName("이벤트 분리 — 집계 실패와 좋아요 저장 독립성")
    @Nested
    class EventSeparation {

        @DisplayName("집계 리스너가 실패해도 좋아요 행은 커밋되고 like()는 정상 반환된다.")
        @Test
        void likeSave_alreadyCommitted_evenWhenListenerFails() {
            // Arrange
            BrandEntity brand = savedBrand();
            ProductEntity product = savedProduct(brand);
            doThrow(new RuntimeException("집계 실패")).when(productRepository).incrementLikeCount(anyLong());

            // Act: Spring이 afterCompletion 예외를 삼키므로 like()는 예외 없이 성공
            likeApplicationService.like(1L, product.getId());

            // Assert: 핵심 트랜잭션(좋아요 저장)은 커밋됨
            assertThat(likeJpaRepository.existsByUserIdAndProductId(1L, product.getId())).isTrue();
            // 집계는 실패했으므로 likeCount는 0
            assertThat(productJpaRepository.findById(product.getId()).orElseThrow()
                .toDomain().getLikeCount()).isEqualTo(0L);
        }
    }
}
