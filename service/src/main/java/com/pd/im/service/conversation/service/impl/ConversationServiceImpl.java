package com.pd.im.service.conversation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.pd.im.codec.pack.conversation.DeleteConversationPack;
import com.pd.im.codec.pack.conversation.UpdateConversationPack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.ConversationEventCommand;
import com.pd.im.common.enums.conversation.ConversationErrorCode;
import com.pd.im.common.enums.conversation.ConversationType;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.common.model.SyncRequest;
import com.pd.im.common.model.SyncResponse;
import com.pd.im.common.model.message.MessageReadContent;
import com.pd.im.service.conversation.dao.ImConversationSetEntity;
import com.pd.im.service.conversation.dao.mapper.ImConversationSetMapper;
import com.pd.im.service.conversation.model.CreateConversationReq;
import com.pd.im.service.conversation.model.DeleteConversationReq;
import com.pd.im.service.conversation.model.UpdateConversationReq;
import com.pd.im.service.conversation.service.ConversationService;
import com.pd.im.service.seq.RedisSequence;
import com.pd.im.service.utils.MessageProducer;
import com.pd.im.service.utils.UserSequenceRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Parker
 * @date 12/5/25
 */
@Service
@Slf4j
public class ConversationServiceImpl implements ConversationService {
    @Autowired
    ImConversationSetMapper imConversationSetMapper;
    @Autowired
    MessageProducer messageProducer;
    @Autowired
    AppConfig appConfig;
    @Autowired
    RedisSequence redisSequence;
    @Autowired
    UserSequenceRepository userSequenceRepository;

    @Override
    public ResponseVO createConversation(CreateConversationReq req) {
        Integer conversationType = req.getConversationType();
        String conversationId = ConversationService.convertConversationId(
                conversationType, req.getFromId(), req.getToId());
        // 创建双方会话
        ImConversationSetEntity imConversationSetEntity = new ImConversationSetEntity();
        imConversationSetEntity.setAppId(req.getAppId());
        imConversationSetEntity.setFromId(req.getFromId());
        imConversationSetEntity.setToId(req.getToId());
        imConversationSetEntity.setConversationId(conversationId);
        imConversationSetEntity.setConversationType(conversationType);
        int insert = imConversationSetMapper.insert(imConversationSetEntity);
        if (insert != 1) {
            return ResponseVO.errorResponse(ConversationErrorCode.CONVERSATION_CREATE_FAIL);
        }
        return ResponseVO.successResponse();
    }

    @Override
    public void messageMarkRead(MessageReadContent messageReadContent) {
        // 抽离 toId, 有不同情况
        String toId = getToIdOrGroupId(messageReadContent);

        // conversationId: 1_fromId_toId
        String conversationId = ConversationService.convertConversationId(
                messageReadContent.getConversationType(), messageReadContent.getFromId(), toId);

        LambdaQueryWrapper<ImConversationSetEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImConversationSetEntity::getConversationId, conversationId);
        query.eq(ImConversationSetEntity::getAppId, messageReadContent.getAppId());
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(query);

