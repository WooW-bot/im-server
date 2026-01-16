package com.pd.im.service.friendship.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.friendship.model.req.ApproveFriendRequestReq;
import com.pd.im.service.friendship.model.req.FriendDto;
import com.pd.im.service.friendship.model.req.ReadFriendShipRequestReq;

/**
 * 好友申请服务接口
 * 参考腾讯云文档: https://cloud.tencent.com/document/product/269/1643
 *
 * @author Parker
 * @date 12/9/25
 */
public interface ImFriendShipRequestService {
    /**
     * 添加好友申请记录
     *
     * @param fromId 发起方ID
     * @param dto    目标方信息
     * @param appId  应用ID
     * @return 操作结果
     */
    ResponseVO addFriendshipRequest(String fromId, FriendDto dto, Integer appId);

    /**
     * 审批好友申请
     * 参考: https://cloud.tencent.com/document/product/269/1643
     *
     * @param req 审批请求
     * @return 操作结果
     */
    ResponseVO approveFriendRequest(ApproveFriendRequestReq req);

    /**
     * 已读好友申请
     * 参考: https://cloud.tencent.com/document/product/269/1647
     *
     * @param req 已读请求
     * @return 操作结果
     */
    ResponseVO readFriendShipRequestReq(ReadFriendShipRequestReq req);

    /**
     * 获取好友申请列表
     * 参考: https://cloud.tencent.com/document/product/269/1647
     *
     * @param fromId 用户ID
     * @param appId  应用ID
     * @return 申请列表
     */
    ResponseVO getFriendRequest(String fromId, Integer appId);
}
