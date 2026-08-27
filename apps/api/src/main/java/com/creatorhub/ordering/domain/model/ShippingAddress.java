package com.creatorhub.ordering.domain.model;

import java.util.Objects;

/** 下单时冻结的收货地址快照，避免用户后续修改资料影响历史订单。 */
public record ShippingAddress(
    String recipientName,
    String line1,
    String line2,
    String city,
    String region,
    String postalCode,
    String countryCode,
    String phone) {

  public ShippingAddress {
    recipientName = required(recipientName, "recipientName", 100);
    line1 = required(line1, "line1", 200);
    line2 = optional(line2, "line2", 200);
    city = required(city, "city", 100);
    region = optional(region, "region", 100);
    postalCode = required(postalCode, "postalCode", 20);
    countryCode = required(countryCode, "countryCode", 2);
    if (!countryCode.matches("^[A-Z]{2}$")) {
      throw new IllegalArgumentException("countryCode is invalid");
    }
    phone = required(phone, "phone", 30);
    if (phone.length() < 6) {
      throw new IllegalArgumentException("phone is invalid");
    }
  }

  private static String required(String value, String name, int maxLength) {
    String normalized = Objects.requireNonNull(value, name).trim();
    if (normalized.isEmpty() || normalized.length() > maxLength) {
      throw new IllegalArgumentException(name + " is invalid");
    }
    return normalized;
  }

  private static String optional(String value, String name, int maxLength) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() > maxLength) {
      throw new IllegalArgumentException(name + " is invalid");
    }
    return normalized;
  }
}
