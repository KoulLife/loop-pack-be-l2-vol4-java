package com.loopers.application.like;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.loopers.domain.like.LikeChangedEvent;
import com.loopers.domain.product.ProductRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class LikeEventListener {

	private final ProductRepository productRepository;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handle(LikeChangedEvent event) {
		if (event.type() == LikeChangedEvent.Type.LIKED) {
			productRepository.incrementLikeCount(event.productId());
		}

		if (event.type() == LikeChangedEvent.Type.UNLIKED) {
			productRepository.decrementLikeCount(event.productId());
		}
	}
}
