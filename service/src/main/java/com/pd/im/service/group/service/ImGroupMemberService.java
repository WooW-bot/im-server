package com.pd.im.service.group.service;

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
    List<String> getGroupMemberIds(String groupId, Integer appId);
}
