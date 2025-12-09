package com.pd.im.service.group.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pd.im.codec.pack.group.GroupMemberSpeakPack;
import com.pd.im.codec.proto.group.AddGroupMemberPack;
import com.pd.im.codec.proto.group.RemoveGroupMemberPack;
import com.pd.im.codec.proto.group.UpdateGroupMemberPack;
import com.pd.im.common.ResponseVO;
import com.pd.im.common.config.AppConfig;
import com.pd.im.common.constant.Constants;
import com.pd.im.common.enums.command.GroupEventCommand;
import com.pd.im.common.enums.group.GroupErrorCode;
import com.pd.im.common.enums.group.GroupMemberRole;
import com.pd.im.common.enums.group.GroupStatus;
import com.pd.im.common.enums.group.GroupType;
import com.pd.im.common.exception.ApplicationException;
import com.pd.im.common.model.ClientInfo;
import com.pd.im.service.callback.CallbackService;
import com.pd.im.service.group.dao.ImGroupEntity;
import com.pd.im.service.group.dao.ImGroupMemberEntity;
import com.pd.im.service.group.dao.mapper.ImGroupMemberMapper;
import com.pd.im.service.group.model.callback.AddMemberAfterCallback;
import com.pd.im.service.group.model.req.*;
import com.pd.im.service.group.model.resp.*;
import com.pd.im.service.group.service.ImGroupMemberService;
import com.pd.im.service.group.service.ImGroupService;
import com.pd.im.service.user.dao.ImUserDataEntity;
import com.pd.im.service.user.service.ImUserService;
import com.pd.im.service.utils.GroupMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Parker
 * @date 12/6/25
 */
@Service
@Slf4j
public class ImGroupMemberServiceImpl implements ImGroupMemberService {
    @Autowired
    ImGroupMemberMapper imGroupMemberMapper;

    @Autowired
    ImGroupService groupService;

    @Autowired
    AppConfig appConfig;

    @Autowired
    CallbackService callbackService;

    @Autowired
    ImUserService imUserService;

    @Autowired
    GroupMessageProducer groupMessageProducer;

