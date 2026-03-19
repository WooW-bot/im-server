package com.pd.im.service.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pd.im.codec.pack.friendship.ApproveFriendRequestPack;
import com.pd.im.codec.pack.friendship.ReadAllFriendRequestPack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.RequestBase;
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
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 发送好友申请逻辑
     * 包含：自添加校验、双向互加检测（双向奔赴）、申请记录维护、多端同步通知
     */
    @Override
    @Transactional
    public ResponseVO<?> addFriendshipRequest(RequestBase requestBase, String fromId, FriendDto dto, Integer appId) {
        // 1. 兜底校验：禁止添加自己为好友
        if (fromId.equals(dto.getToId())) {
            return ResponseVO.errorResponse(FriendshipErrorCode.ADD_FRIEND_ERROR.getCode(), "不能添加自己为好友");
        }

        // 2. 核心优化：检测“双向互加”场景
        // 如果 A 之前申请过 B，此时 B 也主动加 A，则不再创建新申请，而是直接通过 doAddFriend 建立好友关系。
        LambdaQueryWrapper<ImFriendShipRequestEntity> reverseQuery = new LambdaQueryWrapper<>();
        reverseQuery.eq(ImFriendShipRequestEntity::getAppId, appId);
        reverseQuery.eq(ImFriendShipRequestEntity::getFromId, dto.getToId());
        reverseQuery.eq(ImFriendShipRequestEntity::getToId, fromId);
        reverseQuery.eq(ImFriendShipRequestEntity::getApproveStatus, FriendRequestApprovalStatus.NORMAL.getCode());
        ImFriendShipRequestEntity reverseRequest = imFriendShipRequestMapper.selectOne(reverseQuery);

        // 获取好友申请 Timeline 的序列号
        long seq = redisSequence.doGetSeq(appId + ":" + Constants.SeqConstants.FRIENDSHIP_REQUEST);

        if (reverseRequest != null) {
            // “双向奔赴”：直接尝试建立好友
            log.info("检测到双向申请: fromId={}, toId={}, 尝试直接建立好友关系", fromId, dto.getToId());
            ResponseVO<?> responseVO = imFriendShipService.doAddFriend(requestBase, fromId, dto, appId);
            
            if (responseVO.isSuccess()) {
                // 如果建立好友成功，则同步更新对方的申请状态为“已同意”
                reverseRequest.setApproveStatus(FriendRequestApprovalStatus.AGREE.getCode());
                reverseRequest.setUpdateTime(System.currentTimeMillis());
                reverseRequest.setReadStatus(1);
                reverseRequest.setSequence(seq);
                imFriendShipRequestMapper.updateById(reverseRequest);

                // 发送序列号更新
                userSequenceRepository.writeUserSeq(appId, fromId, Constants.SeqConstants.FRIENDSHIP_REQUEST, seq);

                // 通知发起方（即之前的申请人 A），告知其申请已“被系统自动同意”
                ApproveFriendRequestPack approveFriendRequestPack = new ApproveFriendRequestPack();
                approveFriendRequestPack.setId(reverseRequest.getId());
                approveFriendRequestPack.setSequence(seq);
                approveFriendRequestPack.setStatus(FriendRequestApprovalStatus.AGREE.getCode());
                messageProducer.sendToAllClients(reverseRequest.getFromId(),
                        FriendshipEventCommand.FRIEND_REQUEST_APPROVER, approveFriendRequestPack, appId);
            }
            return responseVO;
        }

        // 3. 正常申请流程
        LambdaQueryWrapper<ImFriendShipRequestEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ImFriendShipRequestEntity::getAppId, appId);
        queryWrapper.eq(ImFriendShipRequestEntity::getFromId, fromId);
        queryWrapper.eq(ImFriendShipRequestEntity::getToId, dto.getToId());
        ImFriendShipRequestEntity request = imFriendShipRequestMapper.selectOne(queryWrapper);

        if (request == null) {
            request = new ImFriendShipRequestEntity();
            request.setAppId(appId);
            request.setFromId(fromId);
            request.setToId(dto.getToId());
            request.setReadStatus(0);
            request.setApproveStatus(0);
            request.setCreateTime(System.currentTimeMillis());
        }

        // 更新/设置内容
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
        request.setReadStatus(0);
        request.setApproveStatus(0);
        request.setUpdateTime(System.currentTimeMillis());

        if (request.getId() == null) {
            imFriendShipRequestMapper.insert(request);
        } else {
            imFriendShipRequestMapper.updateById(request);
        }

        // 更新接收方的序列号
        userSequenceRepository.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.FRIENDSHIP_REQUEST, seq);

        // 1. 发送给接收方的所有端
        messageProducer.sendToAllClients(dto.getToId(), FriendshipEventCommand.FRIEND_REQUEST, request, appId);

        // 2. 发送给发起方的其它端
        if (requestBase != null) {
            messageProducer.sendToOtherClients(fromId, FriendshipEventCommand.FRIEND_REQUEST, request,
                    new ClientInfo(requestBase.getAppId(), requestBase.getClientType(), requestBase.getImei()));
        } else {
            messageProducer.sendToAllClients(fromId, FriendshipEventCommand.FRIEND_REQUEST, request, appId);
        }

        return ResponseVO.successResponse();
    }

    /**
     * 审批好友申请
     * 关键逻辑：AppId 校验、审批幂等性、先业务加人后更新状态（保证事务一致）
     */
    @Override
    @Transactional
    public ResponseVO<?> approveFriendRequest(ApproveFriendRequestReq req) {
        ImFriendShipRequestEntity imFriendShipRequestEntity = imFriendShipRequestMapper.selectById(req.getId());
        if (imFriendShipRequestEntity == null) {
            throw new ApplicationException(FriendshipErrorCode.FRIEND_REQUEST_IS_NOT_EXIST);
        }
        if (!req.getOperator().equals(imFriendShipRequestEntity.getToId())) {
            //只能审批发给自己的好友请求
            throw new ApplicationException(FriendshipErrorCode.NOT_APPROVER_OTHER_MAN_REQUEST);
        }

        // 增加 appId 校验，防止越权或逻辑错误
        if (!req.getAppId().equals(imFriendShipRequestEntity.getAppId())) {
            throw new ApplicationException(FriendshipErrorCode.FRIEND_REQUEST_IS_NOT_EXIST);
        }

        if (!FriendRequestApprovalStatus.NORMAL.isMe(imFriendShipRequestEntity.getApproveStatus())) {
            // 如果已经是同意或拒绝状态，原则上就不再重复处理，除非是“由拒绝转同意”
            if (imFriendShipRequestEntity.getApproveStatus().equals(req.getStatus())) {
                 return ResponseVO.successResponse(); // 状态一致，幂等返回
            }
        }

        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP_REQUEST);

        if (FriendRequestApprovalStatus.AGREE.isMe(req.getStatus())) {
            //同意 ===> 去执行添加好友逻辑
            FriendDto dto = new FriendDto();
            dto.setAddSource(imFriendShipRequestEntity.getAddSource());
            dto.setAddWording(imFriendShipRequestEntity.getAddWording());
            dto.setRemark(imFriendShipRequestEntity.getRemark());
            dto.setToId(imFriendShipRequestEntity.getToId());
            ResponseVO responseVO = imFriendShipService.doAddFriend(req, imFriendShipRequestEntity.getFromId(), dto, req.getAppId());
            
            if (!responseVO.isSuccess() && responseVO.getCode() != FriendshipErrorCode.TO_IS_YOUR_FRIEND.getCode()) {
                // 如果添加好友失败（如黑名单拦截），则不更新申请状态，直接返回错误
                return responseVO;
            }
        }

        // 执行到这里说明：要么是拒绝，要么是同意且添加好友成功
        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setApproveStatus(req.getStatus());
        update.setUpdateTime(System.currentTimeMillis());
        update.setReadStatus(1); // 审批动作隐含已读
        update.setSequence(seq);
        update.setId(req.getId());
        imFriendShipRequestMapper.updateById(update);

        userSequenceRepository.writeUserSeq(req.getAppId(), req.getOperator(), Constants.SeqConstants.FRIENDSHIP_REQUEST,
                seq);

        ApproveFriendRequestPack approveFriendRequestPack = new ApproveFriendRequestPack();
        approveFriendRequestPack.setId(req.getId());
        approveFriendRequestPack.setSequence(seq);
        approveFriendRequestPack.setStatus(req.getStatus());
        messageProducer.sendToOtherClients(imFriendShipRequestEntity.getToId(),
                FriendshipEventCommand.FRIEND_REQUEST_APPROVER, approveFriendRequestPack, new ClientInfo(req.getAppId(),
                        req.getClientType(), req.getImei()));
        
        // 审批通过/拒绝后，也要通知发起方（即申请人）
        messageProducer.sendToAllClients(imFriendShipRequestEntity.getFromId(),
                FriendshipEventCommand.FRIEND_REQUEST_APPROVER, approveFriendRequestPack, req.getAppId());
        return ResponseVO.successResponse();
    }

    /**
     * 将好友申请标记为已读
     */
    @Override
    @Transactional
    public ResponseVO<?> readFriendShipRequestReq(ReadFriendShipRequestReq req) {
        LambdaQueryWrapper<ImFriendShipRequestEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImFriendShipRequestEntity::getAppId, req.getAppId());
        query.eq(ImFriendShipRequestEntity::getToId, req.getFromId());

        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FRIENDSHIP_REQUEST);
        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setReadStatus(1);
        update.setSequence(seq);
        imFriendShipRequestMapper.update(update, query);
        
        // 更新接收方的序列号
        userSequenceRepository.writeUserSeq(req.getAppId(), req.getOperator(), Constants.SeqConstants.FRIENDSHIP_REQUEST,
                seq);

        // TCP 通知：告知其它端申请已读（多端同步）
        ReadAllFriendRequestPack readAllFriendRequestPack = new ReadAllFriendRequestPack();
        readAllFriendRequestPack.setFromId(req.getFromId());
        readAllFriendRequestPack.setSequence(seq);
        messageProducer.sendToOtherClients(req.getFromId(), FriendshipEventCommand.FRIEND_REQUEST_READ,
                readAllFriendRequestPack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        return ResponseVO.successResponse();
    }

    /**
     * 获取用户接收到的好友申请列表
     */
    @Override
    public ResponseVO<List<ImFriendShipRequestEntity>> getFriendRequest(String fromId, Integer appId) {
        LambdaQueryWrapper<ImFriendShipRequestEntity> query = new LambdaQueryWrapper<>();
        query.eq(ImFriendShipRequestEntity::getAppId, appId);
        query.eq(ImFriendShipRequestEntity::getToId, fromId);

        List<ImFriendShipRequestEntity> requestList = imFriendShipRequestMapper.selectList(query);

        return ResponseVO.successResponse(requestList);
    }
}
