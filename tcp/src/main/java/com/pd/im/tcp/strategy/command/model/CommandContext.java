package com.pd.im.tcp.strategy.command.model;

import com.pd.im.codec.proto.Message;
import com.pd.im.tcp.feign.FeignMessageService;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

/**
 * 命令执行上下文
 * <p>
 * 封装命令执行所需的所有上下文信息
 *
 * @author Parker
 * @date 12/3/25
 */
@Data
public class CommandContext {
    private ChannelHandlerContext ctx;
    private Message msg;
    private Integer brokeId;
    private FeignMessageService feignMessageService;
}
