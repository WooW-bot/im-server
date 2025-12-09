package com.pd.im.service.message.service;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.codec.pack.message.ChatMessageAck;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.GroupEventCommand;
import com.pd.im.common.enums.conversation.ConversationType;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.common.model.message.GroupChatMessageContent;
import com.pd.im.common.model.message.MessageContent;
import com.pd.im.common.model.message.OfflineMessageContent;
import com.pd.im.service.callback.CallbackService;
import com.pd.im.service.group.model.req.SendGroupMessageReq;
import com.pd.im.service.group.service.ImGroupMemberService;
import com.pd.im.service.message.model.resp.SendMessageResp;
import com.pd.im.service.message.service.check.CheckSendMessageService;
import com.pd.im.service.message.service.store.MessageStoreService;
import com.pd.im.service.seq.RedisSequence;
import com.pd.im.service.utils.MessageProducer;
import com.pd.im.service.utils.ThreadPoolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 群消息处理服务
 * <p>
 * 负责群聊消息的完整处理流程：
 * 1. 消息防重校验（基于缓存）
 * 2. 消息持久化（异步）
 * 3. 发送ACK给发送方
 * 4. 同步消息到发送方其他在线端
 * 5. 分发消息给群成员所有在线端
 * 6. 存储离线消息
 *
 * @author Parker
 * @date 12/6/25
 */
@Slf4j
@Service
public class GroupMessageService {
    @Autowired
    CheckSendMessageService checkSendMessageService;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private ImGroupMemberService imGroupMemberService;

    @Autowired
    private MessageStoreService messageStoreService;

    @Autowired
    private RedisSequence redisSequence;

    @Autowired
    private ThreadPoolManager threadPoolManager;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CallbackService callbackService;

    /**
     * 线程池名称
     */
    private static final String THREAD_POOL_NAME = "group-message-processor";

    /**
     * 消息处理线程池
     */
    private ThreadPoolExecutor messageProcessExecutor;

    /**
     * 初始化线程池
     */
    @PostConstruct
    public void init() {
        // 创建群消息处理线程池
        this.messageProcessExecutor = threadPoolManager.createThreadPool(
                THREAD_POOL_NAME,
                ThreadPoolManager.ThreadPoolConfig.create()
                        .corePoolSize(8)
                        .maxPoolSize(16)
                        .queueCapacity(2000)
                        .keepAliveSeconds(60L)
                        .slowTaskThreshold(1000L)
        );

        log.info("群消息处理线程池初始化完成: {}", THREAD_POOL_NAME);
    }

    public void process(GroupChatMessageContent messageContent) {
        log.info("开始处理群组消息: messageId={}, from={}, groupId={}",
                messageContent.getMessageId(), messageContent.getFromId(), messageContent.getGroupId());
        //前置校验
        //这个用户是否被禁言 是否被禁用
        //发送方和接收方是否是好友
        // 1. 检查消息缓存（防重）
        GroupChatMessageContent cachedMessage = messageStoreService.getMessageCacheByMessageId(
                messageContent.getAppId(), messageContent.getMessageId(), GroupChatMessageContent.class);
        if (cachedMessage != null) {
            // 重复消息，使用缓存直接分发
            log.info("检测到重复消息，使用缓存: messageId={}", messageContent.getMessageId());
            handleDuplicateMessage(cachedMessage);
            return;
        }

        // 2. 新消息，执行完整处理流程
        handleNewMessage(messageContent);
    }

