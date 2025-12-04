package com.pd.im.tcp.strategy.command.impl;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.enums.command.GroupEventCommand;
import com.pd.im.common.model.message.CheckSendMessageReq;
import com.pd.im.tcp.feign.FeignMessageService;
import com.pd.im.tcp.strategy.command.AbstractMessageCommand;

import static com.pd.im.common.constant.Constants.MsgPackConstants.GROUP_ID;

/**
 * 群组消息校验命令
 * <p>
 * 在TCP层校验群组消息发送方的合法性
 *
 * @author Parker
 * @date 12/3/25
 */
public class GroupMsgCommand extends AbstractMessageCommand {

    @Override
    protected String extractToId(JSONObject jsonObject) {
        return jsonObject.getString(GROUP_ID);
    }

    @Override
    protected ResponseVO validateMessage(FeignMessageService feignMessageService, CheckSendMessageReq req) {
        return feignMessageService.checkGroupSendMessage(req);
    }

    @Override
    protected Integer getAckCommand() {
        return GroupEventCommand.GROUP_MSG_ACK.getCommand();
    }
}
