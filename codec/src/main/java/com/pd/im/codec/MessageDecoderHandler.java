package com.pd.im.codec;

import com.pd.im.codec.proto.Message;
import com.pd.im.codec.utils.MessageCodecUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Socket 消息解码类
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class MessageDecoderHandler extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // TCP 流处理：检查协议头长度是否足够
        if (in.readableBytes() < MessageCodecUtils.DECODE_HEADER_LENGTH) {
            // 数据不足，等待更多数据到达
            // ByteToMessageDecoder 会自动保留数据，等待下次 decode 调用
            return;
        }

        // 标记读取位置，如果解析失败可以重置
        in.markReaderIndex();

        // 解析消息
        Message message = MessageCodecUtils.decode(in);
        if (message == null) {
            // 消息解析失败，可能是数据不完整或格式错误
            // 重置读取位置，等待更多数据
            in.resetReaderIndex();
            return;
        }

        // 解析成功，添加到输出列表
        out.add(message);

        if (log.isDebugEnabled()) {
            log.debug("Decoded TCP message from channel {}: command={}, imei={}",
                    ctx.channel().id(),
                    message.getMessageHeader().getCommand(),
                    message.getMessageHeader().getImei());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error occurred in MessageDecoderHandler, channel: {}", ctx.channel().id(), cause);
        ctx.close();
    }
}
