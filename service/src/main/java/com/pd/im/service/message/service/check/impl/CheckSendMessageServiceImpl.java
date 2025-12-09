package com.pd.im.service.message.service.check.impl;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.enums.friend.FriendshipErrorCode;
import com.pd.im.common.enums.friend.FriendshipStatus;
import com.pd.im.common.enums.group.GroupErrorCode;
import com.pd.im.common.enums.group.GroupMemberRole;
import com.pd.im.common.enums.group.GroupMuteType;
import com.pd.im.common.enums.message.MessageErrorCode;
import com.pd.im.common.enums.user.UserForbiddenFlag;
import com.pd.im.common.enums.user.UserSilentFlag;
import com.pd.im.service.friendship.dao.ImFriendShipEntity;
import com.pd.im.service.friendship.model.req.GetRelationReq;
import com.pd.im.service.friendship.service.ImFriendService;
import com.pd.im.service.group.dao.ImGroupEntity;
import com.pd.im.service.group.model.resp.GetRoleInGroupResp;
import com.pd.im.service.group.service.ImGroupMemberService;
import com.pd.im.service.group.service.ImGroupService;
import com.pd.im.service.message.service.check.CheckSendMessageService;
import com.pd.im.service.user.dao.ImUserDataEntity;
import com.pd.im.service.user.service.ImUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author Parker
 * @date 12/7/25
 */
@Service
public class CheckSendMessageServiceImpl implements CheckSendMessageService {
    @Autowired
    ImUserService userService;

    @Autowired
    ImFriendService friendService;

    @Autowired
    ImGroupService groupService;

    @Autowired
    ImGroupMemberService groupMemberService;

    @Autowired
    AppConfig appConfig;

    @Override
    public ResponseVO checkSenderForbidAndMute(String fromId, Integer appId) {
        // 查询用户是否存在
        ResponseVO<ImUserDataEntity> singleUserInfo = userService.getSingleUserInfo(fromId, appId);
        if (!singleUserInfo.isSuccess()) {
            return singleUserInfo;
        }

        // 用户是否被禁言或禁用
        ImUserDataEntity user = singleUserInfo.getData();
        if (user.getForbiddenFlag() == UserForbiddenFlag.FORBIBBEN.getCode()) {
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_FORBIBBEN);
        } else if (user.getSilentFlag() == UserSilentFlag.MUTE.getCode()) {
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_MUTE);
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO checkFriendShip(String fromId, String toId, Integer appId) {
        if (appConfig.isSendMessageCheckFriend()) {
            // 自己与对方的好友关系链是否正常【库表是否有这行记录: from2to】
            ResponseVO<ImFriendShipEntity> fromRelation = getRelation(fromId, toId, appId);
            if (!fromRelation.isSuccess()) {
                return fromRelation;
            }
            // 对方与自己的好友关系链是否正常【库表是否有这行记录: to2from】
            ResponseVO<ImFriendShipEntity> toRelation = getRelation(toId, fromId, appId);
            if (!toRelation.isSuccess()) {
                return toRelation;
            }

            // 检查自己是否删除对方
            if (!FriendshipStatus.FRIEND_STATUS_NORMAL.getCode().equals(fromRelation.getData().getStatus())) {
                return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_DELETED);
            }

            // 检查对方是否删除己方
            if (!FriendshipStatus.FRIEND_STATUS_NORMAL.getCode().equals(toRelation.getData().getStatus())) {
                return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_DELETED);
            }

            if (appConfig.isSendMessageCheckBlack()) {
                if (!FriendshipStatus.BLACK_STATUS_NORMAL.getCode().equals(fromRelation.getData().getBlack())) {
                    return ResponseVO.errorResponse(FriendshipErrorCode.FRIEND_IS_BLACK);
                }

                if (!Objects.equals(FriendshipStatus.BLACK_STATUS_NORMAL.getCode(), toRelation.getData().getBlack())) {
                    return ResponseVO.errorResponse(FriendshipErrorCode.TARGET_IS_BLACK_YOU);
                }
            }
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO checkGroupMessage(String fromId, String groupId, Integer appId) {
        ResponseVO responseVO = checkSenderForbidAndMute(fromId, appId);
        if (!responseVO.isSuccess()) {
            return responseVO;
        }

        // 数据库查询是否有该群
        ResponseVO<ImGroupEntity> group = groupService.getGroup(groupId, appId);
        if (!group.isSuccess()) {
            return group;
        }

        // 查询该成员是否在群，在群里为什么角色
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = groupMemberService.getRoleInGroupOne(groupId, fromId, appId);
        if (!roleInGroupOne.isSuccess()) {
            return roleInGroupOne;
        }
        GetRoleInGroupResp data = roleInGroupOne.getData();

        // 判断群是否被禁言
        // 如果禁言 只有裙管理和群主可以发言
        ImGroupEntity groupData = group.getData();
        boolean isGroupMute = GroupMuteType.MUTE.getCode().equals(groupData.getMute());
        boolean isManager = GroupMemberRole.MANAGER.getCode().equals(data.getRole());
        boolean isOwner = GroupMemberRole.OWNER.getCode().equals(data.getRole());
        if (isGroupMute && !(isManager || isOwner)) {
            return ResponseVO.errorResponse(GroupErrorCode.THIS_GROUP_IS_MUTE);
        }

        // 禁言过期时间大于当前时间
        if (data.getSpeakDate() != null && data.getSpeakDate() > System.currentTimeMillis()) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_MEMBER_IS_SPEAK);
        }

        return ResponseVO.successResponse();
    }

    private ResponseVO<ImFriendShipEntity> getRelation(String fromId, String toId, Integer appId) {
        GetRelationReq getRelationReq = new GetRelationReq();
        getRelationReq.setFromId(fromId);
        getRelationReq.setToId(toId);
        getRelationReq.setAppId(appId);
        ResponseVO<ImFriendShipEntity> relation = friendService.getRelation(getRelationReq);
        return relation;
    }
}
