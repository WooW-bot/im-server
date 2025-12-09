package com.pd.im.service.friendship.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.friendship.dao.ImFriendShipGroupEntity;
import com.pd.im.service.friendship.model.req.*;

/**
 * @author Parker
 * @date 12/9/25
 */
public interface ImFriendShipGroupService {
    ResponseVO addGroup(AddFriendShipGroupReq req);

    ResponseVO deleteGroup(DeleteFriendShipGroupReq req);

    ResponseVO<ImFriendShipGroupEntity> getGroup(String fromId, String groupName, Integer appId);

    Long updateSeq(String fromId, String groupName, Integer appId);
}
