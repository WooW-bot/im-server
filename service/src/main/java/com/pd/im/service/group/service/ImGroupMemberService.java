package com.pd.im.service.group.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.group.model.req.*;
import com.pd.im.service.group.model.resp.*;

import java.util.Collection;
import java.util.List;

/**
 * 私有群（private）	类似普通微信群，创建后仅支持已在群内的好友邀请加群，且无需被邀请方同意或群主审批
 * 公开群（Public）	类似 QQ 群，创建后群主可以指定群管理员，需要群主或管理员审批通过才能入群
 * 群类型 1私有群（类似微信） 2公开群(类似qq）
 *
 * @author Parker
 * @date 12/6/25
 */
public interface ImGroupMemberService {
    ResponseVO importGroupMember(ImportGroupMemberReq req);

    ResponseVO addMember(AddGroupMemberReq req);

    ResponseVO removeMember(RemoveGroupMemberReq req);

    /**
     * 【公有群调用本接口】
     * 添加群成员，拉人入群的逻辑，直接进入群聊。如果是后台管理员，则直接拉入群，
     * 否则只有私有群可以调用本接口，并且群成员也可以拉人入群.
     *
     * @param groupId
     * @param appId
     * @param dto     请求的用户信息
     * @return 群组成功添加该用户
     */
    ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto);

    ResponseVO removeGroupMember(String groupId, Integer appId, String memberId);

    /**
     * 获取该用户在群里的角色
     *
     * @param groupId
     * @param memberId
     * @param appId
     * @return
     */
    ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId);

    ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req);

    ResponseVO<List<GroupMemberDto>> getGroupMembers(String groupId, Integer appId);

    List<String> getGroupMemberIds(String groupId, Integer appId);

    List<GroupMemberDto> getGroupManagers(String groupId, Integer appId);

    ResponseVO transferGroupMember(String owner, String groupId, Integer appId);

    ResponseVO speak(SpeakMemberReq req);

    ResponseVO updateGroupMember(UpdateGroupMemberReq req);

    ResponseVO<Collection<String>> syncMemberJoinedGroup(String operator, Integer appId);
}
