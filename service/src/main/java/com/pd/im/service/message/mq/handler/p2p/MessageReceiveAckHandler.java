package com.pd.im.service.message.mq.handler.p2p;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.enums.command.MessageCommand;
import com.pd.im.common.model.message.MessageReceiveAckContent;
import com.pd.im.service.message.mq.handler.MessageCommandHandler;
import com.pd.im.service.message.service.sync.MessageSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 消息接收确认处理器
 *
 * @author Parker
 * @date 12/6/25
 */
@Slf4j
@Component
public class MessageReceiveAckHandler implements MessageCommandHandler {

    @Autowired
    private MessageSyncService messageSyncService;

    @Override
    public void handle(JSONObject messageBody) throws Exception {
        MessageReceiveAckContent messageContent = messageBody.toJavaObject(MessageReceiveAckContent.class);
        messageSyncService.receiveMark(messageContent);
    }

    @Override
    public Integer getCommand() {
        return MessageCommand.MSG_RECEIVE_ACK.getCommand();
    }
}
