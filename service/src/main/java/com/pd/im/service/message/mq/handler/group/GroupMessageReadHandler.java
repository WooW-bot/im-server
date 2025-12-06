package com.pd.im.service.message.mq.handler.group;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.pd.im.common.enums.command.GroupEventCommand;
import com.pd.im.common.model.message.MessageReadContent;
import com.pd.im.service.message.mq.handler.MessageCommandHandler;
import com.pd.im.service.message.service.MessageSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 群消息已读处理器
 *
 * @author Parker
 * @date 12/6/25
 */
@Slf4j
@Component
public class GroupMessageReadHandler implements MessageCommandHandler {

    @Autowired
    private MessageSyncService messageSyncService;

    @Override
    public void handle(JSONObject messageBody) throws Exception {
        MessageReadContent messageContent = JSON.parseObject(messageBody.toJSONString(),
                new TypeReference<MessageReadContent>() {
                }.getType());
        messageSyncService.groupReadMark(messageContent);
    }

    @Override
    public Integer getCommand() {
        return GroupEventCommand.MSG_GROUP_READ.getCommand();
    }
}
