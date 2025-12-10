package com.pd.im.tcp.handler;

import com.pd.im.codec.proto.Message;
import com.pd.im.tcp.feign.FeignMessageService;
import com.pd.im.tcp.rabbitmq.publish.MqMessageProducer;
import com.pd.im.tcp.strategy.command.factory.CommandFactory;
import com.pd.im.tcp.strategy.command.CommandStrategy;
import com.pd.im.tcp.strategy.command.model.CommandContext;
import com.pd.im.tcp.utils.UserChannelRepository;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty服务端消息处理器
 * <p>
 * 负责处理客户端发送的消息，根据命令码分发到对应的策略执行
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    private final Integer brokerId;
    private final FeignMessageService feignMessageService;

    public NettyServerHandler(Integer brokerId, String logicUrl) {
        this.brokerId = brokerId;
        this.feignMessageService = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .options(new Request.Options(1000, 3500))
                .target(FeignMessageService.class, logicUrl);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        Integer command = msg.getMessageHeader().getCommand();
        CommandFactory commandFactory = CommandFactory.getInstance();
        CommandStrategy commandStrategy = commandFactory.getStrategy(command);

        if (commandStrategy != null) {
            // 执行命令策略
            CommandContext commandContext = buildCommandContext(ctx, msg);
            commandStrategy.execute(commandContext);
        } else {
            // 未注册的命令，直接转发到MQ
            log.debug("未找到命令策略，转发到MQ: command={}", command);
            MqMessageProducer.sendMessage(msg, command);
        }
    }

    /**
     * 构建命令执行上下文
     * <p>
     * 注意：移除了对象池设计，原因如下：
     * 1. CommandContext是简单POJO，现代JVM对象分配速度极快（逃逸分析+栈上分配）
     * 2. 对象池引入复杂度和同步开销，反而可能降低性能
     * 3. 简单直接的代码更易维护和理解
     *
     * @param ctx Channel上下文
     * @param msg 消息对象
     * @return 命令执行上下文
     */
    private CommandContext buildCommandContext(ChannelHandlerContext ctx, Message msg) {
        CommandContext commandContext = new CommandContext();
        commandContext.setCtx(ctx);
        commandContext.setBrokeId(brokerId);
        commandContext.setMsg(msg);
        commandContext.setFeignMessageService(feignMessageService);
        return commandContext;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        UserChannelRepository.add(ctx.channel());
        log.debug("新连接建立: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 连接断开，用户离线（保留Session）
        UserChannelRepository.setOffline(ctx.channel());
        log.debug("连接断开: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Channel异常: {}", ctx.channel().remoteAddress(), cause);
        // 连接异常，用户离线（保留Session）
        UserChannelRepository.setOffline(ctx.channel());
    }
}
