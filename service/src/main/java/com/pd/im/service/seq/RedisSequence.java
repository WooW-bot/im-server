package com.pd.im.service.seq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis 原子自增序列号
 *
 * @author Parker
 * @date 12/5/25
 */
@Service
public class RedisSequence {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public long doGetSeq(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }
}
