package com.pd.im.service.message.service.sync.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.pd.im.codec.proto.MessageReadPack;
import com.pd.im.codec.proto.RecallMessageNotifyPack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.DelFlagEnum;
import com.pd.im.common.enums.command.Command;
import com.pd.im.common.enums.command.GroupEventCommand;
import com.pd.im.common.enums.command.MessageCommand;
import com.pd.im.common.enums.conversation.ConversationType;
import com.pd.im.common.enums.message.MessageErrorCode;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.common.model.SyncReq;
import com.pd.im.common.model.SyncResp;
import com.pd.im.common.model.message.MessageReadContent;
import com.pd.im.common.model.message.MessageReceiveAckContent;
import com.pd.im.common.model.message.OfflineMessageContent;
import com.pd.im.common.model.message.RecallMessageContent;
import com.pd.im.common.util.SnowflakeIdWorker;
import com.pd.im.service.conversation.service.ConversationService;
import com.pd.im.service.group.service.ImGroupMemberService;
import com.pd.im.service.message.dao.ImMessageBodyEntity;
import com.pd.im.service.message.dao.mapper.ImMessageBodyMapper;
import com.pd.im.service.message.service.sync.MessageSyncService;
import com.pd.im.service.seq.RedisSequence;
import com.pd.im.service.utils.ConversationIdGenerate;
import com.pd.im.service.utils.GroupMessageProducer;
import com.pd.im.service.utils.MessageProducer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 消息同步服务类
 * 用于处理消息接收确认，同步等操作
 *
 * @author Parker
 * @date 12/7/25
 */
@Service
public class MessageSyncServiceImpl implements MessageSyncService {
    @Autowired
    MessageProducer messageProducer;

    @Autowired
    ConversationService conversationService;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    ImMessageBodyMapper imMessageBodyMapper;

    @Autowired
    RedisSequence redisSequence;

    @Autowired
    ImGroupMemberService groupMemberService;

    @Autowired
    GroupMessageProducer groupMessageProducer;

    @Override
    public void receiveMark(MessageReceiveAckContent messageContent) {
        messageProducer.sendToAllClients(messageContent.getToId(), MessageCommand.MSG_RECEIVE_ACK, messageContent,
                messageContent.getAppId());
    }

    /**
     * 消息已读。更新会话的seq，通知在线的同步端发送指定command ，发送已读回执通知对方（消息发起方）我已读
     *
     * @param messageContent
     */
    @Override
    public void readMark(MessageReadContent messageContent) {
        conversationService.messageMarkRead(messageContent);
        MessageReadPack messageReadPack = new MessageReadPack();
        BeanUtils.copyProperties(messageContent, messageReadPack);
        // 同步给发送方其它端
        syncToSender(messageReadPack, messageContent, MessageCommand.MSG_READ_NOTIFY);
        // 发送给对方
        messageProducer.sendToAllClients(messageContent.getToId(), MessageCommand.MSG_READ_RECEIPT, messageReadPack,
                messageContent.getAppId());
    }

    @Override
    public void groupReadMark(MessageReadContent messageContent) {
        conversationService.messageMarkRead(messageContent);
        MessageReadPack messageReadPack = new MessageReadPack();
        BeanUtils.copyProperties(messageContent, messageReadPack);
        syncToSender(messageReadPack, messageContent, GroupEventCommand.MSG_GROUP_READ_NOTIFY);
        if (!messageContent.getFromId().equals(messageContent.getToId())) {
            messageProducer.sendToAllClients(messageReadPack.getToId(), GroupEventCommand.MSG_GROUP_READ_RECEIPT,
                    messageContent, messageContent.getAppId());
        }
    }

    @Override
    public ResponseVO syncOfflineMessage(SyncReq req) {
        SyncResp<OfflineMessageContent> resp = new SyncResp<>();
        // 构建用户离线消息队列key: appId:offline:userId
        String userKey = req.getAppId() + Constants.RedisConstants.OFFLINE_MESSAGE + req.getOperator();
        Long maxSeq = 0L;
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();
        // 获取最大的 seq
        Set set = zSetOperations.reverseRangeWithScores(userKey, 0, 0);
        if (!CollectionUtils.isEmpty(set)) {
            List list = new ArrayList(set);
            DefaultTypedTuple o = (DefaultTypedTuple) list.get(0);
            maxSeq = o.getScore().longValue();
        }

        List<OfflineMessageContent> respList = new ArrayList<>();
        resp.setMaxSequence(maxSeq);

        Set<ZSetOperations.TypedTuple> querySet = zSetOperations.rangeByScoreWithScores(
                userKey, req.getLastSequence(), maxSeq, 0, req.getMaxLimit());
        for (ZSetOperations.TypedTuple<String> typedTuple : querySet) {
            String value = typedTuple.getValue();
            OfflineMessageContent offlineMessageContent = JSONObject.parseObject(value, OfflineMessageContent.class);
            respList.add(offlineMessageContent);
        }
        resp.setDataList(respList);

        if (!CollectionUtils.isEmpty(respList)) {
            OfflineMessageContent offlineMessageContent = respList.get(respList.size() - 1);
            resp.setCompleted(maxSeq <= offlineMessageContent.getMessageKey());
        }

        return ResponseVO.successResponse(resp);
    }


