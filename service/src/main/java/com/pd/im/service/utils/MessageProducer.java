package com.pd.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.codec.proto.MessagePack;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.Command;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.common.model.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 消息生产者
 * <p>
 * 负责将业务消息发送到RabbitMQ，由IM服务器分发给客户端
 * 支持多种发送场景：
 * - 发送到用户的所有在线端
 * - 发送到用户的指定客户端
 * - 发送到用户的除指定客户端外的所有端（多端同步）
 *
 * @author Parker
 * @date 12/5/25
 */
@Slf4j
@Service
public class MessageProducer {

    /**
     * 消息队列名称
     */
    private static final String QUEUE_NAME = Constants.RabbitmqConstants.MESSAGE_SERVICE_TO_IM;

    private final RabbitTemplate rabbitTemplate;
    private final UserSessionUtils userSessionUtils;

    @Autowired
    public MessageProducer(RabbitTemplate rabbitTemplate, UserSessionUtils userSessionUtils) {
        this.rabbitTemplate = rabbitTemplate;
        this.userSessionUtils = userSessionUtils;
    }

    /**
     * 发送消息到MQ队列
     * <p>
     * 将消息发送到指定用户会话对应的broker
     *
     * @param session 用户会话信息
     * @param msg     消息内容（已序列化为JSON字符串）
     * @return true-发送成功，false-发送失败
     */
    private boolean sendMessage(UserSession session, Object msg) {
        if (session == null) {
            log.warn("发送消息失败: session为null");
            return false;
        }

        try {
            String routingKey = String.valueOf(session.getBrokerId());
            rabbitTemplate.convertAndSend(QUEUE_NAME, routingKey, msg);

            log.debug("消息发送成功: brokerId={}, userId={}, clientType={}, imei={}",
                    session.getBrokerId(), session.getUserId(), session.getClientType(), session.getImei());
            return true;
        } catch (Exception e) {
            log.error("消息发送失败: brokerId={}, userId={}, clientType={}, imei={}",
                    session.getBrokerId(), session.getUserId(), session.getClientType(), session.getImei(), e);
            return false;
        }
    }

    /**
     * 包装消息并发送
     * <p>
     * 将业务数据包装为MessagePack格式后发送到MQ
     *
     * @param toId    接收方用户ID
     * @param command 消息命令
     * @param msg     消息数据
     * @param session 目标会话
     * @return true-发送成功，false-发送失败
     */
    private boolean sendPack(String toId, Command command, Object msg, UserSession session) {
        if (session == null) {
            log.warn("发送消息失败: session为null, toId={}, command={}", toId, command.getCommand());
            return false;
        }

        try {
            MessagePack messagePack = new MessagePack();
            messagePack.setCommand(command.getCommand());
            messagePack.setToId(toId);
            messagePack.setClientType(session.getClientType());
            messagePack.setAppId(session.getAppId());
            messagePack.setImei(session.getImei());
            messagePack.setTimestamp(System.currentTimeMillis());  // 设置消息时间戳

            // 直接设置数据，避免多余的序列化/反序列化
            if (msg instanceof JSONObject) {
                messagePack.setData((JSONObject) msg);
            } else {
                messagePack.setData((JSONObject) JSONObject.toJSON(msg));
            }

            String body = JSONObject.toJSONString(messagePack);
            return sendMessage(session, body);
        } catch (Exception e) {
            log.error("包装消息失败: toId={}, command={}, sessionUserId={}",
                    toId, command.getCommand(), session.getUserId(), e);
            return false;
        }
    }

    public void sendToClients(String userId, Command command, Object data, Integer appId, Integer clientType, String imei) {
        if (clientType != null && StringUtils.isNotBlank(imei)) {
            // (app 调用)普通用户发起的消息，发送给出本端以外的所有端
            ClientInfo clientInfo = new ClientInfo(appId, clientType, imei);
            sendToOtherClients(userId, command, data, clientInfo);
        } else {
            // (后台调用)管理员发起的消息(管理员没有 imei 号)，发送给所有端
            sendToAllClients(userId, command, data, appId);
        }
    }

