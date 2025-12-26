package com.pd.im.service.app.service;

import com.pd.im.service.app.dao.ImAppEntity;

/**
 * IM应用服务接口
 *
 * @author Parker
 * @date 12/24/25
 */
public interface ImAppService {


    /**
     * 根据应用ID获取应用信息
     *
     * @param appId 应用ID
     * @return 应用实体
     */
    ImAppEntity getAppById(Integer appId);

    /**
     * 根据应用ID获取私钥
     *
     * @param appId 应用ID
     * @return 私钥，如果应用不存在或已停用则返回null
     */
    String getPrivateKey(Integer appId);


    /**
     * 验证应用是否有效（存在且状态正常）
     *
     * @param appId 应用ID
     * @return true-有效 false-无效
     */
    boolean isAppValid(Integer appId);
}
