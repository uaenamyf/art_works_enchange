package com.creatorhub.catalog.interfaces.rest;

import com.creatorhub.catalog.application.port.in.MediaReadUseCases;
import com.creatorhub.catalog.application.port.in.MediaReadUseCases.MediaView;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 公开媒体 URL 适配器；媒体是否属于已发布作品由应用服务和 SQL 共同判断。 */
@RestController
@RequestMapping("/api/v1/media")
public class PublicMediaController {

  private final MediaReadUseCases media;

  public PublicMediaController(MediaReadUseCases media) {
    this.media = media;
  }

  /** 返回短期 GET URL，客户端不得将该 URL 当作永久资源地址保存。 */
  @GetMapping("/{mediaId}")
  ResponseEntity<MediaView> getPublished(@PathVariable UUID mediaId) {
    return ResponseEntity.ok(media.getPublished(mediaId));
  }
}
