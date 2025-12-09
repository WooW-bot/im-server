package com.pd.im.service.conversation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.pd.im.codec.pack.conversation.DeleteConversationPack;
import com.pd.im.codec.pack.conversation.UpdateConversationPack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.ConversationEventCommand;
import com.pd.im.common.enums.conversation.ConversationErrorCode;
import com.pd.im.common.enums.conversation.ConversationTypeEnum;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.common.model.SyncReq;
import com.pd.im.common.model.SyncResp;
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

        QueryWrapper<ImConversationSetEntity> query = new QueryWrapper<>();
        query.eq("conversation_id", conversationId);
        query.eq("app_id", messageReadContent.getAppId());
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(query);

        /* key：appid + Seq
         * 这是因为 conversation seq 是为了对所有会话进行排序的，
         * 即客户端看到的消息从高到低是按照会话里的最新消息进行排序（置顶另外讨论）最新会话在最前
         * 而 p2p，group 的 seq 是为了对一个会话里的消息进行排序
         */
        long seq = redisSequence.doGetSeq(messageReadContent.getAppId() + ":" + Constants.SeqConstants.ConversationSeq);
        if (imConversationSetEntity == null) {
            imConversationSetEntity = new ImConversationSetEntity();
            imConversationSetEntity.setConversationId(conversationId);
            BeanUtils.copyProperties(messageReadContent, imConversationSetEntity);
            imConversationSetEntity.setReadSequence(messageReadContent.getMessageSequence());
            imConversationSetEntity.setToId(toId);
            imConversationSetEntity.setSequence(seq);
            imConversationSetMapper.insert(imConversationSetEntity);
            userSequenceRepository.writeUserSeq(messageReadContent.getAppId(),
                    messageReadContent.getFromId(), Constants.SeqConstants.ConversationSeq, seq);
        } else {
            imConversationSetEntity.setSequence(seq);
            imConversationSetEntity.setReadSequence(messageReadContent.getMessageSequence());
            imConversationSetMapper.readMark(imConversationSetEntity);
            userSequenceRepository.writeUserSeq(messageReadContent.getAppId(),
                    messageReadContent.getFromId(), Constants.SeqConstants.ConversationSeq, seq);
        }
    }

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

    @Override
    public ResponseVO updateConversation(UpdateConversationReq req) {
        if (req.getIsTop() == null && req.getIsMute() == null) {
            return ResponseVO.errorResponse(ConversationErrorCode.CONVERSATION_UPDATE_PARAM_ERROR);
        }
        QueryWrapper<ImConversationSetEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", req.getConversationId());
        queryWrapper.eq("app_id", req.getAppId());
        ImConversationSetEntity imConversationSetEntity = imConversationSetMapper.selectOne(queryWrapper);
        if (imConversationSetEntity != null) {
            long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.ConversationSeq);

            if (req.getIsMute() != null) {
                imConversationSetEntity.setIsTop(req.getIsTop());
            }
            if (req.getIsMute() != null) {
                imConversationSetEntity.setIsMute(req.getIsMute());
            }
            imConversationSetEntity.setSequence(seq);
            imConversationSetMapper.update(imConversationSetEntity, queryWrapper);
            userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(),
                    Constants.SeqConstants.ConversationSeq, seq);

            UpdateConversationPack pack = new UpdateConversationPack();
            pack.setConversationId(req.getConversationId());
            pack.setIsMute(imConversationSetEntity.getIsMute());
            pack.setIsTop(imConversationSetEntity.getIsTop());
            pack.setSequence(seq);
            pack.setConversationType(imConversationSetEntity.getConversationType());
            messageProducer.sendToOtherClients(req.getFromId(), ConversationEventCommand.CONVERSATION_UPDATE, pack,
                    new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO syncConversationSet(SyncReq req) {
        if (req.getMaxLimit() > appConfig.getConversationMaxCount()) {
            req.setMaxLimit(appConfig.getConversationMaxCount());
        }
        SyncResp<ImConversationSetEntity> resp = new SyncResp<>();
        //seq > req.getseq limit maxLimit
        QueryWrapper<ImConversationSetEntity> queryWrapper =
                new QueryWrapper<>();
        queryWrapper.eq("from_id", req.getOperator());
        queryWrapper.gt("sequence", req.getLastSequence());
        queryWrapper.eq("app_id", req.getAppId());
        queryWrapper.last(" limit " + req.getMaxLimit());
        queryWrapper.orderByAsc("sequence");
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
        if (ConversationTypeEnum.GROUP.getCode().equals(messageReadContent.getConversationType())) {
            // 会话类型为群聊，toId 赋值为 groupId
            toId = messageReadContent.getGroupId();
        }
        return toId;
    }
}
