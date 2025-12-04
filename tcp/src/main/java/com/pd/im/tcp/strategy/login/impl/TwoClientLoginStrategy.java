package com.pd.im.tcp.strategy.login.impl;

import com.pd.im.common.enums.device.ClientType;
import com.pd.im.common.model.UserClientDto;
import com.pd.im.tcp.strategy.login.AbstractLoginStrategy;
import com.pd.im.tcp.strategy.login.utils.LoginStrategyUtils;

/**
 * 双端登录策略
 * <p>
 * 规则：
 * - 允许 Web 端与其他端共存
 * - 非 Web 端之间互斥
 * <p>
 * 场景：PC/移动端单端登录，但允许 Web 端同时在线
 *
 * @author Parker
 * @date 12/3/25
 */
public class TwoClientLoginStrategy extends AbstractLoginStrategy {

    @Override
    protected boolean shouldKickOut(UserClientDto newLogin, UserClientDto oldLogin) {
        // 同一设备不踢出
        if (LoginStrategyUtils.isSameDevice(newLogin, oldLogin)) {
            return false;
        }

        // Web 端可以与任何端共存
        if (isWeb(newLogin) || isWeb(oldLogin)) {
            return false;
        }

        // 非 Web 端互斥
        return true;
    }

    /**
     * 判断是否是 Web 端
     */
    private boolean isWeb(UserClientDto dto) {
        return ClientType.WEB.getCode().equals(dto.getClientType());
    }
}
