package com.creatorhub.sharedkernel;

/**
 * Shared Kernel 公共标记包。
 *
 * <p>只放稳定领域概念：{@code Money}、{@code DomainEvent}、{@code AggregateRoot}、强类型 ID、
 * 时间抽象、Problem Details 基础类型。技术工具类、Web 错误格式与 Spring 配置不属于 Shared Kernel。
 */
final class SharedKernelMarker {
    private SharedKernelMarker() {
    }
}
