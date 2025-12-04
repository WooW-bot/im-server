package com.pd.im.tcp;

import com.pd.im.codec.config.ImBootstrapConfig;
import com.pd.im.tcp.rabbitmq.MqFactory;
import com.pd.im.tcp.redis.RedissonManager;
import com.pd.im.tcp.server.ImServer;
import com.pd.im.tcp.server.ImWebSocketServer;
import com.pd.im.tcp.strategy.command.factory.CommandFactory;
import com.pd.im.tcp.strategy.login.factory.LoginStrategyFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;

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


        } catch (Exception e) {
            e.printStackTrace();
            // 程序退出
            System.exit(500);
        }
    }
}
