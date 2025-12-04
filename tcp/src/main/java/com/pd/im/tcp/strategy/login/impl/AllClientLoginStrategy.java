package com.pd.im.tcp.strategy.login.impl;

import com.pd.im.common.model.UserClientDto;
import com.pd.im.tcp.strategy.login.LoginStrategy;

/**
 * 多端登录策略
 *
 * 规则：允许所有端同时在线，不做任何限制
 * 场景：开放性场景，用户可以在多个设备同时使用
 *
 * @author Parker
 * @date 12/3/25
 */
public class AllClientLoginStrategy implements LoginStrategy {

    @Override
    public void handleUserLogin(UserClientDto dto) {
        // 放权，允许多设备登录，同端之间也不做逻辑处理
        // 不需要任何踢出逻辑
    }
}
