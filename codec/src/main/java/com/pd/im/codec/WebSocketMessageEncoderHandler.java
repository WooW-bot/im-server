package com.pd.im.codec;

import com.pd.im.codec.proto.MessagePack;
import com.pd.im.codec.util.MessageCodecUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * WebSocket 消息编码器
 * 遵循私有协议规则，将 MessagePack 编码为 WebSocket 二进制帧
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class WebSocketMessageEncoderHandler extends MessageToMessageEncoder<MessagePack> {

    @Override
    protected void encode(ChannelHandlerContext ctx, MessagePack msg, List<Object> out) throws Exception {
        try {
            // 计算编码后的大小，分配合适的 ByteBuf
            int estimatedSize = MessageCodecUtils.calculateEncodedSize(msg);
            ByteBuf byteBuf = Unpooled.buffer(estimatedSize);

            // 使用工具类编码
            MessageCodecUtils.encode(msg, byteBuf);

            // 包装为 WebSocket 二进制帧
            out.add(new BinaryWebSocketFrame(byteBuf));

            if (log.isDebugEnabled()) {
                log.debug("Encoded WebSocket message to channel {}: command={}, imei={}",
                        ctx.channel().id(),
                        msg.getCommand(),
                        msg.getImei());
            }
        } catch (Exception e) {
            log.error("Failed to encode WebSocket message, channel: {}, command: {}",
                    ctx.channel().id(), msg.getCommand(), e);
            throw e;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error occurred in WebSocketMessageEncoderHandler, channel: {}", ctx.channel().id(), cause);
        ctx.close();
    }
}
