package com.pd.im.message.service;

import com.pd.im.common.model.message.DoStoreGroupMessageDto;
import com.pd.im.common.model.message.DoStoreP2PMessageDto;
import com.pd.im.common.model.message.GroupChatMessageContent;
import com.pd.im.common.model.message.MessageContent;
import com.pd.im.common.model.message.MessageBody;
import com.pd.im.message.dao.ImGroupMessageHistoryEntity;
import com.pd.im.message.dao.ImMessageBodyEntity;
import com.pd.im.message.dao.ImMessageHistoryEntity;
import com.pd.im.message.dao.mapper.ImGroupMessageHistoryMapper;
import com.pd.im.message.dao.mapper.ImMessageBodyMapper;
import com.pd.im.message.dao.mapper.ImMessageHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * MQ 异步持久化服务
 *
 * @author Parker
 * @date 12/5/25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreMessageService {

    private final ImMessageBodyMapper messageBodyMapper;
    private final ImMessageHistoryMapper messageHistoryMapper;
    private final ImGroupMessageHistoryMapper groupMessageHistoryMapper;

    /**
     * MQ异步 单聊消息落库持久化
     * 当出现异常，必须强制回滚: 简单回滚策略（失败就回滚）
     * TODO 回滚策略待升级, 重试三次：
     * 第一次 10s，第二次 20s，第三次 40s。
     * 还是不行就报紧急日志
     *
     * @param doStoreP2PMessageDto P2P消息存储DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void doStoreP2PMessage(DoStoreP2PMessageDto doStoreP2PMessageDto) {
        // 转换 MessageBody 为 ImMessageBodyEntity
        ImMessageBodyEntity messageBodyEntity = convertToEntity(doStoreP2PMessageDto.getMessageBody());
        messageBodyMapper.insert(messageBodyEntity);

        // 生成P2P消息历史记录（写扩散：发送方和接收方各一条）
        List<ImMessageHistoryEntity> messageHistoryEntities = extractToP2PMessageHistory(
                doStoreP2PMessageDto.getMessageContent(),
                messageBodyEntity);
        messageHistoryMapper.insertBatchSomeColumn(messageHistoryEntities);

        log.info("P2P消息持久化成功: messageKey={}, fromId={}, toId={}",
                messageBodyEntity.getMessageKey(),
                doStoreP2PMessageDto.getMessageContent().getFromId(),
                doStoreP2PMessageDto.getMessageContent().getToId());
    }

    /**
     * MQ异步 群聊消息落库持久化
     *
     * @param doStoreGroupMessageDto 群消息存储DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void doStoreGroupMessage(DoStoreGroupMessageDto doStoreGroupMessageDto) {
        // 转换 MessageBody 为 ImMessageBodyEntity
        ImMessageBodyEntity messageBodyEntity = convertToEntity(doStoreGroupMessageDto.getMessageBody());
        messageBodyMapper.insert(messageBodyEntity);

        // 生成群消息历史记录（读扩散）
        ImGroupMessageHistoryEntity groupMessageHistoryEntity = extractToGroupMessageHistory(
                doStoreGroupMessageDto.getMessageContent(),
                messageBodyEntity);
        groupMessageHistoryMapper.insert(groupMessageHistoryEntity);

        log.info("群消息持久化成功: messageKey={}, fromId={}, groupId={}",
                messageBodyEntity.getMessageKey(),
                doStoreGroupMessageDto.getMessageContent().getFromId(),
                doStoreGroupMessageDto.getMessageContent().getGroupId());
    }

    /**
     * 【写扩散】提取P2P消息历史记录
     * 双方消息冗余备份，分别记录消息拥有者 ownerId
     *
     * @param messageContent      消息内容
     * @param messageBodyEntity   消息体实体
     * @return P2P消息历史列表（包含发送方和接收方两条记录）
     */
    private List<ImMessageHistoryEntity> extractToP2PMessageHistory(
            MessageContent messageContent,
            ImMessageBodyEntity messageBodyEntity) {
        List<ImMessageHistoryEntity> list = new ArrayList<>();

        // 发送方的历史消息记录
        ImMessageHistoryEntity fromMsgHistory = buildMessageHistory(
                messageContent.getFromId(),
                messageContent,
                messageBodyEntity);

        // 接收方的历史消息记录
        ImMessageHistoryEntity toMsgHistory = buildMessageHistory(
                messageContent.getToId(),
                messageContent,
                messageBodyEntity);

        list.add(fromMsgHistory);
        list.add(toMsgHistory);
        return list;
    }

    /**
     * 【读扩散】提取群消息历史记录
     *
     * @param messageContent      群消息内容
     * @param messageBodyEntity   消息体实体
     * @return 群消息历史实体
     */
    private ImGroupMessageHistoryEntity extractToGroupMessageHistory(
            GroupChatMessageContent messageContent,
            ImMessageBodyEntity messageBodyEntity) {
        ImGroupMessageHistoryEntity entity = new ImGroupMessageHistoryEntity();
        entity.setAppId(messageContent.getAppId());
        entity.setFromId(messageContent.getFromId());
        entity.setGroupId(messageContent.getGroupId());
        entity.setMessageTime(messageContent.getMessageTime());
        entity.setSequence(messageContent.getMessageSequence());
        entity.setMessageKey(messageBodyEntity.getMessageKey());
        entity.setCreateTime(System.currentTimeMillis());

        return entity;
    }

    /**
     * 构建消息历史记录实体
     *
     * @param ownerId             消息拥有者ID
     * @param messageContent      消息内容
     * @param messageBodyEntity   消息体实体
     * @return 消息历史实体
     */
    private ImMessageHistoryEntity buildMessageHistory(
            String ownerId,
            MessageContent messageContent,
            ImMessageBodyEntity messageBodyEntity) {
        ImMessageHistoryEntity entity = new ImMessageHistoryEntity();
        entity.setAppId(messageContent.getAppId());
        entity.setFromId(messageContent.getFromId());
        entity.setToId(messageContent.getToId());
        entity.setMessageTime(messageContent.getMessageTime());
        entity.setOwnerId(ownerId);
        entity.setMessageKey(messageBodyEntity.getMessageKey());
        entity.setSequence(messageContent.getMessageSequence());
        entity.setCreateTime(System.currentTimeMillis());

        return entity;
    }

    /**
     * 将 MessageBody 转换为 ImMessageBodyEntity
     *
     * @param messageBody 消息体DTO
     * @return 消息体Entity
     */
    private ImMessageBodyEntity convertToEntity(MessageBody messageBody) {
        ImMessageBodyEntity entity = new ImMessageBodyEntity();
        BeanUtils.copyProperties(messageBody, entity);
        return entity;
    }
}
