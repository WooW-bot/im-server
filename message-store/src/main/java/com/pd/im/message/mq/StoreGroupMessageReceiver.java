package com.pd.im.message.mq;

import com.alibaba.fastjson.JSON;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.model.message.DoStoreGroupMessageDto;
import com.pd.im.message.service.StoreMessageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 群消息存储MQ接收器
 *
 * @author Parker
 * @date 12/6/25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreGroupMessageReceiver {
    private final StoreMessageService storeMessageService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = Constants.RabbitmqConstants.StoreGroupMessage, durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitmqConstants.StoreGroupMessage, durable = "true")
            ),
            concurrency = "1"
    )
    public void onChatMessage(@Payload Message message,
                              @Headers Map<String, Object> headers,
                              Channel channel) throws Exception {
        String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);

        log.debug("[群消息存储] 接收MQ消息: {}", messageBody);

        try {
            // 直接反序列化为 DTO
            DoStoreGroupMessageDto dto = JSON.parseObject(messageBody, DoStoreGroupMessageDto.class);

            // 执行持久化
            storeMessageService.doStoreGroupMessage(dto);

            // 手动确认
            channel.basicAck(deliveryTag, false);

            log.debug("[群消息存储] 消息处理成功: messageId={}, groupId={}",
                    dto.getMessageContent().getMessageId(),
                    dto.getMessageContent().getGroupId());
        } catch (Exception e) {
            log.error("[群消息存储] 消息处理失败, 消息体: {}", messageBody, e);
            // 第一个false: 不批量拒绝，第二个false: 不重回队列
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
