package com.pd.im.codec.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Parker
 * @date 12/3/25
 * @description ImBootstrapConfig类
 */
@Data
public class ImBootstrapConfig {
    private TcpConfig im;

    @Data
    public static class TcpConfig {
        /** tcp 绑定的端口号 */
        private Integer tcpPort;
        /** websocket 绑定的端口号 */
        private Integer webSocketPort;
        /** 是否启用webSocket */
        private boolean enableWebSocket;
        /** boss 线程数 默认为 1 */
        private Integer bossThreadSize;
        /** work 线程数 */
        private Integer workThreadSize;
        /** 心跳超时时间 */
        private Long heartBeatTime;
        /** 分布式 Id 区分服务 */
        private Integer brokerId;
        /** Feign RPC 连接 TCP层和业务层内部地址 */
        private String logicUrl;
        /** 端登录策略类型 */
        private Integer loginModel;
        /** redis配置 */
        private RedisConfig redis;
        /** rabbitmq 配置 */
        private Rabbitmq rabbitmq;
        /** zk 配置 */
        private ZkConfig zkConfig;
        /** 应用ID */
        private Integer appId;
        /** UserSign签名密钥 */
        private String privateKey;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisConfig {
        /** 单机模式：single 哨兵模式：sentinel 集群模式：cluster */
        private String mode;
        /** 数据库 */
        private Integer database;
        /** 密码 */
        private String password;
        /** 超时时间 */
        private Integer timeout;
        /** 最小空闲数 */
        private Integer poolMinIdle;
        /** 连接超时时间(毫秒) */
        private Integer poolConnTimeout;
        /** 连接池大小 */
        private Integer poolSize;
        /** redis单机配置 */
        private RedisSingle single;
    }

    /**
     * redis单机配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisSingle {
        /** 地址 */
        private String address;
    }

    /**
     * rabbitmq哨兵模式配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rabbitmq {
        private String host;
        private Integer port;
        private String virtualHost;
        private String userName;
        private String password;
    }


    @Data
    public static class ZkConfig {
        /** zk 连接地址 */
        private String zkAddr;
        /** zk 连接超时时间 */
        private Integer zkConnectTimeOut;
        /** zk 会话超时时间 */
        private Integer zkSessionTimeOut;
        /** 重试等待时间 */
        private Integer retryTimeMs;
        /** 最大重试次数 */
        private Integer maxRetries;
    }
}
