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

    @RequestMapping("/add")
    public ResponseVO add(@RequestBody @Validated AddFriendShipGroupReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendShipGroupService.addGroup(req);
    }

    @RequestMapping("/del")
    public ResponseVO del(@RequestBody @Validated DeleteFriendShipGroupReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendShipGroupService.deleteGroup(req);
    }

    @RequestMapping("/member/add")
    public ResponseVO memberAdd(@RequestBody @Validated AddFriendShipGroupMemberReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendShipGroupMemberService.addGroupMember(req);
    }

    @RequestMapping("/member/del")
    public ResponseVO memberDel(@RequestBody @Validated DeleteFriendShipGroupMemberReq req, Integer appId)  {
        req.setAppId(appId);
        return imFriendShipGroupMemberService.delGroupMember(req);
    }
}