    @Override
    public void recallMessage(RecallMessageContent content) {
        Long messageTime = content.getMessageTime();
        Long now = System.currentTimeMillis();

        RecallMessageNotifyPack pack = new RecallMessageNotifyPack();
        BeanUtils.copyProperties(content, pack);

        if (120000L < now - messageTime) {
            recallAck(pack, ResponseVO.errorResponse(MessageErrorCode.MESSAGE_RECALL_TIME_OUT), content);
            return;
        }

        QueryWrapper<ImMessageBodyEntity> query = new QueryWrapper<>();
        query.eq("app_id", content.getAppId());
        query.eq("message_key", content.getMessageKey());
        ImMessageBodyEntity body = imMessageBodyMapper.selectOne(query);

        if (body == null) {
            //TODO ack失败 不存在的消息不能撤回
            recallAck(pack, ResponseVO.errorResponse(MessageErrorCode.MESSAGEBODY_IS_NOT_EXIST), content);
            return;
        }

        if (body.getDelFlag() == DelFlagEnum.DELETE.getCode()) {
            recallAck(pack, ResponseVO.errorResponse(MessageErrorCode.MESSAGE_IS_RECALLED), content);
            return;
        }

        body.setDelFlag(DelFlagEnum.DELETE.getCode());
        imMessageBodyMapper.update(body, query);

        if (content.getConversationType().equals(ConversationType.P2P.getCode())) {
            // 找到fromId的队列
            String fromKey = content.getAppId() + Constants.RedisConstants.OFFLINE_MESSAGE + content.getFromId();
            // 找到toId的队列
            String toKey = content.getAppId() + Constants.RedisConstants.OFFLINE_MESSAGE + content.getToId();

            OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
            BeanUtils.copyProperties(content, offlineMessageContent);
            offlineMessageContent.setDelFlag(DelFlagEnum.DELETE.getCode());
            offlineMessageContent.setMessageKey(content.getMessageKey());
            offlineMessageContent.setConversationType(ConversationType.P2P.getCode());
            offlineMessageContent.setConversationId(ConversationService.convertConversationId(
                    offlineMessageContent.getConversationType(), content.getFromId(), content.getToId()));
            offlineMessageContent.setMessageBody(body.getMessageBody());

            String conversationId = ConversationIdGenerate.generateP2PId(
                    content.getFromId(), content.getToId());

            String seqKey = content.getAppId() + ":" + Constants.SeqConstants.MESSAGE_SEQ + ":" + conversationId;

            long seq = redisSequence.doGetSeq(seqKey);
            offlineMessageContent.setMessageSequence(seq);

            long messageKey = SnowflakeIdWorker.nextId();

            redisTemplate.opsForZSet().add(fromKey, JSONObject.toJSONString(offlineMessageContent), messageKey);
            redisTemplate.opsForZSet().add(toKey, JSONObject.toJSONString(offlineMessageContent), messageKey);

            //ack
            recallAck(pack, ResponseVO.successResponse(), content);
            //分发给同步端
            messageProducer.sendToOtherClients(content.getFromId(), MessageCommand.MSG_RECALL_NOTIFY, pack, content);
            //分发给对方
            messageProducer.sendToAllClients(content.getToId(), MessageCommand.MSG_RECALL_NOTIFY, pack, content.getAppId());
        } else {
            List<String> getGroupMemberIds = groupMemberService.getGroupMemberIds(content.getToId(), content.getAppId());
            long seq = redisSequence.doGetSeq(content.getAppId() + ":" + Constants.SeqConstants.MESSAGE_SEQ + ":"
                    + ConversationIdGenerate.generateP2PId(content.getFromId(), content.getToId()));
            //ack
            recallAck(pack, ResponseVO.successResponse(), content);
            //发送给同步端
            messageProducer.sendToOtherClients(content.getFromId(), MessageCommand.MSG_RECALL_NOTIFY, pack
                    , content);
            for (String memberId : getGroupMemberIds) {
                String toKey = content.getAppId() + ":" + Constants.SeqConstants.MESSAGE_SEQ + ":" + memberId;
                OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
                offlineMessageContent.setDelFlag(DelFlagEnum.DELETE.getCode());
                BeanUtils.copyProperties(content, offlineMessageContent);
                offlineMessageContent.setConversationType(ConversationType.GROUP.getCode());
                offlineMessageContent.setConversationId(ConversationService.convertConversationId(offlineMessageContent.getConversationType()
                        , content.getFromId(), content.getToId()));
                offlineMessageContent.setMessageBody(body.getMessageBody());
                offlineMessageContent.setMessageSequence(seq);
                redisTemplate.opsForZSet().add(toKey, JSONObject.toJSONString(offlineMessageContent), seq);

                groupMessageProducer.producer(content.getFromId(), MessageCommand.MSG_RECALL_NOTIFY, pack, content);
            }
        }
    }

    private void syncToSender(MessageReadPack pack, MessageReadContent content, Command command) {
        messageProducer.sendToOtherClients(content.getFromId(), command, pack, content);
    }

    private void recallAck(RecallMessageNotifyPack recallPack, ResponseVO<Object> responseVO, ClientInfo clientInfo) {
        messageProducer.sendToSpecificClient(recallPack.getFromId(), MessageCommand.MSG_RECALL_ACK, responseVO, clientInfo);
    }
}
