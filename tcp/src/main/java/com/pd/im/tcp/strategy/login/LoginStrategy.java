package com.pd.im.tcp.strategy.login;

import com.pd.im.common.model.UserClientDto;

/**
 * 登录策略接口
 * <p>
 * 定义多端登录的策略行为，不同的策略处理不同的多端登录规则
 *
 * @author Parker
 * @date 12/3/25
 */
public interface LoginStrategy {

    /**
     * 处理用户登录，根据策略决定是否踢出旧设备
     *
     * @param dto 新登录的用户客户端信息
     */
    void handleUserLogin(UserClientDto dto);
}
