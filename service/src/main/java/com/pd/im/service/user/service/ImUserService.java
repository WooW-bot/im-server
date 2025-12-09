package com.pd.im.service.user.service;

import com.pd.im.common.ResponseVO;
import com.pd.im.service.user.dao.ImUserDataEntity;
import com.pd.im.service.user.model.req.*;
import com.pd.im.service.user.model.resp.GetUserInfoResp;

/**
 * @author Parker
 * @date 12/3/25
 * @description ImUserService接口
 */
public interface ImUserService {
    /**
     * 批量导入用户信息
     *
     * @param req
     * @return
     */
    ResponseVO importUser(ImportUserReq req);

    /**
     * 获取所有正常用户信息
     *
     * @param req
     * @return
     */
    ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req);

    /**
     * 删除正常用户
     *
     * @param req
     * @return
     */
    ResponseVO deleteUser(DeleteUserReq req);

    /**
     * 修改正常用户信息
     * @param req
     * @return
     */
    ResponseVO modifyUserInfo(ModifyUserInfoReq req);

    /**
     * 获取单个正常用户信息
     *
     * @param userId
     * @param appId
     * @return
     */
    ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId, Integer appId);

    /**
     * 用户登录功能
     *
     * @param req
     * @return
     */
    ResponseVO login(LoginReq req);

    /**
     * 客户端向服务端请求该用户各接口需要拉取的数量
     *
     * @param req
     * @return
     */
    ResponseVO getUserSequence(GetUserSequenceReq req);

}
