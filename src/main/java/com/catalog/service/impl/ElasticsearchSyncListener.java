package com.catalog.service.impl;

import com.catalog.domain.event.ProductChangedEvent;
import com.catalog.domain.model.ProductDocument;
import com.catalog.repository.elasticsearch.ProductElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSyncListener {

    private final ProductElasticsearchRepository esRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductChanged(ProductChangedEvent event) {
        log.debug("ES sync: type={}, id={}", event.getType(), event.getProductId());
        try {
            switch (event.getType()) {
                case CREATED, UPDATED -> esRepository.save(ProductDocument.from(event.getProduct()));
                case DELETED          -> esRepository.deleteById(event.getProductId().toString());
            }
        } catch (Exception e) {
            log.error("ES sync failed for id={}: {}", event.getProductId(), e.getMessage(), e);
        }
    }
}
