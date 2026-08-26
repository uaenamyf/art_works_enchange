package com.creatorhub.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.creatorhub.catalog.application.port.out.MediaObjectStore;
import com.creatorhub.catalog.application.port.out.MediaStoragePort;
import com.creatorhub.sharedkernel.error.BusinessError;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证公开媒体只有在绑定到已发布作品后才能获得短期读取地址。 */
class MediaReadServiceTest {

  @Test
  void returnsShortLivedUrlForPublishedMedia() {
    UUID mediaId = UUID.randomUUID();
    RecordingObjects objects = new RecordingObjects(mediaId);
    RecordingStorage storage = new RecordingStorage();
    MediaReadService service = new MediaReadService(objects, storage, 300);

    var result = service.getPublished(mediaId);

    assertThat(result.id()).isEqualTo(mediaId);
    assertThat(result.url()).isEqualTo("http://localhost/media/read");
    assertThat(result.contentType()).isEqualTo("image/webp");
    assertThat(result.expiresAt()).isAfter(Instant.now());
  }

  @Test
  void hidesMediaThatIsNotPublished() {
    RecordingObjects objects = new RecordingObjects(null);
    MediaReadService service = new MediaReadService(objects, new RecordingStorage(), 300);

    assertThatThrownBy(() -> service.getPublished(UUID.randomUUID()))
        .isInstanceOf(BusinessError.class)
        .extracting(error -> ((BusinessError) error).code())
        .isEqualTo("MEDIA_NOT_FOUND");
  }

  private static final class RecordingObjects implements MediaObjectStore {
    private final UUID publishedMediaId;

    private RecordingObjects(UUID publishedMediaId) {
      this.publishedMediaId = publishedMediaId;
    }

    @Override
    public void createPending(
        UUID mediaId,
        UUID ownerId,
        String objectKey,
        String contentType,
        long sizeBytes,
        Instant expiresAt,
        Instant createdAt) {}

    @Override
    public String objectKeyForReadyCandidate(UUID mediaId, UUID ownerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean markReady(UUID mediaId, UUID ownerId, Instant uploadedAt) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<PublicMedia> findPublished(UUID mediaId) {
      return Optional.ofNullable(publishedMediaId)
          .filter(mediaId::equals)
          .map(id -> new PublicMedia(id, "creator/media.webp", "image/webp"));
    }
  }

  private static final class RecordingStorage implements MediaStoragePort {
    @Override
    public PresignedUpload presign(
        UUID mediaId,
        String objectKey,
        com.creatorhub.catalog.domain.model.MediaUploadRequest request,
        Instant expiresAt) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String objectKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String presignGet(String objectKey, String contentType, Instant expiresAt) {
      return "http://localhost/media/read";
    }
  }
}
