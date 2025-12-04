package com.pd.im.tcp.handler;

import com.pd.im.common.constant.Constants;
import com.pd.im.tcp.utils.UserChannelRepository;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    private Long heartBeatTime;

    public HeartBeatHandler(Long heartBeatTime) {
        this.heartBeatTime = heartBeatTime;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断 evt 是否是 IdleStateEvent (用于触发用户事件，包含 读空闲/写空闲/读写空闲)
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;

            if (event.state() == IdleState.READER_IDLE) {
                log.debug("进入读空闲: {}", ctx.channel().remoteAddress());
            } else if (event.state() == IdleState.WRITER_IDLE) {
                log.debug("进入写空闲: {}", ctx.channel().remoteAddress());
            } else if (event.state() == IdleState.ALL_IDLE) {
                // 检查心跳超时
                Long lastReadTime = (Long) ctx.channel()
                        .attr(AttributeKey.valueOf(Constants.ChannelConstants.ReadTime))
                        .get();
                long nowTime = System.currentTimeMillis();

                if (lastReadTime != null && nowTime - lastReadTime > heartBeatTime) {
                    // 心跳超时，用户离线（保留Session）
                    log.warn("心跳超时，用户离线: channel={}, 超时时长={}ms",
                            ctx.channel().remoteAddress(), nowTime - lastReadTime);
                    UserChannelRepository.setOffline(ctx.channel());
                } else {
                    log.debug("读写空闲但心跳未超时: channel={}", ctx.channel().remoteAddress());
                }
            }
        }
    }
}
