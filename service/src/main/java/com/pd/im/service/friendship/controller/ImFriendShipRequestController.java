package com.pd.im.service.friendship.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.friendship.model.req.*;
import com.pd.im.service.friendship.service.ImFriendShipRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 好友申请控制器
 * 参考腾讯云文档: https://cloud.tencent.com/document/product/269/1569
 *
 * @author Parker
 * @date 12/9/25
 */
@RestController
@RequestMapping("v1/friendshipRequest")
public class ImFriendShipRequestController {
    @Autowired
    ImFriendShipRequestService imFriendShipRequestService;

    /**
     * 审批好友申请
     * 参考: https://cloud.tencent.com/document/product/269/1643
     *
     * @param req        ApproveFriendRequestReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/approveFriendRequest")
    public ResponseVO approveFriendRequest(@RequestBody @Validated ApproveFriendRequestReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return imFriendShipRequestService.approveFriendRequest(req);
    }

    /**
     * 获取好友申请
     * 参考: https://cloud.tencent.com/document/product/269/1647
     *
     * @param req   GetFriendShipRequestReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/getFriendRequest")
    public ResponseVO getFriendRequest(@RequestBody @Validated GetFriendShipRequestReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipRequestService.getFriendRequest(req.getFromId(), req.getAppId());
    }

    /**
     * 已读好友申请
     * 参考: https://cloud.tencent.com/document/product/269/1647
     *
     * @param req   ReadFriendShipRequestReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/readFriendShipRequestReq")
    public ResponseVO readFriendShipRequestReq(@RequestBody @Validated ReadFriendShipRequestReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipRequestService.readFriendShipRequestReq(req);
    }
}
