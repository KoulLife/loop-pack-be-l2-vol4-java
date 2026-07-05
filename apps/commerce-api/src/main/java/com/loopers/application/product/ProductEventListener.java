package com.loopers.application.product;

import com.loopers.domain.product.ProductViewedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ProductEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductViewedEvent event) {
        log.info("[상품 조회] productId={} 조회됨", event.productId());
    }
}
