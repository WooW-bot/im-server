package com.pd.im.service.user.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.common.enums.device.ClientType;
import com.pd.im.common.route.RouteHandler;
import com.pd.im.common.route.RouteInfo;
import com.pd.im.common.utils.RouteInfoParseUtil;
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

    @RequestMapping("importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req) {
        return imUserService.importUser(req);
    }

    @RequestMapping("/deleteUser")
    public ResponseVO deleteUser(@RequestBody @Validated DeleteUserReq req) {
        return imUserService.deleteUser(req);
    }

    /**
     * @param req
     * @return im的登录接口，返回im地址
     */
    @RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req) {
        ResponseVO login = imUserService.login(req);
        if (login.isOk()) {
            // 从 Zk 获取 im 地址，返回给 sdk
            List<String> allNode;
            if (ClientType.WEB.getCode().equals(req.getClientType())) {
                allNode = zKit.getAllWebNode();
            } else {
                allNode = zKit.getAllTcpNode();
            }
            String s = routeHandler.routeServer(allNode, req.getUserId());
            RouteInfo parse = RouteInfoParseUtil.parse(s);
            return ResponseVO.successResponse(parse);
        }
        return ResponseVO.errorResponse();
    }

    @RequestMapping("/getUserSequence")
    public ResponseVO getUserSequence(@RequestBody @Validated GetUserSequenceReq req) {
        return imUserService.getUserSequence(req);
    }

    @RequestMapping("/subscribeUserOnlineStatus")
    public ResponseVO subscribeUserOnlineStatus(@RequestBody @Validated SubscribeUserOnlineStatusReq req) {
        imUserStatusService.subscribeUserOnlineStatus(req);
        return ResponseVO.successResponse();
    }

    @RequestMapping("/setUserCustomerStatus")
    public ResponseVO setUserCustomerStatus(@RequestBody @Validated SetUserCustomerStatusReq req) {
        imUserStatusService.setUserCustomerStatus(req);
        return ResponseVO.successResponse();
    }

    @RequestMapping("/queryFriendOnlineStatus")
    public ResponseVO queryFriendOnlineStatus(@RequestBody @Validated PullFriendOnlineStatusReq req) {
        return ResponseVO.successResponse(imUserStatusService.queryFriendOnlineStatus(req));
    }

    @RequestMapping("/queryUserOnlineStatus")
    public ResponseVO queryUserOnlineStatus(@RequestBody @Validated PullUserOnlineStatusReq req) {
        return ResponseVO.successResponse(imUserStatusService.queryUserOnlineStatus(req));
    }
}
