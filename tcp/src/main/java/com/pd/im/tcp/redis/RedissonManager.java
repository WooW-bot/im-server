package com.pd.im.tcp.redis;

import com.pd.im.codec.config.ImBootstrapConfig;
import com.pd.im.tcp.redis.receiver.UserLoginMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

/**
 * Redisson客户端管理类
 * <p>
 * 采用双重检查锁定的线程安全单例模式管理Redis连接
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class RedissonManager {
    private static volatile RedissonClient redissonClient;
    private static volatile UserLoginMessageListener loginMessageListener;
    private static final Object INIT_LOCK = new Object();

    /**
     * 初始化Redisson客户端和用户登录监听器
     * <p>
     * 线程安全的初始化方法
     *
     * @param config 配置对象
     */
    public static void init(ImBootstrapConfig config) {
        if (redissonClient == null) {
            synchronized (INIT_LOCK) {
                if (redissonClient == null) {
                    try {
                        // 根据配置创建Redis客户端
                        SingleClientStrategy singleClientStrategy = new SingleClientStrategy();
                        redissonClient = singleClientStrategy.getRedissonClient(config.getIm().getRedis());
                        log.info("Redisson客户端初始化成功");

                        // 初始化用户登录消息监听器
                        loginMessageListener = new UserLoginMessageListener(config.getIm().getLoginModel());
                        loginMessageListener.listenUserLogin();
                        log.info("用户登录监听器初始化成功");

                    } catch (Exception e) {
                        log.error("初始化Redisson客户端失败", e);
                        throw new RuntimeException("Redis连接初始化失败", e);
                    }
                }
            }
        }
    }

    /**
     * 获取Redisson客户端实例
     *
     * @return RedissonClient实例
     * @throws IllegalStateException 如果客户端未初始化
     */
    public static RedissonClient getRedissonClient() {
        if (redissonClient == null) {
            throw new IllegalStateException("RedissonClient未初始化，请先调用init()方法");
        }
        return redissonClient;
    }

    /**
     * 优雅关闭Redisson客户端
     */
    public static void shutdown() {
        if (redissonClient != null) {
            synchronized (INIT_LOCK) {
                if (redissonClient != null) {
                    try {
                        log.info("开始关闭Redisson客户端...");
                        redissonClient.shutdown();
                        redissonClient = null;
                        loginMessageListener = null;
                        log.info("Redisson客户端已关闭");
                    } catch (Exception e) {
                        log.error("关闭Redisson客户端失败", e);
                    }
                }
            }
        }
    }
}
