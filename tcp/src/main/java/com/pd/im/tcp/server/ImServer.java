package com.pd.im.tcp.server;

import com.pd.im.codec.MessageDecoderHandler;
import com.pd.im.codec.MessageEncoderHandler;
import com.pd.im.codec.config.ImBootstrapConfig;
import com.pd.im.tcp.handler.HeartBeatHandler;
import com.pd.im.tcp.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * TCP服务器
 * <p>
 * 基于Netty实现的TCP长连接服务器
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class ImServer {
    private final ImBootstrapConfig.TcpConfig config;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;
    private final ServerBootstrap bootstrap;
    private ChannelFuture channelFuture;

    public ImServer(ImBootstrapConfig.TcpConfig config) {
        this.config = config;
        // 创建主从线程组
        this.bossGroup = new NioEventLoopGroup(config.getBossThreadSize());
        this.workerGroup = new NioEventLoopGroup(config.getWorkThreadSize());
        this.bootstrap = new ServerBootstrap();

        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // 服务端可连接的最大队列数量
                .option(ChannelOption.SO_BACKLOG, 10240)
                // 允许重复使用本地地址和端口
                .option(ChannelOption.SO_REUSEADDR, true)
                // 禁用Nagle算法，提高消息实时性
                .childOption(ChannelOption.TCP_NODELAY, true)
                // 保活机制
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // 消息解码
                        ch.pipeline().addLast(new MessageDecoderHandler());
                        // 消息编码
                        ch.pipeline().addLast(new MessageEncoderHandler());
                        // 心跳检测（读写空闲1秒触发）
                        ch.pipeline().addLast(new IdleStateHandler(0, 0, 1));
                        // 心跳处理器
                        ch.pipeline().addLast(new HeartBeatHandler(config.getHeartBeatTime()));
                        // 业务逻辑处理器
                        ch.pipeline().addLast(new NettyServerHandler(config.getBrokerId(), config.getLogicUrl()));
                    }
                });
    }

    /**
     * 启动TCP服务器
     */
    public void start() {
        try {
            channelFuture = bootstrap.bind(config.getTcpPort()).sync();
            log.info("TCP服务器启动成功，端口: {}", config.getTcpPort());
        } catch (InterruptedException e) {
            log.error("TCP服务器启动失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("TCP服务器启动失败", e);
        }
    }

    /**
     * 优雅关闭TCP服务器
     */
    public void shutdown() {
        log.info("开始关闭TCP服务器...");

        try {
            // 关闭服务器Channel
            if (channelFuture != null && channelFuture.channel() != null) {
                channelFuture.channel().close().sync();
            }
        } catch (InterruptedException e) {
            log.error("关闭服务器Channel时被中断", e);
            Thread.currentThread().interrupt();
        }

        // 优雅关闭线程组
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        log.info("TCP服务器已关闭");
    }
}
