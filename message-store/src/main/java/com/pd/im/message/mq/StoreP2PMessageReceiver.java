package com.pd.im.message.mq;

import com.pd.im.message.service.StoreMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * MQ 任务队列接收器
 * 接收发布者传递的异步持久化任务，具体的持久化在 {@link StoreMessageService}
 *
 * @author Parker
 * @date 12/5/25
 */
@Slf4j
@Service
public class StoreP2PMessageReceiver {

    @Resource
    StoreMessageService storeMessageService;

    public void onChatMessage(@Payload Message message,
                              @Headers Map<String, Object> headers,
                              Channel channel) throws Exception {

    }
}