        /* key：appid + Seq
         * 这是因为 conversation seq 是为了对所有会话进行排序的，
         * 即客户端看到的消息从高到低是按照会话里的最新消息进行排序（置顶另外讨论）最新会话在最前
         * 而 p2p，group 的 seq 是为了对一个会话里的消息进行排序
         */
        long seq = redisSequence.doGetSeq(messageReadContent.getAppId() + ":" + Constants.SeqConstants.CONVERSATION_SEQ);
        if (imConversationSetEntity == null) {
            imConversationSetEntity = new ImConversationSetEntity();
            imConversationSetEntity.setConversationId(conversationId);
            BeanUtils.copyProperties(messageReadContent, imConversationSetEntity);
            imConversationSetEntity.setReadSequence(messageReadContent.getMessageSequence());
            imConversationSetEntity.setToId(toId);
            imConversationSetEntity.setSequence(seq);
            imConversationSetMapper.insert(imConversationSetEntity);
            userSequenceRepository.writeUserSeq(messageReadContent.getAppId(),
                    messageReadContent.getFromId(), Constants.SeqConstants.CONVERSATION_SEQ, seq);
        } else {
            imConversationSetEntity.setSequence(seq);
            imConversationSetEntity.setReadSequence(messageReadContent.getMessageSequence());
            imConversationSetMapper.readMark(imConversationSetEntity);
            userSequenceRepository.writeUserSeq(messageReadContent.getAppId(),
                    messageReadContent.getFromId(), Constants.SeqConstants.CONVERSATION_SEQ, seq);
        }
    }

    /**
     * 删除会话
     * 参考: https://cloud.tencent.com/document/product/269/62119
     */
    @Override
    public ResponseVO deleteConversation(DeleteConversationReq req) {
        if (appConfig.getDeleteConversationSyncMode() == 1) {
            DeleteConversationPack pack = new DeleteConversationPack();
            pack.setConversationId(req.getConversationId());
            messageProducer.sendToOtherClients(req.getFromId(), ConversationEventCommand.CONVERSATION_DELETE, pack,
                    new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
        }
        return ResponseVO.successResponse();
    }

    /**
     * 更新会话 (置顶/免打扰/草稿)
     * 对应 SDK 的 setConversationDraft/pinConversation 等操作
     */
    @Override
    public ResponseVO updateConversation(UpdateConversationReq req) {
        if (req.getIsTop() == null && req.getIsMute() == null) {
            return ResponseVO.errorResponse(ConversationErrorCode.CONVERSATION_UPDATE_PARAM_ERROR);
        }
        LambdaQueryWrapper<ImConversationSetEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImConversationSetEntity::getConversationId, req.getConversationId());
        queryWrapper.eq(ImConversationSetEntity::getAppId, req.getAppId());
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(queryWrapper);
        if (imConversationSetEntity != null) {
            long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.CONVERSATION_SEQ);

            if (req.getIsTop() != null) {
                // 更新置顶状态
                imConversationSetEntity.setIsTop(req.getIsTop());
            }
            if (req.getIsMute() != null) {
                // 更新禁言状态
                imConversationSetEntity.setIsMute(req.getIsMute());
            }
            imConversationSetEntity.setSequence(seq);
            imConversationSetMapper.update(imConversationSetEntity, queryWrapper);
            userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(),
                    Constants.SeqConstants.CONVERSATION_SEQ, seq);

            UpdateConversationPack pack = new UpdateConversationPack();
            pack.setConversationId(req.getConversationId());
            pack.setIsMute(imConversationSetEntity.getIsMute());
            pack.setIsTop(imConversationSetEntity.getIsTop());
            pack.setSequence(seq);
            pack.setConversationType(imConversationSetEntity.getConversationType());
            messageProducer.sendToOtherClients(req.getFromId(), ConversationEventCommand.CONVERSATION_UPDATE, pack,
                    new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
            return ResponseVO.successResponse();
        }
        return ResponseVO.errorResponse(ConversationErrorCode.CONVERSATION_UPDATE_FAIL);
    }

    /**
     * 同步/获取会话列表
     * 参考: https://cloud.tencent.com/document/product/269/62118
     */
    @Override
    public ResponseVO syncConversationSet(SyncRequest req) {
        if (req.getMaxLimit() > appConfig.getConversationMaxCount()) {
            req.setMaxLimit(appConfig.getConversationMaxCount());
        }
        SyncResponse<ImConversationSetEntity> resp = new SyncResponse<>();
        //seq > req.getseq limit maxLimit
        LambdaQueryWrapper<ImConversationSetEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImConversationSetEntity::getFromId, req.getOperator());
        queryWrapper.gt(ImConversationSetEntity::getSequence, req.getLastSequence());
        queryWrapper.eq(ImConversationSetEntity::getAppId, req.getAppId());
        queryWrapper.last(" limit " + req.getMaxLimit());
        queryWrapper.orderByAsc(ImConversationSetEntity::getSequence);
        List<ImConversationSetEntity> list = imConversationSetMapper
                .selectList(queryWrapper);

        if (!CollectionUtils.isEmpty(list)) {
            ImConversationSetEntity maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            //设置最大seq
            Long friendShipMaxSeq = imConversationSetMapper.geConversationSetMaxSeq(req.getAppId(), req.getOperator());
            resp.setMaxSequence(friendShipMaxSeq);
            //设置是否拉取完毕
            resp.setCompleted(maxSeqEntity.getSequence() >= friendShipMaxSeq);
            return ResponseVO.successResponse(resp);
        }

        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }

    private static String getToIdOrGroupId(MessageReadContent messageReadContent) {
        // 会话类型为单聊，toId 赋值为目标用户
        String toId = messageReadContent.getToId();
        if (ConversationType.GROUP.getCode().equals(messageReadContent.getConversationType())) {
            // 会话类型为群聊，toId 赋值为 groupId
            toId = messageReadContent.getGroupId();
        }
        return toId;
    }
}
