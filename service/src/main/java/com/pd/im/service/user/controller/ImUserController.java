package com.pd.im.service.user.controller;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.user.model.req.LoginReq;
import com.pd.im.service.user.service.ImUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * @param req
     * @return im的登录接口，返回im地址
     */
    @RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req) {
        ResponseVO login = imUserService.login(req);
        if (login.isOk()) {

        }
        return login;
    }

}
