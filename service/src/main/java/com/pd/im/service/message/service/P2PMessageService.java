package com.pd.im.service.message.service;

import com.alibaba.fastjson.JSONObject;
import com.pd.im.codec.pack.message.ChatMessageAck;
import com.pd.im.codec.pack.message.MessageReceiveServerAckPack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.MessageCommand;
import com.pd.im.common.enums.conversation.ConversationType;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.common.model.message.MessageContent;
import com.pd.im.common.model.message.OfflineMessageContent;
import com.pd.im.service.callback.CallbackService;
import com.pd.im.service.message.model.req.SendMessageReq;
import com.pd.im.service.message.model.resp.SendMessageResp;
import com.pd.im.service.message.service.check.CheckSendMessageService;
import com.pd.im.service.message.service.store.MessageStoreService;
import com.pd.im.service.seq.RedisSequence;
import com.pd.im.service.utils.ConversationIdGenerate;
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
 * P2P消息处理服务
 * <p>
 * 负责单聊消息的完整处理流程：
 * 1. 消息防重校验（基于缓存）
 * 2. 消息持久化（异步）
 * 3. 发送ACK给发送方
 * 4. 同步消息到发送方其他在线端
 * 5. 分发消息给接收方所有在线端
 * 6. 离线用户的接收确认处理
 *
 * @author Parker
 * @date 12/5/25
 */
@Slf4j
@Service
public class P2PMessageService {
    @Autowired
    CheckSendMessageService checkSendMessageService;

    @Autowired
    private MessageProducer messageProducer;

    @Autowired
    private MessageStoreService messageStoreService;

    @Autowired
    private RedisSequence redisSequence;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CallbackService callbackService;

    @Autowired
    private ThreadPoolManager threadPoolManager;

    /**
     * 线程池名称
     */
    private static final String THREAD_POOL_NAME = "p2p-message-processor";

    /**
     * 消息处理线程池
     */
    private ThreadPoolExecutor messageProcessExecutor;

    /**
     * 初始化线程池
     */
    @PostConstruct
    public void init() {
        // 创建P2P消息处理线程池
        this.messageProcessExecutor = threadPoolManager.createThreadPool(
                THREAD_POOL_NAME,
                ThreadPoolManager.ThreadPoolConfig.create()
                        .corePoolSize(8)
                        .maxPoolSize(16)
                        .queueCapacity(2000)
                        .keepAliveSeconds(60L)
                        .slowTaskThreshold(1000L)
        );

        log.info("P2P消息处理线程池初始化完成: {}", THREAD_POOL_NAME);
    }