    @Override
    public ResponseVO importGroupMember(ImportGroupMemberReq req) {
        List<AddMemberResp> resp = new ArrayList<>();
        ResponseVO<ImGroupEntity> groupResp = groupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }
        for (GroupMemberDto memberId : req.getMembers()) {
            ResponseVO responseVO = null;
            try {
                responseVO = addGroupMember(req.getGroupId(), req.getAppId(), memberId);
            } catch (Exception e) {
                e.printStackTrace();
                responseVO = ResponseVO.errorResponse();
            }
            AddMemberResp addMemberResp = new AddMemberResp();
            addMemberResp.setMemberId(memberId.getMemberId());
            if (responseVO.isOk()) {
                addMemberResp.setResult(0);
            } else if (responseVO.getCode() == GroupErrorCode.USER_IS_JOINED_GROUP.getCode()) {
                addMemberResp.setResult(2);
            } else {
                addMemberResp.setResult(1);
            }
            resp.add(addMemberResp);
        }
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO addMember(AddGroupMemberReq req) {
        List<AddMemberResp> resp = new ArrayList<>();

        boolean isAdmin = false;
        ResponseVO<ImGroupEntity> groupResp = groupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        List<GroupMemberDto> memberDtos = req.getMembers();
        if (appConfig.isAddGroupMemberBeforeCallback()) {
            ResponseVO responseVO = callbackService.beforeCallback(req.getAppId(), Constants.CallbackCommand.GROUP_MEMBER_ADD_BEFORE, JSONObject.toJSONString(req));
            if (!responseVO.isOk()) {
                return responseVO;
            }
            try {
                memberDtos = JSONArray.parseArray(JSONObject.toJSONString(responseVO.getData()), GroupMemberDto.class);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("GroupMemberAddBefore 回调失败：{}", req.getAppId());
            }
        }

        ImGroupEntity group = groupResp.getData();

        /**
         * 私有群（private）	类似普通微信群，创建后仅支持已在群内的好友邀请加群，且无需被邀请方同意或群主审批
         * 公开群（Public）	类似 QQ 群，创建后群主可以指定群管理员，需要群主或管理员审批通过才能入群
         * 群类型 1私有群（类似微信） 2公开群(类似qq）
         *
         */

        if (!isAdmin && GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
        }

        List<String> successId = new ArrayList<>();

        for (GroupMemberDto memberId : memberDtos) {
            ResponseVO responseVO = null;
            try {
                responseVO = addGroupMember(req.getGroupId(), req.getAppId(), memberId);
            } catch (Exception e) {
                e.printStackTrace();
                responseVO = ResponseVO.errorResponse();
            }
            AddMemberResp addMemberResp = new AddMemberResp();
            addMemberResp.setMemberId(memberId.getMemberId());
            if (responseVO.isOk()) {
                successId.add(memberId.getMemberId());
                addMemberResp.setResult(0);
            } else if (responseVO.getCode() == GroupErrorCode.USER_IS_JOINED_GROUP.getCode()) {
                addMemberResp.setResult(2);
                addMemberResp.setResultMessage(responseVO.getMsg());
            } else {
                addMemberResp.setResult(1);
                addMemberResp.setResultMessage(responseVO.getMsg());
            }
            resp.add(addMemberResp);
        }

        AddGroupMemberPack addGroupMemberPack = new AddGroupMemberPack();
        addGroupMemberPack.setGroupId(req.getGroupId());
        addGroupMemberPack.setMembers(successId);
        groupMessageProducer.producer(req.getOperator(), GroupEventCommand.ADDED_MEMBER, addGroupMemberPack,
                new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        if (appConfig.isAddGroupMemberAfterCallback()) {
            AddMemberAfterCallback dto = new AddMemberAfterCallback();
            dto.setGroupId(req.getGroupId());
            dto.setGroupType(group.getGroupType());
            dto.setMemberId(resp);
            dto.setOperator(req.getOperator());
            callbackService.afterCallback(req.getAppId(), Constants.CallbackCommand.GROUP_MEMBER_ADD_AFTER, JSONObject.toJSONString(dto));
        }

        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO removeMember(RemoveGroupMemberReq req) {
        List<AddMemberResp> resp = new ArrayList<>();
        boolean isAdmin = false;
        ResponseVO<ImGroupEntity> groupResp = groupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        ImGroupEntity group = groupResp.getData();

        if (!isAdmin) {
            if (GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {
                //获取操作人的权限 是管理员or群主or群成员
                ResponseVO<GetRoleInGroupResp> role = getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
                if (!role.isOk()) {
                    return role;
                }

                GetRoleInGroupResp data = role.getData();
                Integer roleInfo = data.getRole();

                boolean isOwner = roleInfo.equals(GroupMemberRole.OWNER.getCode());
                boolean isManager = roleInfo.equals(GroupMemberRole.MANAGER.getCode());

                if (!isOwner && !isManager) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }

                //私有群必须是群主才能踢人
                if (!isOwner && GroupTypeEnum.PRIVATE.getCode() == group.getGroupType()) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }

                //公开群管理员和群主可踢人，但管理员只能踢普通群成员
                if (GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {
                    //获取被踢人的权限
                    ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
                    if (!roleInGroupOne.isOk()) {
                        return roleInGroupOne;
                    }
                    GetRoleInGroupResp memberRole = roleInGroupOne.getData();
                    if (memberRole.getRole().equals(GroupMemberRole.OWNER.getCode())) {
                        throw new ApplicationException(GroupErrorCode.GROUP_OWNER_IS_NOT_REMOVE);
                    }
                    //是管理员并且被踢人不是群成员，无法操作
                    if (isManager && !memberRole.getRole().equals(GroupMemberRole.ORDINARY.getCode())) {
                        throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                    }
                }
            }
        }
        ResponseVO responseVO = removeGroupMember(req.getGroupId(), req.getAppId(), req.getMemberId());
        if (responseVO.isOk()) {
            RemoveGroupMemberPack removeGroupMemberPack = new RemoveGroupMemberPack();
            removeGroupMemberPack.setGroupId(req.getGroupId());
            removeGroupMemberPack.setMember(req.getMemberId());
            groupMessageProducer.producer(req.getMemberId(), GroupEventCommand.DELETED_MEMBER, removeGroupMemberPack,
                    new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
            if (appConfig.isDeleteGroupMemberAfterCallback()) {
                callbackService.afterCallback(req.getAppId(), Constants.CallbackCommand.GROUP_MEMBER_DELETE_AFTER,
                        JSONObject.toJSONString(req));
            }
        }

        return responseVO;
    }

    @Override
    public ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto) {
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(dto.getMemberId(), appId);
        if (!singleUserInfo.isOk()) {
            return singleUserInfo;
        }

        if (dto.getRole() != null && GroupMemberRole.OWNER.getCode().equals(dto.getRole())) {
            QueryWrapper<ImGroupMemberEntity> queryOwner = new QueryWrapper<>();
            queryOwner.eq("group_id", groupId);
            queryOwner.eq("app_id", appId);
            queryOwner.eq("role", GroupMemberRole.OWNER.getCode());
            Integer ownerNum = imGroupMemberMapper.selectCount(queryOwner);
            if (ownerNum > 0) {
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_HAVE_OWNER);
            }
        }

        QueryWrapper<ImGroupMemberEntity> query = new QueryWrapper<>();
        query.eq("group_id", groupId);
        query.eq("app_id", appId);
        query.eq("member_id", dto.getMemberId());
        ImGroupMemberEntity memberDto = imGroupMemberMapper.selectOne(query);

        long now = System.currentTimeMillis();
        if (memberDto == null) {
            //初次加群
            memberDto = new ImGroupMemberEntity();
            BeanUtils.copyProperties(dto, memberDto);
            memberDto.setGroupId(groupId);
            memberDto.setAppId(appId);
            memberDto.setJoinTime(now);
            int insert = imGroupMemberMapper.insert(memberDto);
            if (insert == 1) {
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        } else if (GroupMemberRole.LEAVE.getCode().equals(memberDto.getRole())) {
            //重新进群
            memberDto = new ImGroupMemberEntity();
            BeanUtils.copyProperties(dto, memberDto);
            memberDto.setJoinTime(now);
            int update = imGroupMemberMapper.update(memberDto, query);
            if (update == 1) {
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        }

        return ResponseVO.errorResponse(GroupErrorCode.USER_IS_JOINED_GROUP);
    }

    @Override
    public ResponseVO removeGroupMember(String groupId, Integer appId, String memberId) {
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(memberId, appId);
        if (!singleUserInfo.isOk()) {
            return singleUserInfo;
        }

        ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(groupId, memberId, appId);
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }

        GetRoleInGroupResp data = roleInGroupOne.getData();
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRole.LEAVE.getCode());
        imGroupMemberEntity.setLeaveTime(System.currentTimeMillis());
        imGroupMemberEntity.setGroupMemberId(data.getGroupMemberId());
        imGroupMemberMapper.updateById(imGroupMemberEntity);
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId) {
        GetRoleInGroupResp resp = new GetRoleInGroupResp();

        QueryWrapper<ImGroupMemberEntity> queryOwner = new QueryWrapper<>();
        queryOwner.eq("group_id", groupId);
        queryOwner.eq("app_id", appId);
        queryOwner.eq("member_id", memberId);

        ImGroupMemberEntity imGroupMemberEntity = imGroupMemberMapper.selectOne(queryOwner);
        if (imGroupMemberEntity == null || imGroupMemberEntity.getRole().equals(GroupMemberRole.LEAVE.getCode())) {
            return ResponseVO.errorResponse(GroupErrorCode.MEMBER_IS_NOT_JOINED_GROUP);
        }

        resp.setSpeakDate(imGroupMemberEntity.getSpeakDate());
        resp.setGroupMemberId(imGroupMemberEntity.getGroupMemberId());
        resp.setMemberId(imGroupMemberEntity.getMemberId());
        resp.setRole(imGroupMemberEntity.getRole());
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req) {
        if (req.getLimit() != null) {
            Page<ImGroupMemberEntity> objectPage = new Page<>(req.getOffset(), req.getLimit());
            QueryWrapper<ImGroupMemberEntity> query = new QueryWrapper<>();
            query.eq("app_id", req.getAppId());
            query.eq("member_id", req.getMemberId());
            IPage<ImGroupMemberEntity> imGroupMemberEntityPage = imGroupMemberMapper.selectPage(objectPage, query);

            Set<String> groupId = new HashSet<>();
            List<ImGroupMemberEntity> records = imGroupMemberEntityPage.getRecords();
            records.forEach(e -> {
                groupId.add(e.getGroupId());
            });

            return ResponseVO.successResponse(groupId);
        } else {
            return ResponseVO.successResponse(imGroupMemberMapper.getJoinedGroupIds(req.getAppId(), req.getMemberId()));
        }
    }

    @Override
    public ResponseVO<List<GroupMemberDto>> getGroupMembers(String groupId, Integer appId) {
        List<GroupMemberDto> groupMember = imGroupMemberMapper.getGroupMembers(appId, groupId);
        return ResponseVO.successResponse(groupMember);
    }

    @Override
    public List<String> getGroupMemberIds(String groupId, Integer appId) {
        return imGroupMemberMapper.getGroupMemberIds(appId, groupId);
    }

    @Override
    public List<GroupMemberDto> getGroupManagers(String groupId, Integer appId) {
        return imGroupMemberMapper.getGroupManagers(groupId, appId);
    }

    @Override
    public ResponseVO transferGroupMember(String owner, String groupId, Integer appId) {
        //更新旧群主
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRole.ORDINARY.getCode());
        UpdateWrapper<ImGroupMemberEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("app_id", appId);
        updateWrapper.eq("group_id", groupId);
        updateWrapper.eq("role", GroupMemberRole.OWNER.getCode());
        imGroupMemberMapper.update(imGroupMemberEntity, updateWrapper);

        //更新新群主
        ImGroupMemberEntity newOwner = new ImGroupMemberEntity();
        newOwner.setRole(GroupMemberRole.OWNER.getCode());
        UpdateWrapper<ImGroupMemberEntity> ownerWrapper = new UpdateWrapper<>();
        ownerWrapper.eq("app_id", appId);
        ownerWrapper.eq("group_id", groupId);
        ownerWrapper.eq("member_id", owner);
        imGroupMemberMapper.update(newOwner, ownerWrapper);

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO speak(SpeakMemberReq req) {
        ResponseVO<ImGroupEntity> groupResp = groupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }
        boolean isAdmin = false;
        boolean isOwner = false;
        boolean isManager = false;
        GetRoleInGroupResp memberRole = null;
        if (!isAdmin) {
            //获取操作人的权限 是管理员or群主or群成员
            ResponseVO<GetRoleInGroupResp> role = getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
            if (!role.isOk()) {
                return role;
            }
            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();
            isOwner = roleInfo.equals(GroupMemberRole.OWNER.getCode());
            isManager = roleInfo.equals(GroupMemberRole.MANAGER.getCode());
            if (!isOwner && !isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
            //获取被操作的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(),
                    req.getAppId());
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }
            memberRole = roleInGroupOne.getData();
            //被操作人是群主只能app管理员操作
            if (memberRole.getRole().equals(GroupMemberRole.OWNER.getCode())) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
            }
            //是管理员并且被操作人不是群成员，无法操作
            if (isManager && !memberRole.getRole().equals(GroupMemberRole.ORDINARY.getCode())) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }
        }
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        if (memberRole == null) {
            //获取被操作的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }
            memberRole = roleInGroupOne.getData();
        }
        imGroupMemberEntity.setGroupMemberId(memberRole.getGroupMemberId());
        if (req.getSpeakDate() > 0) {
            imGroupMemberEntity.setSpeakDate(System.currentTimeMillis() + req.getSpeakDate());
        } else {
            imGroupMemberEntity.setSpeakDate(req.getSpeakDate());
        }
        int i = imGroupMemberMapper.updateById(imGroupMemberEntity);
        if (i == 1) {
            GroupMemberSpeakPack pack = new GroupMemberSpeakPack();
            BeanUtils.copyProperties(req, pack);
            groupMessageProducer.producer(req.getOperator(), GroupEventCommand.SPEAK_GROUP_MEMBER, pack,
                    new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO updateGroupMember(UpdateGroupMemberReq req) {
        boolean isAdmin = false;
        ResponseVO<ImGroupEntity> group = groupService.getGroup(req.getGroupId(), req.getAppId());
        if (!group.isOk()) {
            return group;
        }
        ImGroupEntity groupData = group.getData();
        if (groupData.getStatus() == GroupStatus.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }
        //是否是自己修改自己的资料
        boolean isMeOperate = req.getOperator().equals(req.getMemberId());
        if (!isAdmin) {
            //昵称只能自己修改 权限只能群主或管理员修改
            if (StringUtils.isBlank(req.getAlias()) && !isMeOperate) {
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_ONESELF);
            }
            //私有群不能设置管理员
            if (groupData.getGroupType() == GroupTypeEnum.PRIVATE.getCode() &&
                    req.getRole() != null && (req.getRole().equals(GroupMemberRole.MANAGER.getCode()) ||
                    req.getRole().equals(GroupMemberRole.OWNER.getCode()))) {
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
            //如果要修改权限相关的则走下面的逻辑
            if (req.getRole() != null) {
                //获取被操作人的是否在群内
                ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
                if (!roleInGroupOne.isOk()) {
                    return roleInGroupOne;
                }
                //获取操作人权限
                ResponseVO<GetRoleInGroupResp> operateRoleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
                if (!operateRoleInGroupOne.isOk()) {
                    return operateRoleInGroupOne;
                }
                GetRoleInGroupResp data = operateRoleInGroupOne.getData();
                Integer roleInfo = data.getRole();
                boolean isOwner = roleInfo.equals(GroupMemberRole.OWNER.getCode());
                boolean isManager = roleInfo.equals(GroupMemberRole.MANAGER.getCode());
                //不是管理员不能修改权限
                if (req.getRole() != null && !isOwner && !isManager) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }
                //管理员只有群主能够设置
                if (req.getRole() != null && req.getRole().equals(GroupMemberRole.MANAGER.getCode()) && !isOwner) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }
            }
        }

        ImGroupMemberEntity update = new ImGroupMemberEntity();
        if (StringUtils.isNotBlank(req.getAlias())) {
            update.setAlias(req.getAlias());
        }
        //不能直接修改为群主
        if (req.getRole() != null && !req.getRole().equals(GroupMemberRole.OWNER.getCode())) {
            update.setRole(req.getRole());
        }

        UpdateWrapper<ImGroupMemberEntity> objectUpdateWrapper = new UpdateWrapper<>();
        objectUpdateWrapper.eq("app_id", req.getAppId());
        objectUpdateWrapper.eq("member_id", req.getMemberId());
        objectUpdateWrapper.eq("group_id", req.getGroupId());
        imGroupMemberMapper.update(update, objectUpdateWrapper);

        UpdateGroupMemberPack pack = new UpdateGroupMemberPack();
        BeanUtils.copyProperties(req, pack);
        groupMessageProducer.producer(req.getOperator(), GroupEventCommand.UPDATED_MEMBER, pack,
                new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO<Collection<String>> syncMemberJoinedGroup(String operator, Integer appId) {
        return ResponseVO.successResponse(imGroupMemberMapper.syncJoinedGroupIds(appId, operator,
                GroupMemberRole.LEAVE.getCode()));
    }
}
