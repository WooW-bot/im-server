package com.pd.im.service.callback;

import com.pd.im.common.ResponseVO;

/**
 * 回调机制接口定义
 * 参考: https://cloud.tencent.com/document/product/269/1522
 *
 * @author Parker
 * @date 12/5/25
 */
public interface CallbackService {
    /**
     * 在事件执行之前的回调
     * 干预事件的后续流程处理，以及对用户行为埋点，记录日志
     * 需要返回值(用户有感，异步)
     * 参考: https://cloud.tencent.com/document/product/269/1632
     *
     * @param appId           Integer
     * @param callbackCommand String
     * @param jsonBody        String
     * @return ResponseVO
     */
    ResponseVO beforeCallback(Integer appId, String callbackCommand, String jsonBody);

    /**
     * 在事件执行之后的回调
     * 进行数据同步
     * 不需要返回值(用户无感)
     * 参考: https://cloud.tencent.com/document/product/269/2716
     *
     * @param appId           Integer
     * @param callbackCommand String
     * @param jsonBody        String
     */
    void afterCallback(Integer appId, String callbackCommand, String jsonBody);
}
