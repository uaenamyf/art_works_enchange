package com.creatorhub.outbox.application.port.out;

import com.creatorhub.outbox.domain.model.OutboxEvent;

/** Outbox 下游事件处理端口；处理失败必须抛出异常交给 Worker 重试。 */
public interface OutboxEventHandler {

  boolean supports(String eventType, int eventVersion);

  void handle(OutboxEvent event);
}
