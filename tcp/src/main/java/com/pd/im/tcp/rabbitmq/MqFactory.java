package com.pd.im.tcp.rabbitmq;

import com.pd.im.codec.config.ImBootstrapConfig;
import com.pd.im.common.constant.Constants;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ连接工厂
 * <p>
 * 管理RabbitMQ的连接和Channel，采用单例Connection + Channel池的设计
 *
 * @author Parker
 * @date 12/4/25
 */
@Slf4j
public class MqFactory {
    private static volatile ConnectionFactory factory = null;

    /**
     * 单例Connection，所有Channel共享同一个Connection
     * Connection是线程安全的，但Channel不是
     */
    private static volatile Connection connection = null;

    /**
     * Channel缓存池
     * Key: channelName, Value: Channel
     */
    private static final Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    private static final Object CONNECTION_LOCK = new Object();
    private static final Object CHANNEL_LOCK = new Object();

    /**
     * 获取或创建单例Connection
     *
     * @return RabbitMQ连接
     */
    private static Connection getConnection() {
        if (connection == null || !connection.isOpen()) {
            synchronized (CONNECTION_LOCK) {
                if (connection == null || !connection.isOpen()) {
                    try {
                        if (factory == null) {
                            throw new IllegalStateException("MqFactory未初始化，请先调用init()方法");
                        }
                        connection = factory.newConnection();
                        log.info("RabbitMQ连接创建成功");
                    } catch (IOException | TimeoutException e) {
                        log.error("MQ连接失败", e);
                        throw new RuntimeException("无法建立RabbitMQ连接", e);
                    }
                }
            }
        }
        return connection;
    }

    /**
     * 获取或创建Channel
     * <p>
     * 使用双重检查锁定模式确保线程安全
     * 如果Channel已关闭，会自动重新创建
     *
     * @param channelName Channel名称
     * @return RabbitMQ Channel
     */
    public static Channel getChannel(String channelName) {
        Channel channel = channelMap.get(channelName);

        // 检查Channel是否可用
        if (channel == null || !channel.isOpen()) {
            synchronized (CHANNEL_LOCK) {
                // 双重检查
                channel = channelMap.get(channelName);
                if (channel == null || !channel.isOpen()) {
                    try {
                        Connection conn = getConnection();
                        channel = conn.createChannel();
                        channelMap.put(channelName, channel);
                        log.info("创建新的Channel: {}", channelName);
                    } catch (IOException e) {
                        log.error("创建Channel失败: {}", channelName, e);
                        throw new RuntimeException("无法创建RabbitMQ Channel", e);
                    }
                }
            }
        }
        return channel;
    }

    /**
     * 创建Exchange（如果不存在）
     */
    public static void createExchange() {
        Channel channel = null;
        try {
            channel = getConnection().createChannel();
            channel.exchangeDeclare(Constants.RabbitmqConstants.MESSAGE_SERVICE_TO_IM, "direct", true);
            log.info("Exchange创建成功: {}", Constants.RabbitmqConstants.MESSAGE_SERVICE_TO_IM);
        } catch (IOException e) {
            log.error("创建Exchange失败", e);
            throw new RuntimeException("无法创建Exchange", e);
        } finally {
            // 创建Exchange后关闭临时Channel
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (Exception e) {
                    log.warn("关闭临时Channel失败", e);
                }
            }
        }
    }

    /**
     * 初始化RabbitMQ连接工厂
     *
     * @param rabbitmq RabbitMQ配置
     */
    public static void init(ImBootstrapConfig.Rabbitmq rabbitmq) {
        if (factory == null) {
            synchronized (CONNECTION_LOCK) {
                if (factory == null) {
                    factory = new ConnectionFactory();
                    factory.setHost(rabbitmq.getHost());
                    factory.setPort(rabbitmq.getPort());
                    factory.setUsername(rabbitmq.getUserName());
                    factory.setPassword(rabbitmq.getPassword());
                    factory.setVirtualHost(rabbitmq.getVirtualHost());

                    // 设置连接超时和心跳
                    factory.setConnectionTimeout(5000);
                    factory.setRequestedHeartbeat(60);

                    // 设置自动恢复
                    factory.setAutomaticRecoveryEnabled(true);
                    factory.setNetworkRecoveryInterval(5000);

                    log.info("MqFactory初始化完成: host={}, port={}", rabbitmq.getHost(), rabbitmq.getPort());
                }
            }
        }
    }

    /**
     * 关闭所有连接和Channel（优雅关闭时调用）
     */
    public static void shutdown() {
        log.info("开始关闭MqFactory...");

        // 关闭所有Channel
        for (Map.Entry<String, Channel> entry : channelMap.entrySet()) {
            try {
                if (entry.getValue() != null && entry.getValue().isOpen()) {
                    entry.getValue().close();
                    log.debug("关闭Channel: {}", entry.getKey());
                }
            } catch (Exception e) {
                log.warn("关闭Channel失败: {}", entry.getKey(), e);
            }
        }
        channelMap.clear();

        // 关闭Connection
        if (connection != null && connection.isOpen()) {
            try {
                connection.close();
                log.info("RabbitMQ连接已关闭");
            } catch (IOException e) {
                log.error("关闭RabbitMQ连接失败", e);
            }
        }
        connection = null;
    }
}
