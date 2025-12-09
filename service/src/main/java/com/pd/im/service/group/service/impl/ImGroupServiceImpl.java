package com.pd.im.service.group.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.pd.im.codec.pack.group.CreateGroupPack;
import com.pd.im.codec.pack.group.DestroyGroupPack;
import com.pd.im.codec.pack.group.UpdateGroupInfoPack;
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
import com.pd.im.common.model.SyncReq;
import com.pd.im.common.model.SyncResp;
import com.pd.im.service.callback.CallbackService;
import com.pd.im.service.group.dao.ImGroupEntity;
import com.pd.im.service.group.dao.mapper.ImGroupMapper;
import com.pd.im.service.group.model.callback.DestroyGroupCallbackDto;
import com.pd.im.service.group.model.req.*;
import com.pd.im.service.group.model.resp.GetGroupResp;
import com.pd.im.service.group.model.resp.GetJoinedGroupResp;
import com.pd.im.service.group.model.resp.GetRoleInGroupResp;
import com.pd.im.service.group.service.ImGroupMemberService;
import com.pd.im.service.group.service.ImGroupService;
import com.pd.im.service.seq.RedisSequence;
import com.pd.im.service.utils.GroupMessageProducer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Parker
 * @date 12/7/25
 */
@Service
public class ImGroupServiceImpl implements ImGroupService {
    @Autowired
    ImGroupMapper imGroupMapper;
    @Autowired
    ImGroupMemberService groupMemberService;
    @Autowired
    AppConfig appConfig;
    @Autowired
    CallbackService callbackService;
    @Autowired
    GroupMessageProducer groupMessageProducer;
    @Autowired
    RedisSequence redisSequence;

