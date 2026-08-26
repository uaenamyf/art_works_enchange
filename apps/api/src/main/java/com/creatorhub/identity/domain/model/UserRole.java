package com.creatorhub.identity.domain.model;

/**
 * 用户角色稳定枚举，用于 JWT 身份声明和授权决策。
 *
 * <p>{@code USER} 是默认注册身份，{@code CREATOR} 表示已完成创作者入驻， {@code ADMIN} 为平台管理身份。
 */
public enum UserRole {
  USER,
  CREATOR,
  ADMIN
}
