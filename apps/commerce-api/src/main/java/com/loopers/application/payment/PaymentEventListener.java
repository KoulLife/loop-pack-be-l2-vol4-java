package com.loopers.application.payment;

import com.loopers.domain.payment.PaymentFailedEvent;
import com.loopers.domain.payment.PaymentSucceededEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class PaymentEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSuccess(PaymentSucceededEvent event) {
        log.info("[결제] userId={} orderId={} amount={} 결제 완료", event.userId(), event.orderId(), event.amount());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFailed(PaymentFailedEvent event) {
        log.info("[결제] userId={} orderId={} reason={} 결제 실패", event.userId(), event.orderId(), event.reason());
    }
}
