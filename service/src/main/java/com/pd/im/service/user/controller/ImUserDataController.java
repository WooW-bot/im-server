package com.pd.im.service.user.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.user.model.req.GetUserInfoReq;
import com.pd.im.service.user.model.req.ModifyUserInfoReq;
import com.pd.im.service.user.model.req.UserId;
import com.pd.im.service.user.service.ImUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Parker
 * @date 12/8/25
 */
@Slf4j
@RestController
@RequestMapping("v1/user/data")
public class ImUserDataController {
    @Autowired
    ImUserService imUserService;

    /**
     * 拉取用户资料
     * 参考: https://cloud.tencent.com/document/product/269/1639
     *
     * @param req   GetUserInfoReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/getUserInfo")
    public ResponseVO getUserInfo(@RequestBody GetUserInfoReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.getUserInfo(req);
    }

    /**
     * 拉取单个用户资料
     * 参考: https://cloud.tencent.com/document/product/269/1639
     *
     * @param req   UserId
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/getSingleUserInfo")
    public ResponseVO getSingleUserInfo(@RequestBody @Validated UserId req, Integer appId) {
        req.setAppId(appId);
        return imUserService.getSingleUserInfo(req.getUserId(), req.getAppId());
    }

    /**
     * 设置用户资料
     * 参考: https://cloud.tencent.com/document/product/269/1640
     *
     * @param req   ModifyUserInfoReq
     * @param appId Integer
     * @return ResponseVO
     */
    @RequestMapping("/modifyUserInfo")
    public ResponseVO modifyUserInfo(@RequestBody @Validated ModifyUserInfoReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.modifyUserInfo(req);
    }
}
