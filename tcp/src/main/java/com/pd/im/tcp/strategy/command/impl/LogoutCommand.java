package com.pd.im.tcp.strategy.command.impl;

import com.pd.im.common.model.UserClientDto;
import com.pd.im.tcp.strategy.command.CommandStrategy;
import com.pd.im.tcp.strategy.command.model.CommandContext;
import com.pd.im.tcp.utils.UserChannelRepository;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户登出命令
 * <p>
 * 处理用户主动登出逻辑
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class LogoutCommand implements CommandStrategy {

    @Override
    public void execute(CommandContext context) {
        Channel channel = context.getCtx().channel();

        // 获取用户信息
        UserClientDto userInfo = UserChannelRepository.getUserInfo(channel);
        if (userInfo == null) {
            log.warn("用户主动登出，但Channel未绑定用户信息");
            channel.close();
            return;
        }

        log.info("用户主动登出: appId={}, userId={}, clientType={}, imei={}",
                userInfo.getAppId(), userInfo.getUserId(), userInfo.getClientType(), userInfo.getImei());

        // 用户主动登出（删除Session）
        UserChannelRepository.logout(channel);
    }
}
