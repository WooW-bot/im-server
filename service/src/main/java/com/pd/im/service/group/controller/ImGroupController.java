package com.pd.im.service.group.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.model.SyncRequest;
import com.pd.im.service.group.model.req.*;
import com.pd.im.service.group.service.ImGroupService;
import com.pd.im.service.message.service.GroupMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Parker
 * @date 12/8/25
 */
@RestController
@RequestMapping("v1/group")
public class ImGroupController {
    @Autowired
    ImGroupService groupService;

    @Autowired
    GroupMessageService groupMessageService;


    /**
     * 导入群组
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1615
     *
     * @param req        ImportGroupReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/importGroup")
    public ResponseVO importGroup(@RequestBody @Validated ImportGroupReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupService.importGroup(req);
    }

    /**
     * 创建群组
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1615
     *
     * @param req        CreateGroupReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/createGroup")
    public ResponseVO createGroup(@RequestBody @Validated CreateGroupReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupService.createGroup(req);
    }


    /**
     * 获取群组详细资料
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1616
     *
     * @param req   GetGroupReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/getGroupInfo")
    public ResponseVO getGroupInfo(@RequestBody @Validated GetGroupReq req, Integer appId) {
        req.setAppId(appId);
        return groupService.getGroup(req);
    }

    /**
     * 修改群组基础资料
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1620
     *
     * @param req        UpdateGroupReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/update")
    public ResponseVO update(@RequestBody @Validated UpdateGroupReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupService.updateBaseGroupInfo(req);
    }

    /**
     * 获取用户所加入的群组
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1625
     *
     * @param req        GetJoinedGroupReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/getJoinedGroup")
    public ResponseVO getJoinedGroup(@RequestBody @Validated GetJoinedGroupReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupService.getJoinedGroup(req);
    }


    /**
     * 解散群组
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1624
     *
     * @param req        DestroyGroupReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/destroyGroup")
    public ResponseVO destroyGroup(@RequestBody @Validated DestroyGroupReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupService.destroyGroup(req);
    }

    /**
     * 转让群组
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1633
     *
     * @param req        TransferGroupReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/transferGroup")
    public ResponseVO transferGroup(@RequestBody @Validated TransferGroupReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupService.transferGroup(req);
    }

    /**
     * 批量禁言和取消禁言
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1627
     *
     * @param req        MuteGroupReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/forbidSendMessage")
    public ResponseVO forbidSendMessage(@RequestBody @Validated MuteGroupReq req, Integer appId, String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return groupService.muteGroup(req);
    }

    /**
     * 在群组中发送普通消息
     * <p>
     * Refer: https://cloud.tencent.com/document/product/269/1629
     *
     * @param req        SendGroupMessageReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/sendMessage")
    public ResponseVO sendMessage(@RequestBody @Validated SendGroupMessageReq req, Integer appId,
                                  String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return ResponseVO.successResponse(groupMessageService.send(req));
    }

    @RequestMapping("/syncJoinedGroup")
    public ResponseVO syncJoinedGroup(@RequestBody @Validated SyncRequest req, Integer appId) {
        req.setAppId(appId);
        return groupService.syncJoinedGroupList(req);
    }
}
