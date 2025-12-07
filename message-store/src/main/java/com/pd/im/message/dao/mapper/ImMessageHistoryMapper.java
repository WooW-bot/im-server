package com.pd.im.message.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pd.im.message.dao.ImMessageHistoryEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * @author Parker
 * @date 12/6/25
 */
@Repository
public interface ImMessageHistoryMapper extends BaseMapper<ImMessageHistoryEntity> {
    /**
     * 批量插入（mysql）
     *
     * @param entityList
     * @return
     */
    Integer insertBatchSomeColumn(Collection<ImMessageHistoryEntity> entityList);
}
