package com.pd.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pd.im.codec.pack.friendship.ApproveFriendRequestPack;
import com.pd.im.codec.pack.friendship.ReadAllFriendRequestPack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.FriendshipEventCommand;
import com.pd.im.common.enums.friend.FriendRequestApprovalStatus;
import com.pd.im.common.enums.friend.FriendshipErrorCode;
import com.pd.im.common.exception.ApplicationException;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.service.friendship.dao.ImFriendShipRequestEntity;
import com.pd.im.service.friendship.dao.mapper.ImFriendShipRequestMapper;
import com.pd.im.service.friendship.model.req.ApproveFriendRequestReq;
import com.pd.im.service.friendship.model.req.FriendDto;
import com.pd.im.service.friendship.model.req.ReadFriendShipRequestReq;
import com.pd.im.service.friendship.service.ImFriendService;
import com.pd.im.service.friendship.service.ImFriendShipRequestService;
import com.pd.im.service.seq.RedisSequence;
import com.pd.im.service.utils.MessageProducer;
import com.pd.im.service.utils.UserSequenceRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Parker
 * @date 12/9/25
 */
@Slf4j
@Service
public class ImFriendShipRequestServiceImpl implements ImFriendShipRequestService {
    @Autowired
    ImFriendShipRequestMapper imFriendShipRequestMapper;

    @Autowired
    ImFriendService imFriendShipService;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    RedisSequence redisSequence;

    @Autowired
    UserSequenceRepository userSequenceRepository;

    @Override
    public ResponseVO addFriendshipRequest(String fromId, FriendDto dto, Integer appId) {
        LambdaQueryWrapper<ImFriendShipRequestEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImFriendShipRequestEntity::getAppId, appId);
        queryWrapper.eq(ImFriendShipRequestEntity::getFromId, fromId);
        queryWrapper.eq(ImFriendShipRequestEntity::getToId, dto.getToId());
        ImFriendShipRequestEntity request = imFriendShipRequestMapper.selectOne(queryWrapper);
        long seq = redisSequence.doGetSeq(appId + ":" + Constants.SeqConstants.FRIENDSHIP_REQUEST);

        if (request == null) {
            request = new ImFriendShipRequestEntity();
            request.setAddSource(dto.getAddSource());
            request.setAddWording(dto.getAddWording());
            request.setSequence(seq);
            request.setAppId(appId);
            request.setFromId(fromId);
            request.setToId(dto.getToId());
            request.setReadStatus(0);
            request.setApproveStatus(0);
            request.setRemark(dto.getRemark());
            request.setCreateTime(System.currentTimeMillis());
            imFriendShipRequestMapper.insert(request);
        } else {
            //修改记录内容 和更新时间
            if (StringUtils.isNotBlank(dto.getAddSource())) {
                request.setAddSource(dto.getAddSource());
            }
            if (StringUtils.isNotBlank(dto.getRemark())) {
                request.setRemark(dto.getRemark());
            }
            if (StringUtils.isNotBlank(dto.getAddWording())) {
                request.setAddWording(dto.getAddWording());
            }
            request.setSequence(seq);
            request.setApproveStatus(0);
            request.setReadStatus(0);
            imFriendShipRequestMapper.updateById(request);
        }

        userSequenceRepository.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.FRIENDSHIP_REQUEST, seq);

        //发送好友申请的tcp给接收方
        messageProducer.sendToAllClients(dto.getToId(), FriendshipEventCommand.FRIEND_REQUEST, request, appId);

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO approveFriendRequest(ApproveFriendRequestReq req) {
        ImFriendShipRequestEntity imFriendShipRequestEntity = imFriendShipRequestMapper.selectById(req.getId());
        if (imFriendShipRequestEntity == null) {
            throw new ApplicationException(FriendshipErrorCode.FRIEND_REQUEST_IS_NOT_EXIST);
        }
        if (!req.getOperator().equals(imFriendShipRequestEntity.getToId())) {
            //只能审批发给自己的好友请求
            throw new ApplicationException(FriendshipErrorCode.NOT_APPROVER_OTHER_MAN_REQUEST);
        }

        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP_REQUEST);
        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setApproveStatus(req.getStatus());
        update.setUpdateTime(System.currentTimeMillis());
        update.setSequence(seq);
        update.setId(req.getId());
        imFriendShipRequestMapper.updateById(update);
        userSequenceRepository.writeUserSeq(req.getAppId(), req.getOperator(), Constants.SeqConstants.FRIENDSHIP_REQUEST,
                seq);
        if (FriendRequestApprovalStatus.AGREE.getCode() == req.getStatus()) {
            //同意 ===> 去执行添加好友逻辑
            FriendDto dto = new FriendDto();
            dto.setAddSource(imFriendShipRequestEntity.getAddSource());
            dto.setAddWording(imFriendShipRequestEntity.getAddWording());
            dto.setRemark(imFriendShipRequestEntity.getRemark());
            dto.setToId(imFriendShipRequestEntity.getToId());
            ResponseVO responseVO = imFriendShipService.doAddFriend(req, imFriendShipRequestEntity.getFromId(), dto, req.getAppId());
//            if(!responseVO.isSuccess()){
////                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                return responseVO;
//            }
            if (!responseVO.isSuccess() && responseVO.getCode() != FriendshipErrorCode.TO_IS_YOUR_FRIEND.getCode()) {
                return responseVO;
            }
        }

        ApproveFriendRequestPack approveFriendRequestPack = new ApproveFriendRequestPack();
        approveFriendRequestPack.setId(req.getId());
        approveFriendRequestPack.setSequence(seq);
        approveFriendRequestPack.setStatus(req.getStatus());
        messageProducer.sendToOtherClients(imFriendShipRequestEntity.getToId(),
                FriendshipEventCommand.FRIEND_REQUEST_APPROVER, approveFriendRequestPack, new ClientInfo(req.getAppId(),
                        req.getClientType(), req.getImei()));
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO readFriendShipRequestReq(ReadFriendShipRequestReq req) {
        LambdaQueryWrapper<ImFriendShipRequestEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImFriendShipRequestEntity::getAppId, req.getAppId());
        query.eq(ImFriendShipRequestEntity::getToId, req.getFromId());

        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP_REQUEST);
        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setReadStatus(1);
        update.setSequence(seq);
        imFriendShipRequestMapper.update(update, query);
        userSequenceRepository.writeUserSeq(req.getAppId(), req.getOperator(), Constants.SeqConstants.FRIENDSHIP_REQUEST,
                seq);

        //TCP通知
        ReadAllFriendRequestPack readAllFriendRequestPack = new ReadAllFriendRequestPack();
        readAllFriendRequestPack.setFromId(req.getFromId());
        readAllFriendRequestPack.setSequence(seq);
        messageProducer.sendToOtherClients(req.getFromId(), FriendshipEventCommand.FRIEND_REQUEST_READ,
                readAllFriendRequestPack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getFriendRequest(String fromId, Integer appId) {
        LambdaQueryWrapper<ImFriendShipRequestEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImFriendShipRequestEntity::getAppId, appId);
        query.eq(ImFriendShipRequestEntity::getToId, fromId);

        List<ImFriendShipRequestEntity> requestList = imFriendShipRequestMapper.selectList(query);

        return ResponseVO.successResponse(requestList);
    }
}
