package com.creatorhub.catalog.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 数字资产显式 SQL Mapper；所有扫描状态转换都带前置状态条件。 */
@Mapper
interface DigitalAssetMapper {

  @Insert(
      "INSERT INTO digital_assets (id, artwork_id, owner_id, status, created_at, updated_at) SELECT #{assetId}, id, owner_id, 'DRAFT', #{createdAt}, #{createdAt} FROM artworks WHERE id = #{artworkId} AND owner_id = #{ownerId} AND product_type = 'DIGITAL'")
  int insertAssetIfAbsent(UUID assetId, UUID artworkId, UUID ownerId, Instant createdAt);

  @Select("SELECT id FROM digital_assets WHERE artwork_id = #{artworkId}")
  UUID findAssetIdByArtwork(UUID artworkId);

  @Select(
      "SELECT COALESCE(MAX(version), 0) + 1 FROM digital_asset_versions WHERE digital_asset_id = #{assetId}")
  int nextVersion(UUID assetId);

  @Insert(
      "INSERT INTO digital_asset_versions (id, digital_asset_id, version, file_name, object_key, content_type, size_bytes, sha256, scan_status, created_at) VALUES (#{id}, #{digitalAssetId}, #{version}, #{fileName}, #{objectKey}, #{contentType}, #{sizeBytes}, #{sha256}, #{scanStatus}, #{createdAt})")
  int insertVersion(DigitalAssetPersistenceRow row);

  @Select("SELECT * FROM digital_asset_versions WHERE id = #{versionId}")
  DigitalAssetPersistenceRow findVersion(UUID versionId);

  @Update(
      "UPDATE digital_asset_versions SET scan_status = 'SCANNING' WHERE id = #{versionId} AND scan_status = 'PENDING_SCAN'")
  int markScanning(UUID versionId);

  @Update(
      "UPDATE digital_asset_versions SET scan_status = 'READY', scanned_at = #{scannedAt}, scan_error = NULL WHERE id = #{versionId} AND scan_status = 'SCANNING'")
  int markReady(UUID versionId, Instant scannedAt);

  @Update(
      "UPDATE digital_asset_versions SET scan_status = 'QUARANTINED', scanned_at = #{scannedAt}, scan_error = #{reason} WHERE id = #{versionId} AND scan_status IN ('PENDING_SCAN', 'SCANNING')")
  int quarantine(UUID versionId, String reason, Instant scannedAt);

  @Update(
      "UPDATE digital_assets SET status = 'READY', current_version = #{version} WHERE id = #{assetId} AND status = 'DRAFT'")
  int markAssetReady(UUID assetId, int version);
}
