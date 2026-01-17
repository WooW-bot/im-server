package com.pd.im.service.user.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.enums.device.ClientType;
import com.pd.im.common.route.RouteHandler;
import com.pd.im.common.route.RouteInfo;
import com.pd.im.common.util.RouteInfoParser;
import com.pd.im.service.user.model.req.*;
import com.pd.im.service.user.service.ImUserService;
import com.pd.im.service.user.service.ImUserStatusService;
import com.pd.im.service.utils.ZKit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    RouteHandler routeHandler;

    @Autowired
    ImUserStatusService imUserStatusService;

    @Autowired
    ZKit zKit;

    /**
     * 导入单个账号
     * 参考: https://cloud.tencent.com/document/product/269/1608
     *
     * @param req   ImportUserReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req, Integer appId) {
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
    public ResponseVO deleteUser(@RequestBody @Validated DeleteUserReq req, Integer appId) {
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
    public ResponseVO login(@RequestBody @Validated LoginReq req, Integer appId) {
        req.setAppId(appId);
        ResponseVO login = imUserService.login(req);
        if (login.isSuccess()) {
            // 从 Zk 获取 im 地址，返回给 sdk
            List<String> allNode;
            if (ClientType.WEB.getCode().equals(req.getClientType())) {
                allNode = zKit.getAllWebNode();
            } else {
                allNode = zKit.getAllTcpNode();
            }
            String s = routeHandler.routeServer(allNode, req.getUserId());
            RouteInfo parse = RouteInfoParser.parse(s);
            return ResponseVO.successResponse(parse);
        }
        return ResponseVO.errorResponse();
    }

    @RequestMapping("/getUserSequence")
    public ResponseVO getUserSequence(@RequestBody @Validated GetUserSequenceReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.getUserSequence(req);
    }

    @RequestMapping("/subscribeUserOnlineStatus")
    public ResponseVO subscribeUserOnlineStatus(@RequestBody @Validated SubscribeUserOnlineStatusReq req, Integer appId,
            String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        imUserStatusService.subscribeUserOnlineStatus(req);
        return ResponseVO.successResponse();
    }

    @RequestMapping("/setUserCustomerStatus")
    public ResponseVO setUserCustomerStatus(@RequestBody @Validated SetUserCustomerStatusReq req, Integer appId,
            String identifier) {
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
    public ResponseVO queryFriendOnlineStatus(@RequestBody @Validated PullFriendOnlineStatusReq req, Integer appId,
            String identifier) {
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
    public ResponseVO queryUserOnlineStatus(@RequestBody @Validated PullUserOnlineStatusReq req, Integer appId,
            String identifier) {
        req.setAppId(appId);
        req.setOperator(identifier);
        return ResponseVO.successResponse(imUserStatusService.queryUserOnlineStatus(req));
    }
}
