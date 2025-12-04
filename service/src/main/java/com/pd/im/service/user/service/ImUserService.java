package com.pd.im.service.user.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.user.model.req.LoginReq;

/**
 * @author Parker
 * @date 12/3/25
 * @description ImUserService接口
 */
public interface ImUserService {
    public ResponseVO login(LoginReq req);
}
