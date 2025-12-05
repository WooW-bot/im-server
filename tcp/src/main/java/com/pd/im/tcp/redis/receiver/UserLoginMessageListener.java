package com.pd.im.tcp.redis.receiver;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.model.UserClientDto;
import com.pd.im.tcp.redis.RedissonManager;
import com.pd.im.tcp.strategy.login.LoginStrategy;
import com.pd.im.tcp.strategy.login.factory.LoginStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;

/**
 * 多端同步：
 * 1 单端登录: 任何新端登录都踢掉所有旧端
 * 2 双端登录: Web端可共存，其他端（移动端+PC端）互斥
 * 3 三端登录: 移动端互斥、PC端互斥、Web独立
 * 4 多端登录: 所有端可同时在线
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class UserLoginMessageListener {
    private final LoginStrategy loginStrategy;

    public UserLoginMessageListener(Integer loginMode) {
        // 根据登录模式获取对应的策略
        this.loginStrategy = LoginStrategyFactory.getStrategy(loginMode);
    }

    public void listenUserLogin() {
        // 监听者监听 UserLoginChannel 队列
        RTopic topic = RedissonManager.getRedissonClient().getTopic(Constants.RedisConstants.UserLoginChannel);
        topic.addListener(String.class, (CharSequence charSequence, String msg) -> {
            log.info("收到用户上线通知: {}", msg);
            UserClientDto dto = JSONObject.parseObject(msg, UserClientDto.class);

            // 执行登录策略
            loginStrategy.handleUserLogin(dto);
        });
    }
}
