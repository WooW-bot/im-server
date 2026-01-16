package com.pd.im.service.friendship.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.friendship.model.req.*;
import com.pd.im.service.friendship.service.ImFriendShipGroupMemberService;
import com.pd.im.service.friendship.service.ImFriendShipGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 好友分组控制器
 * 参考腾讯云文档: https://cloud.tencent.com/document/product/269/10107
 *
 * @author Parker
 * @date 12/9/25
 */
@RestController
@RequestMapping("v1/friendship/group")
public class ImFriendShipGroupController {
    @Autowired
    ImFriendShipGroupService imFriendShipGroupService;
    @Autowired
    ImFriendShipGroupMemberService imFriendShipGroupMemberService;

    /**
     * 添加好友分组
     * 参考: https://cloud.tencent.com/document/product/269/10107
     *
     * @param req   AddFriendShipGroupReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/add")
    public ResponseVO add(@RequestBody @Validated AddFriendShipGroupReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipGroupService.addGroup(req);
    }

    /**
     * 删除好友分组
     * 参考: https://cloud.tencent.com/document/product/269/10108
     *
     * @param req   DeleteFriendShipGroupReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/del")
    public ResponseVO del(@RequestBody @Validated DeleteFriendShipGroupReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipGroupService.deleteGroup(req);
    }

    /**
     * 添加分组成员
     * 参考: https://cloud.tencent.com/document/product/269/10107
     *
     * @param req   AddFriendShipGroupMemberReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/member/add")
    public ResponseVO memberAdd(@RequestBody @Validated AddFriendShipGroupMemberReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipGroupMemberService.addGroupMember(req);
    }

    /**
     * 删除分组成员
     * 参考: https://cloud.tencent.com/document/product/269/10108
     *
     * @param req   DeleteFriendShipGroupMemberReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/member/del")
    public ResponseVO memberDel(@RequestBody @Validated DeleteFriendShipGroupMemberReq req, Integer appId) {
        req.setAppId(appId);
        return imFriendShipGroupMemberService.delGroupMember(req);
    }
}
