package com.pd.im.service.message.service.sync;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.SyncReq;
import com.pd.im.common.model.message.MessageReadContent;
import com.pd.im.common.model.message.MessageReceiveAckContent;
import com.pd.im.common.model.message.RecallMessageContent;
import com.pd.im.service.message.service.P2PMessageService;

/**
 * @author Parker
 * @date 12/5/25
 */
public interface MessageSyncService {

    /**
     * 在线目标用户同步接收消息确认
     * 在 {@link P2PMessageService}
     * 和 {@link com.pd.im.service.message.mq.receiver.GroupChatOperateReceiver} 里被调度
     *
     * @param messageContent
     */
    void receiveMark(MessageReceiveAckContent messageContent);

    /**
     * 消息已读功能
     * 在 {@link P2PMessageService}
     * 和 {@link com.pd.im.service.message.mq.receiver.GroupChatOperateReceiver} 里被调度
     * 1. 更新会话 Seq
     * 2. 通知在线同步端发送指定 command
     * 3. 发送已读回执通知原消息发送方
     *
     * @param messageContent
     */
    void readMark(MessageReadContent messageContent);

    /**
     * 群消息已读功能
     * 在 {@link com.pd.im.service.message.mq.receiver.GroupChatOperateReceiver} 里被调度
     * 1. 更新会话 Seq
     * 2. 通知在线同步端发送指定 command
     * 3. 发送已读回执通知原消息发送方
     *
     * @param messageContent
     */
    void groupReadMark(MessageReadContent messageContent);

    /**
     * 增量拉取离线消息功能
     * @param req
     * @return
     */
    ResponseVO syncOfflineMessage(SyncReq req);

    //修改历史消息的状态
    //修改离线消息的状态
    //ack给发送方
    //发送给同步端
    //分发给消息的接收方
    void recallMessage(RecallMessageContent content);
}
