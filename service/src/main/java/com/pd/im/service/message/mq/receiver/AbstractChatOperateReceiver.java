package com.pd.im.service.message.mq.receiver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pd.im.service.message.mq.handler.MessageCommandHandler;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息操作接收器抽象基类
 * <p>
 * 提供消息接收、解析、分发的通用逻辑
 * 使用策略模式处理不同类型的消息命令
 *
 * @author Parker
 * @date 12/6/25
 */
@Slf4j
public abstract class AbstractChatOperateReceiver {

    /**
     * 命令处理器映射表
     * Key: 命令代码
     * Value: 命令处理器
     */
    private final Map<Integer, MessageCommandHandler> commandHandlers = new ConcurrentHashMap<>();

    /**
     * 注入所有命令处理器
     */
    @Autowired
    private List<MessageCommandHandler> handlerList;

    /**
     * 初始化命令处理器映射表
     */
    @PostConstruct
    public void initHandlers() {
        for (MessageCommandHandler handler : handlerList) {
            Integer command = handler.getCommand();
            if (isSupportedCommand(command)) {
                commandHandlers.put(command, handler);
                log.info("注册命令处理器: queue={}, command={}, handler={}",
                        getQueueName(), command, handler.getClass().getSimpleName());
            }
        }
        log.info("{}命令处理器初始化完成，共注册{}个处理器",
                getQueueName(), commandHandlers.size());
    }

    /**
     * 处理MQ消息
     *
     * @param message MQ消息
     * @param headers 消息头
     * @param channel MQ通道
     * @throws Exception 处理异常
     */
    public void onChatMessage(@Payload Message message,
                              @Headers Map<String, Object> headers,
                              Channel channel) throws Exception {
        // 1. 解析消息体
        String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);

        log.info("MQ接收到消息: queue={}, message={}", getQueueName(), messageBody);

        try {
            // 2. 解析JSON
            JSONObject jsonObject = JSON.parseObject(messageBody);
            Integer command = jsonObject.getInteger("command");

            if (command == null) {
                log.error("消息缺少command字段: queue={}, message={}", getQueueName(), messageBody);
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            // 3. 查找命令处理器
            MessageCommandHandler handler = commandHandlers.get(command);
            if (handler == null) {
                log.warn("未找到命令处理器: queue={}, command={}", getQueueName(), command);
                // 未知命令直接ACK，避免重复消费
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 4. 执行命令处理
            handler.handle(jsonObject);

            // 5. 发送ACK成功应答
            channel.basicAck(deliveryTag, false);

            log.debug("消息处理成功: queue={}, command={}", getQueueName(), command);

        } catch (Exception e) {
            log.error("处理消息异常: queue={}, message={}", getQueueName(), messageBody, e);

            // 拒绝消息，不重新入队（避免死循环）
            channel.basicNack(deliveryTag, false, false);

            // 可以在这里添加补偿机制，如：
            // 1. 记录到死信队列
            // 2. 写入数据库待补偿
            // 3. 发送告警
        }
    }

    /**
     * 获取队列名称
     *
     * @return 队列名称
     */
    protected abstract String getQueueName();

    /**
     * 判断是否支持该命令
     * <p>
     * 子类可以重写此方法来过滤只处理特定的命令
     *
     * @param command 命令代码
     * @return true-支持，false-不支持
     */
    protected abstract boolean isSupportedCommand(Integer command);
}
