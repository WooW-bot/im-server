package com.pd.im.service.message.mq.handler.p2p;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.enums.command.MessageCommand;
import com.pd.im.common.model.message.MessageReadContent;
import com.pd.im.service.message.mq.handler.MessageCommandHandler;
import com.pd.im.service.message.service.sync.MessageSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 消息已读处理器
 *
 * @author Parker
 * @date 12/6/25
 */
@Slf4j
@Component
public class MessageReadHandler implements MessageCommandHandler {

    @Autowired
    private MessageSyncService messageSyncService;

    @Override
    public void handle(JSONObject messageBody) throws Exception {
        MessageReadContent messageContent = messageBody.toJavaObject(MessageReadContent.class);
        messageSyncService.readMark(messageContent);
    }

    @Override
    public Integer getCommand() {
        return MessageCommand.MSG_READ.getCommand();
    }
}
