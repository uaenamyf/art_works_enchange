package com.creatorhub.digitaldelivery.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 下载凭证显式 SQL Mapper；消费操作带未使用和未过期条件，避免重复核销。 */
@Mapper
interface DownloadGrantMapper {

  @Insert(
      "INSERT INTO digital_download_grants (id, entitlement_id, token_digest, expires_at, request_ip_hash, user_agent_hash, created_at) VALUES (#{id}, #{entitlementId}, #{tokenDigest}, #{expiresAt}, #{requestIpHash}, #{userAgentHash}, #{createdAt})")
  int insert(DownloadGrantPersistenceRow row);

  @Select("SELECT * FROM digital_download_grants WHERE id = #{grantId}")
  DownloadGrantPersistenceRow findById(UUID grantId);

  @Update(
      "UPDATE digital_download_grants SET used_at = #{usedAt} WHERE id = #{grantId} AND used_at IS NULL AND revoked_at IS NULL AND expires_at > #{usedAt}")
  int markUsed(UUID grantId, Instant usedAt);

  @Update(
      "UPDATE digital_download_grants SET revoked_at = #{revokedAt} WHERE entitlement_id IN (SELECT id FROM digital_entitlements WHERE asset_version_id = #{assetVersionId}) AND used_at IS NULL AND revoked_at IS NULL")
  int revokeByAssetVersion(
      @org.apache.ibatis.annotations.Param("assetVersionId") UUID assetVersionId,
      @org.apache.ibatis.annotations.Param("revokedAt") Instant revokedAt);
}
