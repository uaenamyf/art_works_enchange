package com.creatorhub.sharedkernel.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * 聚合根基类。
 *
 * <p>约定：
 * <ul>
 *   <li>聚合根负责不变量并暴露业务行为，不使用公共 setter 构造贫血模型。</li>
 *   <li>每个聚合在一次事务中修改；跨上下文副作用通过领域事件 + Outbox 投递。</li>
 *   <li>{@link #id()} 一旦确定不可变。</li>
 * </ul>
 */
public abstract class AggregateRoot<ID> {

    private final ID id;
    private long version;

    protected AggregateRoot(ID id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public ID id() {
        return id;
    }

    public long version() {
        return version;
    }

    /** 仓储或 ORM 加载时回放版本号，事件溯源场景可覆写。 */
    protected void loadVersion(long version) {
        this.version = version;
    }

    /** 由仓储在保存前调用，用于事件 Outbox 排序与乐观锁。 */
    protected void bumpVersion() {
        this.version = this.version + 1;
    }
}
