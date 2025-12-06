package com.pd.im.service.message.mq.receiver;

import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.GroupEventCommand;
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
 * 群消息操作接收器
 * <p>
 * 负责接收和处理群消息相关的MQ消息：
 * - MSG_GROUP: 群消息
 * - MSG_GROUP_READ: 群消息已读
 *
 * @author Parker
 * @date 12/5/25
 */
@Component
@Slf4j
public class GroupChatOperateReceiver extends AbstractChatOperateReceiver {

    @Override
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = Constants.RabbitmqConstants.Im2GroupService, durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitmqConstants.Im2GroupService, durable = "true")
            ),
            concurrency = "1"
    )
    public void onChatMessage(@Payload Message message,
                              @Headers Map<String,Object> headers,
                              Channel channel) throws Exception {
        super.onChatMessage(message, headers, channel);
    }

    @Override
    protected String getQueueName() {
        return Constants.RabbitmqConstants.Im2GroupService;
    }

    @Override
    protected boolean isSupportedCommand(Integer command) {
        // 只处理群消息相关的命令
        return command.equals(GroupEventCommand.MSG_GROUP.getCommand())
                || command.equals(GroupEventCommand.MSG_GROUP_READ.getCommand());
    }
}
