package com.creatorhub.webhook.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** 验证原始 Webhook 载荷的签名成功、格式兼容和失败即拒绝边界。 */
class HmacWebhookVerifierTest {

  private static final String SECRET = "webhook-test-secret";
  private static final byte[] PAYLOAD =
      "{\"event\":\"payment.succeeded\"}".getBytes(StandardCharsets.UTF_8);

  @Test
  void acceptsProviderPrefixedSignature() {
    String signature = HmacWebhookVerifier.signatureFor(PAYLOAD, SECRET);

    HmacWebhookVerifier.verify(PAYLOAD, signature, SECRET);
  }

  @Test
  void acceptsRawHexSignature() {
    String signature =
        HmacWebhookVerifier.signatureFor(PAYLOAD, SECRET).substring("sha256=".length());

    HmacWebhookVerifier.verify(PAYLOAD, signature, SECRET);
  }

  @Test
  void rejectsChangedPayload() {
    String signature = HmacWebhookVerifier.signatureFor(PAYLOAD, SECRET);

    assertThatThrownBy(
            () ->
                HmacWebhookVerifier.verify(
                    "changed".getBytes(StandardCharsets.UTF_8), signature, SECRET))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("signature is invalid");
  }

  @Test
  void rejectsMalformedSignatureAndMissingSecret() {
    assertThatThrownBy(() -> HmacWebhookVerifier.verify(PAYLOAD, "not-hex", SECRET))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("signature is invalid");
    assertThatThrownBy(() -> HmacWebhookVerifier.verify(PAYLOAD, "sha256=00", " "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("secret is required");
  }
}
