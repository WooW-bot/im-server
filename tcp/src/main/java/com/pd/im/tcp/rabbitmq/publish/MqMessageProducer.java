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
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * MQ消息生产者
 * <p>
 * 负责将IM消息发送到RabbitMQ，根据命令类型路由到不同的服务队列
 *
 * @author Parker
 * @date 12/4/25
 */
@Slf4j
public class MqMessageProducer {

    /**
     * 命令类型到队列名称的映射表
     * <p>
     * 使用静态Map替代if-else链，提高性能和可维护性
     */
    private static final Map<CommandType, String> COMMAND_TYPE_TO_QUEUE_MAP;

    static {
        Map<CommandType, String> map = new HashMap<>();
        map.put(CommandType.MESSAGE, Constants.RabbitmqConstants.IM_TO_MESSAGE_SERVICE);
        map.put(CommandType.GROUP, Constants.RabbitmqConstants.IM_TO_GROUP_SERVICE);
        map.put(CommandType.FRIEND, Constants.RabbitmqConstants.IM_TO_FRIENDSHIP_SERVICE);
        map.put(CommandType.USER, Constants.RabbitmqConstants.IM_TO_USER_SERVICE);
        COMMAND_TYPE_TO_QUEUE_MAP = map;
    }

    /**
     * 发送消息到MQ（Message对象）
     *
     * @param message 消息对象
     * @param command 命令代码
     */
    public static void sendMessage(Message message, Integer command) {
        if (message == null || message.getMessagePack() == null || message.getMessageHeader() == null) {
            log.warn("发送消息失败：消息对象或消息头为空");
            return;
        }

        MessageHeader header = message.getMessageHeader();
        sendMessage(message.getMessagePack(), header, command);
    }

    /**
     * 发送消息到MQ（通用对象）
     * <p>
     * 根据命令类型自动路由到对应的服务队列：
     * - 命令以1开头 → MessageService
     * - 命令以2开头 → GroupService
     * - 命令以3开头 → FriendshipService
     * - 命令以4开头 → UserService
     *
     * @param messagePack 消息内容对象
     * @param header      消息头
     * @param command     命令代码
     */
    public static void sendMessage(Object messagePack, MessageHeader header, Integer command) {
        // 参数校验
        if (!validateParameters(messagePack, header, command)) {
            return;
        }

        // 解析队列名称
        String queueName = resolveQueueName(command);
        if (queueName == null) {
            log.warn("发送消息失败：无法解析队列名称, command={}", command);
            return;
        }

        // 发送消息
        doSendMessage(messagePack, header, command, queueName);
    }

    /**
     * 参数校验
     */
    private static boolean validateParameters(Object messagePack, MessageHeader header, Integer command) {
        if (messagePack == null) {
            log.warn("发送消息失败：消息内容为空");
            return false;
        }

        if (header == null) {
            log.warn("发送消息失败：消息头为空");
            return false;
        }

        if (command == null) {
            log.warn("发送消息失败：命令为空");
            return false;
        }

        return true;
    }

    /**
     * 解析队列名称
     * <p>
     * 根据命令代码的第一位数字确定命令类型，然后映射到对应的队列
     *
     * @param command 命令代码
     * @return 队列名称，解析失败返回null
     */
    private static String resolveQueueName(Integer command) {
        try {
            String commandStr = command.toString();
            if (StringUtils.isEmpty(commandStr)) {
                return null;
            }

            // 获取命令类型（第一位数字）
            String commandTypeCode = commandStr.substring(0, 1);
            CommandType commandType = CommandType.getCommandType(commandTypeCode);

            if (commandType == null) {
                log.warn("未知的命令类型: commandTypeCode={}, command={}", commandTypeCode, command);
                return null;
            }

            // 从映射表获取队列名称
            return COMMAND_TYPE_TO_QUEUE_MAP.get(commandType);
        } catch (Exception e) {
            log.error("解析队列名称失败: command={}", command, e);
            return null;
        }
    }

    /**
     * 执行消息发送
     *
     * @param messagePack 消息内容
     * @param header      消息头
     * @param command     命令代码
     * @param queueName   队列名称
     */
    private static void doSendMessage(Object messagePack, MessageHeader header, Integer command, String queueName) {
        Channel channel = null;
        try {
            // 获取MQ Channel
            channel = MqFactory.getChannel(queueName);
            if (channel == null) {
                log.error("发送消息失败：无法获取MQ Channel, queueName={}", queueName);
                return;
            }

            // 构建消息体
            JSONObject messageBody = buildMessageBody(messagePack, header, command);

            // 发送消息
            // TODO 开启镜像队列防止 MQ 丢失数据
            channel.basicPublish(queueName, "", null, messageBody.toJSONString().getBytes());

            log.debug("发送消息成功: queueName={}, command={}, userId={}, appId={}",
                    queueName, command, header.getAppId(), header.getClientType());

        } catch (Exception e) {
            log.error("发送消息失败: queueName={}, command={}, appId={}, clientType={}, imei={}",
                    queueName, command, header.getAppId(), header.getClientType(), header.getImei(), e);
            // TODO 可以在这里添加消息发送失败的补偿机制，如：
            // 1. 重试机制
            // 2. 写入死信队列
            // 3. 写入数据库待补偿
        }
    }

    /**
     * 构建消息体
     *
     * @param messagePack 消息内容
     * @param header      消息头
     * @param command     命令代码
     * @return JSON消息体
     */
    private static JSONObject buildMessageBody(Object messagePack, MessageHeader header, Integer command) {
        JSONObject messageBody = (JSONObject) JSON.toJSON(messagePack);
        messageBody.put("command", command);
        messageBody.put("clientType", header.getClientType());
        messageBody.put("imei", header.getImei());
        messageBody.put("appId", header.getAppId());
        return messageBody;
    }
}
