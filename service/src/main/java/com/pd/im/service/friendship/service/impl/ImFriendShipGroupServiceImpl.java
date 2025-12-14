package com.pd.im.service.friendship.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pd.im.codec.pack.friendship.AddFriendGroupPack;
import com.pd.im.codec.pack.friendship.DeleteFriendGroupPack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.DeleteFlag;
import com.pd.im.common.enums.command.FriendshipEventCommand;
import com.pd.im.common.enums.friend.FriendshipErrorCode;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.service.friendship.dao.ImFriendShipGroupEntity;
import com.pd.im.service.friendship.dao.mapper.ImFriendShipGroupMapper;
import com.pd.im.service.friendship.model.req.*;
import com.pd.im.service.friendship.service.ImFriendShipGroupMemberService;
import com.pd.im.service.friendship.service.ImFriendShipGroupService;
import com.pd.im.service.seq.RedisSequence;
import com.pd.im.service.user.service.ImUserService;
import com.pd.im.service.utils.MessageProducer;
import com.pd.im.service.utils.UserSequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ImFriendShipGroupServiceImpl implements ImFriendShipGroupService {

    @Autowired
    ImFriendShipGroupMapper imFriendShipGroupMapper;

    @Autowired
    ImFriendShipGroupMemberService imFriendShipGroupMemberService;

    @Autowired
    ImUserService imUserService;

    @Autowired
    RedisSequence redisSequence;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    UserSequenceRepository userSequenceRepository;

    @Override
    @Transactional
    public ResponseVO addGroup(AddFriendShipGroupReq req) {

        LambdaQueryWrapper<ImFriendShipGroupEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImFriendShipGroupEntity::getGroupName, req.getGroupName())
                .eq(ImFriendShipGroupEntity::getAppId, req.getAppId())
                .eq(ImFriendShipGroupEntity::getFromId, req.getFromId())
                .eq(ImFriendShipGroupEntity::getDelFlag, DeleteFlag.NORMAL.getCode());

        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);

        if (entity != null) {
            return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);
        }

        //写入db
        ImFriendShipGroupEntity insert = new ImFriendShipGroupEntity();
        insert.setAppId(req.getAppId());
        insert.setCreateTime(System.currentTimeMillis());
        insert.setDelFlag(DeleteFlag.NORMAL.getCode());
        insert.setGroupName(req.getGroupName());
        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP_GROUP);
        insert.setSequence(seq);
        insert.setFromId(req.getFromId());
        try {
            int insert1 = imFriendShipGroupMapper.insert(insert);

            if (insert1 != 1) {
                return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_SHIP_GROUP_CREATE_ERROR);
            }
            if (CollectionUtil.isNotEmpty(req.getToIds())) {
                AddFriendShipGroupMemberReq addFriendShipGroupMemberReq = new AddFriendShipGroupMemberReq();
                addFriendShipGroupMemberReq.setFromId(req.getFromId());
                addFriendShipGroupMemberReq.setGroupName(req.getGroupName());
                addFriendShipGroupMemberReq.setToIds(req.getToIds());
                addFriendShipGroupMemberReq.setAppId(req.getAppId());
                imFriendShipGroupMemberService.addGroupMember(addFriendShipGroupMemberReq);
            }
        } catch (DuplicateKeyException e) {
            log.error("Failed to add friend group: duplicate key for groupName={}, fromId={}, appId={}", 
                    req.getGroupName(), req.getFromId(), req.getAppId(), e);
            return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);
        }

        AddFriendGroupPack addFriendGroupPack = new AddFriendGroupPack();
        addFriendGroupPack.setFromId(req.getFromId());
        addFriendGroupPack.setGroupName(req.getGroupName());
        addFriendGroupPack.setSequence(seq);
        messageProducer.sendToOtherClients(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_ADD,
                addFriendGroupPack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
        //写入seq
        userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FRIENDSHIP_GROUP, seq);

        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO deleteGroup(DeleteFriendShipGroupReq req) {

        for (String groupName : req.getGroupName()) {
            LambdaQueryWrapper<ImFriendShipGroupEntity> query = new LambdaQueryWrapper<>();
            query.eq(ImFriendShipGroupEntity::getGroupName, groupName)
                    .eq(ImFriendShipGroupEntity::getAppId, req.getAppId())
                    .eq(ImFriendShipGroupEntity::getFromId, req.getFromId())
                    .eq(ImFriendShipGroupEntity::getDelFlag, DeleteFlag.NORMAL.getCode());

            ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);

            if (entity != null) {
                long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP_GROUP);
                ImFriendShipGroupEntity update = new ImFriendShipGroupEntity();
                update.setSequence(seq);
                update.setGroupId(entity.getGroupId());
                update.setDelFlag(DeleteFlag.DELETE.getCode());
                imFriendShipGroupMapper.updateById(update);
                imFriendShipGroupMemberService.clearGroupMember(entity.getGroupId());
                DeleteFriendGroupPack deleteFriendGroupPack = new DeleteFriendGroupPack();
                deleteFriendGroupPack.setFromId(req.getFromId());
                deleteFriendGroupPack.setGroupName(groupName);
                deleteFriendGroupPack.setSequence(seq);
                //TCP通知
                messageProducer.sendToOtherClients(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_DELETE,
                        deleteFriendGroupPack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
                //写入seq
                userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FRIENDSHIP_GROUP, seq);
            }
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getGroup(String fromId, String groupName, Integer appId) {
        LambdaQueryWrapper<ImFriendShipGroupEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImFriendShipGroupEntity::getGroupName, groupName)
                .eq(ImFriendShipGroupEntity::getAppId, appId)
                .eq(ImFriendShipGroupEntity::getFromId, fromId)
                .eq(ImFriendShipGroupEntity::getDelFlag, DeleteFlag.NORMAL.getCode());

        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);
        if (entity == null) {
            return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_SHIP_GROUP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }

    @Override
    public Long updateSeq(String fromId, String groupName, Integer appId) {
        LambdaQueryWrapper<ImFriendShipGroupEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImFriendShipGroupEntity::getGroupName, groupName)
                .eq(ImFriendShipGroupEntity::getAppId, appId)
                .eq(ImFriendShipGroupEntity::getFromId, fromId);

        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);

        long seq = redisSequence.doGetSeq(appId + ":" + Constants.SeqConstants.FRIENDSHIP_GROUP);

        ImFriendShipGroupEntity group = new ImFriendShipGroupEntity();
        group.setGroupId(entity.getGroupId());
        group.setSequence(seq);
        imFriendShipGroupMapper.updateById(group);
        return seq;
    }

}
