package com.creatorhub.digitaldelivery.infrastructure.persistence;

import com.creatorhub.digitaldelivery.application.port.out.DigitalDownloadAsset;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 授权显式 SQL Mapper；订单唯一约束保证重复支付事件不会重复建权。 */
@Mapper
interface EntitlementMapper {

  @Insert(
      "INSERT INTO digital_entitlements (id, order_id, buyer_id, artwork_id, asset_version_id, status, created_at, updated_at) VALUES (#{id}, #{orderId}, #{buyerId}, #{artworkId}, #{assetVersionId}, #{status}, #{createdAt}, #{updatedAt}) ON CONFLICT (order_id) DO NOTHING")
  int insert(EntitlementPersistenceRow row);

  @Select("SELECT * FROM digital_entitlements WHERE order_id = #{orderId}")
  EntitlementPersistenceRow findByOrderId(UUID orderId);

  @Select(
      "SELECT * FROM digital_entitlements WHERE buyer_id = #{buyerId} AND order_id = #{orderId} AND status = 'AVAILABLE'")
  EntitlementPersistenceRow findAvailableByBuyer(UUID buyerId, UUID orderId);

  @Select(
      "SELECT de.id AS entitlement_id, dav.id AS asset_version_id, dav.file_name, dav.object_key, dav.content_type, dav.size_bytes, dav.version FROM digital_entitlements de JOIN digital_asset_versions dav ON dav.id = de.asset_version_id WHERE de.buyer_id = #{buyerId} AND de.order_id = #{orderId} AND de.status = 'AVAILABLE' AND dav.scan_status = 'READY'")
  DigitalDownloadAsset findDownloadAsset(UUID buyerId, UUID orderId);

  @Select(
      "SELECT 1 FROM digital_asset_versions WHERE id = #{assetVersionId} AND scan_status = 'READY'")
  Integer isReadyAssetVersion(UUID assetVersionId);
}
