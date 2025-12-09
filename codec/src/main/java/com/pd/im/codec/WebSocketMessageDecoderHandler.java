package com.pd.im.codec;

import com.pd.im.codec.proto.Message;
import com.pd.im.codec.util.MessageCodecUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * WebSocket 解码器
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class WebSocketMessageDecoderHandler extends MessageToMessageDecoder<BinaryWebSocketFrame> {
    @Override
    protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame msg, List<Object> out) throws Exception {
        ByteBuf content = msg.content();

        // 检查 WebSocket 帧数据长度
        // 注意：WebSocket 帧已经是完整的，不会像 TCP 那样分包
        // 如果数据不足，说明客户端发送了错误的数据
        if (content.readableBytes() < MessageCodecUtils.DECODE_HEADER_LENGTH) {
            log.error("Invalid WebSocket frame size: {} bytes, expected at least {} bytes, channel: {}",
                    msg.content().readableBytes(),
                    MessageCodecUtils.DECODE_HEADER_LENGTH,
                    ctx.channel().id());
            // WebSocket 帧已经完整，数据不足说明是错误的帧，关闭连接
            ctx.channel().close();
            return;
        }

        // 解析消息
        Message message = MessageCodecUtils.decode(content);
        if (message == null) {
            // 消息解析失败，可能是格式错误
            log.error("Failed to decode WebSocket message, channel: {}", ctx.channel().id());
            // 解析失败，关闭连接
            ctx.channel().close();
            return;
        }

        // 解析成功，添加到输出列表
        out.add(message);

        if (log.isDebugEnabled()) {
            log.debug("Decoded WebSocket message from channel {}: command={}, imei={}",
                    ctx.channel().id(),
                    message.getMessageHeader().getCommand(),
                    message.getMessageHeader().getImei());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error occurred in WebSocketMessageDecoderHandler, channel: {}", ctx.channel().id(), cause);
        ctx.close();
    }
}
