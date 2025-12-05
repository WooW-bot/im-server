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
     * 踢出旧设备
     * <p>
     * 采用服务端主导的设计（与微信、QQ等主流IM一致）：
     * 1. 服务端发送MUTALOGIN通知消息（尽力而为，用于UX优化）
     * 2. 服务端立即删除Session并关闭连接（确保策略一定生效）
     * <p>
     * 客户端收到MUTALOGIN后的处理（可选，用于优化体验）：
     * - 显示"你已在其他设备登录"提示
     * - 清理本地缓存和草稿
     * - 跳转到登录页面
     * <p>
     * 设计理由：
     * - 可靠性：不依赖客户端响应，网络抖动不影响策略执行
     * - 一致性：策略一定被执行，不会出现多设备同时在线的情况
     * - 安全性：防止恶意客户端拒绝退出
     *
     * @param oldChannel 旧设备的 channel
     * @param oldLogin   旧设备信息
     * @param newLogin   新设备信息
     */
    protected void kickOut(Channel oldChannel, UserClientDto oldLogin, UserClientDto newLogin) {
        // 1. 发送通知消息（UX优化，尽力而为）
        LoginStrategyUtils.sendMutualLoginMsg(oldChannel, oldLogin.getClientType(), oldLogin.getImei(), newLogin);

        // 2. 服务端立即处理（业务逻辑，必须执行）
        // 删除Session并关闭连接，确保登录策略一定生效
        UserChannelRepository.logout(oldChannel);
    }
}
