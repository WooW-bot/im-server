package com.pd.im.service.user.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.user.model.req.*;
import com.pd.im.service.user.service.ImUserService;
import com.pd.im.service.user.service.ImUserStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Parker
 * @date 12/2/25
 * @description ImUserController类
 */
@Slf4j
@RestController
@RequestMapping("v1/user")
public class ImUserController {
    @Autowired
    ImUserService imUserService;

    @Autowired
    ImUserStatusService imUserStatusService;

    /**
     * 导入单个账号
     * 参考: https://cloud.tencent.com/document/product/269/1608
     *
     * @param req   ImportUserReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req, @RequestHeader("appId") Integer appId) {
        req.setAppId(appId);
        return imUserService.importUser(req);
    }

    /**
     * 删除账号
     * 参考: https://cloud.tencent.com/document/product/269/1609
     *
     * @param req   DeleteUserReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/deleteUser")
    public ResponseVO deleteUser(@RequestBody @Validated DeleteUserReq req, @RequestHeader("appId") Integer appId) {
        req.setAppId(appId);
        return imUserService.deleteUser(req);
    }

    /**
     * 登录接口，返回im地址
     * 参考: https://cloud.tencent.com/document/product/269/31999
     *
     * @param req   LoginReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req, @RequestHeader("appId") Integer appId) {
        req.setAppId(appId);
        return imUserService.login(req);
    }

    @RequestMapping("/getUserSequence")
    public ResponseVO getUserSequence(@RequestBody @Validated GetUserSequenceReq req, @RequestHeader("appId") Integer appId) {
        req.setAppId(appId);
        return imUserService.getUserSequence(req);
    }

    @RequestMapping("/subscribeUserOnlineStatus")
    public ResponseVO subscribeUserOnlineStatus(@RequestBody @Validated SubscribeUserOnlineStatusReq req, @RequestHeader("appId") Integer appId,
            @RequestHeader("identifier") String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        imUserStatusService.subscribeUserOnlineStatus(req);
        return ResponseVO.successResponse();
    }

    @RequestMapping("/setUserCustomerStatus")
    public ResponseVO setUserCustomerStatus(@RequestBody @Validated SetUserCustomerStatusReq req, @RequestHeader("appId") Integer appId,
            @RequestHeader("identifier") String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        imUserStatusService.setUserCustomerStatus(req);
        return ResponseVO.successResponse();
    }

    /**
     * 仅支持查询好友的在线状态
     * 参考: https://cloud.tencent.com/document/product/269/1643
     *
     * @param req        PullFriendOnlineStatusReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/queryFriendOnlineStatus")
    public ResponseVO queryFriendOnlineStatus(@RequestBody @Validated PullFriendOnlineStatusReq req, @RequestHeader("appId") Integer appId,
            @RequestHeader("identifier") String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return ResponseVO.successResponse(imUserStatusService.queryFriendOnlineStatus(req));
    }

    /**
     * 查询账户在线状态
     * 参考: https://cloud.tencent.com/document/product/269/2566
     *
     * @param req        PullUserOnlineStatusReq
     * @param appId      Integer
     * @param identifier String
     * @return ResponseVO
     */
    @RequestMapping("/queryUserOnlineStatus")
    public ResponseVO queryUserOnlineStatus(@RequestBody @Validated PullUserOnlineStatusReq req, @RequestHeader("appId") Integer appId,
            @RequestHeader("identifier") String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return ResponseVO.successResponse(imUserStatusService.queryUserOnlineStatus(req));
    }
}
