package com.pd.im.service.message.mq.handler;

import com.alibaba.fastjson.JSONObject;

/**
 * 消息命令处理器接口
 * <p>
 * 使用策略模式处理不同类型的消息命令
 *
 * @author Parker
 * @date 12/6/25
 */
public interface MessageCommandHandler {

    /**
     * 处理消息命令
     *
     * @param messageBody 消息体
     * @throws Exception 处理异常
     */
    void handle(JSONObject messageBody) throws Exception;

    /**
     * 获取支持的命令代码
     *
     * @return 命令代码
     */
    Integer getCommand();
}
