package com.pd.im.service.friendship.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.RequestBase;
import com.pd.im.common.model.SyncRequest;
import com.pd.im.service.friendship.model.req.*;

import java.util.List;

/**
 * @author Parker
 * @date 12/7/25
 */
public interface ImFriendService {
    ResponseVO importFriendShip(ImportFriendShipReq req);

    ResponseVO addFriend(AddFriendReq req);

    ResponseVO updateFriend(UpdateFriendReq req);

    ResponseVO deleteFriend(DeleteFriendReq req);

    ResponseVO deleteAllFriend(DeleteFriendReq req);

    ResponseVO getAllFriendShip(GetAllFriendShipReq req);

    /**
     * 查询指定好友关系 [是否落库持久化]
     *
     * @param req fromId、toId
     * @return
     */
    ResponseVO getRelation(GetRelationReq req);

    ResponseVO doAddFriend(RequestBase requestBase, String fromId, FriendDto dto, Integer appId);

    ResponseVO checkFriendship(CheckFriendShipReq req);

    ResponseVO addBlack(AddFriendShipBlackReq req);

    ResponseVO deleteBlack(DeleteBlackReq req);

    ResponseVO checkBlack(CheckFriendShipReq req);

    ResponseVO syncFriendshipList(SyncRequest req);

    List<String> getFriendIds(String userId, Integer appId);
}
