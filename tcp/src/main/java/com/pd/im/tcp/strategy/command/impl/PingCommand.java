package com.pd.im.tcp.strategy.command.impl;

import com.pd.im.common.constant.Constants;
import com.pd.im.tcp.strategy.command.CommandStrategy;
import com.pd.im.tcp.strategy.command.model.CommandContext;
import io.netty.util.AttributeKey;

/**
 * 心跳检测命令
 * <p>
 * 更新channel的最后读取时间，用于心跳超时检测
 *
 * @author Parker
 * @date 12/3/25
 */
public class PingCommand implements CommandStrategy {

    @Override
    public void execute(CommandContext context) {
        // 更新channel的最后读取时间
        context.getCtx().channel()
                .attr(AttributeKey.valueOf(Constants.ChannelConstants.READ_TIME))
                .set(System.currentTimeMillis());
    }
}
