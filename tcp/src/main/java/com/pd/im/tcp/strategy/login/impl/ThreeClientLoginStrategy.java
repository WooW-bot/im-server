package com.pd.im.tcp.strategy.login.impl;

import com.pd.im.common.enums.device.ClientType;
import com.pd.im.common.model.UserClientDto;
import com.pd.im.tcp.strategy.login.AbstractLoginStrategy;
import com.pd.im.tcp.strategy.login.utils.LoginStrategyUtils;

/**
 * 三端登录策略
 *
 * 规则：
 * - Web 端可以与任何端共存
 * - 移动端（Android/iOS）之间互斥
 * - PC 端（Windows/Mac）之间互斥
 * - 移动端和 PC 端可以共存
 *
 * 场景：移动端+PC端+Web端，三端同时在线
 *
 * @author Parker
 * @date 12/3/25
 */
public class ThreeClientLoginStrategy extends AbstractLoginStrategy {

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

        // 同类客户端互斥（移动端互斥、PC端互斥）
        return ClientType.isSameClient(newLogin.getClientType(), oldLogin.getClientType());
    }

    /**
     * 判断是否是 Web 端
     */
    private boolean isWeb(UserClientDto dto) {
        return ClientType.WEB.getCode().equals(dto.getClientType());
    }
}
