package com.pd.im.tcp.strategy.command;

import com.pd.im.tcp.strategy.command.model.CommandContext;

/**
 * 命令执行策略接口
 *
 * @author Parker
 * @date 12/3/25
 */
public interface CommandStrategy {
    /**
     * 执行命令策略
     *
     * @param context 命令执行上下文
     */
    void execute(CommandContext context);
}
