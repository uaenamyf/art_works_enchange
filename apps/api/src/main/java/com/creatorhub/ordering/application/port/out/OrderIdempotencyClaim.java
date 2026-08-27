package com.creatorhub.ordering.application.port.out;

import com.creatorhub.ordering.application.port.in.OrderUseCases.OrderView;

/** 订单幂等键占位结果；占位期间不允许第二个请求重复执行库存操作。 */
public record OrderIdempotencyClaim(boolean acquired, String requestHash, OrderView response) {}
