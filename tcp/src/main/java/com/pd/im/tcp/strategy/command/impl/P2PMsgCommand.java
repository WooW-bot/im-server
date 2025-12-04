package com.pd.im.tcp.strategy.command.impl;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.enums.command.MessageCommand;
import com.pd.im.common.model.message.CheckSendMessageReq;
import com.pd.im.tcp.feign.FeignMessageService;
import com.pd.im.tcp.strategy.command.AbstractMessageCommand;

import static com.pd.im.common.constant.Constants.MsgPackConstants.TO_ID;

/**
 * P2P消息校验命令
 * <p>
 * 在TCP层校验P2P消息发送方的合法性
 *
 * @author Parker
 * @date 12/3/25
 */
public class P2PMsgCommand extends AbstractMessageCommand {

    @Override
    protected String extractToId(JSONObject jsonObject) {
        return jsonObject.getString(TO_ID);
    }

    @Override
    protected ResponseVO validateMessage(FeignMessageService feignMessageService, CheckSendMessageReq req) {
        return feignMessageService.checkP2PSendMessage(req);
    }

    @Override
    protected Integer getAckCommand() {
        return MessageCommand.MSG_ACK.getCommand();
    }
}
