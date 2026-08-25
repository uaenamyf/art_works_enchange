package com.creatorhub.architecture;

import com.creatorhub.sharedkernel.domain.Money;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SharedKernelSmokeTest {

    @Test
    void moneyScalesByCurrencyDefaultFractionDigits() {
        Money usd = new Money(new BigDecimal("9.99"), Currency.getInstance("USD"));
        assertThat(usd.amount().toPlainString()).isEqualTo("9.99");
    }

    @Test
    void moneyRejectsMismatchedCurrency() {
        Money usd = new Money(BigDecimal.ONE, Currency.getInstance("USD"));
        Money eur = new Money(BigDecimal.ONE, Currency.getInstance("EUR"));
        try {
            usd.plus(eur);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).contains("Currency mismatch");
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }
}
