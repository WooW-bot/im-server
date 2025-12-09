package com.pd.im.service.friendship.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.friendship.model.req.ApproveFriendRequestReq;
import com.pd.im.service.friendship.model.req.FriendDto;
import com.pd.im.service.friendship.model.req.ReadFriendShipRequestReq;

/**
 * @author Parker
 * @date 12/9/25
 */
public interface ImFriendShipRequestService {
    ResponseVO addFriendshipRequest(String fromId, FriendDto dto, Integer appId);

    ResponseVO approveFriendRequest(ApproveFriendRequestReq req);

    ResponseVO readFriendShipRequestReq(ReadFriendShipRequestReq req);

    ResponseVO getFriendRequest(String fromId, Integer appId);
}
