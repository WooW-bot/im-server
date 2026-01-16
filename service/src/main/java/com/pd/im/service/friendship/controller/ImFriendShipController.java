package com.pd.im.service.friendship.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.SyncRequest;
import com.pd.im.service.friendship.model.req.*;
import com.pd.im.service.friendship.service.ImFriendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 好友关系链控制器
 * 参考腾讯云文档: https://cloud.tencent.com/document/product/269/1569
 *
 * @author Parker
 * @date 12/8/25
 */
@RestController
@RequestMapping("v1/friendship")
public class ImFriendShipController {
    @Autowired
    ImFriendService imFriendShipService;

    /**
     * 导入好友关系 (仅管理员调用)
     * 参考: https://cloud.tencent.com/document/product/269/8301
     *
     * @param req   ImportFriendShipReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/importFriendShip")
    public ResponseVO importFriendShip(@RequestBody @Validated ImportFriendShipReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.importFriendShip(req);
    }

    /**
     * 添加好友
     * 参考: https://cloud.tencent.com/document/product/269/1643
     *
     * @param req   AddFriendReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/addFriend")
    public ResponseVO addFriend(@RequestBody @Validated AddFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.addFriend(req);
    }

    /**
     * 更新好友信息
     * 参考: https://cloud.tencent.com/document/product/269/12525
     *
     * @param req   UpdateFriendReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/updateFriend")
    public ResponseVO updateFriend(@RequestBody @Validated UpdateFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.updateFriend(req);
    }

    /**
     * 删除好友
     * 参考: https://cloud.tencent.com/document/product/269/1644
     *
     * @param req   DeleteFriendReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/deleteFriend")
    public ResponseVO deleteFriend(@RequestBody @Validated DeleteFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.deleteFriend(req);
    }

    /**
     * 删除所有好友 (仅管理员调用)
     * 参考: https://cloud.tencent.com/document/product/269/1645
     *
     * @param req   DeleteFriendReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/deleteAllFriend")
    public ResponseVO deleteAllFriend(@RequestBody @Validated DeleteFriendReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.deleteAllFriend(req);
    }

    /**
     * 获取所有好友关系
     * 参考: https://cloud.tencent.com/document/product/269/1647
     *
     * @param req   GetAllFriendShipReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/getAllFriendShip")
    public ResponseVO getAllFriendShip(@RequestBody @Validated GetAllFriendShipReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.getAllFriendShip(req);
    }

    /**
     * 获取好友关系状态
     * 参考: https://cloud.tencent.com/document/product/269/1646
     *
     * @param req   GetRelationReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/getRelation")
    public ResponseVO getRelation(@RequestBody @Validated GetRelationReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.getRelation(req);
    }

    /**
     * 校验好友关系
     * 参考: https://cloud.tencent.com/document/product/269/1646
     *
     * @param req   CheckFriendShipReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/checkFriend")
    public ResponseVO checkFriend(@RequestBody @Validated CheckFriendShipReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.checkFriendship(req);
    }

    /**
     * 拉黑用户
     * 参考: https://cloud.tencent.com/document/product/269/3718
     *
     * @param req   AddFriendShipBlackReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/addBlack")
    public ResponseVO addBlack(@RequestBody @Validated AddFriendShipBlackReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.addBlack(req);
    }

    /**
     * 移除黑名单
     * 参考: https://cloud.tencent.com/document/product/269/3719
     *
     * @param req   DeleteBlackReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/deleteBlack")
    public ResponseVO deleteBlack(@RequestBody @Validated DeleteBlackReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.deleteBlack(req);
    }

    /**
     * 校验黑名单
     * 参考: https://cloud.tencent.com/document/product/269/3725
     *
     * @param req   CheckFriendShipReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/checkBlack")
    public ResponseVO checkBlack(@RequestBody @Validated CheckFriendShipReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.checkBlack(req);
    }

    /**
     * 同步好友列表
     * 参考: https://cloud.tencent.com/document/product/269/1647
     *
     * @param req   SyncRequest
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/syncFriendshipList")
    public ResponseVO syncFriendshipList(@RequestBody @Validated SyncRequest req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipService.syncFriendshipList(req);
    }
}
