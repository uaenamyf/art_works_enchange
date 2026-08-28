package com.creatorhub.notification.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.creatorhub.notification.application.port.in.NotificationUseCases;
import com.creatorhub.notification.domain.repository.NotificationRepository;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证通知应用服务的游标校验、分页边界和当前用户已读权限错误。 */
class NotificationServiceTest {

  @Test
  void rejectsMalformedCursorBeforeCallingRepository() {
    NotificationRepository repository = mock(NotificationRepository.class);
    NotificationService service = new NotificationService(repository);

    assertThatThrownBy(() -> service.list(UUID.randomUUID(), "not-a-cursor", 20, false))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("INVALID_CURSOR");
    verify(repository, never())
        .findByUser(any(), any(Boolean.class), any(), any(), any(Integer.class));
  }

  @Test
  void capsPageSizeAndReturnsUnreadCount() {
    NotificationRepository repository = mock(NotificationRepository.class);
    UUID userId = UUID.randomUUID();
    when(repository.findByUser(userId, true, null, null, 51)).thenReturn(List.of());
    when(repository.countUnread(userId)).thenReturn(3);

    NotificationUseCases.NotificationPage page =
        new NotificationService(repository).list(userId, null, 100, true);

    verify(repository).findByUser(userId, true, null, null, 51);
    verify(repository).countUnread(userId);
    org.assertj.core.api.Assertions.assertThat(page.unreadCount()).isEqualTo(3);
    org.assertj.core.api.Assertions.assertThat(page.hasMore()).isFalse();
  }

  @Test
  void mapsMissingOwnedNotificationToStableNotFoundError() {
    NotificationRepository repository = mock(NotificationRepository.class);
    when(repository.markRead(any(), any(), any(Instant.class))).thenReturn(false);

    assertThatThrownBy(
            () ->
                new NotificationService(repository).markRead(UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("NOTIFICATION_NOT_FOUND");
  }
}
