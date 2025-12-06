package com.pd.im.service.message.mq.handler.p2p;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.enums.command.MessageCommand;
import com.pd.im.common.model.message.MessageContent;
import com.pd.im.service.message.mq.handler.MessageCommandHandler;
import com.pd.im.service.message.service.P2PMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * P2P消息处理器
 *
 * @author Parker
 * @date 12/6/25
 */
@Slf4j
@Component
public class P2PMessageHandler implements MessageCommandHandler {

    @Autowired
    private P2PMessageService p2PMessageService;

    @Override
    public void handle(JSONObject messageBody) throws Exception {
        MessageContent messageContent = messageBody.toJavaObject(MessageContent.class);
        p2PMessageService.process(messageContent);
    }

    @Override
    public Integer getCommand() {
        return MessageCommand.MSG_P2P.getCommand();
    }
}