    /**
     * 处理P2P消息
     * <p>
     * 处理流程：
     * 1. 检查消息缓存（防重）
     * 2. 如果是重复消息，直接使用缓存的消息进行分发
     * 3. 如果是新消息，执行完整处理流程（回调、持久化、分发）
     *
     * @param messageContent 消息内容
     */
    public void process(MessageContent messageContent) {
        log.info("开始处理P2P消息: messageId={}, from={}, to={}",
                messageContent.getMessageId(), messageContent.getFromId(), messageContent.getToId());

        // 1. 检查消息缓存（防重）
        MessageContent cachedMessage = messageStoreService.getMessageCacheByMessageId(
                messageContent.getAppId(), messageContent.getMessageId(), MessageContent.class);

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
    private void handleDuplicateMessage(MessageContent cachedMessage) {
        try {
            messageProcessExecutor.execute(() -> {
                try {
                    // 1. 返回ACK给发送方当前端
                    sendAck(cachedMessage, ResponseVO.successResponse());

                    // 2. 同步消息到发送方其他在线端
                    syncToSender(cachedMessage);

                    // 3. 分发消息给接收方所有在线端
                    List<ClientInfo> receiverClients = dispatchToReceiver(cachedMessage);

                    // 4. 如果接收方离线，服务端代发接收确认
                    if (receiverClients.isEmpty()) {
                        sendReceiveAckByServer(cachedMessage);
                    }

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
     * 处理新消息
     * <p>
     * 完整处理流程：回调 → 生成Seq → 持久化 → 分发 → 缓存
     *
     * @param messageContent 消息内容
     */
    private void handleNewMessage(MessageContent messageContent) {
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

                    // 3.2 存储离线消息
                    storeOfflineMessage(messageContent);

                    // 3.3 返回ACK给发送方当前端
                    sendAck(messageContent, ResponseVO.successResponse());

                    // 3.4 同步消息到发送方其他在线端
                    syncToSender(messageContent);

                    // 3.5 分发消息给接收方所有在线端
                    List<ClientInfo> receiverClients = dispatchToReceiver(messageContent);

                    // 3.6 缓存消息（用于防重）
                    messageStoreService.setMessageCacheByMessageId(
                            messageContent.getAppId(),
                            messageContent.getMessageId(),
                            messageContent);

                    // 3.7 如果接收方离线，服务端代发接收确认
                    if (receiverClients.isEmpty()) {
                        sendReceiveAckByServer(messageContent);
                    }

                    // 3.8 后置回调
                    executeAfterCallback(messageContent);

                    log.info("新消息处理完成: messageId={}", messageContent.getMessageId());
                } catch (Exception e) {
                    log.error("处理新消息异常: messageId={}, from={}, to={}",
                            messageContent.getMessageId(), messageContent.getFromId(),
                            messageContent.getToId(), e);
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
     * 执行前置回调
     *
     * @param messageContent 消息内容
     * @return 回调结果
     */
    private ResponseVO executeBeforeCallback(MessageContent messageContent) {
        if (!appConfig.isSendMessageBeforeCallback()) {
            return ResponseVO.successResponse();
        }

        try {
            return callbackService.beforeCallback(
                    messageContent.getAppId(),
                    Constants.CallbackCommand.SEND_MESSAGE_BEFORE,
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
    private void executeAfterCallback(MessageContent messageContent) {
        if (!appConfig.isSendMessageAfterCallback()) {
            return;
        }

        try {
            callbackService.afterCallback(
                    messageContent.getAppId(),
                    Constants.CallbackCommand.SEND_MESSAGE_AFTER,
                    JSONObject.toJSONString(messageContent));
        } catch (Exception e) {
            log.error("后置回调执行异常: messageId={}", messageContent.getMessageId(), e);
        }
    }

    /**
     * 生成消息序列号
     *
     * @param messageContent 消息内容
     * @return 序列号
     */
    private long generateMessageSequence(MessageContent messageContent) {
        String conversationId = ConversationIdGenerate.generateP2PId(
                messageContent.getFromId(), messageContent.getToId());

        String seqKey = messageContent.getAppId() + ":" + Constants.SeqConstants.MESSAGE_SEQ + ":" + conversationId;

        return redisSequence.doGetSeq(seqKey);
    }

    /**
     * 持久化消息
     *
     * @param messageContent 消息内容
     */
    private void persistMessage(MessageContent messageContent) {
        messageStoreService.storeP2PMessage(messageContent);
    }

    /**
     * 存储离线消息
     *
     * @param messageContent 消息内容
     */
    private void storeOfflineMessage(MessageContent messageContent) {
        OfflineMessageContent offlineMessage = new OfflineMessageContent();
        BeanUtils.copyProperties(messageContent, offlineMessage);
        offlineMessage.setConversationType(ConversationType.P2P.getCode());
        offlineMessage.setMessageKey(messageContent.getMessageKey());
        messageStoreService.storeOfflineMessage(offlineMessage);
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
                MessageCommand.MSG_ACK,
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
                MessageCommand.MSG_P2P,
                messageContent,
                senderClient);
    }

    /**
     * 分发消息给接收方所有在线端
     *
     * @param messageContent 消息内容
     * @return 成功接收的客户端列表（空列表表示接收方离线）
     */
    private List<ClientInfo> dispatchToReceiver(MessageContent messageContent) {
        return messageProducer.sendToAllClients(
                messageContent.getToId(),
                MessageCommand.MSG_P2P,
                messageContent,
                messageContent.getAppId());
    }

    /**
     * 服务端代发接收确认
     * <p>
     * 当接收方离线时，服务端代替接收方发送接收确认ACK
     * 这样发送方就知道消息已经被服务器接收并存储
     *
     * @param messageContent 消息内容
     */
    private void sendReceiveAckByServer(MessageContent messageContent) {
        MessageReceiveServerAckPack ackPack = new MessageReceiveServerAckPack();
        ackPack.setFromId(messageContent.getToId());
        ackPack.setToId(messageContent.getFromId());
        ackPack.setMessageKey(messageContent.getMessageKey());
        ackPack.setMessageSequence(messageContent.getMessageSequence());
        ackPack.setServerSend(true);

        // 发送接收确认给发送方当前端
        ClientInfo senderClient = new ClientInfo(
                messageContent.getAppId(),
                messageContent.getClientType(),
                messageContent.getImei());
        messageProducer.sendToSpecificClient(
                messageContent.getFromId(),
                MessageCommand.MSG_RECEIVE_ACK,
                ackPack,
                senderClient);
    }

    /**
     * 前置校验
     * 1. 这个用户是否被禁言 是否被禁用
     * 2. 发送方和接收方是否是好友
     *
     * @param fromId
     * @param toId
     * @param appId
     * @return
     */
    public ResponseVO serverPermissionCheck(String fromId, String toId, Integer appId) {
        ResponseVO responseVO = checkSendMessageService.checkSenderForbidAndMute(fromId, appId);
        if (!responseVO.isSuccess()) {
            return responseVO;
        }
        responseVO = checkSendMessageService.checkFriendShip(fromId, toId, appId);
        return responseVO;
    }

    public SendMessageResp send(SendMessageReq req) {
        SendMessageResp sendMessageResp = new SendMessageResp();

        MessageContent message = new MessageContent();
        message.setAppId(req.getAppId());
        message.setClientType(req.getClientType());
        message.setImei(req.getImei());
        message.setMessageId(req.getMessageId());
        message.setFromId(req.getFromId());
        message.setToId(req.getToId());
        message.setMessageBody(req.getMessageBody());
        message.setMessageTime(req.getMessageTime());

        // 生成消息序列号
        long seq = generateMessageSequence(message);
        message.setMessageSequence(seq);

        //插入数据
        messageStoreService.storeP2PMessage(message);

        // 存储离线消息
        storeOfflineMessage(message);

        sendMessageResp.setMessageId(message.getMessageId());
        sendMessageResp.setMessageKey(message.getMessageKey());
        sendMessageResp.setMessageTime(System.currentTimeMillis());

        //2.发消息给同步在线端
        syncToSender(message);
        //3.发消息给对方在线端
        dispatchToReceiver(message);
        return sendMessageResp;
    }
}
