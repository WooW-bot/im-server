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
    /**
     * 导入群成员
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1618
     *
     * @param req ImportGroupMemberReq
     * @return ResponseVO
     */
    ResponseVO importGroupMember(ImportGroupMemberReq req);

    /**
     * 增加群成员
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1621
     *
     * @param req AddGroupMemberReq
     * @return ResponseVO
     */
    ResponseVO addMember(AddGroupMemberReq req);

    /**
     * 删除群成员
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1622
     *
     * @param req RemoveGroupMemberReq
     * @return ResponseVO
     */
    ResponseVO removeMember(RemoveGroupMemberReq req);


    /**
     * 【公有群调用本接口】
     * 添加群成员，拉人入群的逻辑，直接进入群聊。如果是后台管理员，则直接拉入群，
     * 否则只有私有群可以调用本接口，并且群成员也可以拉人入群.
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1621
     *
     * @param groupId
     * @param appId
     * @param dto     请求的用户信息
     * @return 群组成功添加该用户
     */
    ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto);

    /**
     * 删除群成员 (Internal or Helper)
     *
     * @param groupId  String
     * @param appId    Integer
     * @param memberId String
     * @return ResponseVO
     */
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

    /**
     * 获取群组成员列表
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1617
     *
     * @param groupId String
     * @param appId   Integer
     * @return GroupMemberDto List
     */
    ResponseVO<List<GroupMemberDto>> getGroupMembers(String groupId, Integer appId);

    List<String> getGroupMemberIds(String groupId, Integer appId);

    List<GroupMemberDto> getGroupManagers(String groupId, Integer appId);

    ResponseVO transferGroupMember(String owner, String groupId, Integer appId);

    /**
     * 批量禁言和取消禁言 (成员级别)
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1627
     *
     * @param req MuteMemberReq
     * @return ResponseVO
     */
    ResponseVO muteMember(MuteMemberReq req);

    /**
     * 修改群成员资料
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1623
     *
     * @param req UpdateGroupMemberReq
     * @return ResponseVO
     */
    ResponseVO updateGroupMember(UpdateGroupMemberReq req);

    ResponseVO<Collection<String>> syncMemberJoinedGroup(String operator, Integer appId);
}
