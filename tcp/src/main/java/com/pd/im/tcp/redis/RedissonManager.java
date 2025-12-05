package com.pd.im.tcp.redis;

import com.pd.im.codec.config.ImBootstrapConfig;
import com.pd.im.tcp.redis.receiver.UserLoginMessageListener;
import org.redisson.api.RedissonClient;

/**
 * Redisson 客户端管理类
 *
 * @author Parker
 * @date 12/3/25
 */
public class RedissonManager {
    private static RedissonClient redissonClient;

    public static void init(ImBootstrapConfig config) {
        SingleClientStrategy singleClientStrategy = new SingleClientStrategy();
        redissonClient = singleClientStrategy.getRedissonClient(config.getIm().getRedis());
        // 初始化监听类
        UserLoginMessageListener userLoginMessageListener = new UserLoginMessageListener(config.getIm().getLoginModel());
        userLoginMessageListener.listenUserLogin();
    }

    public static RedissonClient getRedissonClient() {
        return redissonClient;
    }
}