    /**
     * 处理重复消息（缓存命中）
     * <p>
     * 对于重复消息，只需要重新分发，不需要持久化
     *
     * @param cachedMessage 缓存的消息
     */
    private void handleDuplicateMessage(GroupChatMessageContent cachedMessage) {
        try {
            messageProcessExecutor.execute(() -> {
                try {
                    // 1. 返回ACK给发送方当前端
                    sendAck(cachedMessage, ResponseVO.successResponse());

                    // 2. 同步消息到发送方其他在线端
                    syncToSender(cachedMessage);

                    // 3. 分发消息给接收方所有在线端
                    dispatchToReceiver(cachedMessage);

                    log.info("重复消息处理完成: messageId={}", cachedMessage.getMessageId());
                } catch (Exception e) {
                    log.error("处理重复消息异常: messageId={}", cachedMessage.getMessageId(), e);
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("线程池队列已满，拒绝处理重复消息: messageId={}", cachedMessage.getMessageId(), e);
        }
    }

    /**
     * 发送ACK给发送方当前端
     *
     * @param messageContent 消息内容
     * @param responseVO     响应结果
     */
    private void sendAck(MessageContent messageContent, ResponseVO responseVO) {
        log.debug("发送ACK: messageId={}, result={}", messageContent.getMessageId(), responseVO.getCode());

        ChatMessageAck ackData = new ChatMessageAck(
                messageContent.getMessageId(),
                messageContent.getMessageSequence());
        responseVO.setData(ackData);

        // 发送ACK给发送方当前端
        ClientInfo senderClient = new ClientInfo(
                messageContent.getAppId(),
                messageContent.getClientType(),
                messageContent.getImei());
        messageProducer.sendToSpecificClient(
                messageContent.getFromId(),
                GroupEventCommand.GROUP_MSG_ACK,
                responseVO,
                senderClient);
    }

    /**
     * 同步消息到发送方其他在线端
     * <p>
     * 实现多端同步：发送方在其他设备上也能看到自己发送的消息
     *
     * @param messageContent 消息内容
     */
    private void syncToSender(MessageContent messageContent) {
        ClientInfo senderClient = new ClientInfo(
                messageContent.getAppId(),
                messageContent.getClientType(),
                messageContent.getImei());
        messageProducer.sendToOtherClients(
                messageContent.getFromId(),
                GroupEventCommand.MSG_GROUP,
                messageContent,
                senderClient);
    }

    /**
     * 分发消息给接收方所有在线端
     *
     * @param messageContent 消息内容
     */
    private void dispatchToReceiver(GroupChatMessageContent messageContent) {
        for (String memberId : messageContent.getMemberIds()) {
            if (!memberId.equals(messageContent.getFromId())) {
                messageProducer.sendToAllClients(
                        memberId,
                        GroupEventCommand.MSG_GROUP,
                        messageContent,
                        messageContent.getAppId());
            }
        }
    }

    /**
     * 处理新消息
     * <p>
     * 完整处理流程：回调 → 生成Seq → 持久化 → 分发 → 缓存
     *
     * @param messageContent 消息内容
     */
    private void handleNewMessage(GroupChatMessageContent messageContent) {
        // 1. 前置回调校验
        ResponseVO callbackResponse = executeBeforeCallback(messageContent);
        if (!callbackResponse.isSuccess()) {
            // 回调失败，返回ACK告知发送方
            sendAck(messageContent, callbackResponse);
            return;
        }

        // 2. 生成消息序列号
        // Seq用于客户端排序，格式：appId + Seq + conversationId
        long seq = generateMessageSequence(messageContent);
        messageContent.setMessageSequence(seq);

        // 3. 异步执行持久化和分发
        try {
            messageProcessExecutor.execute(() -> {
                try {
                    // 3.1 持久化消息
                    persistMessage(messageContent);

                    List<String> groupMemberIds = imGroupMemberService.getGroupMemberIds(
                            messageContent.getGroupId(),
                            messageContent.getAppId());
                    messageContent.setMemberIds(groupMemberIds);

                    // 3.2 存储离线消息
                    storeOfflineMessage(messageContent, groupMemberIds);

                    // 3.3 返回ACK给发送方当前端
                    sendAck(messageContent, ResponseVO.successResponse());

                    // 3.4 同步消息到发送方其他在线端
                    syncToSender(messageContent);

                    // 3.5 分发消息给接收方所有在线端
                    dispatchToReceiver(messageContent);

                    // 3.6 缓存消息（用于防重）
                    messageStoreService.setMessageCacheByMessageId(
                            messageContent.getAppId(),
                            messageContent.getMessageId(),
                            messageContent);

                    // 3.7 后置回调
                    executeAfterCallback(messageContent);

                    log.info("新消息处理完成: messageId={}", messageContent.getMessageId());
                } catch (Exception e) {
                    log.error("处理新消息异常: messageId={}, from={}, groupId={}",
                            messageContent.getMessageId(), messageContent.getFromId(),
                            messageContent.getGroupId(), e);
                    // TODO 可以在这里添加补偿机制：重试、告警、记录失败日志等
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("线程池队列已满，拒绝处理新消息: messageId={}", messageContent.getMessageId(), e);
            // 队列满时，返回失败ACK
            sendAck(messageContent, ResponseVO.errorResponse());
        }
    }

    /**
     * 生成消息序列号
     *
     * @param messageContent 消息内容
     * @return 序列号
     */
    private long generateMessageSequence(GroupChatMessageContent messageContent) {
        // 定义群聊消息的 Sequence, 客户端根据 seq 进行排序
        // key: appId + Seq + (from + toId) / groupId
        String seqKey = messageContent.getAppId() + ":" + Constants.SeqConstants.GROUP_MESSAGE_SEQ + ":"
                + messageContent.getGroupId();

        return redisSequence.doGetSeq(seqKey);
    }

    /**
     * 持久化消息
     *
     * @param messageContent 消息内容
     */
    private void persistMessage(GroupChatMessageContent messageContent) {
        messageStoreService.storeGroupMessage(messageContent);
    }

    /**
     * 存储离线消息
     *
     * @param messageContent 消息内容
     */
    private void storeOfflineMessage(GroupChatMessageContent messageContent, List<String> groupMemberIds) {
        OfflineMessageContent offlineMessage = new OfflineMessageContent();
        BeanUtils.copyProperties(messageContent, offlineMessage);
        offlineMessage.setToId(messageContent.getGroupId());
        offlineMessage.setConversationType(ConversationType.GROUP.getCode());
        messageStoreService.storeGroupOfflineMessage(offlineMessage, groupMemberIds);
    }

    /**
     * 执行前置回调
     *
     * @param messageContent 消息内容
     * @return 回调结果
     */
    private ResponseVO executeBeforeCallback(GroupChatMessageContent messageContent) {
        if (!appConfig.isSendMessageBeforeCallback()) {
            return ResponseVO.successResponse();
        }

        try {
            return callbackService.beforeCallback(
                    messageContent.getAppId(),
                    Constants.CallbackCommand.SEND_GROUP_MESSAGE_BEFORE,
                    JSONObject.toJSONString(messageContent));
        } catch (Exception e) {
            log.error("前置回调执行异常: messageId={}", messageContent.getMessageId(), e);
            return ResponseVO.errorResponse();
        }
    }

    /**
     * 执行后置回调
     *
     * @param messageContent 消息内容
     */
    private void executeAfterCallback(GroupChatMessageContent messageContent) {
        if (!appConfig.isSendMessageAfterCallback()) {
            return;
        }

        try {
            callbackService.afterCallback(
                    messageContent.getAppId(),
                    Constants.CallbackCommand.SEND_GROUP_MESSAGE_AFTER,
                    JSONObject.toJSONString(messageContent));
        } catch (Exception e) {
            log.error("后置回调执行异常: messageId={}", messageContent.getMessageId(), e);
        }
    }

    /**
     * 前置校验
     * 1. 这个用户是否被禁言 是否被禁用
     * 2. 发送方是否在群组内
     *
     * @param fromId
     * @param groupId
     * @param appId
     * @return
     */
    public ResponseVO serverPermissionCheck(String fromId, String groupId, Integer appId) {
        return checkSendMessageService.checkGroupMessage(fromId, groupId, appId);
    }

    public SendMessageResp send(SendGroupMessageReq req) {

        SendMessageResp sendMessageResp = new SendMessageResp();
        GroupChatMessageContent message = new GroupChatMessageContent();
        BeanUtils.copyProperties(req, message);

        messageStoreService.storeGroupMessage(message);

        sendMessageResp.setMessageId(message.getMessageId());
        sendMessageResp.setMessageTime(System.currentTimeMillis());
        //2.发消息给同步在线端
        syncToSender(message);
        //3.发消息给对方在线端
        dispatchToReceiver(message);

        return sendMessageResp;

    }
}
