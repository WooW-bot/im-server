package com.pd.im.codec;

import com.pd.im.codec.proto.MessagePack;
import com.pd.im.codec.util.MessageCodecUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Socket 消息编码类
 * 遵循私有协议规则，将 MessagePack 编码为字节流
 * 服务端向客户端发送数据使用
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class MessageEncoderHandler extends MessageToByteEncoder<MessagePack> {

    @Override
    protected void encode(ChannelHandlerContext ctx, MessagePack msg, ByteBuf out) throws Exception {
        try {
            // 使用工具类编码
            MessageCodecUtils.encode(msg, out);

            if (log.isDebugEnabled()) {
                log.debug("Encoded TCP message to channel {}: command={}, imei={}",
                        ctx.channel().id(),
                        msg.getCommand(),
                        msg.getImei());
            }
        } catch (Exception e) {
            log.error("Failed to encode TCP message, channel: {}, command: {}",
                    ctx.channel().id(), msg.getCommand(), e);
            throw e;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error occurred in MessageEncoderHandler, channel: {}", ctx.channel().id(), cause);
        ctx.close();
    }
}
