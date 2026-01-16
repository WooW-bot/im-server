package com.pd.im.service.group.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.group.model.req.*;
import com.pd.im.service.group.service.ImGroupMemberService;
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
@RequestMapping("v1/group/member")
public class ImGroupMemberController {
    @Autowired
    ImGroupMemberService groupMemberService;

    /**
     * 导入群成员
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1618
     *
     * @param req        ImportGroupMemberReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/importGroupMember")
    public ResponseVO importGroupMember(@RequestBody @Validated ImportGroupMemberReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupMemberService.importGroupMember(req);
    }

    /**
     * 增加群成员
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1621
     *
     * @param req        AddGroupMemberReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/add")
    public ResponseVO addMember(@RequestBody @Validated AddGroupMemberReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupMemberService.addMember(req);
    }

    /**
     * 删除群成员
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1622
     *
     * @param req        RemoveGroupMemberReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/remove")
    public ResponseVO removeMember(@RequestBody @Validated RemoveGroupMemberReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupMemberService.removeMember(req);
    }

    /**
     * 修改群成员资料
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1623
     *
     * @param req        UpdateGroupMemberReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/update")
    public ResponseVO updateGroupMember(@RequestBody @Validated UpdateGroupMemberReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupMemberService.updateGroupMember(req);
    }

    /**
     * 批量禁言和取消禁言 (成员级别)
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1627
     *
     * @param req        MuteMemberReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/mute")
    public ResponseVO muteMember(@RequestBody @Validated MuteMemberReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupMemberService.muteMember(req);
    }
}
