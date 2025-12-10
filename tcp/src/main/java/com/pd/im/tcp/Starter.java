package com.pd.im.tcp;

import com.pd.im.codec.config.ImBootstrapConfig;
import com.pd.im.tcp.rabbitmq.MqFactory;
import com.pd.im.tcp.rabbitmq.receiver.MqMessageReceiver;
import com.pd.im.tcp.redis.RedissonManager;
import com.pd.im.tcp.server.ImServer;
import com.pd.im.tcp.server.ImWebSocketServer;
import com.pd.im.tcp.strategy.command.factory.CommandFactory;
import com.pd.im.tcp.strategy.login.factory.LoginStrategyFactory;
import com.pd.im.tcp.zookeeper.RegistryZK;
import com.pd.im.tcp.zookeeper.ZKit;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * TCP服务器启动类
 * <p>
 * 负责初始化和启动IM服务器的所有组件
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class Starter {

    private static ImServer imServer;
    private static ImWebSocketServer imWebSocketServer;

    public static void main(String[] args) {
        if (args.length == 0) {
            log.error("启动失败：缺少配置文件路径参数");
            log.info("使用方式: java -jar tcp.jar <config-file-path>");
            System.exit(1);
        }

        // 注册优雅关闭钩子
        registerShutdownHook();

        // 启动服务
        start(args[0]);
    }

    /**
     * 启动IM服务器
     *
     * @param configPath 配置文件路径
     */
    private static void start(String configPath) {
        ImBootstrapConfig config = null;

        try {
            // 1. 加载配置文件
            config = loadConfig(configPath);
            log.info("配置文件加载成功: {}", configPath);

            // 2. 启动网络服务器
            imServer = new ImServer(config.getIm());
            imServer.start();

            imWebSocketServer = new ImWebSocketServer(config.getIm());
            imWebSocketServer.start();

            // 3. 初始化Redis连接池并启动用户登录监听
            RedissonManager.init(config);
            log.info("Redis连接池初始化成功");

            // 4. 初始化策略工厂
            CommandFactory.init();
            LoginStrategyFactory.init();
            log.info("命令策略工厂初始化成功");

            // 5. 初始化MQ
            MqFactory.init(config.getIm().getRabbitmq());
            MqMessageReceiver.init(String.valueOf(config.getIm().getBrokerId()));
            log.info("RabbitMQ初始化成功");

            // 6. 注册到Zookeeper
            registerZK(config);
            log.info("Zookeeper注册成功");

            log.info("========================================");
            log.info("IM服务器启动成功！");
            log.info("TCP端口: {}", config.getIm().getTcpPort());
            log.info("WebSocket端口: {}", config.getIm().getWebSocketPort());
            log.info("BrokerId: {}", config.getIm().getBrokerId());
            log.info("========================================");

        } catch (Exception e) {
            log.error("IM服务器启动失败", e);
            // 启动失败时清理资源
            shutdown();
            System.exit(1);
        }
    }

    /**
     * 加载配置文件
     *
     * @param configPath 配置文件路径
     * @return 配置对象
     * @throws Exception 加载失败时抛出异常
     */
    private static ImBootstrapConfig loadConfig(String configPath) throws Exception {
        try (InputStream is = new FileInputStream(configPath)) {
            Yaml yaml = new Yaml();
            ImBootstrapConfig config = yaml.loadAs(is, ImBootstrapConfig.class);

            if (config == null || config.getIm() == null) {
                throw new IllegalArgumentException("配置文件格式错误或为空");
            }

            return config;
        }
    }

    /**
     * 注册服务到Zookeeper
     * <p>
     * 对于每一个IP地址，都开启一个线程去注册ZK节点
     *
     * @param config 配置对象
     * @throws UnknownHostException 获取本机地址失败
     */
    private static void registerZK(ImBootstrapConfig config) throws UnknownHostException {
        String hostAddress = InetAddress.getLocalHost().getHostAddress();

        ZkClient zkClient = new ZkClient(
                config.getIm().getZkConfig().getZkAddr(),
                config.getIm().getZkConfig().getZkConnectTimeOut()
        );

        ZKit zKit = new ZKit(zkClient);
        RegistryZK registryZK = new RegistryZK(zKit, hostAddress, config.getIm());

        Thread zkThread = new Thread(registryZK, "zk-registry-thread");
        zkThread.setDaemon(true);
        zkThread.start();
    }

    /**
     * 注册JVM关闭钩子，实现优雅关闭
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("收到关闭信号，开始优雅关闭...");
            shutdown();
            log.info("服务器已关闭");
        }, "shutdown-hook-thread"));
    }

    /**
     * 优雅关闭所有资源
     */
    private static void shutdown() {
        try {
            // 关闭网络服务器
            if (imServer != null) {
                imServer.shutdown();
            }
            if (imWebSocketServer != null) {
                imWebSocketServer.shutdown();
            }

            // 关闭MQ连接
            MqFactory.shutdown();

            // 关闭Redis连接
            RedissonManager.shutdown();

            log.info("所有资源已释放");
        } catch (Exception e) {
            log.error("关闭资源时发生错误", e);
        }
    }
}
