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
import org.I0Itec.zkclient.ZkClient;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Parker
 * @date 12/3/25
 * @description Starter类
 */
public class Starter {
    public static void main(String[] args) {
        if (args.length > 0) {
            start(args[0]);
        }
    }

    private static void start(String path) {
        try {
            Yaml yaml = new Yaml();
            FileInputStream is = new FileInputStream(path);
            ImBootstrapConfig config = yaml.loadAs(is, ImBootstrapConfig.class);

            new ImServer(config.getIm()).start();
            new ImWebSocketServer(config.getIm()).start();

            // redisson 在系统启动之初就初始化，开始监听用户登陆消息
            RedissonManager.init(config);

            // 策略工厂初始化
            CommandFactory.init();
            LoginStrategyFactory.init();
            // MQ 工厂初始化
            MqFactory.init(config.getIm().getRabbitmq());
            // MqFactory.createExchange();
            // MQ 监听器初始化
            MqMessageReceiver.init(String.valueOf(config.getIm().getBrokerId()));

            // 每个服务器都注册 Zk
            registerZK(config);
        } catch (Exception e) {
            e.printStackTrace();
            // 程序退出
            System.exit(500);
        }
    }

    /**
     * 对于每一个 IP 地址，都开启一个线程去启动 Zk
     *
     * @param config
     * @throws UnknownHostException
     */
    public static void registerZK(ImBootstrapConfig config) throws UnknownHostException {
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        ZkClient zkClient = new ZkClient(config.getIm().getZkConfig().getZkAddr(),
                config.getIm().getZkConfig().getZkConnectTimeOut());
        ZKit zKit = new ZKit(zkClient);
        RegistryZK registryZK = new RegistryZK(zKit, hostAddress, config.getIm());
        Thread thread = new Thread(registryZK);
        thread.start();
    }
}
