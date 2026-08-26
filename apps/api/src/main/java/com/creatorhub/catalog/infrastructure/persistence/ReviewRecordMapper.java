package com.creatorhub.catalog.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 审核记录 Mapper；记录采用追加写入，不允许修改历史审核结论。 */
@Mapper
interface ReviewRecordMapper extends BaseMapper<ReviewRecordPersistenceRow> {}
