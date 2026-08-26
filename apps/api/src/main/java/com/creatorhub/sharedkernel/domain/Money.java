package com.creatorhub.sharedkernel.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * 金额值对象。
 *
 * <p>金额在领域内统一使用 {@code BigDecimal}，避免浮点误差；币种与最小货币单位不可变。 跨上下文传递必须以值对象形式，禁止直接透传 PersistenceRow/DO
 * 或基本类型。
 */
public final class Money {

  public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;

  private final BigDecimal amount;
  private final Currency currency;

  public Money(BigDecimal amount, Currency currency) {
    this.amount = scaleAmount(Objects.requireNonNull(amount, "amount"), currency);
    this.currency = Objects.requireNonNull(currency, "currency");
  }

  public static Money of(String amount, String currencyCode) {
    return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
  }

  public static Money zero(Currency currency) {
    return new Money(BigDecimal.ZERO, currency);
  }

  public BigDecimal amount() {
    return amount;
  }

  public Currency currency() {
    return currency;
  }

  public Money plus(Money other) {
    requireSameCurrency(other);
    return new Money(this.amount.add(other.amount), this.currency);
  }

  public Money minus(Money other) {
    requireSameCurrency(other);
    return new Money(this.amount.subtract(other.amount), this.currency);
  }

  public boolean isPositive() {
    return amount.signum() > 0;
  }

  public boolean isZeroOrNegative() {
    return amount.signum() <= 0;
  }

  private void requireSameCurrency(Money other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          "Currency mismatch: " + this.currency + " vs " + other.currency);
    }
  }

  private static BigDecimal scaleAmount(BigDecimal amount, Currency currency) {
    int scale = currency.getDefaultFractionDigits();
    return amount.setScale(scale, DEFAULT_ROUNDING);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Money money)) {
      return false;
    }
    return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount.stripTrailingZeros(), currency);
  }

  @Override
  public String toString() {
    return amount.toPlainString() + " " + currency.getCurrencyCode();
  }
}
