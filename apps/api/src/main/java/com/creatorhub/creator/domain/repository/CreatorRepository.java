package com.creatorhub.creator.domain.repository;

import com.creatorhub.creator.domain.model.Creator;
import com.creatorhub.creator.domain.model.CreatorId;
import com.creatorhub.sharedkernel.UserId;
import java.util.Optional;

/** 以 Creator 聚合为单位定义的领域仓储端口。 */
public interface CreatorRepository {

  boolean existsByUserId(UserId userId);

  boolean existsBySlug(String slug);

  Optional<Creator> findById(CreatorId id);

  Optional<Creator> findByUserId(UserId userId);

  Creator save(Creator creator);
}
