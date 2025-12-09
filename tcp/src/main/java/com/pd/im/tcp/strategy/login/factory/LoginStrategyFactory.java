package com.pd.im.tcp.strategy.login.factory;

import com.pd.im.common.enums.device.DeviceMultiLoginMode;
import com.pd.im.tcp.strategy.login.LoginStrategy;
import com.pd.im.tcp.strategy.login.impl.AllClientLoginStrategy;
import com.pd.im.tcp.strategy.login.impl.OneClientLoginStrategy;
import com.pd.im.tcp.strategy.login.impl.ThreeClientLoginStrategy;
import com.pd.im.tcp.strategy.login.impl.TwoClientLoginStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录策略工厂
 * <p>
 * 根据多端登录模式获取对应的策略实现
 * 工厂是无状态的，策略实例在启动时初始化并复用
 *
 * @author Parker
 * @date 12/3/25
 */
@Slf4j
public class LoginStrategyFactory {

    /**
     * 策略映射表
     */
    private static final Map<Integer, LoginStrategy> STRATEGY_MAP = new ConcurrentHashMap<>();

    /**
     * 默认策略：多端登录
     */
    private static final LoginStrategy DEFAULT_STRATEGY = new AllClientLoginStrategy();

    /**
     * 初始化策略映射
     */
    public static void init() {
        STRATEGY_MAP.put(DeviceMultiLoginMode.ONE.getCode(), new OneClientLoginStrategy());
        STRATEGY_MAP.put(DeviceMultiLoginMode.TWO.getCode(), new TwoClientLoginStrategy());
        STRATEGY_MAP.put(DeviceMultiLoginMode.THREE.getCode(), new ThreeClientLoginStrategy());
        STRATEGY_MAP.put(DeviceMultiLoginMode.ALL.getCode(), new AllClientLoginStrategy());

        log.info("LoginStrategyFactory initialized with {} strategies", STRATEGY_MAP.size());
    }

    /**
     * 获取登录策略
     *
     * @param loginMode 登录模式（1-单端，2-双端，3-三端，4-多端）
     * @return 对应的登录策略，未找到则返回默认策略（多端登录）
     */
    public static LoginStrategy getStrategy(Integer loginMode) {
        LoginStrategy strategy = STRATEGY_MAP.get(loginMode);

        if (strategy == null) {
            log.warn("Unknown login mode: {}, using default strategy (ALL)", loginMode);
            return DEFAULT_STRATEGY;
        }

        return strategy;
    }

    private LoginStrategyFactory() {
        // 工具类，禁止实例化
    }
}
