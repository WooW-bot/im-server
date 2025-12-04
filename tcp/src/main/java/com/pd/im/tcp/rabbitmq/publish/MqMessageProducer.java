package com.pd.im.tcp.rabbitmq.publish;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pd.im.codec.proto.Message;
import com.pd.im.codec.proto.MessageHeader;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.CommandType;
import com.pd.im.tcp.rabbitmq.MqFactory;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Parker
 * @date 12/4/25
 */
@Slf4j
public class MqMessageProducer {
    public static void sendMessage(Message message, Integer command) {
        Channel channel = null;
        String num = command.toString();
        String substring = num.substring(0, 1);
        CommandType commandType = CommandType.getCommandType(substring);
        String channelName = null;

        assert commandType != null;
        if (commandType == CommandType.MESSAGE) {
            channelName = Constants.RabbitmqConstants.Im2MessageService;
        } else if (commandType == CommandType.GROUP) {
            channelName = Constants.RabbitmqConstants.Im2GroupService;
        } else if (commandType == CommandType.FRIEND) {
            channelName = Constants.RabbitmqConstants.Im2FriendshipService;
        } else if (commandType == CommandType.USER) {
            channelName = Constants.RabbitmqConstants.Im2UserService;
        }

        try {
            channel = MqFactory.getChannel(channelName);

            // 解析私有协议的内容
            JSONObject o = (JSONObject) JSON.toJSON(message.getMessagePack());
            o.put("command", command);
            o.put("clientType", message.getMessageHeader().getClientType());
            o.put("imei", message.getMessageHeader().getImei());
            o.put("appId", message.getMessageHeader().getAppId());

            // TODO 开启镜像队列防止 MQ 丢失数据
            channel.basicPublish(channelName, "",
                    null, o.toJSONString().getBytes());
        } catch (Exception e) {
            log.error("发送消息出现异常：{}", e.getMessage());
        }
    }

    public static void sendMessage(Object message, MessageHeader header, Integer command) {
        Channel channel = null;
        String com = command.toString();
        String commandSub = com.substring(0, 1);
        CommandType commandType = CommandType.getCommandType(commandSub);
        String channelName = "";

        if (commandType == CommandType.MESSAGE) {
            channelName = Constants.RabbitmqConstants.Im2MessageService;
        } else if (commandType == CommandType.GROUP) {
            channelName = Constants.RabbitmqConstants.Im2GroupService;
        } else if (commandType == CommandType.FRIEND) {
            channelName = Constants.RabbitmqConstants.Im2FriendshipService;
        } else if (commandType == CommandType.USER) {
            channelName = Constants.RabbitmqConstants.Im2UserService;
        }
        try {
            channel = MqFactory.getChannel(channelName);

            JSONObject o = (JSONObject) JSON.toJSON(message);
            o.put("command", command);
            o.put("clientType", header.getClientType());
            o.put("imei", header.getImei());
            o.put("appId", header.getAppId());
            channel.basicPublish(channelName, "", null, o.toJSONString().getBytes());
        } catch (Exception e) {
            log.error("发送消息出现异常：{}", e.getMessage());
        }
    }
}
