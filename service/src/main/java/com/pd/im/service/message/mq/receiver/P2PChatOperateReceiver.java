package com.pd.im.service.message.mq.receiver;

import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.MessageCommand;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * P2P消息操作接收器
 * <p>
 * 负责接收和处理P2P消息相关的MQ消息：
 * - MSG_P2P: P2P消息
 * - MSG_RECEIVE_ACK: 消息接收确认
 * - MSG_READ: 消息已读
 * - MSG_RECALL: 消息撤回
 *
 * @author Parker
 * @date 12/5/25
 */
@Component
@Slf4j
public class P2PChatOperateReceiver extends AbstractChatOperateReceiver {

    @Override
    @RabbitListener(
            bindings = @QueueBinding(
                    // 绑定 MQ 队列
                    value = @Queue(value = Constants.RabbitmqConstants.IM_TO_MESSAGE_SERVICE, durable = "true"),
                    // 绑定 MQ 交换机
                    exchange = @Exchange(value = Constants.RabbitmqConstants.IM_TO_MESSAGE_SERVICE, durable = "true")
            ),
            concurrency = "1"
    )
    public void onChatMessage(@Payload Message message, @Headers Map<String, Object> headers, Channel channel) throws Exception {
        super.onChatMessage(message, headers, channel);
    }

    @Override
    protected String getQueueName() {
        return Constants.RabbitmqConstants.IM_TO_MESSAGE_SERVICE;
    }

    @Override
    protected boolean isSupportedCommand(Integer command) {
        // 只处理P2P消息相关的命令
        return command.equals(MessageCommand.MSG_P2P.getCommand())
                || command.equals(MessageCommand.MSG_RECEIVE_ACK.getCommand())
                || command.equals(MessageCommand.MSG_READ.getCommand())
                || command.equals(MessageCommand.MSG_RECALL.getCommand());
    }
}
