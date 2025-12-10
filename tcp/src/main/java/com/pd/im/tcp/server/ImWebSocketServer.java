package com.pd.im.tcp.server;

import com.pd.im.codec.WebSocketMessageDecoderHandler;
import com.pd.im.codec.WebSocketMessageEncoderHandler;
import com.pd.im.codec.config.ImBootstrapConfig;
import com.pd.im.tcp.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket服务器
 * <p>
 * 基于Netty实现的WebSocket长连接服务器，支持浏览器客户端连接
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class ImWebSocketServer {
    private final ImBootstrapConfig.TcpConfig config;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;
    private final ServerBootstrap bootstrap;
    private ChannelFuture channelFuture;

    public ImWebSocketServer(ImBootstrapConfig.TcpConfig config) {
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
                    protected void initChannel(SocketChannel socketChannel) {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        // HTTP编解码器
                        pipeline.addLast("http-codec", new HttpServerCodec());
                        // 支持大数据流写入
                        pipeline.addLast("http-chunked", new ChunkedWriteHandler());
                        // HTTP消息聚合
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65535));
                        // WebSocket协议处理器（路由: /ws）
                        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                        // 自定义消息编解码器
                        pipeline.addLast(new WebSocketMessageDecoderHandler());
                        pipeline.addLast(new WebSocketMessageEncoderHandler());
                        // 业务逻辑处理器
                        pipeline.addLast(new NettyServerHandler(config.getBrokerId(), config.getLogicUrl()));
                    }
                });
    }

    /**
     * 启动WebSocket服务器
     */
    public void start() {
        try {
            channelFuture = bootstrap.bind(config.getWebSocketPort()).sync();
            log.info("WebSocket服务器启动成功，端口: {}", config.getWebSocketPort());
        } catch (InterruptedException e) {
            log.error("WebSocket服务器启动失败", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("WebSocket服务器启动失败", e);
        }
    }

    /**
     * 优雅关闭WebSocket服务器
     */
    public void shutdown() {
        log.info("开始关闭WebSocket服务器...");

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

        log.info("WebSocket服务器已关闭");
    }
}
