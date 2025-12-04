package com.pd.im.tcp.strategy.command;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pd.im.codec.pack.message.ChatMessageAck;
import com.pd.im.codec.proto.Message;
import com.pd.im.codec.proto.MessagePack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.message.CheckSendMessageReq;
import com.pd.im.tcp.feign.FeignMessageService;
import com.pd.im.tcp.rabbitmq.publish.MqMessageProducer;
import com.pd.im.tcp.strategy.command.model.CommandContext;
import io.netty.channel.ChannelHandlerContext;

import static com.pd.im.common.constant.Constants.MsgPackConstants.*;

/**
 * 消息校验命令抽象类
 * <p>
 * 使用模板方法模式处理消息校验的通用流程：
 * 1. 解析消息
 * 2. 调用业务层校验接口
 * 3. 校验通过则发送到MQ，失败则返回ACK响应
 *
 * @author Parker
 * @date 12/4/25
 */
public abstract class AbstractMessageCommand implements CommandStrategy {

    @Override
    public void execute(CommandContext context) {
        ChannelHandlerContext ctx = context.getCtx();
        Message msg = context.getMsg();
        FeignMessageService feignMessageService = context.getFeignMessageService();

        // 解析消息体
        JSONObject jsonObject = JSON.parseObject(JSONObject.toJSONString(msg.getMessagePack()));
        String fromId = jsonObject.getString(FROM_ID);
        String toId = extractToId(jsonObject);

        // 构建校验请求
        CheckSendMessageReq req = CheckSendMessageReq.builder()
                .appId(msg.getMessageHeader().getAppId())
                .command(msg.getMessageHeader().getCommand())
                .fromId(fromId)
                .toId(toId)
                .build();

        // 调用业务层校验接口
        ResponseVO responseVO = validateMessage(feignMessageService, req);

        if (responseVO.isOk()) {
            // 校验通过，发送到MQ
            MqMessageProducer.sendMessage(msg, msg.getMessageHeader().getCommand());
        } else {
            // 校验失败，返回ACK响应
            sendAckResponse(ctx, jsonObject, responseVO);
        }
    }

    /**
     * 从消息体中提取目标ID
     * <p>
     * 不同消息类型的目标ID字段不同（P2P是toId，Group是groupId）
     *
     * @param jsonObject 消息体JSON对象
     * @return 目标ID
     */
    protected abstract String extractToId(JSONObject jsonObject);

    /**
     * 调用业务层校验接口
     *
     * @param feignMessageService Feign客户端
     * @param req                 校验请求
     * @return 校验结果
     */
    protected abstract ResponseVO validateMessage(FeignMessageService feignMessageService, CheckSendMessageReq req);

    /**
     * 获取ACK响应的命令码
     *
     * @return ACK命令码
     */
    protected abstract Integer getAckCommand();

    /**
     * 发送ACK响应
     *
     * @param ctx        Channel上下文
     * @param jsonObject 消息体JSON对象
     * @param responseVO 响应结果
     */
    private void sendAckResponse(ChannelHandlerContext ctx, JSONObject jsonObject, ResponseVO responseVO) {
        ChatMessageAck chatMessageAck = new ChatMessageAck(jsonObject.getString(MSG_ID));
        responseVO.setData(chatMessageAck);

        MessagePack<ResponseVO> ack = new MessagePack<>();
        ack.setData(responseVO);
        ack.setCommand(getAckCommand());

        ctx.channel().writeAndFlush(ack);
    }
}
