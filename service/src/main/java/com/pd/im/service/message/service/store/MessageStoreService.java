package com.pd.im.service.message.service.store;

import com.pd.im.common.model.message.GroupChatMessageContent;
import com.pd.im.common.model.message.MessageContent;
import com.pd.im.common.model.message.OfflineMessageContent;

import java.util.List;

/**
 * @author Parker
 * @date 12/5/25
 */
public interface MessageStoreService {

    /**
     * 单聊消息持久化(MQ 异步持久化)
     *
     * @param messageContent
     */
    void storeP2PMessage(MessageContent messageContent);

    /**
     * 群聊消息持久化(MQ 异步持久化)
     *
     * @param messageContent
     */
    void storeGroupMessage(GroupChatMessageContent messageContent);

    /**
     * 通过 MessageId 设置消息缓存
     *
     * @param appId
     * @param messageId
     * @param messageContent
     */
    void setMessageCacheByMessageId(Integer appId, String messageId, Object messageContent);

    /**
     * 通过 MessageId 获取消息缓存
     *
     * @param appId
     * @param messageId
     * @return
     */
    public <T> T getMessageCacheByMessageId(Integer appId, String messageId, Class<T> clazz);

    /**
     * 【读扩散】存储单聊离线消息
     *
     * @param offlineMessage
     */
    void storeOfflineMessage(OfflineMessageContent offlineMessage);

    /**
     * 【读扩散】存储群聊离线消息
     *
     * @param messageContent
     * @param memberIds
     */
    void storeGroupOfflineMessage(OfflineMessageContent messageContent, List<String> memberIds);
}
