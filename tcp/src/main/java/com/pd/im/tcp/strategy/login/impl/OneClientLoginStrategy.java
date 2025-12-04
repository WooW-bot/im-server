package com.pd.im.tcp.strategy.login.impl;

import com.pd.im.common.model.UserClientDto;
import com.pd.im.tcp.strategy.login.AbstractLoginStrategy;
import com.pd.im.tcp.strategy.login.utils.LoginStrategyUtils;

/**
 * 单端登录策略
 * <p>
 * 规则：任何新端登录都会踢掉所有旧端
 * 场景：安全性要求高的场景，确保用户同时只能在一个设备登录
 *
 * @author Parker
 * @date 12/3/25
 */
public class OneClientLoginStrategy extends AbstractLoginStrategy {

    @Override
    protected boolean shouldKickOut(UserClientDto newLogin, UserClientDto oldLogin) {
        // 单端登录：只要不是同一个设备，就踢出
        return !LoginStrategyUtils.isSameDevice(newLogin, oldLogin);
    }
}
