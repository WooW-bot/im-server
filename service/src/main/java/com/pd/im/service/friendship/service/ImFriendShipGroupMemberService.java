package com.pd.im.service.friendship.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.friendship.model.req.*;

/**
 * @author Parker
 * @date 12/9/25
 */
public interface ImFriendShipGroupMemberService {
    ResponseVO addGroupMember(AddFriendShipGroupMemberReq req);

    ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req);

    int doAddGroupMember(Long groupId, String toId);

    int clearGroupMember(Long groupId);
}
