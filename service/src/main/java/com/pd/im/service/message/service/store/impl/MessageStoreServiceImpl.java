package com.pd.im.service.message.service.store.impl;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.DelFlagEnum;
import com.pd.im.common.enums.conversation.ConversationType;
import com.pd.im.common.model.message.*;
import com.pd.im.common.model.message.MessageBody;
import com.pd.im.common.util.SnowflakeIdWorker;
import com.pd.im.service.conversation.service.ConversationService;
import com.pd.im.service.message.service.store.MessageStoreService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 消息存储服务实现
 * <p>
 * 负责消息的持久化存储、缓存管理和离线消息队列维护
 *
 * @author Parker
 * @date 12/5/25
 */
@Slf4j
@Service
public class MessageStoreServiceImpl implements MessageStoreService {

    /**
     * 消息缓存过期时间（秒）
     */
    private static final long MESSAGE_CACHE_EXPIRE_SECONDS = 300L;

    private final RabbitTemplate rabbitTemplate;
    private final AppConfig appConfig;
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public MessageStoreServiceImpl(RabbitTemplate rabbitTemplate,
                                   AppConfig appConfig,
                                   StringRedisTemplate stringRedisTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.appConfig = appConfig;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void storeP2PMessage(MessageContent messageContent) {
        try {
            // 将 MessageContent 转换成 MessageBody
            MessageBody messageBody = extractMessageBody(messageContent);

            // 设置 messageKey（必须在发送MQ前设置）
            messageContent.setMessageKey(messageBody.getMessageKey());

            // 构建存储DTO
            DoStoreP2PMessageDto dto = new DoStoreP2PMessageDto();
            dto.setMessageContent(messageContent);
            dto.setMessageBody(messageBody);

            // MQ 异步持久化, 将实体消息传递给 MQ
            rabbitTemplate.convertAndSend(
                    Constants.RabbitmqConstants.STORE_P2P_MESSAGE, "",
                    JSONObject.toJSONString(dto));

            log.debug("P2P消息发送到存储队列成功: messageId={}, messageKey={}",
                    messageContent.getMessageId(), messageBody.getMessageKey());
        } catch (Exception e) {
            log.error("P2P消息存储失败: messageId={}, fromId={}, toId={}",
                    messageContent.getMessageId(),
                    messageContent.getFromId(),
                    messageContent.getToId(), e);
            throw new RuntimeException("P2P消息存储失败", e);
        }
    }

    @Override
    public void storeGroupMessage(GroupChatMessageContent messageContent) {
        try {
            // 将 MessageContent 转换成 MessageBody
            MessageBody messageBody = extractMessageBody(messageContent);

            // 设置 messageKey（必须在发送MQ前设置，与P2P保持一致）
            messageContent.setMessageKey(messageBody.getMessageKey());

            // 构建存储DTO
            DoStoreGroupMessageDto dto = new DoStoreGroupMessageDto();
            dto.setMessageContent(messageContent);
            dto.setMessageBody(messageBody);

            // MQ 异步持久化
            rabbitTemplate.convertAndSend(
                    Constants.RabbitmqConstants.STORE_GROUP_MESSAGE, "",
                    JSONObject.toJSONString(dto));

            log.debug("群消息发送到存储队列成功: messageId={}, groupId={}, messageKey={}",
                    messageContent.getMessageId(), messageContent.getGroupId(), messageBody.getMessageKey());
        } catch (Exception e) {
            log.error("群消息存储失败: messageId={}, fromId={}, groupId={}",
                    messageContent.getMessageId(),
                    messageContent.getFromId(),
                    messageContent.getGroupId(), e);
            throw new RuntimeException("群消息存储失败", e);
        }
    }

    @Override
    public void setMessageCacheByMessageId(Integer appId, String messageId, Object messageContent) {
        try {
            // 构建缓存key: appId:cache:messageId
            String key = appId + Constants.RedisConstants.CACHE_MESSAGE + messageId;
            String value = JSONObject.toJSONString(messageContent);

            stringRedisTemplate.opsForValue().set(key, value, MESSAGE_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);

            log.debug("消息缓存设置成功: appId={}, messageId={}", appId, messageId);
        } catch (Exception e) {
            log.error("消息缓存设置失败: appId={}, messageId={}", appId, messageId, e);
            // 缓存失败不影响主流程，仅记录日志
        }
    }

    @Override
    public <T> T getMessageCacheByMessageId(Integer appId, String messageId, Class<T> clazz) {
        try {
            // 构建缓存key: appId:cache:messageId
            String key = appId + Constants.RedisConstants.CACHE_MESSAGE + messageId;
            String msg = stringRedisTemplate.opsForValue().get(key);

            if (StringUtils.isBlank(msg)) {
                log.debug("消息缓存未命中: appId={}, messageId={}", appId, messageId);
                return null;
            }

            log.debug("消息缓存命中: appId={}, messageId={}", appId, messageId);
            return JSONObject.parseObject(msg, clazz);
        } catch (Exception e) {
            log.error("获取消息缓存失败: appId={}, messageId={}", appId, messageId, e);
            return null;
        }
    }

    @Override
    public void storeOfflineMessage(OfflineMessageContent offlineMessage) {
        try {
            // 为 fromId 添加离线消息
            addToOfflineMsgQueue(offlineMessage, offlineMessage.getFromId(),
                    offlineMessage.getToId(), ConversationType.P2P);
            // 为 toId 添加离线消息
            addToOfflineMsgQueue(offlineMessage, offlineMessage.getToId(),
                    offlineMessage.getFromId(), ConversationType.P2P);

            log.debug("P2P离线消息存储成功: fromId={}, toId={}, messageKey={}",
                    offlineMessage.getFromId(), offlineMessage.getToId(), offlineMessage.getMessageKey());
        } catch (Exception e) {
            log.error("P2P离线消息存储失败: fromId={}, toId={}, messageKey={}",
                    offlineMessage.getFromId(), offlineMessage.getToId(), offlineMessage.getMessageKey(), e);
            throw new RuntimeException("P2P离线消息存储失败", e);
        }
    }

    @Override
    public void storeGroupOfflineMessage(OfflineMessageContent offlineMessage, List<String> memberIds) {
        try {
            // 对每个群成员添加离线消息
            memberIds.forEach(memberId ->
                    addToOfflineMsgQueue(offlineMessage, memberId, offlineMessage.getToId(), ConversationType.GROUP));

            log.debug("群离线消息存储成功: groupId={}, memberCount={}, messageKey={}",
                    offlineMessage.getToId(), memberIds.size(), offlineMessage.getMessageKey());
        } catch (Exception e) {
            log.error("群离线消息存储失败: groupId={}, messageKey={}",
                    offlineMessage.getToId(), offlineMessage.getMessageKey(), e);
            throw new RuntimeException("群离线消息存储失败", e);
        }
    }

    /**
     * 添加离线消息到用户的离线消息队列
     * <p>
     * 使用Redis ZSet存储，以messageKey作为分值实现有序存储
     * 当队列长度超过阈值时，自动删除最旧的消息
     *
     * @param offlineMessage   离线消息内容
     * @param userId           接收消息的用户ID
     * @param conversationWith 会话对方ID（P2P为对方用户ID，群聊为群ID）
     * @param conversationType 会话类型
     */
    private void addToOfflineMsgQueue(OfflineMessageContent offlineMessage,
                                      String userId,
                                      String conversationWith,
                                      ConversationTypeEnum conversationType) {
        try {
            // 构建用户离线消息队列key: appId:offline:userId
            String userKey = offlineMessage.getAppId() + Constants.RedisConstants.OFFLINE_MESSAGE + userId;

            ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();

            // 检查队列长度，超过阈值则删除最旧的消息
            Long queueSize = operations.zCard(userKey);
            if (queueSize != null && queueSize >= appConfig.getOfflineMessageCount()) {
                // 删除分值最小（最旧）的消息
                operations.removeRange(userKey, 0, 0);
                log.debug("离线消息队列已满，删除最旧消息: userId={}, queueSize={}", userId, queueSize);
            }

            // 设置会话信息
            offlineMessage.setConversationType(conversationType.getCode());
            offlineMessage.setConversationId(
                    ConversationService.convertConversationId(conversationType.getCode(), userId, conversationWith));

            // 插入离线消息，使用messageKey作为分值保证有序
            operations.add(userKey, JSONObject.toJSONString(offlineMessage), offlineMessage.getMessageKey());

            log.debug("离线消息添加成功: userId={}, conversationType={}, messageKey={}",
                    userId, conversationType.name(), offlineMessage.getMessageKey());
        } catch (Exception e) {
            log.error("添加离线消息失败: userId={}, conversationType={}, messageKey={}",
                    userId, conversationType.name(), offlineMessage.getMessageKey(), e);
            throw e;
        }
    }

    /**
     * 从消息内容中提取消息体
     * <p>
     * 将MessageContent转换为MessageBody用于持久化存储
     *
     * @param messageContent 消息内容
     * @return 消息体
     */
    private MessageBody extractMessageBody(MessageContent messageContent) {
        MessageBody messageBody = new MessageBody();
        messageBody.setAppId(messageContent.getAppId());

        // 生成消息唯一ID（使用雪花算法）
        messageBody.setMessageKey(SnowflakeIdWorker.nextId());
        messageBody.setCreateTime(System.currentTimeMillis());

        // TODO: 实现消息加密功能，当前暂不加密
        messageBody.setSecurityKey(null);

        messageBody.setExtra(messageContent.getExtra());
        messageBody.setDelFlag(DelFlagEnum.NORMAL.getCode());
        messageBody.setMessageTime(messageContent.getMessageTime());
        messageBody.setMessageBody(messageContent.getMessageBody());

        return messageBody;
    }
}
