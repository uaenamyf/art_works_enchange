package com.creatorhub.outbox.application.port.out;

import com.creatorhub.outbox.domain.model.OutboxEvent;

/** Outbox 事件发布端口；下游通过集成事件消费，不能把投递实现绑死到消息中间件。 */
public interface OutboxEventPublisher {

  void publish(OutboxEvent event);
}
