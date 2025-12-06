package com.pd.im.service.message.mq.handler.group;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.enums.command.GroupEventCommand;
import com.pd.im.common.model.message.GroupChatMessageContent;
import com.pd.im.service.message.service.GroupMessageService;
import com.pd.im.service.message.mq.handler.MessageCommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 群消息处理器
 *
 * @author Parker
 * @date 12/6/25
 */
@Slf4j
@Component
public class GroupMessageHandler implements MessageCommandHandler {

    @Autowired
    private GroupMessageService groupMessageService;

    @Override
    public void handle(JSONObject messageBody) throws Exception {
        GroupChatMessageContent messageContent = messageBody.toJavaObject(GroupChatMessageContent.class);
        groupMessageService.process(messageContent);
    }

    @Override
    public Integer getCommand() {
        return GroupEventCommand.MSG_GROUP.getCommand();
    }
}
