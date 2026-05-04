package com.vimainsurance.vimaadmin.messaging;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "aws.sqs.mail-queue.listener-enabled", havingValue = "true")
public class VimaMailQueueListener {

    @SqsListener("${aws.sqs.mail-queue}")
    public void onMessage(String payload) {
        log.debug("Vima-Mail-Queue payload length={}", payload != null ? payload.length() : 0);
    }
}
