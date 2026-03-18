package com.pd.im.tcp.strategy.command;

import com.pd.im.codec.proto.Message;
import com.pd.im.codec.proto.MessagePack;
import com.pd.im.codec.proto.generated.ChatMessageAck;
import com.pd.im.codec.proto.generated.ChatMessagePack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.message.CheckSendMessageReq;
import com.pd.im.tcp.feign.FeignMessageService;
import com.pd.im.tcp.rabbitmq.publish.MqMessageProducer;
import com.pd.im.tcp.strategy.command.model.CommandContext;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public abstract class AbstractMessageCommand implements CommandStrategy {

    @Override
    public void execute(CommandContext context) {
        ChannelHandlerContext ctx = context.getCtx();
        Message msg = context.getMsg();
        FeignMessageService feignMessageService = context.getFeignMessageService();

        Object pack = msg.getMessagePack();
        String fromId = "";
        String toId = "";
        String messageId = "";

        if (pack instanceof ChatMessagePack) {
            ChatMessagePack chatPack = (ChatMessagePack) pack;
            fromId = chatPack.getFromId();
            toId = extractToIdFromProto(chatPack);
            messageId = chatPack.getMessageId();
        } else {
            log.error("不支持的数据包类型，期待 ChatMessagePack");
            return;
        }

        // 构建校验请求
        CheckSendMessageReq req = CheckSendMessageReq.builder()
                .appId(msg.getMessageHeader().getAppId())
                .command(msg.getMessageHeader().getCommand())
                .fromId(fromId)
                .toId(toId)
                .build();

        // 调用业务层校验接口
        ResponseVO responseVO = validateMessage(feignMessageService, req);

        if (responseVO.isSuccess()) {
            // 校验通过，发送到MQ
            MqMessageProducer.sendMessage(msg, msg.getMessageHeader().getCommand());
        } else {
            // 校验失败，返回ACK响应
            sendAckResponse(ctx, messageId, responseVO);
        }
    }



    /**
     * 从 Protobuf 消息体中提取目标ID
     */
    protected abstract String extractToIdFromProto(ChatMessagePack chatPack);

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
     * @param messageId  消息ID
     * @param responseVO 响应结果
     */
    private void sendAckResponse(ChannelHandlerContext ctx, String messageId, ResponseVO responseVO) {
        ChatMessageAck chatMessageAck = ChatMessageAck.newBuilder()
                .setMessageId(messageId != null ? messageId : "")
                .build();
        responseVO.setData(chatMessageAck);

        MessagePack<ResponseVO> ack = new MessagePack<>();
        ack.setData(responseVO);
        ack.setCommand(getAckCommand());
        ack.setTimestamp(System.currentTimeMillis());

        ctx.channel().writeAndFlush(ack);
    }
}
