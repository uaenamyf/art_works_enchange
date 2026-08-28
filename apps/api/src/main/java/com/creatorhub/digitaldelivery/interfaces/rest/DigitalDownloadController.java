package com.creatorhub.digitaldelivery.interfaces.rest;

import com.creatorhub.digitaldelivery.application.service.DigitalDownloadService;
import com.creatorhub.digitaldelivery.application.service.DigitalDownloadService.DownloadView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 数字下载授权 HTTP 适配器；只返回短期地址，不返回长期 Token 或对象存储 key。 */
@RestController
@RequestMapping("/api/v1/orders")
public class DigitalDownloadController {

  private final DigitalDownloadService downloads;

  public DigitalDownloadController(DigitalDownloadService downloads) {
    this.downloads = downloads;
  }

  @PostMapping("/{orderId}/download-grants")
  @PreAuthorize("hasRole('USER') or hasRole('CREATOR')")
  ResponseEntity<DownloadView> createGrant(
      @AuthenticationPrincipal UUID buyerId,
      @PathVariable UUID orderId,
      HttpServletRequest request) {
    return ResponseEntity.ok(
        downloads.createGrant(
            buyerId, orderId, request.getRemoteAddr(), request.getHeader("User-Agent")));
  }
}
