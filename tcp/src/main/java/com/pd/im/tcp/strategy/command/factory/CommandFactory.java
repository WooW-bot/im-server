package com.pd.im.tcp.strategy.command.factory;

import com.pd.im.common.enums.command.GroupEventCommand;
import com.pd.im.common.enums.command.MessageCommand;
import com.pd.im.common.enums.command.SystemCommand;
import com.pd.im.tcp.strategy.command.CommandStrategy;
import com.pd.im.tcp.strategy.command.impl.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令策略工厂
 * <p>
 * 使用单例模式和策略模式，根据命令码获取对应的命令执行策略
 *
 * @author Parker
 * @date 12/3/25
 */
public class CommandFactory {

    /**
     * 命令策略映射表
     */
    private static final Map<Integer, CommandStrategy> STRATEGY_MAP = new ConcurrentHashMap<>();

    /**
     * 预创建的策略实例（策略无状态，可复用）
     */
    private static final LoginCommand LOGIN_COMMAND = new LoginCommand();
    private static final LogoutCommand LOGOUT_COMMAND = new LogoutCommand();
    private static final PingCommand PING_COMMAND = new PingCommand();
    private static final P2PMsgCommand P2P_MSG_COMMAND = new P2PMsgCommand();
    private static final GroupMsgCommand GROUP_MSG_COMMAND = new GroupMsgCommand();

    /**
     * 私有构造函数，防止外部实例化
     */
    private CommandFactory() {
    }

    /**
     * 静态内部类实现单例（延迟加载，线程安全）
     */
    private static class SingletonHolder {
        private static final CommandFactory INSTANCE = new CommandFactory();
    }

    /**
     * 获取工厂单例
     */
    public static CommandFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 初始化命令策略映射
     * <p>
     * 必须在应用启动时调用
     */
    public static void init() {
        // 系统命令策略
        STRATEGY_MAP.put(SystemCommand.LOGIN.getCommand(), LOGIN_COMMAND);
        STRATEGY_MAP.put(SystemCommand.LOGOUT.getCommand(), LOGOUT_COMMAND);
        STRATEGY_MAP.put(SystemCommand.PING.getCommand(), PING_COMMAND);

        // 消息命令策略
        STRATEGY_MAP.put(MessageCommand.MSG_P2P.getCommand(), P2P_MSG_COMMAND);
        STRATEGY_MAP.put(GroupEventCommand.MSG_GROUP.getCommand(), GROUP_MSG_COMMAND);
    }

    /**
     * 根据命令码获取对应的策略
     *
     * @param command 命令码
     * @return 命令策略，如果不存在则返回null
     */
    public CommandStrategy getStrategy(Integer command) {
        return STRATEGY_MAP.get(command);
    }
}
