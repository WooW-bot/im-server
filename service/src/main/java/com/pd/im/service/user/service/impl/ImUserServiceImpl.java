package com.pd.im.service.user.service.impl;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.user.model.req.LoginReq;
import com.pd.im.service.user.service.ImUserService;
import org.springframework.stereotype.Service;

/**
 * @author Parker
 * @date 12/3/25
 * @description ImUserviceImpl类
 */
@Service
public class ImUserServiceImpl implements ImUserService {
    @Override
    public ResponseVO login(LoginReq req) {
        // TODO 后期补充鉴权
        return ResponseVO.successResponse();
    }
}