    /**
     * 发送消息到用户的所有在线客户端
     * <p>
     * 场景：消息分发，需要通知用户的所有在线设备
     *
     * @param userId  接收方用户ID
     * @param command 消息命令
     * @param data    消息数据
     * @param appId   应用ID
     * @return 成功接收消息的客户端列表（空列表表示用户不在线或全部发送失败）
     */
    public List<ClientInfo> sendToAllClients(String userId, Command command, Object data, Integer appId) {
        List<UserSession> sessions = userSessionUtils.getUserSession(appId, userId);
        List<ClientInfo> successClients = new ArrayList<>();

        if (sessions == null || sessions.isEmpty()) {
            log.debug("用户无在线会话: appId={}, userId={}", appId, userId);
            return successClients;
        }

        log.debug("发送消息到用户所有客户端: appId={}, userId={}, sessionCount={}, command={}",
                appId, userId, sessions.size(), command.getCommand());

        for (UserSession session : sessions) {
            boolean success = sendPack(userId, command, data, session);
            if (success) {
                successClients.add(new ClientInfo(session.getAppId(), session.getClientType(), session.getImei()));
            }
        }

        log.debug("消息发送完成: appId={}, userId={}, 成功={}/总数={}",
                appId, userId, successClients.size(), sessions.size());

        return successClients;
    }

    /**
     * 发送消息到用户的指定客户端
     * <p>
     * 场景：精确推送到某一个设备
     *
     * @param userId     接收方用户ID
     * @param command    消息命令
     * @param data       消息数据
     * @param clientInfo 目标客户端信息（appId、clientType、imei）
     */
    public void sendToSpecificClient(String userId, Command command, Object data, ClientInfo clientInfo) {
        if (clientInfo == null) {
            log.warn("发送消息失败: clientInfo为null, userId={}", userId);
            return;
        }

        UserSession session = userSessionUtils.getUserSession(
                clientInfo.getAppId(),
                userId,
                clientInfo.getClientType(),
                clientInfo.getImei());

        if (session == null) {
            log.debug("用户指定客户端不在线: userId={}, clientType={}, imei={}",
                    userId, clientInfo.getClientType(), clientInfo.getImei());
            return;
        }

        boolean success = sendPack(userId, command, data, session);
        log.debug("发送到指定客户端{}: userId={}, clientType={}, imei={}",
                success ? "成功" : "失败", userId, clientInfo.getClientType(), clientInfo.getImei());
    }

    /**
     * 判断会话是否匹配客户端信息
     *
     * @param session    用户会话
     * @param clientInfo 客户端信息
     * @return true-匹配，false-不匹配
     */
    private boolean isMatch(UserSession session, ClientInfo clientInfo) {
        return Objects.equals(session.getAppId(), clientInfo.getAppId())
                && Objects.equals(session.getImei(), clientInfo.getImei())
                && Objects.equals(session.getClientType(), clientInfo.getClientType());
    }

    /**
     * 发送消息到用户的其他客户端（排除指定客户端）
     * <p>
     * 场景：多端同步，将消息同步到发送方的其他在线端
     * 例如：用户在手机上发送消息后，需要同步到Web端、iPad端等
     *
     * @param userId        接收方用户ID
     * @param command       消息命令
     * @param data          消息数据
     * @param excludeClient 要排除的客户端信息（通常是发送方当前使用的客户端）
     */
    public void sendToOtherClients(String userId, Command command, Object data, ClientInfo excludeClient) {
        if (excludeClient == null) {
            log.warn("发送消息失败: excludeClient为null, userId={}", userId);
            return;
        }

        List<UserSession> sessions = userSessionUtils.getUserSession(excludeClient.getAppId(), userId);

        if (sessions == null || sessions.isEmpty()) {
            log.debug("用户无在线会话: appId={}, userId={}", excludeClient.getAppId(), userId);
            return;
        }

        int totalCount = 0;
        int successCount = 0;

        log.debug("发送消息到用户其他客户端: appId={}, userId={}, sessionCount={}, 排除clientType={}",
                excludeClient.getAppId(), userId, sessions.size(), excludeClient.getClientType());

        for (UserSession session : sessions) {
            if (!isMatch(session, excludeClient)) {
                totalCount++;
                boolean success = sendPack(userId, command, data, session);
                if (success) {
                    successCount++;
                }
            }
        }

        log.debug("消息发送完成（排除指定端）: appId={}, userId={}, 成功={}/总数={}",
                excludeClient.getAppId(), userId, successCount, totalCount);
    }
}
