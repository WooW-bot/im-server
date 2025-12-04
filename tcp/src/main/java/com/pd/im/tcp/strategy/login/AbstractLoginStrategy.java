package com.pd.im.tcp.strategy.login;

import com.pd.im.common.model.UserClientDto;
import com.pd.im.tcp.strategy.login.utils.LoginStrategyUtils;
import com.pd.im.tcp.utils.UserChannelRepository;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 登录策略抽象类
 * <p>
 * 使用模板方法模式，提取公共逻辑，子类只需实现具体的判断逻辑
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public abstract class AbstractLoginStrategy implements LoginStrategy {

    @Override
    public void handleUserLogin(UserClientDto newLoginDto) {
        // 获取该用户的所有在线 channel
        List<Channel> userChannels = getUserChannels(newLoginDto);

        // 遍历所有在线设备，判断是否需要踢出
        for (Channel oldChannel : userChannels) {
            UserClientDto oldLoginDto = getUserInfo(oldChannel);

            // 钩子方法：子类实现具体的踢出逻辑
            if (shouldKickOut(newLoginDto, oldLoginDto)) {
                kickOut(oldChannel, oldLoginDto, newLoginDto);
            }
        }
    }

    /**
     * 钩子方法：判断是否需要踢出旧设备
     * <p>
     * 子类实现具体的判断逻辑
     *
     * @param newLogin 新登录的设备信息
     * @param oldLogin 旧设备的信息
     * @return true-需要踢出，false-不需要踢出
     */
    protected abstract boolean shouldKickOut(UserClientDto newLogin, UserClientDto oldLogin);

    /**
     * 获取用户的所有在线 channel
     *
     * @param dto 用户信息
     * @return 在线 channel 列表
     */
    protected List<Channel> getUserChannels(UserClientDto dto) {
        return UserChannelRepository.getUserChannels(dto.getAppId(), dto.getUserId());
    }

    /**
     * 从 channel 获取用户信息
     *
     * @param channel channel
     * @return 用户信息
     */
    protected UserClientDto getUserInfo(Channel channel) {
        return UserChannelRepository.getUserInfo(channel);
    }

    /**
     * 通知旧设备被踢下线
     * <p>
     * 服务端只负责发送MUTALOGIN通知消息，不主动关闭连接
     * 由旧设备客户端收到通知后，自行决定如何处理：
     * 1. 显示"你已在其他设备登录"提示
     * 2. 保存未完成的工作（草稿、缓存等）
     * 3. 清理本地状态
     * 4. 主动发送LOGOUT命令
     * 5. 断开连接
     * <p>
     * 如果客户端没有响应，依靠心跳超时机制自动离线
     *
     * @param oldChannel 旧设备的 channel
     * @param oldLogin   旧设备信息
     * @param newLogin   新设备信息
     */
    protected void kickOut(Channel oldChannel, UserClientDto oldLogin, UserClientDto newLogin) {
        // 只发送互踢通知消息，不关闭连接
        // 让客户端自己决定如何退出
        LoginStrategyUtils.sendMutualLoginMsg(oldChannel, oldLogin.getClientType(), oldLogin.getImei(), newLogin);
    }
}
