package com.creatorhub.creator.domain.repository;

import com.creatorhub.creator.domain.model.Creator;
import com.creatorhub.creator.domain.model.CreatorId;
import com.creatorhub.sharedkernel.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** 以 Creator 聚合为单位定义的领域仓储端口。 */
public interface CreatorRepository {

  boolean existsByUserId(UserId userId);

  boolean existsBySlug(String slug);

  Optional<Creator> findById(CreatorId id);

  Optional<Creator> findByUserId(UserId userId);

  List<Creator> findPage(PageQuery query);

  record PageQuery(String category, Instant beforeCreatedAt, CreatorId beforeId, int limit) {}

  Creator save(Creator creator);
}
