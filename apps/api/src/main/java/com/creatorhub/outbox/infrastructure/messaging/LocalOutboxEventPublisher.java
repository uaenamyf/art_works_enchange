package com.creatorhub.outbox.infrastructure.messaging;

import com.creatorhub.outbox.application.port.out.OutboxEventHandler;
import com.creatorhub.outbox.application.port.out.OutboxEventPublisher;
import com.creatorhub.outbox.domain.model.OutboxEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 本地事件发布适配器；在消息总线暂停期间只输出脱敏事件元数据，不伪造下游业务处理成功。 */
@Component
public class LocalOutboxEventPublisher implements OutboxEventPublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalOutboxEventPublisher.class);

  private final List<OutboxEventHandler> handlers;

  public LocalOutboxEventPublisher() {
    this(List.of());
  }

  @Autowired
  public LocalOutboxEventPublisher(List<OutboxEventHandler> handlers) {
    this.handlers = handlers;
  }

  @Override
  public void publish(OutboxEvent event) {
    handlers.stream()
        .filter(handler -> handler.supports(event.eventType(), event.eventVersion()))
        .forEach(handler -> handler.handle(event));
    LOGGER.info(
        "Outbox event ready for local consumer: eventId={}, eventType={}, aggregateType={}, aggregateId={}",
        event.eventId(),
        event.eventType(),
        event.aggregateType(),
        event.aggregateId());
  }
}
