package com.creatorhub.creator.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.creatorhub.creator.domain.model.Creator;
import com.creatorhub.creator.domain.model.CreatorId;
import com.creatorhub.creator.domain.repository.CreatorRepository;
import com.creatorhub.sharedkernel.UserId;
import com.creatorhub.sharedkernel.domain.AppClock;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Creator 聚合仓储适配器，显式转换领域模型和 MyBatis 持久化行。 */
@Repository
public class CreatorRepositoryAdapter implements CreatorRepository {

  private final CreatorMapper mapper;

  public CreatorRepositoryAdapter(CreatorMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean existsByUserId(UserId userId) {
    return mapper.selectCount(
            new QueryWrapper<CreatorPersistenceRow>().eq("user_id", userId.value()))
        > 0;
  }

  @Override
  public boolean existsBySlug(String slug) {
    return mapper.selectCount(new QueryWrapper<CreatorPersistenceRow>().eq("slug", slug)) > 0;
  }

  @Override
  public Optional<Creator> findById(CreatorId id) {
    return Optional.ofNullable(mapper.findByIdValue(id.value()))
        .map(CreatorRepositoryAdapter::toDomain);
  }

  @Override
  public Optional<Creator> findByUserId(UserId userId) {
    return Optional.ofNullable(
            mapper.selectOne(
                new QueryWrapper<CreatorPersistenceRow>()
                    .eq("user_id", userId.value())
                    .last("LIMIT 1")))
        .map(CreatorRepositoryAdapter::toDomain);
  }

  @Override
  public Creator save(Creator creator) {
    CreatorPersistenceRow row = mapper.findByIdValue(creator.id().value());
    boolean inserting = row == null;
    if (inserting) {
      row = new CreatorPersistenceRow();
      row.setId(creator.id().value());
      row.setVersion(0);
    }
    row.setUserId(creator.userId().value());
    row.setSlug(creator.slug());
    row.setDisplayName(creator.displayName());
    row.setCategory(creator.category());
    row.setBio(creator.bio());
    row.setLocation(creator.location());
    row.setCreatedAt(creator.createdAt());
    row.setUpdatedAt(AppClock.now());
    if (inserting) {
      mapper.insert(row);
    } else if (mapper.updateById(row) != 1) {
      throw new IllegalStateException("Creator optimistic lock conflict");
    }
    return toDomain(mapper.findByIdValue(row.getId()));
  }

  private static Creator toDomain(CreatorPersistenceRow row) {
    return Creator.reconstitute(
        CreatorId.of(row.getId()),
        UserId.of(row.getUserId()),
        row.getSlug(),
        row.getDisplayName(),
        row.getCategory(),
        row.getBio(),
        row.getLocation(),
        row.getCreatedAt());
  }
}
