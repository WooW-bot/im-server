package com.pd.im.service.utils;

import com.pd.im.common.constant.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Parker
 * @date 12/8/25
 */
@Service
public class UserSequenceRepository {
    //redis
    //uid friend 10
    //    group 12
    //    conversation 123
    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 记录用户所有模块: 好友、群组、会话的数据偏序
     * Redis Hash 记录
     * uid 做 key, 具体 seq 做 value
     *
     * @param appId
     * @param userId
     * @param type
     * @param seq
     */
    public void writeUserSeq(Integer appId, String userId, String type, Long seq) {
        String key = appId + ":" + Constants.RedisConstants.SEQ_PREFIX + ":" + userId;
        redisTemplate.opsForHash().put(key, type, seq);
    }
}
