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
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

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

    /**
     * 对象池，用于复用CommandContext对象
     * <p>
     * 注意：CommandContext是简单POJO，对象池可能是过度设计
     * 保留此设计是为了避免高并发下的GC压力
     */
    private final GenericObjectPool<CommandContext> commandContextPool
            = new GenericObjectPool<>(new CommandContextFactory());

    public NettyServerHandler(Integer brokerId, String logicUrl) {
        this.brokerId = brokerId;
        this.feignMessageService = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .options(new Request.Options(1000, 3500))
                .target(FeignMessageService.class, logicUrl);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        Integer command = msg.getMessageHeader().getCommand();
        CommandFactory commandFactory = CommandFactory.getInstance();
        CommandStrategy commandStrategy = commandFactory.getStrategy(command);

        CommandContext commandContext = null;
        try {
            // 从对象池获取CommandContext对象
            commandContext = getCommandContext(ctx, msg);

            if (commandStrategy != null) {
                // 执行命令策略
                commandStrategy.execute(commandContext);
            } else {
                // 未注册的命令，直接转发到MQ
                log.debug("未找到命令策略，转发到MQ: command={}", command);
                MqMessageProducer.sendMessage(msg, command);
            }
        } finally {
            // 将对象归还给对象池
            if (commandContext != null) {
                commandContextPool.returnObject(commandContext);
            }
        }
    }

    /**
     * 从对象池获取CommandContext对象并初始化
     */
    private CommandContext getCommandContext(ChannelHandlerContext ctx, Message msg) {
        CommandContext commandContext = null;
        try {
            commandContext = commandContextPool.borrowObject();
            commandContext.setCtx(ctx);
            commandContext.setBrokeId(brokerId);
            commandContext.setMsg(msg);
            commandContext.setFeignMessageService(feignMessageService);
        } catch (Exception e) {
            log.error("从对象池获取CommandContext失败", e);
        }
        return commandContext;
    }

    /**
     * CommandContext对象工厂
     */
    private static class CommandContextFactory extends BasePooledObjectFactory<CommandContext> {

        @Override
        public CommandContext create() {
            return new CommandContext();
        }

        @Override
        public PooledObject<CommandContext> wrap(CommandContext obj) {
            return new DefaultPooledObject<>(obj);
        }
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