    @Override
    public ResponseVO importGroup(ImportGroupReq req) {
        // 1.生成群聊 groupId
        req.setGroupId(getGroupId(req.getAppId(), req.getGroupId()));

        ImGroupEntity imGroupEntity = new ImGroupEntity();
        if (req.getGroupType() == GroupType.PUBLIC.getCode() && StringUtils.isBlank(req.getOwnerId())) {
            throw new ApplicationException(GroupErrorCode.PUBLIC_GROUP_MUST_HAVE_OWNER);
        }
        if (req.getCreateTime() == null) {
            imGroupEntity.setCreateTime(System.currentTimeMillis());
        }
        imGroupEntity.setStatus(GroupStatus.NORMAL.getCode());
        BeanUtils.copyProperties(req, imGroupEntity);
        int insert = imGroupMapper.insert(imGroupEntity);
        if (insert != 1) {
            throw new ApplicationException(GroupErrorCode.IMPORT_GROUP_ERROR);
        }
        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO createGroup(CreateGroupReq req) {
        boolean isAdmin = false;
        if (!isAdmin) {
            req.setOwnerId(req.getOperator());
        }
        // 1.生成群聊 groupId
        req.setGroupId(getGroupId(req.getAppId(), req.getGroupId()));

        // 公开群需要指定群主
        if (req.getGroupType() == GroupType.PUBLIC.getCode() && StringUtils.isBlank(req.getOwnerId())) {
            throw new ApplicationException(GroupErrorCode.PUBLIC_GROUP_MUST_HAVE_OWNER);
        }

        ImGroupEntity imGroupEntity = new ImGroupEntity();
        BeanUtils.copyProperties(req, imGroupEntity);
        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.GROUP_SEQ);
        imGroupEntity.setSequence(seq);
        imGroupEntity.setCreateTime(System.currentTimeMillis());
        imGroupEntity.setStatus(GroupStatus.NORMAL.getCode());
        int insert = imGroupMapper.insert(imGroupEntity);

        GroupMemberDto groupMemberDto = new GroupMemberDto();
        groupMemberDto.setMemberId(req.getOwnerId());
        groupMemberDto.setRole(GroupMemberRole.OWNER.getCode());
        groupMemberDto.setJoinTime(System.currentTimeMillis());
        groupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), groupMemberDto);

        //插入群成员
        for (GroupMemberDto dto : req.getMember()) {
            groupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), dto);
        }

        if (appConfig.isCreateGroupAfterCallback()) {
            callbackService.afterCallback(req.getAppId(), Constants.CallbackCommand.CREATE_GROUP_AFTER,
                    JSONObject.toJSONString(imGroupEntity));
        }

        CreateGroupPack createGroupPack = new CreateGroupPack();
        BeanUtils.copyProperties(imGroupEntity, createGroupPack);
        groupMessageProducer.producer(req.getOperator(), GroupEventCommand.CREATED_GROUP, createGroupPack,
                new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO updateBaseGroupInfo(UpdateGroupReq req) {
        //1.判断群id是否存在
        LambdaQueryWrapper<ImGroupEntity> queryWrapper = new LambdaQueryWrapper<ImGroupEntity>()
                .eq(ImGroupEntity::getGroupId, req.getGroupId())
                .eq(ImGroupEntity::getAppId, req.getAppId());
        ImGroupEntity imGroupEntity = imGroupMapper.selectOne(queryWrapper);

        Optional.ofNullable(imGroupEntity)
                .orElseThrow(() -> new ApplicationException(GroupErrorCode.GROUP_IS_NOT_EXIST));
        Optional.ofNullable(imGroupEntity.getStatus())
                .filter(status -> status.equals(GroupStatus.DESTROY.getCode()))
                .orElseThrow(() -> new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY));

        boolean isAdmin = false;

        if (!isAdmin) {
            //不是后台调用需要检查权限
            ResponseVO<GetRoleInGroupResp> role = groupMemberService.getRoleInGroupOne(req.getGroupId(),
                    req.getOperator(), req.getAppId());
            if (!role.isSuccess()) {
                // 用户不在群内
                return role;
            }
            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();
            boolean isManager = Objects.equals(roleInfo, GroupMemberRole.MANAGER.getCode())
                    || Objects.equals(roleInfo, GroupMemberRole.OWNER.getCode());
            //公开群只能群主修改资料
            if (!isManager && GroupType.PUBLIC.getCode() == imGroupEntity.getGroupType()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
        }

        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.GROUP_SEQ);
        ImGroupEntity update = new ImGroupEntity();
        update.setSequence(seq);
        update.setGroupId(req.getGroupId());
        update.setAppId(req.getAppId());
        update.setGroupName(req.getGroupName());
        update.setMute(req.getMute());
        update.setApplyJoinType(req.getApplyJoinType());
        update.setIntroduction(req.getIntroduction());
        update.setNotification(req.getNotification());
        update.setPhoto(req.getPhoto());
        update.setMaxMemberCount(req.getMaxMemberCount());
        update.setExtra(req.getExtra());
        update.setUpdateTime(System.currentTimeMillis());
        int row = imGroupMapper.update(update, queryWrapper);
        if (row != 1) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
        }

        // 发送 TCP 通知
        UpdateGroupInfoPack pack = new UpdateGroupInfoPack();
        pack.setSequence(seq);
        pack.setGroupId(req.getGroupId());
        pack.setGroupName(req.getGroupName());
        pack.setMute(req.getMute());
        pack.setIntroduction(req.getIntroduction());
        pack.setNotification(req.getNotification());
        pack.setPhoto(req.getPhoto());
        groupMessageProducer.producer(req.getOperator(), GroupEventCommand.UPDATED_GROUP, pack,
                new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        // 之后回调
        if (appConfig.isModifyGroupAfterCallback()) {
            callbackService.afterCallback(req.getAppId(), Constants.CallbackCommand.UPDATE_GROUP_AFTER,
                    // 将修改之后的群聊信息查询给服务器 TCP 服务层
                    JSONObject.toJSONString(imGroupMapper.selectOne(queryWrapper)));
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getJoinedGroup(GetJoinedGroupReq req) {
        // 1. 获取用户加入所有群 ID
        ResponseVO<Collection<String>> memberJoinedGroup = groupMemberService.getMemberJoinedGroup(req);
        if (memberJoinedGroup.isSuccess()) {
            GetJoinedGroupResp resp = new GetJoinedGroupResp();
            if (CollectionUtils.isEmpty(memberJoinedGroup.getData())) {
                resp.setTotalCount(0);
                resp.setGroupList(new ArrayList<>());
                return ResponseVO.successResponse(resp);
            }
            LambdaQueryWrapper<ImGroupEntity> query = new LambdaQueryWrapper<ImGroupEntity>()
                    .eq(ImGroupEntity::getAppId, req.getAppId())
                    .in(ImGroupEntity::getGroupId, memberJoinedGroup.getData());
            if (CollectionUtils.isNotEmpty(req.getGroupType())) {
                query.in(ImGroupEntity::getGroupType, req.getGroupType());
            }
            List<ImGroupEntity> groupList = imGroupMapper.selectList(query);
            resp.setGroupList(groupList);
            // 获取单次拉取的群组数量，如果为空拉取所有群组
            int totalCount = ObjectUtil.isNotEmpty(req.getLimit())
                    // 当前用户所有群组
                    ? groupList.size()
                    // 传入的 groupId 中，有效的 group 数量
                    : imGroupMapper.selectCount(query);
            resp.setTotalCount(totalCount);
            return ResponseVO.successResponse(resp);
        } else {
            return memberJoinedGroup;
        }
    }

    @Override
    public ResponseVO destroyGroup(DestroyGroupReq req) {
        boolean isAdmin = false;
        LambdaQueryWrapper<ImGroupEntity> queryWrapper = new LambdaQueryWrapper<ImGroupEntity>()
                .eq(ImGroupEntity::getGroupId, req.getGroupId())
                .eq(ImGroupEntity::getAppId, req.getAppId());
        ImGroupEntity imGroupEntity = imGroupMapper.selectOne(queryWrapper);
        if (imGroupEntity == null) {
            throw new ApplicationException(GroupErrorCode.PRIVATE_GROUP_CAN_NOT_DESTORY);
        }
        if (imGroupEntity.getStatus() == GroupStatus.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }
        if (!isAdmin && imGroupEntity.getGroupType() == GroupType.PUBLIC.getCode() &&
                !imGroupEntity.getOwnerId().equals(req.getOperator())) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
        }
        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.GROUP_SEQ);
        ImGroupEntity update = new ImGroupEntity();
        update.setSequence(seq);
        update.setStatus(GroupStatus.DESTROY.getCode());
        int update1 = imGroupMapper.update(update, queryWrapper);
        if (update1 != 1) {
            throw new ApplicationException(GroupErrorCode.UPDATE_GROUP_BASE_INFO_ERROR);
        }
        // 发送 TCP 通知
        DestroyGroupPack pack = new DestroyGroupPack();
        pack.setSequence(seq);
        pack.setGroupId(req.getGroupId());
        groupMessageProducer.producer(req.getOperator(),
                GroupEventCommand.DESTROY_GROUP, pack,
                new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        // 事件处理回调
        if (appConfig.isModifyGroupAfterCallback()) {
            DestroyGroupCallbackDto dto = new DestroyGroupCallbackDto();
            dto.setGroupId(req.getGroupId());
            callbackService.afterCallback(req.getAppId(), Constants.CallbackCommand.DESTROY_GROUP_AFTER,
                    JSONObject.toJSONString(dto));
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO transferGroup(TransferGroupReq req) {
        ResponseVO<GetRoleInGroupResp> roleInGroupOne = groupMemberService.getRoleInGroupOne(req.getGroupId(),
                req.getOperator(), req.getAppId());
        if (!roleInGroupOne.isSuccess()) {
            return roleInGroupOne;
        }
        if (!Objects.equals(roleInGroupOne.getData().getRole(), GroupMemberRole.OWNER.getCode())) {
            return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
        }
        ResponseVO<GetRoleInGroupResp> newOwnerRole = groupMemberService.getRoleInGroupOne(req.getGroupId(),
                req.getOwnerId(), req.getAppId());
        if (!newOwnerRole.isSuccess()) {
            return newOwnerRole;
        }
        LambdaQueryWrapper<ImGroupEntity> queryWrapper = new LambdaQueryWrapper<ImGroupEntity>()
                .eq(ImGroupEntity::getGroupId, req.getGroupId())
                .eq(ImGroupEntity::getAppId, req.getAppId());
        ImGroupEntity imGroupEntity = imGroupMapper.selectOne(queryWrapper);
        if (imGroupEntity.getStatus() == GroupStatus.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }
        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.GROUP_SEQ);
        ImGroupEntity updateGroup = new ImGroupEntity();
        updateGroup.setSequence(seq);
        updateGroup.setOwnerId(req.getOwnerId());
        imGroupMapper.update(updateGroup, queryWrapper);
        groupMemberService.transferGroupMember(req.getOwnerId(), req.getGroupId(), req.getAppId());
        return ResponseVO.successResponse();
    }

    private String getGroupId(Integer appId, String groupId) {
        // 如果 groupId 不为空，检查一下数据库是否存有数据，如果有则报错
        if (!StringUtils.isEmpty(groupId)) {
            Integer nums = imGroupMapper.selectCount(
                    new LambdaQueryWrapper<ImGroupEntity>()
                            .eq(ImGroupEntity::getGroupId, groupId)
                            .eq(ImGroupEntity::getAppId, appId));
            if (nums > 0) {
                throw new ApplicationException(GroupErrorCode.GROUP_IS_EXIST);
            }
        }
        // 如果数据库没有则代表该群是导入的，需要重新生成 groupId 覆盖，或者 groupId 为空生成随机 groupId
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public ResponseVO getGroup(String groupId, Integer appId) {
        ImGroupEntity imGroupEntity = imGroupMapper.selectOne(new LambdaQueryWrapper<ImGroupEntity>()
                .eq(ImGroupEntity::getGroupId, groupId)
                .eq(ImGroupEntity::getAppId, appId));

        if (imGroupEntity == null) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }

        return ResponseVO.successResponse(imGroupEntity);
    }

    @Override
    public ResponseVO getGroup(GetGroupReq req) {
        ResponseVO group = getGroup(req.getGroupId(), req.getAppId());
        if (!group.isSuccess()) {
            return group;
        }
        GetGroupResp getGroupResp = new GetGroupResp();
        BeanUtils.copyProperties(group.getData(), getGroupResp);
        try {
            ResponseVO<List<GroupMemberDto>> groupMember = groupMemberService.getGroupMembers(req.getGroupId(),
                    req.getAppId());
            if (groupMember.isSuccess()) {
                getGroupResp.setMemberList(groupMember.getData());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseVO.successResponse(getGroupResp);
    }

    @Override
    public ResponseVO muteGroup(MuteGroupReq req) {
        ResponseVO<ImGroupEntity> groupResp = getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isSuccess()) {
            return groupResp;
        }
        if (groupResp.getData().getStatus() == GroupStatus.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }
        boolean isAdmin = false;
        if (!isAdmin) {
            //不是后台调用需要检查权限
            ResponseVO<GetRoleInGroupResp> role = groupMemberService
                    .getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
            if (!role.isSuccess()) {
                return role;
            }
            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();
            boolean isManager = Objects.equals(roleInfo, GroupMemberRole.MANAGER.getCode()) || Objects.equals(roleInfo, GroupMemberRole.OWNER.getCode());
            //公开群只能群主修改资料
            if (!isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
        }
        ImGroupEntity update = new ImGroupEntity();
        update.setMute(req.getMute());
        imGroupMapper.update(update, new LambdaUpdateWrapper<ImGroupEntity>()
                .eq(ImGroupEntity::getGroupId, req.getGroupId())
                .eq(ImGroupEntity::getAppId, req.getAppId()));
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO syncJoinedGroupList(SyncReq req) {
        if (req.getMaxLimit() > appConfig.getJoinGroupMaxCount()) {
            // 前端传输限制，保证一次增量拉取数据量不超过配置文件的值
            req.setMaxLimit(appConfig.getJoinGroupMaxCount());
        }
        SyncResp<ImGroupEntity> resp = new SyncResp<>();
        ResponseVO<Collection<String>> memberJoinedGroup = groupMemberService
                .syncMemberJoinedGroup(req.getOperator(), req.getAppId());
        if (memberJoinedGroup.isSuccess()) {
            Collection<String> data = memberJoinedGroup.getData();

            List<ImGroupEntity> list = imGroupMapper.selectList(
                    new LambdaQueryWrapper<ImGroupEntity>()
                            .eq(ImGroupEntity::getAppId, req.getAppId())
                            .in(ImGroupEntity::getGroupId, data)
                            .gt(ImGroupEntity::getSequence, req.getLastSequence())
                            .last("LIMIT " + req.getMaxLimit())
                            .orderByAsc(ImGroupEntity::getSequence));

            if (!CollectionUtils.isEmpty(list)) {
                ImGroupEntity maxSeqEntity = list.get(list.size() - 1);
                resp.setDataList(list);
                //设置最大seq
                Long maxSeq = imGroupMapper.getJoinGroupMaxSeq(data, req.getAppId());
                resp.setMaxSequence(maxSeq);
                //设置是否拉取完毕
                resp.setCompleted(maxSeqEntity.getSequence() >= maxSeq);
                return ResponseVO.successResponse(resp);
            }
        }
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public Long getUserGroupMaxSeq(String userId, Integer appId) {
        ResponseVO<Collection<String>> memberJoinedGroup = groupMemberService.syncMemberJoinedGroup(userId, appId);
        if (!memberJoinedGroup.isSuccess()) {
            throw new ApplicationException(500, "");
        }
        Long maxSeq = imGroupMapper.getJoinGroupMaxSeq(memberJoinedGroup.getData(), appId);
        return maxSeq;
    }
}
